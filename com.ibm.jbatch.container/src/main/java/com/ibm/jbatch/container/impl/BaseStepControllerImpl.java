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

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.batch.operations.JobExecutionNotMostRecentException;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.JobStartException;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobInstance;

import com.ibm.jbatch.container.IExecutionElementController;
import com.ibm.jbatch.container.context.impl.MetricImpl;
import com.ibm.jbatch.container.context.impl.StepContextImpl;
import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.jobinstance.RuntimeJobExecution;
import com.ibm.jbatch.container.jobinstance.StepExecutionImpl;
import com.ibm.jbatch.container.persistence.PersistentDataWrapper;
import com.ibm.jbatch.container.services.IBatchKernelService;
import com.ibm.jbatch.container.services.IJobStatusManagerService;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.servicesmanager.ServicesManagerImpl;
import com.ibm.jbatch.container.status.ExecutionStatus;
import com.ibm.jbatch.container.status.ExtendedBatchStatus;
import com.ibm.jbatch.container.status.StepStatus;
import com.ibm.jbatch.container.util.PartitionDataWrapper;
import com.ibm.jbatch.jsl.model.JSLProperties;
import com.ibm.jbatch.jsl.model.Property;
import com.ibm.jbatch.jsl.model.Step;
import com.ibm.jbatch.spi.services.ITransactionManagementService;
import com.ibm.jbatch.spi.services.TransactionManagerAdapter;

/** Change the name of this class to something else!! Or change BaseStepControllerImpl. */
public abstract class BaseStepControllerImpl implements IExecutionElementController {

	private final static String sourceClass = BatchletStepControllerImpl.class.getName();
	private final static Logger logger = Logger.getLogger(sourceClass);

	protected RuntimeJobExecution jobExecutionImpl;
	protected JobInstance jobInstance;

	protected StepContextImpl stepContext;
	protected Step step;
	protected String stepName;
	protected StepStatus stepStatus;

	protected BlockingQueue<PartitionDataWrapper> analyzerStatusQueue = null;

	protected long rootJobExecutionId;
	
	// Restart of partitioned steps needs to be handled specially 
	protected boolean restartAfterCompletion = false;

	protected static IBatchKernelService batchKernel = ServicesManagerImpl.getInstance().getBatchKernelService();

	protected TransactionManagerAdapter	transactionManager = null;

	protected static IPersistenceManagerService _persistenceManagementService = ServicesManagerImpl.getInstance().getPersistenceManagerService();

	private static IJobStatusManagerService _jobStatusService = (IJobStatusManagerService) ServicesManagerImpl.getInstance().getJobStatusManagerService();

	protected BaseStepControllerImpl(RuntimeJobExecution jobExecution, Step step, StepContextImpl stepContext, long rootJobExecutionId) {
		this.jobExecutionImpl = jobExecution;
		this.jobInstance = jobExecution.getJobInstance();
		this.stepContext = stepContext;
		this.rootJobExecutionId = rootJobExecutionId;
		if (step == null) {
			throw new IllegalArgumentException("Step parameter to ctor cannot be null.");
		}
		this.step = step;
		this.stepName = step.getId();
	}
	
	protected BaseStepControllerImpl(RuntimeJobExecution jobExecution, Step step, StepContextImpl stepContext,  long rootJobExecutionId, BlockingQueue<PartitionDataWrapper> analyzerStatusQueue) {
		this(jobExecution, step, stepContext, rootJobExecutionId);
		this.analyzerStatusQueue = analyzerStatusQueue;
	}

	///////////////////////////
	// ABSTRACT METHODS ARE HERE
	///////////////////////////
	protected abstract void invokeCoreStep() throws JobRestartException, JobStartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException;

	protected abstract void setupStepArtifacts();

	protected abstract void invokePreStepArtifacts();

	protected abstract void invokePostStepArtifacts();

	// This is only useful from the partition threads
	protected abstract void sendStatusFromPartitionToAnalyzerIfPresent();

