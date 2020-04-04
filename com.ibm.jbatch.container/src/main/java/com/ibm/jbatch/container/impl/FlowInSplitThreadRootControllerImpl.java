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

import com.ibm.jbatch.container.jobinstance.RuntimeFlowInSplitExecution;
import com.ibm.jbatch.container.status.ExtendedBatchStatus;
import com.ibm.jbatch.container.status.ExecutionStatus;
import com.ibm.jbatch.container.util.FlowInSplitBuilderConfig;

public class FlowInSplitThreadRootControllerImpl extends JobThreadRootControllerImpl {

	private final static String CLASSNAME = FlowInSplitThreadRootControllerImpl.class.getName();
	
	// Careful, we have a separately named reference to the same object in the parent class
	RuntimeFlowInSplitExecution flowInSplitExecution;
	
	public FlowInSplitThreadRootControllerImpl(RuntimeFlowInSplitExecution flowInSplitExecution, FlowInSplitBuilderConfig config) {
		super(flowInSplitExecution, config.getRootJobExecutionId());
		this.flowInSplitExecution = flowInSplitExecution;
	}
	
	@Override
	/**
	 * Not only are we setting the status correctly at the subjob level, we are also setting it on the execution
	 * so that it is visible by the parent split.
	 */
	public ExecutionStatus originateExecutionOnThread() {
		ExecutionStatus status = super.originateExecutionOnThread();
		flowInSplitExecution.setFlowStatus(status);
		return status;
	}
	
	@Override 
	protected void batchStatusFailedFromException() {
		super.batchStatusFailedFromException();
		flowInSplitExecution.getFlowStatus().setExtendedBatchStatus(ExtendedBatchStatus.EXCEPTION_THROWN);
	}
	
}
