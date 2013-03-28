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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.impl.JobControllerImpl;
import com.ibm.jbatch.container.jobinstance.RuntimeJobContextJobExecutionBridge;
import com.ibm.jbatch.container.services.IBatchKernelService;

/*
 * I took out the 'work type' constant since I don't see that we want to use
 * the same thread pool for start requests as we'd use for stop requests.
 * The stop seems like it should be synchronous from the JobOperator's
 * perspective, as it returns a 'success' boolean.
 */
public class BatchParallelWorkUnit extends BatchWorkUnit {

	private String CLASSNAME = BatchParallelWorkUnit.class.getName();
	private Logger logger = Logger.getLogger(BatchParallelWorkUnit.class.getPackage().getName());

	private final JobControllerImpl controller;

	private BlockingQueue<PartitionDataWrapper> analyzerQueue;
	private BlockingQueue<BatchParallelWorkUnit> completedThreadQueue;

	private RuntimeJobContextJobExecutionBridge rootJobExecution = null;

	public BatchParallelWorkUnit(IBatchKernelService batchKernel, RuntimeJobContextJobExecutionBridge jobExecutionImpl) {
		this(batchKernel, jobExecutionImpl, null, null, jobExecutionImpl, true);
	}

	public BatchParallelWorkUnit(IBatchKernelService batchKernel, RuntimeJobContextJobExecutionBridge jobExecutionImpl,
			BlockingQueue<PartitionDataWrapper> analyzerQueue, BlockingQueue<BatchParallelWorkUnit> completedThreadQueue, 
			RuntimeJobContextJobExecutionBridge rootJobExecution,
			boolean notifyCallbackWhenDone) {
		super(batchKernel, jobExecutionImpl, notifyCallbackWhenDone);
		this.setAnalyzerQueue(analyzerQueue);
		this.setCompletedThreadQueue(completedThreadQueue);

		//if root is null we don't want to find the children on a query
		//this is only for partitioned steps since the partitioned steps are only seen internally
		//externally it is still considered only 1 step
		if (rootJobExecution == null) {
			this.setRootJobExecution(jobExecutionImpl);
		} else {
			this.setRootJobExecution(rootJobExecution);
		}

		controller = new JobControllerImpl(this.getJobExecutionImpl(), this.rootJobExecution);
		controller.setAnalyzerQueue(this.analyzerQueue);
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
			
			if (isNotifyCallbackWhenDone()) {
				getBatchKernel().jobExecutionDone(getJobExecutionImpl());
			}
			
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("==========================================================");
				logger.fine("Done invoking executeJob on JobController; " + "JobInstance id=" + getJobExecutionImpl().getInstanceId()
						+ ", executionId=" + getJobExecutionImpl().getExecutionId());
				logger.fine("Job Batch Status = " + getBatchStatus() + ";  Job Exit Status = "
						+ getExitStatus());
				logger.fine("==========================================================");
			}

		} catch (Throwable t) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			t.printStackTrace(pw);
			logger.warning("Caught throwable from run().  Stack trace: " + sw.toString());
			
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Exception when invoking executeJob on JobController; " + "JobInstance id="
						+ getJobExecutionImpl().getInstanceId() + ", executionId=" + getJobExecutionImpl().getExecutionId());
				logger.fine("Job Batch Status = " + getBatchStatus() + ";  Job Exit Status = "
						+ getExitStatus());
			}

			if (isNotifyCallbackWhenDone()) {
				getBatchKernel().jobExecutionDone(getJobExecutionImpl());
			}

			throw new BatchContainerRuntimeException("This job failed unexpectedly.", t);
		}  finally {
			// Put this in finally to minimize chance of tying up threads.
			if (this.completedThreadQueue != null) {
				completedThreadQueue.add(this);
			}
		}

		if (logger.isLoggable(Level.FINER)) {
			logger.exiting(CLASSNAME, method);
		}
	}

	public BlockingQueue<PartitionDataWrapper> getAnalyzerQueue() {
		return analyzerQueue;
	}

	public void setAnalyzerQueue(BlockingQueue<PartitionDataWrapper> analyzerQueue) {
		this.analyzerQueue = analyzerQueue;
	}

	public BlockingQueue<BatchParallelWorkUnit> getCompletedThreadQueue() {
		return completedThreadQueue;
	}

	public void setCompletedThreadQueue(BlockingQueue<BatchParallelWorkUnit> completedThreadQueue) {
		this.completedThreadQueue = completedThreadQueue;
	}

	public RuntimeJobContextJobExecutionBridge getRootJobExecution() {
		return rootJobExecution;
	}

	public void setRootJobExecution(RuntimeJobContextJobExecutionBridge rootJobExecution) {
		this.rootJobExecution = rootJobExecution;
	}

}