	@Override
	public ExecutionStatus execute() {

		// Here we're just setting up to decide if we're going to run the step or not (if it's already complete and 
		// allow-start-if-complete=false.
		try {
			boolean executeStep = shouldStepBeExecuted();
			if (!executeStep) {
				logger.fine("Not going to run this step.  Returning previous exit status of: " + stepStatus.getExitStatus());
				return new ExecutionStatus(ExtendedBatchStatus.DO_NOT_RUN, stepStatus.getExitStatus());
			} 
		} catch (Throwable t) {
			// Treat an error at this point as unrecoverable, so fail job too.
			markJobAndStepFailed();
			rethrowWithWarning("Caught throwable while determining if step should be executed.  Failing job.", t);
		}

		// At this point we have a StepExecution.  Setup so that we're ready to invoke artifacts.
		try {
			startStep();
		} catch (Throwable t) {
			// Treat an error at this point as unrecoverable, so fail job too.
			markJobAndStepFailed();
			rethrowWithWarning("Caught throwable while starting step.  Failing job.", t);
		}

		// At this point artifacts are in the picture so we want to try to invoke afterStep() on a failure.
		try {
			invokePreStepArtifacts();    //Call PartitionReducer and StepListener(s)
			invokeCoreStep();
		} catch (Exception e) {
			// We're going to continue on so that we can execute the afterStep() and analyzer
			try {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				logger.warning("Caught exception executing step: " + sw.toString());
				markStepFailed();
			} catch(Throwable t) {
				// Since the first one is the original first failure, let's rethrow t1 and not the second error,
				// but we'll log a severe error pointing out that the failure didn't get persisted..
				// We won't try to call the afterStep() in this case either.
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				t.printStackTrace(pw);
				rethrowWithSevere("ERROR. PERSISTING BATCH STATUS FAILED.  STEP EXECUTION STATUS TABLES MIGHT HAVE CONSISTENCY ISSUES" +
						"AND/OR UNEXPECTED ENTRIES.", t);
			}
		} catch (Throwable t) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			t.printStackTrace(pw);
			logger.warning("Failing both step AND job after catching error executing step: " + sw.toString());
			markJobAndStepFailed();
		}

