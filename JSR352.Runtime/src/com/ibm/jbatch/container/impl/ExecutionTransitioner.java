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

import javax.batch.runtime.BatchStatus;

import com.ibm.jbatch.container.IController;
import com.ibm.jbatch.container.context.impl.JobContextImpl;
import com.ibm.jbatch.container.context.impl.StepContextImpl;
import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.jobinstance.RuntimeJobExecution;
import com.ibm.jbatch.container.jsl.ExecutionElement;
import com.ibm.jbatch.container.jsl.IllegalTransitionException;
import com.ibm.jbatch.container.jsl.ModelNavigator;
import com.ibm.jbatch.container.jsl.Transition;
import com.ibm.jbatch.container.jsl.TransitionElement;
import com.ibm.jbatch.container.status.JobOrFlowBatchStatus;
import com.ibm.jbatch.container.status.JobOrFlowStatus;
import com.ibm.jbatch.container.status.SplitStatus;
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
	private IController currentStoppableElementController;
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
	public JobOrFlowStatus doExecutionLoop() {

		final String methodName = "doExecutionLoop";
		ExecutionElement previousExecutionElement = null;
		IController previousElementController = null;
		ExecutionElement currentExecutionElement = null;

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
				return new JobOrFlowStatus(JobOrFlowBatchStatus.JOB_OPERATOR_STOPPING);
			}

			if (!(currentExecutionElement instanceof Step) && !(currentExecutionElement instanceof Decision) && !(currentExecutionElement instanceof Flow) && !(currentExecutionElement instanceof Split)) {
				throw new IllegalStateException("Found unknown currentExecutionElement type = " + currentExecutionElement.getClass().getName());
			}

			logger.fine("Next execution element = " + currentExecutionElement.getId());

			IController elementController =null;

			String executionElementExitStatus = null;
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

			// Supports stop processing
			currentStoppableElementController = elementController;

			if (currentExecutionElement instanceof Decision) {
				executionElementExitStatus = ((DecisionControllerImpl)elementController).execute();
				
			} else if (currentExecutionElement instanceof Flow) {
				JobOrFlowStatus flowStatus = ((FlowControllerImpl)elementController).execute(); // recursive
				JobOrFlowBatchStatus flowBatchStatus = flowStatus.getBatchStatus();
				// Exit status and restartOn should both be in the job context.
				if (!flowBatchStatus.equals(JobOrFlowBatchStatus.NORMAL_COMPLETION)) {
					logger.fine("Breaking out of loop with return status = " + flowBatchStatus.name());
					return flowStatus;
				}
				executionElementExitStatus = flowStatus.getExitStatus();
				logger.fine("Normal retrun from flow with exit status = " + executionElementExitStatus);
			} else if (currentExecutionElement instanceof Split) {
				SplitStatus splitStatus = ((SplitControllerImpl)elementController).execute();
				JobOrFlowBatchStatus determiningBatchStatus = splitStatus.getDeterminingFlowBatchStatus();
				if (!determiningBatchStatus.equals(JobOrFlowBatchStatus.NORMAL_COMPLETION)) {
					logger.fine("Breaking out of loop with return status = " + determiningBatchStatus.name());
					return new JobOrFlowStatus(determiningBatchStatus);
				}
				// We could use a special "unset" value here but we just use 'null'.  Splits don't have
				// transition elements and will only transition via @next attribute.
				executionElementExitStatus = null;
			} else if (currentExecutionElement instanceof Step) {
				executionElementExitStatus = ((BaseStepControllerImpl)elementController).execute();
			}

			// Throw an exception on fail 
			if (jobContext.getBatchStatus().equals(BatchStatus.FAILED)) {
				logger.warning("Sub-execution returned its own BatchStatus of FAILED.  Deal with this by throwing exception to the next layer.");
				throw new BatchContainerRuntimeException("Sub-execution returned its own BatchStatus of FAILED.  Deal with this by throwing exception to the next layer.");
			}

			// set the execution element controller to null so we don't try to
			// call stop on it after the element has finished executing
			this.currentStoppableElementController = null; 
			previousElementController = elementController;

			logger.fine("Done executing element=" + currentExecutionElement.getId() + ", exitStatus=" + executionElementExitStatus);

			if (jobContext.getBatchStatus().equals(BatchStatus.STOPPING)) {
				logger.fine(methodName + " Exiting as job has been stopped");
				return new JobOrFlowStatus(JobOrFlowBatchStatus.JOB_OPERATOR_STOPPING);
			}

			Transition nextTransition = null;
			try {
				nextTransition = modelNavigator.getNextTransition(currentExecutionElement, executionElementExitStatus);
			} catch (IllegalTransitionException e) {
				String errorMsg = "Problem transitioning to next execution element.";
				logger.warning(errorMsg);
				throw new IllegalArgumentException(errorMsg, e);
			}

			// Break out of loop since there's nothing left to execute.  
			// In this case we actually flow the exit status back as well, unlike in the termination because of
			// transition element case.
			if (nextTransition == null) {
				logger.fine(methodName + "No next execution element, and no transition element found either.  Looks like we're done and ready for COMPLETED state.");
				
				this.stepExecIds =  elementController.getLastRunStepExecutions();
				
				return new JobOrFlowStatus(JobOrFlowBatchStatus.NORMAL_COMPLETION, executionElementExitStatus);
			}

			// Loop back to the top.
			if (nextTransition.getNextExecutionElement() != null) {
				// hold on to the previous execution element for the decider
				// we need it because we need to inject the context of the
				// previous execution element into the decider
				previousExecutionElement = currentExecutionElement;
				currentExecutionElement = nextTransition.getNextExecutionElement();
			} else if (nextTransition.getTransitionElement() != null) {
				JobOrFlowStatus terminatingStatus = handleTerminatingTransitionElement(nextTransition.getTransitionElement());
				logger.finer(methodName + " , Breaking out of execution loop after processing terminating transition element.");
				return terminatingStatus;
			} else {
				throw new IllegalStateException("Not sure how we'd end up in this state...aborting rather than looping.");
			}
		}
	}

	private JobOrFlowStatus handleTerminatingTransitionElement(TransitionElement transitionElement) {

		JobOrFlowStatus retVal;
		
		logger.fine("Found terminating transition element (stop, end, or fail).");

		if (transitionElement instanceof Stop) {

			Stop stopElement = (Stop)transitionElement;
			String restartOn = stopElement.getRestart();
			String exitStatusFromJSL = stopElement.getExitStatus();
			logger.fine("Next transition element is a <stop> : " + transitionElement + " with restartOn=" + restartOn + 
					" , and JSL exit status = " + exitStatusFromJSL);

			retVal = new JobOrFlowStatus(JobOrFlowBatchStatus.JSL_STOP);
			
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
			retVal = new JobOrFlowStatus(JobOrFlowBatchStatus.JSL_END);
			if (exitStatusFromJSL != null) {
				jobContext.setExitStatus(exitStatusFromJSL);  
				retVal.setExitStatus(exitStatusFromJSL);  
			}
		} else if (transitionElement instanceof Fail) {

			Fail failElement = (Fail)transitionElement;
			String exitStatusFromJSL = failElement.getExitStatus();
			logger.fine("Next transition element is a <fail> : " + transitionElement + 
					" with JSL exit status = " + exitStatusFromJSL);
			retVal = new JobOrFlowStatus(JobOrFlowBatchStatus.JSL_FAIL);
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