/*
 * Copyright 2012 International Business Machines Corp.
 * 
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.jbatch.container.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.runtime.BatchStatus;

import com.ibm.jbatch.container.IController;
import com.ibm.jbatch.container.IThreadRootController;
import com.ibm.jbatch.container.artifact.proxy.InjectionReferences;
import com.ibm.jbatch.container.artifact.proxy.JobListenerProxy;
import com.ibm.jbatch.container.artifact.proxy.ListenerFactory;
import com.ibm.jbatch.container.context.impl.JobContextImpl;
import com.ibm.jbatch.container.jobinstance.RuntimeJobExecution;
import com.ibm.jbatch.container.jsl.ModelNavigator;
import com.ibm.jbatch.container.services.IJobStatusManagerService;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.servicesmanager.ServicesManagerImpl;
import com.ibm.jbatch.container.status.JobOrFlowBatchStatus;
import com.ibm.jbatch.container.status.JobOrFlowStatus;
import com.ibm.jbatch.container.util.PartitionDataWrapper;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.jbatch.jsl.model.JSLProperties;
import com.ibm.jbatch.jsl.model.Property;

public abstract class JobThreadRootControllerImpl implements IThreadRootController {

	private final static String CLASSNAME = JobThreadRootControllerImpl.class.getName();
	private final static Logger logger = Logger.getLogger(CLASSNAME);

	protected RuntimeJobExecution jobExecution;
	protected JobContextImpl jobContext;
	protected long rootJobExecutionId;
	protected long jobInstanceId;
	protected IJobStatusManagerService jobStatusService;
	protected IPersistenceManagerService persistenceService;
	private ListenerFactory listenerFactory = null;

	private ExecutionTransitioner transitioner; 
	protected final ModelNavigator<JSLJob> jobNavigator;
	private BlockingQueue<PartitionDataWrapper> analyzerQueue;

	public JobThreadRootControllerImpl(RuntimeJobExecution jobExecution, long rootJobExecutionId) {
		this.jobExecution = jobExecution;
		this.jobContext = jobExecution.getJobContext();
		this.rootJobExecutionId = rootJobExecutionId;
		this.jobInstanceId = jobExecution.getInstanceId();
		this.jobStatusService = ServicesManagerImpl.getInstance().getJobStatusManagerService();
		this.persistenceService = ServicesManagerImpl.getInstance().getPersistenceManagerService();
		this.jobNavigator = jobExecution.getJobNavigator();
		setupListeners();
	}

	public JobThreadRootControllerImpl(RuntimeJobExecution jobExecution, long rootJobExecutionId, BlockingQueue<PartitionDataWrapper> analyzerQueue) {
		this(jobExecution, rootJobExecutionId);
		this.analyzerQueue = analyzerQueue;
	}
	
	/*
	 * By not passing the rootJobExecutionId, we are "orphaning" the subjob execution and making it not findable from the parent.  
	 * This is exactly what we want for getStepExecutions()... we don't want it to get extraneous entries for the partitions.   
	 */
	public JobThreadRootControllerImpl(RuntimeJobExecution jobExecution, BlockingQueue<PartitionDataWrapper> analyzerQueue) {
		this(jobExecution, jobExecution.getExecutionId());
		this.analyzerQueue = analyzerQueue;
	}

	@Override
	public JobOrFlowStatus originateExecutionOnThread() {
		String methodName = "executeJob";
		logger.entering(CLASSNAME, methodName);

		JobOrFlowStatus retVal = null;
		try {
			// Check if we've already gotten the stop() command.
			if (!jobContext.getBatchStatus().equals(BatchStatus.STOPPING)) { 

				// Now that we're ready to start invoking artifacts, set the status to 'STARTED'
				markJobStarted();

				jobListenersBeforeJob();

				// --------------------
				// The BIG loop transitioning 
				// within the job !!!
				// --------------------
				transitioner = new ExecutionTransitioner(jobExecution, rootJobExecutionId, jobNavigator, analyzerQueue);
				retVal = transitioner.doExecutionLoop();
				JobOrFlowBatchStatus flowBatchStatus = retVal.getBatchStatus();
				switch (flowBatchStatus)  {
					case JSL_STOP : 		jslStop();
											break;
					case JSL_FAIL : 		updateJobBatchStatus(BatchStatus.FAILED);
											break;
					case EXCEPTION_THROWN : updateJobBatchStatus(BatchStatus.FAILED);
											break;
				}
			}
		} catch (Throwable t) {
			// We still want to try to call the afterJob() listener and persist the batch and exit
			// status for the failure in an orderly fashion.  So catch and continue.
			logWarning("Caught throwable in main execution loop", t);
			batchStatusFailedFromException();
		}

		endOfJob();

		logger.exiting(CLASSNAME, methodName);
		return retVal;
	}

	protected void setContextProperties() {
		JSLJob jobModel = jobExecution.getJobNavigator().getRootModelElement();
		JSLProperties jslProps = jobModel.getProperties();

		if (jslProps != null) {
			Properties contextProps = jobContext.getProperties();
			for (Property property : jslProps.getPropertyList()) {
				contextProps.setProperty(property.getName(), property.getValue());
			}	
		}
	}

	protected void jslStop() {
		String restartOn = jobContext.getRestartOn();	
		logger.fine("Logging JSL stop(): exitStatus = " + jobContext.getExitStatus() + ", restartOn = " +restartOn );
		batchStatusStopping();
		jobStatusService.updateJobStatusFromJSLStop(jobInstanceId, restartOn);
		return;
	}

	protected void markJobStarted() {
		updateJobBatchStatus(BatchStatus.STARTED);
		long time = System.currentTimeMillis();
		Timestamp timestamp = new Timestamp(time);
		jobExecution.setLastUpdateTime(timestamp);
		jobExecution.setStartTime(timestamp);
		persistenceService.markJobStarted(jobExecution.getExecutionId(), timestamp);
	}

	/*
	 *  Follow similar pattern for end of step in BaseStepControllerImpl
	 *  
	 *  1. Execute the very last artifacts (jobListener)
	 *  2. transition to final batch status
	 *  3. default ExitStatus if necessary
	 *  4. persist statuses and end time data
	 *  
	 *  We don't want to give up on the orderly process of 2,3,4, if we blow up 
	 *  in after job, so catch that and keep on going.
	 */
	protected void endOfJob() {


		// 1. Execute the very last artifacts (jobListener)
		try {
			jobListenersAfterJob();
		} catch (Throwable t) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			t.printStackTrace(pw);
			logger.warning("Error invoking jobListener.afterJob(). Stack trace: " + sw.toString());
			batchStatusFailedFromException();
		}

		// 2. transition to final batch status
		transitionToFinalBatchStatus();

		// 3. default ExitStatus if necessary
		if (jobContext.getExitStatus() == null) {
			logger.fine("No job-level exitStatus set, defaulting to job batch Status = " + jobContext.getBatchStatus());
			jobContext.setExitStatus(jobContext.getBatchStatus().name());
		}

		// 4. persist statuses and end time data
		logger.fine("Job complete for job id=" + jobExecution.getJobInstance().getJobName() + ", executionId=" + jobExecution.getExecutionId() 
				+ ", batchStatus=" + jobContext.getBatchStatus() + ", exitStatus=" + jobContext.getExitStatus());
		persistJobBatchAndExitStatus();

	}

	private void persistJobBatchAndExitStatus() {
		BatchStatus batchStatus = jobContext.getBatchStatus();

		// Take a current timestamp for last updated no matter what the status.
		long time = System.currentTimeMillis();
		Timestamp timestamp = new Timestamp(time);
		jobExecution.setLastUpdateTime(timestamp);

		// Perhaps these should be coordinated in a tran but probably better still would be
		// rethinking the table design to let the database provide us consistently with a single update.
		jobStatusService.updateJobBatchStatus(jobInstanceId, batchStatus);
		jobStatusService.updateJobExecutionStatus(jobExecution.getInstanceId(), jobContext.getBatchStatus(), jobContext.getExitStatus());

		if (batchStatus.equals(BatchStatus.COMPLETED) || batchStatus.equals(BatchStatus.STOPPED) ||  
				batchStatus.equals(BatchStatus.FAILED)) {

			jobExecution.setEndTime(timestamp);
			persistenceService.updateWithFinalExecutionStatusesAndTimestamps(jobExecution.getExecutionId(), 
					batchStatus, jobContext.getExitStatus(), timestamp);
		} else {
			throw new IllegalStateException("Not expected to encounter batchStatus of " + batchStatus +" at this point.  Aborting.");
		}
	}

	/**
	 * The only valid states at this point are STARTED or STOPPING.   Shouldn't have
	 * been able to get to COMPLETED, STOPPED, or FAILED at this point in the code.
	 */

	private void transitionToFinalBatchStatus() {
		BatchStatus currentBatchStatus = jobContext.getBatchStatus();
		if (currentBatchStatus.equals(BatchStatus.STARTED)) {
			updateJobBatchStatus(BatchStatus.COMPLETED);
		} else if (currentBatchStatus.equals(BatchStatus.STOPPING)) {
			updateJobBatchStatus(BatchStatus.STOPPED);
		} else if (currentBatchStatus.equals(BatchStatus.FAILED)) {
			updateJobBatchStatus(BatchStatus.FAILED);  // Should have already been done but maybe better for possible code refactoring to have it here.
		} else {
			throw new IllegalStateException("Step batch status should not be in a " + currentBatchStatus.name() + " state");
		}
	}

	protected void updateJobBatchStatus(BatchStatus batchStatus) {
		logger.fine("Setting job batch status to: " + batchStatus);
		jobContext.setBatchStatus(batchStatus);
	}


	protected void logWarning(String msg, Throwable t) {
		StringWriter sw = new StringWriter();
		t.printStackTrace(new PrintWriter(sw));
		logger.warning(msg + " with Throwable message: " + t.getMessage() + ", and stack trace: " + sw.toString());
	}

	/*
	 * The thought here is that while we don't persist all the transitions in batch status (given
	 * we plan to persist at the very end), we do persist STOPPING right away, since if we end up
	 * "stuck in STOPPING" we at least will have a record in the database.
	 */
	protected void batchStatusStopping() {
		updateJobBatchStatus(BatchStatus.STOPPING);
		long time = System.currentTimeMillis();
		Timestamp timestamp = new Timestamp(time);
		jobExecution.setLastUpdateTime(timestamp);
		persistenceService.updateBatchStatusOnly(jobExecution.getExecutionId(), BatchStatus.STOPPING, timestamp);
	}



	private void setupListeners() {
		JSLJob jobModel = jobExecution.getJobNavigator().getRootModelElement();   
		InjectionReferences injectionRef = new InjectionReferences(jobContext, null, null);
		listenerFactory = new ListenerFactory(jobModel, injectionRef);
		jobExecution.setListenerFactory(listenerFactory);
	}


	@Override
	public void stop() {
		if (jobContext.getBatchStatus().equals(BatchStatus.STARTING) || jobContext.getBatchStatus().equals(BatchStatus.STARTED)) {

			batchStatusStopping();

			IController stoppableElementController = transitioner.getCurrentStoppableElementController();
			if (stoppableElementController != null) {
				stoppableElementController.stop();
			}
		} else {
			logger.info("Stop ignored since batch status for job is already set to: " + jobContext.getBatchStatus());
		}
	}

	// Call beforeJob() on all the job listeners
	protected void jobListenersBeforeJob() {
		List<JobListenerProxy> jobListeners = listenerFactory.getJobListeners();
		for (JobListenerProxy listenerProxy : jobListeners) {
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Invoking beforeJob() on jobListener: " + listenerProxy.getDelegate() + " of type: " + listenerProxy.getDelegate().getClass());
			}
			listenerProxy.beforeJob();
		}
	}

	// Call afterJob() on all the job listeners
	private void jobListenersAfterJob() {
		List<JobListenerProxy> jobListeners = listenerFactory.getJobListeners();
		for (JobListenerProxy listenerProxy : jobListeners) {
			if (logger.isLoggable(Level.FINE)) {
				logger.fine(" Invoking afterJob() on jobListener: " + listenerProxy.getDelegate() + " of type: " + listenerProxy.getDelegate().getClass());
			}
			listenerProxy.afterJob();
		}	
	}

	protected void batchStatusFailedFromException() {
		updateJobBatchStatus(BatchStatus.FAILED);
	}
	
    @Override
    public List<Long> getLastRunStepExecutions() {
        
        return this.transitioner.getStepExecIds();
        
    }
}