		//
		// At this point we may have already failed the step, but we still try to invoke the end of step artifacts.
		//
		try {
			//Call PartitionAnalyzer, PartitionReducer and StepListener(s)
			invokePostStepArtifacts();   
		} catch (Throwable t) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			t.printStackTrace(pw);
			logger.warning("Error invoking end of step artifacts. Stack trace: " + sw.toString());
			markStepFailed();
		}

		//
		// No more application code is on the path from here on out (excluding the call to the PartitionAnalyzer
		// analyzeStatus().  If an exception bubbles up and leaves the statuses inconsistent or incorrect then so be it; 
		// maybe there's a runtime bug that will need to be fixed.
		// 
		try {
			// Now that all step-level artifacts have had a chance to run, 
			// we set the exit status to one of the defaults if it is still unset.

			// This is going to be the very last sequence of calls from the step running on the main thread,
			// since the call back to the partition analyzer only happens on the partition threads.
			// On the partition threads, then, we harden the status at the partition level before we
			// send it back to the main thread.
			persistUserData();
			transitionToFinalBatchStatus();
			defaultExitStatusIfNecessary();
			persistExitStatusEndTimestampAndStepExecution();
		} catch (Throwable t) {
			// Don't let an exception caught here prevent us from persisting the failed batch status.
			markJobAndStepFailed();
			rethrowWithWarning("Failure ending step execution", t);
		} 

		//
		// Only happens on main thread.
		//
		sendStatusFromPartitionToAnalyzerIfPresent();

		logger.finer("Returning step batchStatus: " + stepStatus.getBatchStatus() + 
				", exitStatus: " + stepStatus.getExitStatus()); 

		if (stepStatus.getBatchStatus().equals(BatchStatus.FAILED)) {
			return new ExecutionStatus(ExtendedBatchStatus.EXCEPTION_THROWN, stepStatus.getExitStatus());
		} else {
			return new ExecutionStatus(ExtendedBatchStatus.NORMAL_COMPLETION, stepStatus.getExitStatus());
		}
	}

	private void defaultExitStatusIfNecessary() {
		String stepExitStatus = stepContext.getExitStatus();
		String processRetVal = stepContext.getBatchletProcessRetVal(); 
		if (stepExitStatus != null) {
			logger.fine("Returning with user-set exit status: " + stepExitStatus);
		} else if (processRetVal != null) {
			logger.fine("Returning with exit status from batchlet.process(): " + processRetVal);
			stepContext.setExitStatus(processRetVal);
		} else {
			logger.fine("Returning with default exit status");
			stepContext.setExitStatus(stepContext.getBatchStatus().name());
		}
	}	
	
	private void markStepFailed() {
		updateBatchStatus(BatchStatus.FAILED);
	}
	
	protected void markJobAndStepFailed() {
		jobExecutionImpl.getJobContext().setBatchStatus(BatchStatus.FAILED);
		markStepFailed();
	}
	
	private void startStep() {
		// Update status
		statusStarting();
		//Set Step context properties
		setContextProperties();
		//Set up step artifacts like step listeners, partition reducers
		setupStepArtifacts();
		// Move batch status to started.
		updateBatchStatus(BatchStatus.STARTED);
		
		long time = System.currentTimeMillis();
		Timestamp startTS = new Timestamp(time);
		stepContext.setStartTime(startTS);
		
		_persistenceManagementService.updateStepExecution(stepContext);
	}
	

	/**
	 * The only valid states at this point are STARTED,STOPPING, or FAILED.
	 * been able to get to STOPPED, or COMPLETED yet at this point in the code.
	 */
	private void transitionToFinalBatchStatus() {
		BatchStatus currentBatchStatus = stepContext.getBatchStatus();
		if (currentBatchStatus.equals(BatchStatus.STARTED)) {
			updateBatchStatus(BatchStatus.COMPLETED);
		} else if (currentBatchStatus.equals(BatchStatus.STOPPING)) {
			updateBatchStatus(BatchStatus.STOPPED);
		} else if (currentBatchStatus.equals(BatchStatus.FAILED)) {
			updateBatchStatus(BatchStatus.FAILED);           // Should have already been done but maybe better for possible code refactoring to have it here.
		} else {
			throw new IllegalStateException("Step batch status should not be in a " + currentBatchStatus.name() + " state");
		}
	}

	protected void updateBatchStatus(BatchStatus updatedBatchStatus) {
		logger.fine("Updating batch status from : " + stepStatus.getBatchStatus() + ", to: " + updatedBatchStatus);
		stepStatus.setBatchStatus(updatedBatchStatus);
		_jobStatusService.updateStepStatus(stepStatus.getStepExecutionId(), stepStatus);
		stepContext.setBatchStatus(updatedBatchStatus);
	}

	protected boolean shouldStepBeExecuted() {

		if (logger.isLoggable(Level.FINER)) {
			logger.finer("In shouldStepBeExecuted() with stepContext =  " + this.stepContext);
		}

		this.stepStatus = _jobStatusService.getStepStatus(jobInstance.getInstanceId(), step.getId());
		if (stepStatus == null) {
			logger.finer("No existing step status found.  Create new step execution and proceed to execution.");
			// create new step execution
			StepExecutionImpl stepExecution = getNewStepExecution(rootJobExecutionId, stepContext);
			// create new step status for this run
			stepStatus = _jobStatusService.createStepStatus(stepExecution.getStepExecutionId());
			((StepContextImpl) stepContext).setStepExecutionId(stepExecution.getStepExecutionId());
			return true;
		} else {
			logger.finer("Existing step status found.");
			// if a step status already exists for this instance id. It means this
			// is a restart and we need to get the previously persisted data
			((StepContextImpl) stepContext).setPersistentUserData(stepStatus.getPersistentUserData());
			if (shouldStepBeExecutedOnRestart()) {
				// Seems better to let the start count get incremented without getting a step execution than
				// vice versa (in an unexpected error case).
				stepStatus.incrementStartCount();
				// create new step execution
				StepExecutionImpl stepExecution = getNewStepExecution(rootJobExecutionId, stepContext);
				this.stepStatus.setLastRunStepExecutionId(stepExecution.getStepExecutionId());
				((StepContextImpl) stepContext).setStepExecutionId(stepExecution.getStepExecutionId());
				return true;
			} else {
				return false;
			}
		}
	}

	private boolean shouldStepBeExecutedOnRestart() {
		
		BatchStatus stepBatchStatus = stepStatus.getBatchStatus();
		if (stepBatchStatus.equals(BatchStatus.COMPLETED)) {
			// A bit of parsing involved since the model gives us a String not a
			// boolean, but it should default to 'false', which is the spec'd default.
			if (!Boolean.parseBoolean(step.getAllowStartIfComplete())) {
				logger.fine("Step: " + step.getId() + " already has batch status of COMPLETED, so won't be run again since it does not allow start if complete.");
				return false;
			} else {
				logger.fine("Step: " + step.getId() + " already has batch status of COMPLETED, and allow-start-if-complete is set to 'true'");
				restartAfterCompletion = true;
			}
		}

		// The spec default is '0', which we get by initializing to '0' in the next line
		int startLimit = 0;
		String startLimitString = step.getStartLimit();
		if (startLimitString != null) {
			try {
				startLimit = Integer.parseInt(startLimitString);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Could not parse start limit value.  Received NumberFormatException for start-limit value:  " + startLimitString 
						+ " for stepId: " + step.getId() + ", with start-limit=" + step.getStartLimit());
			}
		}

		if (startLimit < 0) {
			throw new IllegalArgumentException("Found negative start-limit of " + startLimit + "for stepId: " + step.getId());
		}

		if (startLimit > 0) {
			int newStepStartCount = stepStatus.getStartCount() + 1;
			if (newStepStartCount > startLimit) {
				throw new IllegalStateException("For stepId: " + step.getId() + ", tried to start step for the " + newStepStartCount
						+ " time, but startLimit = " + startLimit);
			} else {
				logger.fine("Starting (possibly restarting) step: " + step.getId() + ", since newStepStartCount = " + newStepStartCount
						+ " and startLimit=" + startLimit);
			}
		}
		return true;
	}

	protected boolean isRestartExecution() {
		return stepStatus.getStartCount() > 1;
	}
	
	protected void statusStarting() {
		stepStatus.setBatchStatus(BatchStatus.STARTING);
		_jobStatusService.updateJobCurrentStep(jobInstance.getInstanceId(), step.getId());
		_jobStatusService.updateStepStatus(stepStatus.getStepExecutionId(), stepStatus);
		stepContext.setBatchStatus(BatchStatus.STARTING);
	}

	protected void persistUserData() {
		ByteArrayOutputStream persistentBAOS = new ByteArrayOutputStream();
		ObjectOutputStream persistentDataOOS = null;

		try {
			persistentDataOOS = new ObjectOutputStream(persistentBAOS);
			persistentDataOOS.writeObject(stepContext.getPersistentUserData());
			persistentDataOOS.close();
		} catch (Exception e) {
			throw new BatchContainerServiceException("Cannot persist the persistent user data for the step.", e);
		}

		stepStatus.setPersistentUserData(new PersistentDataWrapper(persistentBAOS.toByteArray()));
		_jobStatusService.updateStepStatus(stepStatus.getStepExecutionId(), stepStatus);
	}

	protected void persistExitStatusEndTimestampAndStepExecution() {
		stepStatus.setExitStatus(stepContext.getExitStatus());
		_jobStatusService.updateStepStatus(stepStatus.getStepExecutionId(), stepStatus);

		// set the end time metric before flushing
		long time = System.currentTimeMillis();
		Timestamp endTS = new Timestamp(time);
		stepContext.setEndTime(endTS);

		persistStepExecution();
	} 

	protected void persistStepExecution() {
		_persistenceManagementService.updateStepExecution(stepContext);
	}

	private StepExecutionImpl getNewStepExecution(long rootJobExecutionId, StepContextImpl stepContext) {
		return _persistenceManagementService.createStepExecution(rootJobExecutionId, stepContext);
	}

	private void setContextProperties() {
		JSLProperties jslProps = step.getProperties();

		if (jslProps != null) {
			for (Property property : jslProps.getPropertyList()) {
				Properties contextProps = stepContext.getJSLProperties();
				contextProps.setProperty(property.getName(), property.getValue());
			}	
		}

		// set up metrics
		stepContext.addMetric(MetricImpl.MetricType.READ_COUNT, 0);
		stepContext.addMetric(MetricImpl.MetricType.WRITE_COUNT, 0);
		stepContext.addMetric(MetricImpl.MetricType.READ_SKIP_COUNT, 0);
		stepContext.addMetric(MetricImpl.MetricType.PROCESS_SKIP_COUNT, 0);
		stepContext.addMetric(MetricImpl.MetricType.WRITE_SKIP_COUNT, 0);
		stepContext.addMetric(MetricImpl.MetricType.FILTER_COUNT, 0);
		stepContext.addMetric(MetricImpl.MetricType.COMMIT_COUNT, 0);
		stepContext.addMetric(MetricImpl.MetricType.ROLLBACK_COUNT, 0);

		ITransactionManagementService transMgr = ServicesManagerImpl.getInstance().getTransactionManagementService();
		transactionManager = transMgr.getTransactionManager(stepContext);
	}

	public void setStepContext(StepContextImpl stepContext) {
		this.stepContext = stepContext;
	}

	protected BlockingQueue<PartitionDataWrapper> getAnalyzerQueue() {
		return analyzerStatusQueue;
	}

	public void setAnalyzerQueue(BlockingQueue<PartitionDataWrapper> analyzerQueue) {
		this.analyzerStatusQueue = analyzerQueue;
	}
	
    @Override
    public List<Long> getLastRunStepExecutions() {
        
        List<Long> stepExecIdList = new ArrayList<Long>(1);
        stepExecIdList.add(this.stepStatus.getLastRunStepExecutionId());
        
        return stepExecIdList;
    }

	private void rethrowWithMsg(String msgBeginning, Throwable t, Level level) {
		String errorMsg = msgBeginning + " ; Caught exception/error: " + t.getLocalizedMessage();
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		logger.log(level, errorMsg + " : Stack trace: " + sw.toString());
		throw new BatchContainerRuntimeException(errorMsg, t);
	}
	
	private void rethrowWithWarning(String msgBeginning, Throwable t) {
		rethrowWithMsg(msgBeginning, t, Level.WARNING);
	}
	
	private void rethrowWithSevere(String msgBeginning, Throwable t) {
		rethrowWithMsg(msgBeginning, t, Level.SEVERE);
	}

	public String toString() {
		return "BaseStepControllerImpl for step = " + step.getId();
	}
}
