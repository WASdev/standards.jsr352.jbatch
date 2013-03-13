/**
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
package com.ibm.jbatch.container.util;

import java.util.List;
import java.util.Stack;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator.BatchStatus;

import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.impl.JobControllerImpl;
import com.ibm.jbatch.container.jobinstance.RuntimeJobExecutionHelper;
import com.ibm.jbatch.container.services.IBatchKernelService;

/*
 * I took out the 'work type' constant since I don't see that we want to use
 * the same thread pool for start requests as we'd use for stop requests.
 * The stop seems like it should be synchronous from the JobOperator's
 * perspective, as it returns a 'success' boolean.
 */
public class BatchWorkUnit implements Runnable {

	private String CLASSNAME = BatchWorkUnit.class.getName();
	private Logger logger = Logger.getLogger(BatchWorkUnit.class.getPackage().getName());

	private RuntimeJobExecutionHelper jobExecutionImpl = null;
	private IBatchKernelService batchKernel = null;
	private final JobControllerImpl controller;

	private BlockingQueue<PartitionDataWrapper> analyzerQueue;
	private Stack<String> subJobExitStatusQueue;
	private BlockingQueue<BatchWorkUnit> completedThreadQueue;
	private boolean notifyCallbackWhenDone;
	
	private List<String> containment = null;
	
	private RuntimeJobExecutionHelper rootJobExecution = null;

	public BatchWorkUnit(IBatchKernelService batchKernel, RuntimeJobExecutionHelper jobExecutionImpl) {
		this(batchKernel, jobExecutionImpl, null, null, null, null, jobExecutionImpl, true);
	}

    public BatchWorkUnit(IBatchKernelService batchKernel, RuntimeJobExecutionHelper jobExecutionImpl,
            BlockingQueue<PartitionDataWrapper> analyzerQueue, Stack<String> subJobExitStatusQueue,
            BlockingQueue<BatchWorkUnit> completedThreadQueue, List<String> containment, RuntimeJobExecutionHelper rootJobExecution,
            boolean notifyCallbackWhenDone) {
        this.setBatchKernel(batchKernel);
        this.setJobExecutionImpl(jobExecutionImpl);
        this.setAnalyzerQueue(analyzerQueue);
        this.setSubJobExitStatus(subJobExitStatusQueue);
        this.setCompletedThreadQueue(completedThreadQueue);
        this.setNotifyCallbackWhenDone(notifyCallbackWhenDone);
        this.setContainment(containment);
        
        //if root is null we don't want to find the children on a query
        //this is only for partitioned steps since the partitioned steps are only seen internally
        //externally it is still considered only 1 step
        if (rootJobExecution == null) {
            this.setRootJobExecution(jobExecutionImpl);
        } else {
            this.setRootJobExecution(rootJobExecution);
        }
        

        controller = new JobControllerImpl(this.getJobExecutionImpl(), this.containment, this.rootJobExecution);
        controller.setAnalyzerQueue(this.analyzerQueue);
        controller.setSubJobExitStatusQueue(subJobExitStatusQueue);

    }

	public JobControllerImpl getController() {
		return this.controller;
	}

	@Override
	public void run() {
		String method = "run";
		if (logger.isLoggable(Level.FINER)) {
			logger.entering(CLASSNAME, method);
		}

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("==========================================================");
			logger.fine("Invoking executeJob on JobController; " + "JobInstance id=" + getJobExecutionImpl().getInstanceId()
					+ ", executionId=" + getJobExecutionImpl().getExecutionId());
			logger.fine("==========================================================");
		}




		try {
			controller.executeJob();
		} catch (Exception e) {
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Exception when invoking executeJob on JobController; " + "JobInstance id="
						+ getJobExecutionImpl().getInstanceId() + ", executionId=" + getJobExecutionImpl().getExecutionId());
				logger.fine("Job Batch Status = " + getBatchStatus() + ";  Job Exit Status = "
						+ getExitStatus());
			}


			if (isNotifyCallbackWhenDone()) {
				getBatchKernel().jobExecutionDone(getJobExecutionImpl());
			}

	        if (this.completedThreadQueue != null) {
	            completedThreadQueue.add(this);
	        }
			
			throw new BatchContainerRuntimeException("This job failed unexpectedly.", e);

		} 
		
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("==========================================================");
			logger.fine("Done invoking executeJob on JobController; " + "JobInstance id=" + getJobExecutionImpl().getInstanceId()
					+ ", executionId=" + getJobExecutionImpl().getExecutionId());
			logger.fine("Job Batch Status = " + getBatchStatus() + ";  Job Exit Status = "
					+ getExitStatus());
			logger.fine("==========================================================");
		}

		if (isNotifyCallbackWhenDone()) {
			getBatchKernel().jobExecutionDone(getJobExecutionImpl());
		}

        if (this.completedThreadQueue != null) {
            completedThreadQueue.add(this);
        }

		if (logger.isLoggable(Level.FINER)) {
			logger.exiting(CLASSNAME, method);
		}
	}



	private BatchStatus getBatchStatus() {
		return jobExecutionImpl.getJobContext().getBatchStatus();
	}

	private String getExitStatus() {
		return jobExecutionImpl.getJobContext().getExitStatus();
	}

	public void setBatchKernel(IBatchKernelService batchKernel) {
		this.batchKernel = batchKernel;
	}

	public IBatchKernelService getBatchKernel() {
		return batchKernel;
	}

	public void setJobExecutionImpl(RuntimeJobExecutionHelper jobExecutionImpl) {
		this.jobExecutionImpl = jobExecutionImpl;
	}

	public RuntimeJobExecutionHelper getJobExecutionImpl() {
		return jobExecutionImpl;
	}

	public void setNotifyCallbackWhenDone(boolean notifyCallbackWhenDone) {
		this.notifyCallbackWhenDone = notifyCallbackWhenDone;
	}

	public boolean isNotifyCallbackWhenDone() {
		return notifyCallbackWhenDone;
	}

    public BlockingQueue<PartitionDataWrapper> getAnalyzerQueue() {
        return analyzerQueue;
    }

    public void setAnalyzerQueue(BlockingQueue<PartitionDataWrapper> analyzerQueue) {
        this.analyzerQueue = analyzerQueue;
    }
    
    public Stack<String> getSubJobExitStatusQueue() {
        return subJobExitStatusQueue;
    }

    public void setSubJobExitStatus(Stack<String> subJobExitStatusQueue2) {
        this.subJobExitStatusQueue = subJobExitStatusQueue2;
    }

    public BlockingQueue<BatchWorkUnit> getCompletedThreadQueue() {
        return completedThreadQueue;
    }

    public void setCompletedThreadQueue(BlockingQueue<BatchWorkUnit> completedThreadQueue) {
        this.completedThreadQueue = completedThreadQueue;
    }

    public List<String> getContainment() {
        return containment;
    }

    public void setContainment(List<String> containment) {
        this.containment = containment;
    }

    public RuntimeJobExecutionHelper getRootJobExecution() {
        return rootJobExecution;
    }

    public void setRootJobExecution(RuntimeJobExecutionHelper rootJobExecution) {
        this.rootJobExecution = rootJobExecution;
    }

}
