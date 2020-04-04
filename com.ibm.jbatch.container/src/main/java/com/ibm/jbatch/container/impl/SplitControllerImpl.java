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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.batch.operations.JobExecutionAlreadyCompleteException;
import jakarta.batch.operations.JobExecutionNotMostRecentException;
import jakarta.batch.operations.JobExecutionNotRunningException;
import jakarta.batch.operations.JobRestartException;
import jakarta.batch.operations.JobStartException;
import jakarta.batch.operations.NoSuchJobExecutionException;

import com.ibm.jbatch.container.IExecutionElementController;
import com.ibm.jbatch.container.context.impl.JobContextImpl;
import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.jobinstance.RuntimeFlowInSplitExecution;
import com.ibm.jbatch.container.jobinstance.RuntimeJobExecution;
import com.ibm.jbatch.container.services.IBatchKernelService;
import com.ibm.jbatch.container.servicesmanager.ServicesManager;
import com.ibm.jbatch.container.servicesmanager.ServicesManagerImpl;
import com.ibm.jbatch.container.status.ExecutionStatus;
import com.ibm.jbatch.container.status.ExtendedBatchStatus;
import com.ibm.jbatch.container.status.SplitExecutionStatus;
import com.ibm.jbatch.container.util.BatchFlowInSplitWorkUnit;
import com.ibm.jbatch.container.util.BatchParallelWorkUnit;
import com.ibm.jbatch.container.util.FlowInSplitBuilderConfig;
import com.ibm.jbatch.jsl.model.Flow;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.jbatch.jsl.model.Split;

public class SplitControllerImpl implements IExecutionElementController {

	private final static String sourceClass = SplitControllerImpl.class.getName();
	private final static Logger logger = Logger.getLogger(sourceClass);

	private final RuntimeJobExecution jobExecution;

	private volatile List<BatchFlowInSplitWorkUnit> parallelBatchWorkUnits;

	private final ServicesManager servicesManager;
	private final IBatchKernelService batchKernel;
	private final JobContextImpl jobContext;
	private final BlockingQueue<BatchFlowInSplitWorkUnit> completedWorkQueue = new LinkedBlockingQueue<BatchFlowInSplitWorkUnit>();
	private final long rootJobExecutionId;

	final List<JSLJob> subJobs = new ArrayList<JSLJob>();

	protected Split split;

	// Moving to a field to hold state across flow statuses.
	private ExtendedBatchStatus aggregateStatus = null;

	public SplitControllerImpl(RuntimeJobExecution jobExecution, Split split, long rootJobExecutionId) {
		this.jobExecution = jobExecution;
		this.jobContext = jobExecution.getJobContext();
		this.rootJobExecutionId = rootJobExecutionId;
		this.split = split;

		servicesManager = ServicesManagerImpl.getInstance();
		batchKernel = servicesManager.getBatchKernelService();
	}

	@Override
	public void stop() { 

		// It's possible we may try to stop a split before any
		// sub steps have been started.
		synchronized (subJobs) {

			if (parallelBatchWorkUnits != null) {
				for (BatchParallelWorkUnit subJob : parallelBatchWorkUnits) {
					long jobExecutionId = -1;
					try {
						jobExecutionId = subJob.getJobExecutionImpl().getExecutionId();
						batchKernel.stopJob(jobExecutionId);
					} catch (JobExecutionNotRunningException e) {
						logger.fine("Caught exception trying to stop subjob: " + jobExecutionId + ", which was not running.");
						// We want to stop all running split-flows
						// We do not want to throw an exception if a split-flow has already been completed.
					} catch (Exception e) {
						// TODO - Is this what we want to know.  
						// Blow up if it happens to force the issue.
						throw new IllegalStateException(e);
					}
				}
			}
		}
	}

	@Override
	public SplitExecutionStatus execute() throws JobRestartException, JobStartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException, NoSuchJobExecutionException {
		String sourceMethod = "execute";
		if (logger.isLoggable(Level.FINER)) {
			logger.entering(sourceClass, sourceMethod, "Root JobExecution Id = " + rootJobExecutionId);
		}

		// Build all sub jobs from partitioned step
		buildSubJobBatchWorkUnits();

		// kick off the threads
		executeWorkUnits();

		// Deal with the results.
		SplitExecutionStatus status = waitForCompletionAndAggregateStatus();

		if (logger.isLoggable(Level.FINER)) {
			logger.exiting(sourceClass, sourceMethod, status);
		}

		return status;
	}

