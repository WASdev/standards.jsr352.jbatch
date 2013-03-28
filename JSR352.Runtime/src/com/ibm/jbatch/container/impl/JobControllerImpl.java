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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.StepExecution;

import com.ibm.jbatch.container.AbortedBeforeStartException;
import com.ibm.jbatch.container.IController;
import com.ibm.jbatch.container.IExecutionElementController;
import com.ibm.jbatch.container.artifact.proxy.InjectionReferences;
import com.ibm.jbatch.container.artifact.proxy.JobListenerProxy;
import com.ibm.jbatch.container.artifact.proxy.ListenerFactory;
import com.ibm.jbatch.container.context.impl.JobContextImpl;
import com.ibm.jbatch.container.context.impl.StepContextImpl;
import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.jobinstance.RuntimeJobContextJobExecutionBridge;
import com.ibm.jbatch.container.jsl.ExecutionElement;
import com.ibm.jbatch.container.jsl.IllegalTransitionException;
import com.ibm.jbatch.container.jsl.JobNavigator;
import com.ibm.jbatch.container.jsl.Transition;
import com.ibm.jbatch.container.jsl.TransitionElement;
import com.ibm.jbatch.container.services.IJobStatusManagerService;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.servicesmanager.ServicesManagerImpl;
import com.ibm.jbatch.container.status.InternalExecutionElementStatus;
import com.ibm.jbatch.container.util.BatchWorkUnit;
import com.ibm.jbatch.container.util.PartitionDataWrapper;
import com.ibm.jbatch.jsl.model.Decision;
import com.ibm.jbatch.jsl.model.End;
import com.ibm.jbatch.jsl.model.Fail;
import com.ibm.jbatch.jsl.model.Flow;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.jbatch.jsl.model.JSLProperties;
import com.ibm.jbatch.jsl.model.Property;
import com.ibm.jbatch.jsl.model.Split;
import com.ibm.jbatch.jsl.model.Step;
import com.ibm.jbatch.jsl.model.Stop;

public class JobControllerImpl implements IController {

	private final static String CLASSNAME = JobControllerImpl.class.getName();
	private final static Logger logger = Logger.getLogger(CLASSNAME);

	private IJobStatusManagerService jobStatusService = null;
	private IPersistenceManagerService persistenceService = null;

	private RuntimeJobContextJobExecutionBridge jobExecution = null;

	private final JobContextImpl jobContext;
	private final JobNavigator jobNavigator;

	private BlockingQueue<PartitionDataWrapper> analyzerQueue;
	private ListenerFactory listenerFactory = null;
	private final long jobInstanceId;
	private RuntimeJobContextJobExecutionBridge rootJobExecution = null;

	//
	// The currently executing controller, this will only be set to the 
	// local variable reference when we are ready to accept stop events for
	// this execution.
	private volatile IExecutionElementController currentStoppableElementController = null;

	public JobControllerImpl(RuntimeJobContextJobExecutionBridge jobExecution) {
		this (jobExecution, jobExecution);
	}

	public JobControllerImpl(RuntimeJobContextJobExecutionBridge jobExecution, RuntimeJobContextJobExecutionBridge rootJobExecution) {
		this.jobExecution = jobExecution;
		this.jobContext = jobExecution.getJobContext();
		this.rootJobExecution = rootJobExecution;
		jobNavigator = jobExecution.getJobNavigator();
		jobInstanceId = jobExecution.getJobInstance().getInstanceId();
		jobStatusService = ServicesManagerImpl.getInstance().getJobStatusManagerService();
		persistenceService = ServicesManagerImpl.getInstance().getPersistenceManagerService();

		setContextProperties();
		setupListeners();
	}

	public void executeJob() {
		String methodName = "executeJob";
		logger.entering(CLASSNAME, methodName);

		try {
			// Periodic check for stopping job
			if (!jobContext.getBatchStatus().equals(BatchStatus.STOPPING)) { 

				// Now that we're ready to start invoking artifacts, set the status to 'STARTED'
				markJobStarted();

				jobListenersBeforeJob();

				// --------------------
				// The BIG loop transitioning 
				// within the job !!!
				// --------------------
				doExecutionLoop();
			}

		} catch (Throwable t) {
			// We still want to try to call the afterJob() listener and persist the batch and exit
			// status for the failure in an orderly fashion.  So catch and continue.
			logWarning("Caught throwable in main execution loop", t);
			updateJobBatchStatus(BatchStatus.FAILED);
		}

		endOfJob();

		logger.exiting(CLASSNAME, methodName);
		return;
	}

