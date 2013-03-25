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
import com.ibm.jbatch.container.jobinstance.JobExecutionHelper;
import com.ibm.jbatch.container.jobinstance.RuntimeJobContextJobExecutionBridge;
import com.ibm.jbatch.container.jsl.ExecutionElement;
import com.ibm.jbatch.container.jsl.IllegalTransitionException;
import com.ibm.jbatch.container.jsl.JobNavigator;
import com.ibm.jbatch.container.jsl.Transition;
import com.ibm.jbatch.container.jsl.TransitionElement;
import com.ibm.jbatch.container.services.IJobStatusManagerService;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.services.impl.JDBCPersistenceManagerImpl;
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

	private void setupListeners() {
		JSLJob jobModel = jobExecution.getJobNavigator().getJSLJob();   

		InjectionReferences injectionRef = new InjectionReferences(jobContext, null, null);

		listenerFactory = new ListenerFactory(jobModel, injectionRef);
		jobExecution.setListenerFactory(listenerFactory);
	}

	private boolean isJobStopping() {
		if (BatchStatus.STOPPING.equals(jobContext.getBatchStatus())) {
			updateJobBatchStatus(BatchStatus.STOPPED);
			logger.fine("Exiting job execution since it is stopping state; a stop has been issued.");
			return true;
		} else {
			logger.finest("Job execution not detected as stopping.");
			return false;
		}
	}
	public void executeJob() {

		final String methodName = "executeJob";
		if (logger.isLoggable(Level.FINE)) {
			logger.entering(CLASSNAME, methodName);
		}

		Throwable throwable = null;
		BatchStatus currentStatus = null;

		try {

			updateJobBatchStatus(BatchStatus.STARTING);

			// Periodic check for stopping job
			if (isJobStopping()) {
				return;
			}

			updateJobBatchStatus(BatchStatus.STARTED);

			List<JobListenerProxy> jobListeners = listenerFactory.getJobListeners();

			// Call @BeforeJob on all the job listeners
			for (JobListenerProxy listenerProxy : jobListeners) {
				if (logger.isLoggable(Level.FINE)) {
					logger.fine(methodName + " Invoking @BeforeJob on jobListener: " + listenerProxy.getDelegate() + " of type: " + listenerProxy.getDelegate().getClass());
				}
				listenerProxy.beforeJob();
			}

			// Periodic check for stopping job
			if (isJobStopping()) {
				return;
			}

			// --------------------
			//
			// The BIG loop!!!
			//
			// --------------------
			doExecutionLoop();

			currentStatus = jobContext.getBatchStatus();
			if (currentStatus == null) {
				throw new IllegalStateException("Job BatchStatus should have been set by now");
			}
		} catch (Throwable t) {
			// We still want to try to call the afterJob() listener and persist the batch and exit
			// status for the failure in an orderly fashion.  So catch and continue.
			throwable = t;
			logWarning("Caught throwable in main execution loop", t);
			currentStatus = BatchStatus.FAILED;
			updateJobBatchStatus(BatchStatus.FAILED);
		}

		endOfJob(currentStatus, throwable);
		
		if (logger.isLoggable(Level.FINE)) {
			logger.exiting(CLASSNAME, methodName);
		}
		
		return;
	}

	/*
	 * Probably we'll eventually need to be more careful about one of these methods themselves
	 * throwing an uncaught exception.   But the lack of more complete error handling doesn't seem to affect
	 * any API, so we'll cross that bridge when we come to it.
	 */
	private void endOfJob(BatchStatus currentStatus, Throwable caughtException) {
		
		// Call @AfterJob on all the job listeners
		List<JobListenerProxy> jobListeners = listenerFactory.getJobListeners();
		for (JobListenerProxy listenerProxy : jobListeners) {
			if (logger.isLoggable(Level.FINE)) {
				logger.fine(" Invoking @AfterJob on jobListener: " + listenerProxy.getDelegate() + " of type: " + listenerProxy.getDelegate().getClass());
			}
			listenerProxy.afterJob();
		}	

		// Update batch status which may already have been set by a JSL <stop> or <fail> decision directive.
		if (!(currentStatus.equals(BatchStatus.FAILED) || currentStatus.equals(BatchStatus.STOPPED))) {
			updateJobBatchStatus(BatchStatus.COMPLETED);
		}
		
		// Now that all job artifacts have run, we can default job exit status
		if (jobContext.getExitStatus() == null) {
			logger.fine("No job-level exitStatus set, defaulting to job batch Status = " + jobContext.getBatchStatus());
			jobContext.setExitStatus(jobContext.getBatchStatus().name());
		}

		// Persist exit status, setting default if not set
		logger.fine("Job complete for job id=" + jobExecution.getJobInstance().getJobName() + ", executionId=" + jobExecution.getExecutionId() 
				+ ", batchStatus=" + jobContext.getBatchStatus() + ", exitStatus=" + jobContext.getExitStatus());
		jobStatusService.updateJobExecutionStatus(jobExecution.getInstanceId(), jobContext.getBatchStatus(), jobContext.getExitStatus());
		//set update time onto the runtime JobExecution Obj - should I also update the status string here too?
		long time = System.currentTimeMillis();
		Timestamp updateTS = new Timestamp(time);
		jobExecution.setLastUpdateTime(updateTS);
		jobExecution.setEndTime(updateTS);
		persistenceService.jobExecutionStatusStringUpdate(jobExecution.getExecutionId(), JDBCPersistenceManagerImpl.EXIT_STATUS, jobContext.getExitStatus(), updateTS);
		persistenceService.jobExecutionTimestampUpdate(jobExecution.getExecutionId(), JDBCPersistenceManagerImpl.END_TIME, updateTS);

		if (caughtException != null) {
			logger.fine("Rethrowing earlier caught exception wrapped.");
			throw new BatchContainerRuntimeException(caughtException);
		}
	}
	
	private void logWarning(String msg, Throwable t) {
		StringWriter sw = new StringWriter();
		t.printStackTrace(new PrintWriter(sw));
		logger.warning(msg + " with Throwable message: " + t.getMessage() + ", and stack trace: " + sw.toString());
	}

	private void doExecutionLoop() throws Exception {
		final String methodName = "doExecutionLoop";

		JobContextImpl jobContext = jobExecution.getJobContext();

		ExecutionElement currentExecutionElement = null;
		try {
			currentExecutionElement = jobNavigator.getFirstExecutionElementInJob(jobExecution.getRestartOn());
		} catch (IllegalTransitionException e) {
			String errorMsg = "Could not transition to first execution element within job.";
			logger.warning(errorMsg);
			throw new IllegalArgumentException(errorMsg, e);
		}

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("First execution element = " + currentExecutionElement.getId());
		}

		StepContextImpl stepContext = null;

		ExecutionElement previousExecutionElement = null;

		IExecutionElementController previousElementController = null;

		while (true) {

			if (!(currentExecutionElement instanceof Step) && !(currentExecutionElement instanceof Decision) 
					&& !(currentExecutionElement instanceof Flow) && !(currentExecutionElement instanceof Split)) {
				throw new IllegalStateException("Found unknown currentExecutionElement type = " + currentExecutionElement.getClass().getName());
			}

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Next execution element = " + currentExecutionElement.getId());
			}

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
					// the context from the
					// previous execution element
					StepExecution lastStepExecution = getLastStepExecution((Step) previousExecutionElement);

					((DecisionControllerImpl)elementController).setStepExecution((Step)previousExecutionElement, lastStepExecution);

				} else if (previousExecutionElement instanceof Split) {

					List<StepExecution> stepExecutions = getSplitStepExecutions(previousElementController);
					((DecisionControllerImpl)elementController).setStepExecutions((Split)previousExecutionElement, stepExecutions);

				} else if (previousExecutionElement instanceof Flow) {

					// get last step in flow
					Step last = getLastStepInTheFlow(previousExecutionElement);

					// get last step StepExecution
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
				updateJobBatchStatus(BatchStatus.STOPPED);

				if (logger.isLoggable(Level.FINE)) {
					logger.fine(methodName + " Exiting as job has been stopped");
				}
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
				if (logger.isLoggable(Level.FINE)) {
					logger.fine("Execution failed before even getting to execute execution element = " + currentExecutionElement.getId());
				}
				logger.warning("Execution failed, InstanceId: " + this.jobInstanceId + ", executionId = " + this.jobExecution.getExecutionId());
				throw new BatchContainerRuntimeException("Execution failed before even getting to execute execution element = " + 
						currentExecutionElement.getId() + "; breaking out of execution loop.");                
			}

			// Throw an exception on fail 
			if (executionElementStatus.getBatchStatus().equals(BatchStatus.FAILED)) {
				logger.warning("Sub-execution returned its own BatchStatus of FAILED.  Deal with this by throwing exception to the next layer.");
				throw new BatchContainerRuntimeException("Sub-execution returned its own BatchStatus of FAILED.  Deal with this by throwing exception to the next layer.");
			}

			// JSL Stop
			if (executionElementStatus.getBatchStatus().equals(BatchStatus.STOPPED)) {
				String restartOn = executionElementStatus.getRestartOn();
				jslStop(executionElementStatus, executionElementStatus.getExitStatus(), restartOn);
				return;
			}

			// set the execution element controller to null so we don't try to
			// call stop on it after the element has finished executing
			this.currentStoppableElementController = null; 
			previousElementController = elementController;

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Done executing element=" + currentExecutionElement.getId() + ", exitStatus=" + executionElementStatus);
			}

			// If we are currently in STOPPING state, then we can now move transition to STOPPED state.
			if (jobContext.getBatchStatus().equals(BatchStatus.STOPPING)) {

				updateJobBatchStatus(BatchStatus.STOPPED);

				if (logger.isLoggable(Level.FINE)) {
					logger.fine(methodName + " Exiting as job has been stopped");
				} 
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
				if (logger.isLoggable(Level.FINE)) {
					logger.fine(methodName + "Looks like we're done, nothing left to execute.");
				}
				return;
			}

			if (nextTransition.getNextExecutionElement() != null) {
				// hold on to the previous execution element for the decider
				// we need it because we need to inject the context of the
				// previous execution element into the decider
				previousExecutionElement = currentExecutionElement;
				currentExecutionElement = nextTransition.getNextExecutionElement();

				if (logger.isLoggable(Level.FINE)) {
					logger.fine(methodName + " , Looping through to next execution element=" + currentExecutionElement.getId());
				}
			} else if (nextTransition.getTransitionElement() != null) {
				// TODO - update job status mgr
				TransitionElement transitionElement = nextTransition.getTransitionElement();

				if (logger.isLoggable(Level.FINE)) {
					logger.fine(methodName + " , Looping through to next control element=" + transitionElement);
				}

				if (transitionElement instanceof Stop) {
					String restartOn = ((Stop) transitionElement).getRestart();

					if (logger.isLoggable(Level.FINE)) {
						logger.fine(methodName + " , next control element is a <stop> : " + transitionElement + " with restartOn=" + restartOn);
					}

					String newExitStatus = ((Stop) transitionElement).getExitStatus();
					if (newExitStatus != null && !newExitStatus.isEmpty()) { // overrides with exit status in JSL @exit-status
						jobContext.setExitStatus(newExitStatus);  
						if (logger.isLoggable(Level.FINE)) {
							logger.fine(methodName + " , on stop, setting new JSL-specified exit status to: " + newExitStatus);
						}
					} else {
						//exit status from job context is used
						newExitStatus = jobContext.getExitStatus();
					}

					InternalExecutionElementStatus internalStopStatus = new InternalExecutionElementStatus(BatchStatus.STOPPED, newExitStatus);
					jslStop(internalStopStatus, newExitStatus, restartOn);

					if (logger.isLoggable(Level.FINE)) {
						logger.fine(methodName + " Exiting stopped job");
					}
					return;

				} else if (transitionElement instanceof End) {
					if (logger.isLoggable(Level.FINE)) {
						logger.fine(methodName + " , next control element is an <end>: " + transitionElement);
					}
					updateJobBatchStatus(BatchStatus.COMPLETED);
					String newExitStatus = ((End) transitionElement).getExitStatus();
					if (newExitStatus != null && !newExitStatus.isEmpty()) { // overrides with exit status in JSL @exit-status
						jobContext.setExitStatus(newExitStatus); 
						if (logger.isLoggable(Level.FINE)) {
							logger.fine(methodName + " , on end, setting new JSL-specified exit status to: " + newExitStatus);
						}
					} else {
						//exit status from job context is used
						newExitStatus = jobContext.getExitStatus();
					}
				} else if (transitionElement instanceof Fail) {
					if (logger.isLoggable(Level.FINE)) {
						logger.fine(methodName + " , next control element is a <fail>: " + transitionElement);
					}
					updateJobBatchStatus(BatchStatus.FAILED);
					String newExitStatus = ((Fail) transitionElement).getExitStatus();
					if (newExitStatus != null && !newExitStatus.isEmpty()) { // overrides
						// with
						jobContext.setExitStatus(newExitStatus); // exit status
						// in
						if (logger.isLoggable(Level.FINE)) {
							logger.fine(methodName + " , on fail, setting new JSL-specified exit status to: " + newExitStatus);
						}
					} // <fail> @exit-status
					else {
						//exit status from job context is used
						newExitStatus = jobContext.getExitStatus();
					}
				} else {
					throw new IllegalStateException("Not sure how we'd get here but better than looping.");
				}
				return;
			} else {

				if (logger.isLoggable(Level.FINE)) {
					logger.fine(methodName + " Exiting as there are no more execution elements= ");
				}
				return;
			}
		}
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

	@Override
	public void stop() {
		if (jobContext.getBatchStatus().equals(BatchStatus.STARTING) ||
				jobContext.getBatchStatus().equals(BatchStatus.STARTED)) {

			updateJobBatchStatus(BatchStatus.STOPPING);
			if (this.currentStoppableElementController != null) {
				this.currentStoppableElementController.stop();
			}
		} else {
			//TODO do we need to throw an error if the batchlet is already stopping/stopped
			//a stop gets issued twice
		}

	}

	private void updateJobBatchStatus(BatchStatus batchStatus) {
		String methodName = "updateJobBatchStatus";

		if (logger.isLoggable(Level.FINE)) {
			logger.fine(methodName + " Setting job batch status to: " + batchStatus);
		}

		jobContext.setBatchStatus(batchStatus);
		jobStatusService.updateJobBatchStatus(jobInstanceId, batchStatus);

		//set update time onto the runtime JobExecution Obj - should I also update the status string here too?
		long time = System.currentTimeMillis();
		Timestamp timestamp = new Timestamp(time);
		jobExecution.setLastUpdateTime(timestamp);

		switch (batchStatus) {
		case STARTING:
			//perisistence call to update batch status and update time
			JobExecutionHelper.updateBatchStatusUPDATEonly(jobExecution.getExecutionId(), batchStatus.name(), timestamp);
			break;
		case STARTED:
			//perisistence call to update batch status and update time and start time
			// Timestamp startTS = new Timestamp(time);
			JobExecutionHelper.updateBatchStatusSTART(jobExecution.getExecutionId(), batchStatus.name(), timestamp);
			break;
		case STOPPING:
			//perisistence call to update batch status and update time
			JobExecutionHelper.updateBatchStatusUPDATEonly(jobExecution.getExecutionId(), batchStatus.name(), timestamp);
			break;
		case STOPPED:
			//perisistence call to update batch status and update time and stop time
			// Timestamp stopTS = new Timestamp(time);
			JobExecutionHelper.updateBatchStatusSTOP(jobExecution.getExecutionId(), batchStatus.name(), timestamp);
			break;
		case COMPLETED:
			//perisistence call to update batch status and update time and end time
			// Timestamp stopTS = new Timestamp(time);
			JobExecutionHelper.updateBatchStatusCOMPLETED(jobExecution.getExecutionId(), batchStatus.name(), timestamp);
			break;
		case FAILED:
			//perisistence call to update batch status and update time and end time
			// Timestamp endTS = new Timestamp(time);
			JobExecutionHelper.updateBatchStatusFAILED(jobExecution.getExecutionId(), batchStatus.name(), timestamp);
			break;
		default:
			//?
		}

		// update job execution instance information keyed by execution id
		//if (persistenceService instanceof JDBCPersistenceManagerImpl){	
		//	persistenceService.jobExecutionStatusStringUpdate(jobExecution.getExecutionId(), JDBCPersistenceManagerImpl.BATCH_STATUS, ExecutionStatus.getStringValue(batchStatus), updateTS);
		//}

	}

	public void setAnalyzerQueue(BlockingQueue<PartitionDataWrapper> analyzerQueue) {
		this.analyzerQueue = analyzerQueue;
	}

	private void jslStop(InternalExecutionElementStatus status, String exitStatus, String restartOn) {
		logger.fine("Logging JSL stop(): status = " + status + ", exitStatus = " + exitStatus + ", restartOn = " +restartOn );
		updateJobBatchStatus(BatchStatus.STOPPED);
		jobStatusService.updateJobStatusFromJSLStop(jobInstanceId, restartOn);
		jobContext.setExitStatus(exitStatus);  
		return;
	}

}