	/**
	 * Note we restart all flows.  There is no concept of "the flow completed".   It is only steps
	 * within the flows that may have already completed and so may not have needed to be rerun.
	 * 
	 */
	private void buildSubJobBatchWorkUnits() {

		List<Flow> flows = this.split.getFlows();

		parallelBatchWorkUnits = new ArrayList<BatchFlowInSplitWorkUnit>();

		// Build all sub jobs from flows in split
		synchronized (subJobs) {
			for (Flow flow : flows) {
				subJobs.add(PartitionedStepBuilder.buildFlowInSplitSubJob(jobContext, this.split, flow));
			}
			// Go back to earlier idea that we may have seen this id before, and need a special "always restart" behavior
			// for split-flows.
			for (JSLJob job : subJobs) {				
				int count = batchKernel.getJobInstanceCount(job.getId());
				FlowInSplitBuilderConfig config = new FlowInSplitBuilderConfig(job, completedWorkQueue, rootJobExecutionId);
				if (count == 0) {
					parallelBatchWorkUnits.add(batchKernel.buildNewFlowInSplitWorkUnit(config));
				} else if (count == 1) {
					parallelBatchWorkUnits.add(batchKernel.buildOnRestartFlowInSplitWorkUnit(config));
				} else {
					throw new IllegalStateException("There is an inconsistency somewhere in the internal subjob creation");
				}
			}
		}
	}

	private void executeWorkUnits () {
		// Then start or restart all subjobs in parallel
		for (BatchParallelWorkUnit work : parallelBatchWorkUnits) {
			int count = batchKernel.getJobInstanceCount(work.getJobExecutionImpl().getJobInstance().getJobName());

			assert (count <= 1);

			if (count == 1) {
				batchKernel.startGeneratedJob(work);
			} else if (count > 1) {
				batchKernel.restartGeneratedJob(work);
			} else {
				throw new IllegalStateException("There is an inconsistency somewhere in the internal subjob creation");
			}
		}
	}

	private SplitExecutionStatus waitForCompletionAndAggregateStatus() {

		SplitExecutionStatus splitStatus = new SplitExecutionStatus();

		for (int i=0; i < subJobs.size(); i++) {
			BatchFlowInSplitWorkUnit batchWork;
			try {
				batchWork = completedWorkQueue.take(); //wait for each thread to finish and then look at it's status
			} catch (InterruptedException e) {
				throw new BatchContainerRuntimeException(e);
			}

			RuntimeFlowInSplitExecution flowExecution = batchWork.getJobExecutionImpl();
			ExecutionStatus flowStatus = flowExecution.getFlowStatus();
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Subjob " + flowExecution.getExecutionId() + "ended with flow-in-split status: " + flowStatus);
			}
			aggregateTerminatingStatusFromSingleFlow(flowStatus, splitStatus);
		}
		
		// If this is still set to 'null' that means all flows completed normally without terminating the job.
		if (aggregateStatus == null) {
			logger.fine("Setting normal split status as no contained flows ended the job.");
			aggregateStatus = ExtendedBatchStatus.NORMAL_COMPLETION;
		}

