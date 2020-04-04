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

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

import jakarta.batch.runtime.BatchStatus;

import com.ibm.jbatch.container.IController;
import com.ibm.jbatch.container.IExecutionElementController;
import com.ibm.jbatch.container.context.impl.JobContextImpl;
import com.ibm.jbatch.container.context.impl.StepContextImpl;
import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.jobinstance.RuntimeJobExecution;
import com.ibm.jbatch.container.jsl.ExecutionElement;
import com.ibm.jbatch.container.jsl.IllegalTransitionException;
import com.ibm.jbatch.container.jsl.Transition;
import com.ibm.jbatch.container.jsl.TransitionElement;
import com.ibm.jbatch.container.navigator.ModelNavigator;
import com.ibm.jbatch.container.status.ExtendedBatchStatus;
import com.ibm.jbatch.container.status.ExecutionStatus;
import com.ibm.jbatch.container.util.PartitionDataWrapper;
import com.ibm.jbatch.jsl.model.Decision;
import com.ibm.jbatch.jsl.model.End;
import com.ibm.jbatch.jsl.model.Fail;
import com.ibm.jbatch.jsl.model.Flow;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.jbatch.jsl.model.Split;
import com.ibm.jbatch.jsl.model.Step;
import com.ibm.jbatch.jsl.model.Stop;

public class ExecutionTransitioner {

	private final static String CLASSNAME = ExecutionTransitioner.class.getName();
	private final static Logger logger = Logger.getLogger(CLASSNAME);

	private RuntimeJobExecution jobExecution;
	private long rootJobExecutionId;
	private ModelNavigator<?> modelNavigator;
	
	// 'volatile' since it receives stop on separate thread.
	private volatile IExecutionElementController currentStoppableElementController;
	private IExecutionElementController previousElementController;
	private ExecutionElement currentExecutionElement = null;
	private ExecutionElement previousExecutionElement = null;

	
	private JobContextImpl jobContext;
	private BlockingQueue<PartitionDataWrapper> analyzerQueue = null;
	
	private List<Long> stepExecIds;
	
	public ExecutionTransitioner(RuntimeJobExecution jobExecution, long rootJobExecutionId, ModelNavigator<?> modelNavigator) {
		this.jobExecution = jobExecution;
		this.rootJobExecutionId = rootJobExecutionId;
		this.modelNavigator = modelNavigator;
		this.jobContext = jobExecution.getJobContext(); 
	}
	
	public ExecutionTransitioner(RuntimeJobExecution jobExecution, long rootJobExecutionId, ModelNavigator<JSLJob> jobNavigator, BlockingQueue<PartitionDataWrapper> analyzerQueue) {
		this.jobExecution = jobExecution;
		this.rootJobExecutionId = rootJobExecutionId;
		this.modelNavigator = jobNavigator;
		this.jobContext = jobExecution.getJobContext(); 
		this.analyzerQueue = analyzerQueue;
	}
	