	private void markJobStarted() {
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
	private void endOfJob() {

		// 1. Execute the very last artifacts (jobListener)
		try {
			jobListenersAfterJob();
		} catch (Throwable t) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			t.printStackTrace(pw);
			logger.warning("Error invoking jobListener.afterJob(). Stack trace: " + sw.toString());
			updateJobBatchStatus(BatchStatus.FAILED);
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
	
	private void updateJobBatchStatus(BatchStatus batchStatus) {
		logger.fine("Setting job batch status to: " + batchStatus);
		jobContext.setBatchStatus(batchStatus);
	}
		
	private void doExecutionLoop() throws Exception {

		final String methodName = "doExecutionLoop";
		StepContextImpl stepContext = null;
		ExecutionElement previousExecutionElement = null;
		IExecutionElementController previousElementController = null;
		ExecutionElement currentExecutionElement = null;

		JobContextImpl jobContext = jobExecution.getJobContext();
		try {
			currentExecutionElement = jobNavigator.getFirstExecutionElementInJob(jobExecution.getRestartOn());
		} catch (IllegalTransitionException e) {
			String errorMsg = "Could not transition to first execution element within job.";
			logger.warning(errorMsg);
			throw new IllegalArgumentException(errorMsg, e);
		}

		logger.fine("First execution element = " + currentExecutionElement.getId());

		while (true) {

			if (jobContext.getBatchStatus().equals(BatchStatus.STOPPING)) {
				logger.fine(methodName + " Exiting execution loop as job is now in stopping state.");
				return;

			}
			if (!(currentExecutionElement instanceof Step) && !(currentExecutionElement instanceof Decision) 
					&& !(currentExecutionElement instanceof Flow) && !(currentExecutionElement instanceof Split)) {
				throw new IllegalStateException("Found unknown currentExecutionElement type = " + currentExecutionElement.getClass().getName());
			}

			logger.fine("Next execution element = " + currentExecutionElement.getId());

			IExecutionElementController elementController = 
					ExecutionElementControllerFactory.getExecutionElementController(jobExecution, currentExecutionElement);

			//If this is a sub job it may have a analyzer queue we need to pass along
			elementController.setAnalyzerQueue(this.analyzerQueue);

			// Depending on the execution element new up the associated context
			// and add it to the controller
			if (currentExecutionElement instanceof Decision) {
				if (previousExecutionElement == null) {
					// only job context is available to the decider 
				} else if (previousExecutionElement instanceof Decision) {
					throw new BatchContainerRuntimeException("A decision cannot precede another decision.");
				} else if (previousExecutionElement instanceof Step) {
					// the context from the previous execution element
					StepExecution lastStepExecution = getLastStepExecution((Step) previousExecutionElement);

					((DecisionControllerImpl)elementController).setStepExecution((Step)previousExecutionElement, lastStepExecution);

				} else if (previousExecutionElement instanceof Split) {

					List<StepExecution> stepExecutions = getSplitStepExecutions(previousElementController);
					((DecisionControllerImpl)elementController).setStepExecutions((Split)previousExecutionElement, stepExecutions);

				} else if (previousExecutionElement instanceof Flow) {

					Step last = getLastStepInTheFlow(previousExecutionElement);
					StepExecution lastStepExecution = getLastStepExecution(last);

					((DecisionControllerImpl)elementController).setStepExecution((Flow)previousExecutionElement, lastStepExecution);
				}

			} else if (currentExecutionElement instanceof Step) {
				String stepId = ((Step) currentExecutionElement).getId();
				stepContext = new StepContextImpl(stepId);
				elementController.setStepContext(stepContext);
			} else if (currentExecutionElement instanceof Flow) {
				String flowId = ((Flow) currentExecutionElement).getId();
			} else if (currentExecutionElement instanceof Split) {
				String splitId = ((Split) currentExecutionElement).getId();
			}

			// check for stop before every executing each execution element
			if (jobContext.getBatchStatus().equals(BatchStatus.STOPPING)) {
				logger.fine(methodName + " Exiting execution loop as job is now in stopping state.");
				return;
			}

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Start executing element = " + currentExecutionElement.getId());
			}

			/*
			 * NOTE:
			 * One approach would be to call:  jobStatusService.updateJobCurrentStep()
			 * now.  However for something like a flow the element controller (flow controller) will
			 * have a better view of what the "current step" is, so let's delegate to it instead. 
			 */

			this.currentStoppableElementController = elementController;
			InternalExecutionElementStatus executionElementStatus = null;
			try {
				//////////////////////////////////////////////////////////////////////////
				//  This is it ... the point where we actually execute the next element !
				//////////////////////////////////////////////////////////////////////////
				executionElementStatus = elementController.execute(this.rootJobExecution);

			} catch (AbortedBeforeStartException e) {
				logger.warning("Execution failed, InstanceId: " + this.jobInstanceId + ", executionId = " + this.jobExecution.getExecutionId());
				throw new BatchContainerRuntimeException("Execution failed before even getting to execute execution element = " + 
						currentExecutionElement.getId() + "; breaking out of execution loop.");                
			}

			//
			// We have to be careful here as we are mediating between the batch status of the execution element (e.g. step), which is what
			// has been returned, and the batch status of the job, which is our primary concern in this class.   
			//	
			// Another thing to watch out for is the fact that the job batch and exit status and the step batch and exit status are externals,
			// visible via JobOperator JobExecution/StepExecution.   On the other hand, there is no external for a flow batch/exit status, and
			// none for that of an individual partition.   These are just implementation details.  For that matter, the use of the "JobControllerImpl"
			// to execute flows-within-splits and partitions-within-partitioned steps, via the "subjob" concept... also produces batch and exit statuses
			// which are not external but only implementation details.
			// 

			// Throw an exception on fail 
			if (executionElementStatus.getBatchStatus().equals(BatchStatus.FAILED)) {
				logger.warning("Sub-execution returned its own BatchStatus of FAILED.  Deal with this by throwing exception to the next layer.");
				throw new BatchContainerRuntimeException("Sub-execution returned its own BatchStatus of FAILED.  Deal with this by throwing exception to the next layer.");
			}

			// JSL Stop
			if (executionElementStatus.getBatchStatus().equals(BatchStatus.STOPPED)) {
				String restartOn = executionElementStatus.getRestartOn();
				jobContext.setExitStatus(executionElementStatus.getExitStatus());
				jslStop(restartOn);
				return;
			}

			// set the execution element controller to null so we don't try to
			// call stop on it after the element has finished executing
			this.currentStoppableElementController = null; 
			previousElementController = elementController;

			logger.fine("Done executing element=" + currentExecutionElement.getId() + ", exitStatus=" + executionElementStatus);

			if (jobContext.getBatchStatus().equals(BatchStatus.STOPPING)) {
				logger.fine(methodName + " Exiting as job has been stopped");
				return;
			}

			Transition nextTransition = null;
			try {
				nextTransition = jobNavigator.getNextTransitionInJob(currentExecutionElement, executionElementStatus.getExitStatus());
			} catch (IllegalTransitionException e) {
				String errorMsg = "Problem transitioning to next execution element.";
				logger.warning(errorMsg);
				throw new IllegalArgumentException(errorMsg, e);
			}

			if (nextTransition == null) {
				logger.fine(methodName + "No next execution element, and no transition element found either.  Looks like we're done and ready for COMPLETED state.");
				return;
			}

			if (nextTransition.getNextExecutionElement() != null) {
				// hold on to the previous execution element for the decider
				// we need it because we need to inject the context of the
				// previous execution element into the decider
				previousExecutionElement = currentExecutionElement;
				currentExecutionElement = nextTransition.getNextExecutionElement();
			} else if (nextTransition.getTransitionElement() != null) {
				handleTerminatingTransitionElement(nextTransition.getTransitionElement());
				logger.finer(methodName + " , Breaking out of execution loop after processing terminating transition element.");
				return;  // break out of loop
			} else {
				throw new IllegalStateException("Not sure how we'd end up in this state...aborting rather than looping.");
			}
		}
	}

	// overrides with exit status in JSL @exit-status
	private void updateExitStatusFromJSL(String exitStatusFromJSL) {
		if (exitStatusFromJSL != null) {
			jobContext.setExitStatus(exitStatusFromJSL);  
			logger.fine("On stop, setting new JSL-specified exit status to: " + exitStatusFromJSL);
		}
	}

	private void handleTerminatingTransitionElement(TransitionElement transitionElement) {

		logger.fine("Found terminating transition element (stop, end, or fail).");

		if (transitionElement instanceof Stop) {

			Stop stopElement = (Stop)transitionElement;
			String restartOn = stopElement.getRestart();
			String exitStatusFromJSL = stopElement.getExitStatus();
			logger.fine("Next transition element is a <stop> : " + transitionElement + " with restartOn=" + restartOn + 
					" , and JSL exit status = " + exitStatusFromJSL);

			updateExitStatusFromJSL(exitStatusFromJSL);
			jslStop(restartOn);

		} else if (transitionElement instanceof End) {

			End endElement = (End)transitionElement;
			String exitStatusFromJSL = endElement.getExitStatus();
			logger.fine("Next transition element is an <end> : " + transitionElement + 
					" with JSL exit status = " + exitStatusFromJSL);
			updateExitStatusFromJSL(exitStatusFromJSL);

		} else if (transitionElement instanceof Fail) {

			Fail failElement = (Fail)transitionElement;
			String exitStatusFromJSL = failElement.getExitStatus();
			logger.fine("Next transition element is a <fail> : " + transitionElement + 
					" with JSL exit status = " + exitStatusFromJSL);
			updateJobBatchStatus(BatchStatus.FAILED);
			updateExitStatusFromJSL(exitStatusFromJSL);
		} else {
			throw new IllegalStateException("Not sure how we'd get here...aborting.");
		}
	}

	private void setupListeners() {
		JSLJob jobModel = jobExecution.getJobNavigator().getJSLJob();   
		InjectionReferences injectionRef = new InjectionReferences(jobContext, null, null);
		listenerFactory = new ListenerFactory(jobModel, injectionRef);
		jobExecution.setListenerFactory(listenerFactory);
	}

	// Call beforeJob() on all the job listeners
	private void jobListenersBeforeJob() {
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


	private void logWarning(String msg, Throwable t) {
		StringWriter sw = new StringWriter();
		t.printStackTrace(new PrintWriter(sw));
		logger.warning(msg + " with Throwable message: " + t.getMessage() + ", and stack trace: " + sw.toString());
	}

	private List<StepExecution> getSplitStepExecutions(
			IExecutionElementController previousElementController) {
		List<StepExecution> stepExecutions = new ArrayList<StepExecution>();
		if(previousElementController != null) {
			SplitControllerImpl controller = (SplitControllerImpl)previousElementController;
			for (BatchWorkUnit batchWorkUnit : controller.getParallelJobExecs()) {

				StepExecution lastStepExecution = null;
				List<StepExecution> stepExecs = persistenceService.getStepExecutionIDListQueryByJobID(batchWorkUnit.getJobExecutionImpl().getExecutionId());
				for (StepExecution stepExecution : stepExecs) {
					lastStepExecution = stepExecution;
				}
				stepExecutions.add(lastStepExecution);
			}
		}
		return stepExecutions;
	}

	private StepExecution getLastStepExecution(Step last) {
		StepExecution lastStepExecution = null;
		List<StepExecution> stepExecs = persistenceService.getStepExecutionIDListQueryByJobID(jobExecution.getExecutionId());
		for (StepExecution stepExecution : stepExecs) {
			if(last.getId().equals(stepExecution.getStepName())) {
				lastStepExecution = stepExecution;
			}
		}
		return lastStepExecution;
	}

	private Step getLastStepInTheFlow(ExecutionElement previousExecutionElement) {
		Flow flow = (Flow)previousExecutionElement;
		Step last = null;
		for (ExecutionElement elem : flow.getExecutionElements()) {
			if(elem instanceof Step) {
				last = (Step) elem;
			}
		}
		return last;
	}

	/*
	 * The thought here is that while we don't persist all the transitions in batch status (given
	 * we plan to persist at the very end), we do persist STOPPING right away, since if we end up
	 * "stuck in STOPPING" we at least will have a record in the database.
	 */
	private void batchStatusStopping() {
		updateJobBatchStatus(BatchStatus.STOPPING);
		long time = System.currentTimeMillis();
		Timestamp timestamp = new Timestamp(time);
		jobExecution.setLastUpdateTime(timestamp);
		persistenceService.updateBatchStatusOnly(jobExecution.getExecutionId(), BatchStatus.STOPPING, timestamp);
	}
	
	@Override
	public void stop() {
		if (jobContext.getBatchStatus().equals(BatchStatus.STARTING) ||
				jobContext.getBatchStatus().equals(BatchStatus.STARTED)) {
			
			batchStatusStopping();
			
			if (this.currentStoppableElementController != null) {
				this.currentStoppableElementController.stop();
			}
		} else {
			logger.info("Stop ignored since batch status for job is already set to: " + jobContext.getBatchStatus());
		}
	}

	private void setContextProperties() {
		JSLJob jobModel = jobExecution.getJobNavigator().getJSLJob();
		JSLProperties jslProps = jobModel.getProperties();

		if (jslProps != null) {
			Properties contextProps = jobContext.getProperties();
			for (Property property : jslProps.getPropertyList()) {
				contextProps.setProperty(property.getName(), property.getValue());
			}	
		}
	}

	public void setAnalyzerQueue(BlockingQueue<PartitionDataWrapper> analyzerQueue) {
		this.analyzerQueue = analyzerQueue;
	}

	private void jslStop(String restartOn) {
		logger.fine("Logging JSL stop(): exitStatus = " + jobContext.getExitStatus() + ", restartOn = " +restartOn );
		batchStatusStopping();
		jobStatusService.updateJobStatusFromJSLStop(jobInstanceId, restartOn);
		return;
	}

}
