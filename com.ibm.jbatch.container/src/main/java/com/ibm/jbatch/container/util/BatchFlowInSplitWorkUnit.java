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

import java.util.concurrent.BlockingQueue;

import com.ibm.jbatch.container.impl.FlowInSplitThreadRootControllerImpl;
import com.ibm.jbatch.container.jobinstance.RuntimeFlowInSplitExecution;
import com.ibm.jbatch.container.services.IBatchKernelService;

public class BatchFlowInSplitWorkUnit extends BatchParallelWorkUnit {

	public BatchFlowInSplitWorkUnit(IBatchKernelService batchKernelService,
			RuntimeFlowInSplitExecution jobExecution,
			FlowInSplitBuilderConfig config) {
		super(batchKernelService, jobExecution, true);
		this.completedThreadQueue = config.getCompletedQueue();
		this.controller = new FlowInSplitThreadRootControllerImpl(jobExecution, config);
	}

	protected BlockingQueue<BatchFlowInSplitWorkUnit> completedThreadQueue;

	public BlockingQueue<BatchFlowInSplitWorkUnit> getCompletedThreadQueue() {
		return completedThreadQueue;
	}

	@Override
	protected void markThreadCompleted() {
		if (this.completedThreadQueue != null) {
			completedThreadQueue.add(this);
		}
	}
	
	@Override
	public RuntimeFlowInSplitExecution getJobExecutionImpl() {
		return (RuntimeFlowInSplitExecution)jobExecutionImpl;
	}
}