		splitStatus.setExtendedBatchStatus(aggregateStatus);
		logger.fine("Returning from waitForCompletionAndAggregateStatus with return value: " + splitStatus);
		return splitStatus;
	}


	//
	// A <fail> and an uncaught exception are peers.  They each take precedence over a <stop>, which take precedence over an <end>.
	// Among peers the last one seen gets to set the exit stauts.
	//
	private void aggregateTerminatingStatusFromSingleFlow(ExecutionStatus flowStatus, SplitExecutionStatus splitStatus) {

		String exitStatus = flowStatus.getExitStatus();
		String restartOn = flowStatus.getRestartOn();
		ExtendedBatchStatus flowBatchStatus = flowStatus.getExtendedBatchStatus();
		
		logger.fine("Aggregating possible terminating status for flow ending with status: " + flowStatus 
				+ ", restartOn = " + restartOn);

		if ( flowBatchStatus.equals(ExtendedBatchStatus.JSL_END) || flowBatchStatus.equals(ExtendedBatchStatus.JSL_STOP) || 
				flowBatchStatus.equals(ExtendedBatchStatus.JSL_FAIL) || flowBatchStatus.equals(ExtendedBatchStatus.EXCEPTION_THROWN) ) {
			if (aggregateStatus == null) {
				logger.fine("A flow detected as ended because of a terminating condition: " + flowBatchStatus.name() 
						+ ". First flow detected in terminating state.  Setting exitStatus if non-null.");
				setInJobContext(flowBatchStatus, exitStatus, restartOn);
				aggregateStatus = flowBatchStatus;
			} else {
				splitStatus.setCouldMoreThanOneFlowHaveTerminatedJob(true);
				if (aggregateStatus.equals(ExtendedBatchStatus.JSL_END)) {
					logger.warning("Current flow's batch and exit status will take precedence over and override earlier one from <end> transition element. " + 
									"Overriding, setting exit status if non-null and preparing to end job.");
					setInJobContext(flowBatchStatus, exitStatus, restartOn);
					aggregateStatus = flowBatchStatus;
				} else if (aggregateStatus.equals(ExtendedBatchStatus.JSL_STOP)) {
					// Everything but an <end> overrides a <stop>
					if (!(flowBatchStatus.equals(ExtendedBatchStatus.JSL_END))) {
						logger.warning("Current flow's batch and exit status will take precedence over and override earlier one from <stop> transition element. " + 
										"Overriding, setting exit status if non-null and preparing to end job.");
						setInJobContext(flowBatchStatus, exitStatus, restartOn);
						aggregateStatus = flowBatchStatus;
					} else {
						logger.fine("End does not override stop.  The flow with <end> will effectively be ignored with respect to terminating the job.");
					}
				} else if (aggregateStatus.equals(ExtendedBatchStatus.JSL_FAIL) || aggregateStatus.equals(ExtendedBatchStatus.EXCEPTION_THROWN)) {
					if (flowBatchStatus.equals(ExtendedBatchStatus.JSL_FAIL) || flowBatchStatus.equals(ExtendedBatchStatus.EXCEPTION_THROWN)) {
						logger.warning("Current flow's batch and exit status will take precedence over and override earlier one from <fail> transition element or exception thrown. " + 
										"Overriding, setting exit status if non-null and preparing to end job.");
						setInJobContext(flowBatchStatus, exitStatus, restartOn);
						aggregateStatus = flowBatchStatus;
					} else {
						logger.fine("End and stop do not override exception thrown or <fail>.   The flow with <end> or <stop> will effectively be ignored with respect to terminating the job.");
					}
				}
			}
		} else {
			logger.fine("Flow completing normally without any terminating transition or exception thrown.");
		}
	}
	
	private void setInJobContext(ExtendedBatchStatus flowBatchStatus, String exitStatus, String restartOn) {
		if (exitStatus != null) {
			jobContext.setExitStatus(exitStatus);
		}			
		if (ExtendedBatchStatus.JSL_STOP.equals(flowBatchStatus)) {
			if (restartOn != null) {
				jobContext.setRestartOn(restartOn);
			}			
		}
	}
	
	public List<BatchFlowInSplitWorkUnit> getParallelJobExecs() {
		
		return parallelBatchWorkUnits;
	}

    @Override
    public List<Long> getLastRunStepExecutions() {
        
        List<Long> stepExecIdList = new ArrayList<Long>();
        
        for (BatchFlowInSplitWorkUnit workUnit : parallelBatchWorkUnits) {
            
            List<Long> stepExecIds = workUnit.getController().getLastRunStepExecutions();
            
            // Though this would have been one way to have a failure in a constituent flow
            // "bubble up" to a higher-level failure, let's not use this as the mechanism, so 
            // it's clearer how our transitioning logic functions.
            if (stepExecIds != null) {
            	stepExecIdList.addAll(stepExecIds);
            }
        }
        
        return stepExecIdList;
    }



}