	/**
	 * Used for job and flow.
	 * @return
	 */
	public ExecutionStatus doExecutionLoop() {

		final String methodName = "doExecutionLoop";
		
		try {
			currentExecutionElement = modelNavigator.getFirstExecutionElement(jobExecution.getRestartOn());
		} catch (IllegalTransitionException e) {
			String errorMsg = "Could not transition to first execution element within job.";
			logger.warning(errorMsg);
			throw new IllegalArgumentException(errorMsg, e);
		}

		logger.fine("First execution element = " + currentExecutionElement.getId());

		while (true) {

			if (jobContext.getBatchStatus().equals(BatchStatus.STOPPING)) {
				logger.fine(methodName + " Exiting execution loop as job is now in stopping state.");
				return new ExecutionStatus(ExtendedBatchStatus.JOB_OPERATOR_STOPPING);
			}
			
			IExecutionElementController currentElementController = getNextElementController();
			currentStoppableElementController = currentElementController;
			
			ExecutionStatus status = currentElementController.execute();

			// Nothing special for decision or step except to get exit status.  For flow and split we want to bubble up though.
			if ((currentExecutionElement instanceof Split) || (currentExecutionElement instanceof Flow)) {
				// Exit status and restartOn should both be in the job context.
				if (!status.getExtendedBatchStatus().equals(ExtendedBatchStatus.NORMAL_COMPLETION)) {
					logger.fine("Breaking out of loop with return status = " + status.getExtendedBatchStatus().name());
					return status;
				}
			} 

			// Seems like this should only happen if an Error is thrown at the step level, since normally a step-level
			// exception is caught and the fact that it was thrown capture in the ExecutionStatus
			if (jobContext.getBatchStatus().equals(BatchStatus.FAILED)) {
				String errorMsg = "Sub-execution returned its own BatchStatus of FAILED.  Deal with this by throwing exception to the next layer.";
				logger.warning(errorMsg);
				throw new BatchContainerRuntimeException(errorMsg);
			}

			// set the execution element controller to null so we don't try to call stop on it after the element has finished executing
			currentStoppableElementController = null;
			
			logger.fine("Done executing element=" + currentExecutionElement.getId() + ", exitStatus=" + status.getExitStatus());

			if (jobContext.getBatchStatus().equals(BatchStatus.STOPPING)) {
				logger.fine(methodName + " Exiting as job has been stopped");
				return new ExecutionStatus(ExtendedBatchStatus.JOB_OPERATOR_STOPPING);
			}

			Transition nextTransition = null;
			try {
				nextTransition = modelNavigator.getNextTransition(currentExecutionElement, status);
			} catch (IllegalTransitionException e) {
				String errorMsg = "Problem transitioning to next execution element.";
				logger.warning(errorMsg);
				throw new BatchContainerRuntimeException(errorMsg, e);
			} 

			//
			// We will find ourselves in one of four states now.  
			// 
			// 1. Finished transitioning after a normal execution, but nothing to do 'next'.
			// 2. We just executed a step which through an exception, but didn't match a transition element.
			// 3. We are going to 'next' to another execution element (and jump back to the top of this '
			//    'while'-loop.
			// 4. We matched a terminating transition element (<end>, <stop> or <fail).
			//
			
			// 1.
			if (nextTransition.isFinishedTransitioning()) {
				logger.fine(methodName + "No next execution element, and no transition element found either.  Looks like we're done and ready for COMPLETED state.");
				this.stepExecIds =  currentElementController.getLastRunStepExecutions();
				// Consider just passing the last 'status' back, but let's unwrap the exit status and pass a new NORMAL_COMPLETION
				// status back instead.
				return new ExecutionStatus(ExtendedBatchStatus.NORMAL_COMPLETION, status.getExitStatus());
			// 2.
			} else if (nextTransition.noTransitionElementMatchedAfterException()) {
				return new ExecutionStatus(ExtendedBatchStatus.EXCEPTION_THROWN, status.getExitStatus());
			// 3.
			} else if (nextTransition.getNextExecutionElement() != null) {
				// hold on to the previous execution element for the decider
				// we need it because we need to inject the context of the
				// previous execution element into the decider
				previousExecutionElement = currentExecutionElement;
				previousElementController = currentElementController;
				currentExecutionElement = nextTransition.getNextExecutionElement();
			// 4.
			} else if (nextTransition.getTransitionElement() != null) {
				ExecutionStatus terminatingStatus = handleTerminatingTransitionElement(nextTransition.getTransitionElement());
				logger.finer(methodName + " , Breaking out of execution loop after processing terminating transition element.");
				return terminatingStatus;
			} else {
				throw new IllegalStateException("Not sure how we'd end up in this state...aborting rather than looping.");
			}
		}
	}

	
	private IExecutionElementController getNextElementController() {
		IExecutionElementController elementController =null;

		if (currentExecutionElement instanceof Decision) {
			Decision decision = (Decision)currentExecutionElement;
			elementController = ExecutionElementControllerFactory.getDecisionController(jobExecution, decision);			
			DecisionControllerImpl decisionController = (DecisionControllerImpl)elementController;
			decisionController.setPreviousStepExecutions(previousExecutionElement, previousElementController);
		} else if (currentExecutionElement instanceof Flow) {
			Flow flow = (Flow)currentExecutionElement;
			elementController = ExecutionElementControllerFactory.getFlowController(jobExecution, flow, rootJobExecutionId);
		} else if (currentExecutionElement instanceof Split) {
			Split split = (Split)currentExecutionElement;
			elementController = ExecutionElementControllerFactory.getSplitController(jobExecution, split, rootJobExecutionId);
		} else if (currentExecutionElement instanceof Step) {
			Step step = (Step)currentExecutionElement;
			StepContextImpl stepContext = new StepContextImpl(step.getId());
			elementController = ExecutionElementControllerFactory.getStepController(jobExecution, step, stepContext, rootJobExecutionId, analyzerQueue);
		}
		logger.fine("Next execution element controller = " + elementController);
		return elementController;
	}
			
			
	private ExecutionStatus handleTerminatingTransitionElement(TransitionElement transitionElement) {

		ExecutionStatus retVal;
		
		logger.fine("Found terminating transition element (stop, end, or fail).");

		if (transitionElement instanceof Stop) {

			Stop stopElement = (Stop)transitionElement;
			String restartOn = stopElement.getRestart();
			String exitStatusFromJSL = stopElement.getExitStatus();
			logger.fine("Next transition element is a <stop> : " + transitionElement + " with restartOn=" + restartOn + 
					" , and JSL exit status = " + exitStatusFromJSL);

			retVal = new ExecutionStatus(ExtendedBatchStatus.JSL_STOP);
			
			if (exitStatusFromJSL != null) {
				jobContext.setExitStatus(exitStatusFromJSL);  
				retVal.setExitStatus(exitStatusFromJSL);  
			}
			if (restartOn != null) {
				jobContext.setRestartOn(restartOn);				
				retVal.setRestartOn(restartOn);				
			}
		} else if (transitionElement instanceof End) {

			End endElement = (End)transitionElement;
			String exitStatusFromJSL = endElement.getExitStatus();
			logger.fine("Next transition element is an <end> : " + transitionElement + 
					" with JSL exit status = " + exitStatusFromJSL);
			retVal = new ExecutionStatus(ExtendedBatchStatus.JSL_END);
			if (exitStatusFromJSL != null) {
				jobContext.setExitStatus(exitStatusFromJSL);  
				retVal.setExitStatus(exitStatusFromJSL);  
			}
		} else if (transitionElement instanceof Fail) {

			Fail failElement = (Fail)transitionElement;
			String exitStatusFromJSL = failElement.getExitStatus();
			logger.fine("Next transition element is a <fail> : " + transitionElement + 
					" with JSL exit status = " + exitStatusFromJSL);
			retVal = new ExecutionStatus(ExtendedBatchStatus.JSL_FAIL);
			if (exitStatusFromJSL != null) {
				jobContext.setExitStatus(exitStatusFromJSL);  
				retVal.setExitStatus(exitStatusFromJSL);  
			}
		} else {
			throw new IllegalStateException("Not sure how we'd get here...aborting.");
		}
		return retVal;
	}

	public IController getCurrentStoppableElementController() {
		return currentStoppableElementController;
	}

    public List<Long> getStepExecIds() {
        return stepExecIds;
    }


}