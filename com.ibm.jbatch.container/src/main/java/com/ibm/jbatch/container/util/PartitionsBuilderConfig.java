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
import java.util.Properties;
import java.util.concurrent.BlockingQueue;

import com.ibm.jbatch.jsl.model.JSLJob;

public class PartitionsBuilderConfig {

	private List<JSLJob> jobModels;
    private Properties[] partitionProperties;
    private BlockingQueue<PartitionDataWrapper> analyzerQueue;
    private BlockingQueue<BatchPartitionWorkUnit> completedQueue;
    private long rootJobExecutionId;
    
	public PartitionsBuilderConfig(List<JSLJob> jobModels,
			Properties[] partitionProperties,
			BlockingQueue<PartitionDataWrapper> analyzerQueue,
			BlockingQueue<BatchPartitionWorkUnit> completedQueue, 
			long rootJobExecutionId) {
		super();
		this.jobModels = jobModels;
		this.partitionProperties = partitionProperties;
		this.analyzerQueue = analyzerQueue;
		this.completedQueue = completedQueue;
		this.rootJobExecutionId = rootJobExecutionId;
	}
    
	public long getRootJobExecutionId() {
		return rootJobExecutionId;
	}

    public List<JSLJob> getJobModels() {
		return jobModels;
	}
	public void setJobModels(List<JSLJob> jobModels) {
		this.jobModels = jobModels;
	}
	public Properties[] getPartitionProperties() {
		return partitionProperties;
	}
	public void setPartitionProperties(Properties[] partitionProperties) {
		this.partitionProperties = partitionProperties;
	}
	public BlockingQueue<PartitionDataWrapper> getAnalyzerQueue() {
		return analyzerQueue;
	}
	public void setAnalyzerQueue(BlockingQueue<PartitionDataWrapper> analyzerQueue) {
		this.analyzerQueue = analyzerQueue;
	}
	public BlockingQueue<BatchPartitionWorkUnit> getCompletedQueue() {
		return completedQueue;
	}
	public void setCompletedQueue(
			BlockingQueue<BatchPartitionWorkUnit> completedQueue) {
		this.completedQueue = completedQueue;
	}


	

}
