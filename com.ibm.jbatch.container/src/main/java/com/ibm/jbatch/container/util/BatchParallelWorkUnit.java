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

import com.ibm.jbatch.container.jobinstance.RuntimeJobExecution;
import com.ibm.jbatch.container.services.IBatchKernelService;

/*
 * I took out the 'work type' constant since I don't see that we want to use
 * the same thread pool for start requests as we'd use for stop requests.
 * The stop seems like it should be synchronous from the JobOperator's
 * perspective, as it returns a 'success' boolean.
 */
public abstract class BatchParallelWorkUnit extends BatchWorkUnit {

	public BatchParallelWorkUnit(IBatchKernelService batchKernel, RuntimeJobExecution jobExecutionImpl,	boolean notifyCallbackWhenDone) {
		super(batchKernel, jobExecutionImpl, notifyCallbackWhenDone);
	}

}
