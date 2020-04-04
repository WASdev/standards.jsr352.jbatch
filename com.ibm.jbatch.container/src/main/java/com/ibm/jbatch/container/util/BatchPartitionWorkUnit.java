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

import com.ibm.jbatch.container.impl.PartitionThreadRootControllerImpl;
import com.ibm.jbatch.container.jobinstance.RuntimeJobExecution;
import com.ibm.jbatch.container.services.IBatchKernelService;

public class BatchPartitionWorkUnit extends BatchParallelWorkUnit {

	public BatchPartitionWorkUnit(IBatchKernelService batchKernelService,
			RuntimeJobExecution jobExecution,
			PartitionsBuilderConfig config) {
		super(batchKernelService, jobExecution, true);
		this.completedThreadQueue = config.getCompletedQueue();
		this.controller = new PartitionThreadRootControllerImpl(jobExecution, config);
	}

	protected BlockingQueue<BatchPartitionWorkUnit> completedThreadQueue;

	public BlockingQueue<BatchPartitionWorkUnit> getCompletedThreadQueue() {
		return completedThreadQueue;
	}

	@Override
	protected void markThreadCompleted() {
		if (this.completedThreadQueue != null) {
			completedThreadQueue.add(this);
		}
	}
}
