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

import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.batch.operations.JobExecutionNotMostRecentException;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.JobStartException;
import javax.batch.operations.NoSuchJobExecutionException;

import com.ibm.jbatch.container.IController;
import com.ibm.jbatch.container.context.impl.JobContextImpl;
import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.jobinstance.RuntimeFlowInSplitExecution;
import com.ibm.jbatch.container.jobinstance.RuntimeJobExecution;
import com.ibm.jbatch.container.services.IBatchKernelService;
import com.ibm.jbatch.container.servicesmanager.ServicesManager;
import com.ibm.jbatch.container.servicesmanager.ServicesManagerImpl;
import com.ibm.jbatch.container.status.JobOrFlowBatchStatus;
import com.ibm.jbatch.container.status.JobOrFlowStatus;
import com.ibm.jbatch.container.status.SplitStatus;
import com.ibm.jbatch.container.util.BatchFlowInSplitWorkUnit;
import com.ibm.jbatch.container.util.BatchParallelWorkUnit;
import com.ibm.jbatch.container.util.FlowInSplitBuilderConfig;
import com.ibm.jbatch.jsl.model.Flow;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.jbatch.jsl.model.Split;

public class SplitControllerImpl implements IController {

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
					try {
						batchKernel.stopJob(subJob.getJobExecutionImpl().getExecutionId());
					} catch (Exception e) {
						// TODO - Is this what we want to know.  
						// Blow up if it happens to force the issue.
						throw new IllegalStateException(e);
					}
				}
			}
		}
	}

	public SplitStatus execute() throws JobRestartException, JobStartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException, NoSuchJobExecutionException {
		String sourceMethod = "execute";
		if (logger.isLoggable(Level.FINER)) {
			logger.entering(sourceClass, sourceMethod, "Root JobExecution Id = " + rootJobExecutionId);
		}

		// Build all sub jobs from partitioned step
		buildSubJobBatchWorkUnits();

		// kick off the threads
		executeWorkUnits();

		// Deal with the results.
		SplitStatus status = waitForCompletionAndAggregateStatus();

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
				subJobs.add(PartitionedStepBuilder.buildFlowInSplitSubJob(jobExecution.getExecutionId(), jobContext, this.split, flow));
			}
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

	private SplitStatus waitForCompletionAndAggregateStatus() {

		SplitStatus splitStatus = new SplitStatus();
		JobOrFlowBatchStatus aggregateTerminatingStatus = null;

		for (int i=0; i < subJobs.size(); i++) {
			BatchFlowInSplitWorkUnit batchWork;
			try {
				batchWork = completedWorkQueue.take(); //wait for each thread to finish and then look at it's status
			} catch (InterruptedException e) {
				throw new BatchContainerRuntimeException(e);
			}

			RuntimeFlowInSplitExecution flowExecution = batchWork.getJobExecutionImpl();
			JobOrFlowStatus flowStatus = flowExecution.getFlowStatus();
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Subjob " + flowExecution.getExecutionId() + "ended with flow-in-split status: " + flowStatus);
			}
			aggregateTerminatingStatusFromSingleFlow(aggregateTerminatingStatus, flowStatus, splitStatus);
		}
		
		// If this is still set to 'null' that means all flows completed normally without terminating the job.
		if (aggregateTerminatingStatus == null) {
			logger.fine("Setting normal split status as no contained flows ended the job.");
			aggregateTerminatingStatus = JobOrFlowBatchStatus.NORMAL_COMPLETION;
		}

		splitStatus.setDeterminingFlowBatchStatus(aggregateTerminatingStatus);
		logger.fine("Returning from waitForCompletionAndAggregateStatus with return value: " + splitStatus);
		return splitStatus;
	}


	//
	// Fail(s) will take precedence over Stop(s) which in turn take precedence over End(s).  Within fails, stops and ends, the 
	// first exit status takes precedence. 
	//
	private void aggregateTerminatingStatusFromSingleFlow(JobOrFlowBatchStatus aggregateStatus, JobOrFlowStatus flowStatus, SplitStatus splitStatus) {

		String exitStatus = flowStatus.getExitStatus();
		String restartOn = flowStatus.getRestartOn();
		
		logger.fine("Aggregating possible terminating status for flow ending with status: " + flowStatus + ", and exitStatus = " + exitStatus + ", restartOn = " + restartOn);
		
		if (flowStatus.equals(JobOrFlowBatchStatus.EXCEPTION_THROWN)) {
			//
			// Exception thrown and JSL fail are peers in the precedence rules here.   The exception case doesn't result in an exit status being set
			// while the JSL_FAIL might, though since they are peers, a JSL_FAIL happening after an exception throw will NOT result in the JSL_FAIL
			// @exit-status being set. 
			//
			if (aggregateStatus == null) {
				logger.fine("A flow detected as FAILED with exception thrown. First flow detected in terminating state.");
				aggregateStatus = JobOrFlowBatchStatus.EXCEPTION_THROWN;
			} else {
				if (!aggregateStatus.equals(JobOrFlowBatchStatus.EXCEPTION_THROWN)) {
					logger.warning("Another flow already reached a terminating state for the job. The exception thrown will take precedence.");
					splitStatus.setCouldMoreThanOneFlowHaveTerminatedJob(true);
				}
				aggregateStatus = JobOrFlowBatchStatus.EXCEPTION_THROWN;
			}
		} else if (flowStatus.equals(JobOrFlowBatchStatus.JSL_FAIL)) {
			if (aggregateStatus == null) {
				logger.fine("A flow detected as FAILED because of a <fail> transition element. First flow detected in terminating state.  Setting exitStatus if non-null.");
				if (exitStatus != null) {
					jobContext.setExitStatus(exitStatus);
				}
				aggregateStatus = JobOrFlowBatchStatus.JSL_FAIL;
			} else {
				splitStatus.setCouldMoreThanOneFlowHaveTerminatedJob(true);
				if (aggregateStatus.equals(JobOrFlowBatchStatus.EXCEPTION_THROWN) || aggregateStatus.equals(JobOrFlowBatchStatus.JSL_FAIL)) {
					logger.warning("A flow detected as FAILED because of a <fail> transition element, after having already seen a previous failure.  Will NOT overwrite earlier exit status.");
				} else {
					logger.warning("Another flow already reached a terminating state for the job. But the fail element will take precedence.  Overwriting exitStatus if non-null");
					if (exitStatus != null) {
						jobContext.setExitStatus(exitStatus);
					}
					aggregateStatus = JobOrFlowBatchStatus.JSL_FAIL;
				}
			}
		} else if (flowStatus.equals(JobOrFlowBatchStatus.JSL_STOP)){
			if (aggregateStatus == null) {
				logger.fine("A flow detected as stopped because of a <stop> transition element. First flow detected in terminating state.  Setting exitStatus, restartOn if non-null.");
				if (exitStatus != null) {
					jobContext.setExitStatus(exitStatus);
				}
				if (restartOn != null) {
					jobContext.setRestartOn(restartOn);
				}
				aggregateStatus = JobOrFlowBatchStatus.JSL_STOP;
			} else {
				splitStatus.setCouldMoreThanOneFlowHaveTerminatedJob(true);
				if (aggregateStatus.equals(JobOrFlowBatchStatus.EXCEPTION_THROWN) || aggregateStatus.equals(JobOrFlowBatchStatus.JSL_FAIL) || aggregateStatus.equals(JobOrFlowBatchStatus.JSL_STOP)) {
					logger.warning("A flow detected as stopped because of a <stop> transition element, after having already seen a previous failure.  Will NOT overwrite earlier exit status.");
				} else {
					logger.warning("Another flow already reached a terminating state for the job. But the stop element will take precedence.  Overwriting exitStatus if non-null");
					if (exitStatus != null) {
						jobContext.setExitStatus(exitStatus);
					}
					if (restartOn != null) {
						jobContext.setRestartOn(restartOn);
					}
					aggregateStatus = JobOrFlowBatchStatus.JSL_STOP;
				}
			}
		} else if (flowStatus.equals(JobOrFlowBatchStatus.JSL_END)){
			if (aggregateStatus == null) {
				logger.fine("A flow detected as ended because of a <end> transition element. First flow detected in terminating state.  Setting exitStatus if non-null.");
				if (exitStatus != null) {
					jobContext.setExitStatus(exitStatus);
				}			
				aggregateStatus = JobOrFlowBatchStatus.JSL_END;
			} else {
				splitStatus.setCouldMoreThanOneFlowHaveTerminatedJob(true);
				logger.warning("A flow detected as stopped because of a <stop> transition element, after having already seen a previous failure.  Will NOT overwrite earlier exit status.");
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
            
            stepExecIdList.addAll(stepExecIds);
        }
        
        return stepExecIdList;
    }



}
