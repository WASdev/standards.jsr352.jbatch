/*
 * Copyright 2013 International Business Machines Corp.
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
package com.ibm.jbatch.tck.spi;

import javax.batch.operations.JobOperator;

/**
 * Factory class for JobExecutionWaiter
 */
public interface JobExecutionWaiterFactory {
	/**
	 * Create a waiter to wait for JobExecution to reach a "final" state.
	 * 
	 * <p>
	 * For discussion of "final" states, 
	 * @see JobExecutionWaiter#awaitTermination
	 * 
	 * @param executionId JobExecution id of the execution to wait for.
	 * @param jobOp Reference to JobOperator instance used to get execution id. Note the exact instance
	 * shouldn't matter, i.e. getting a new JobOperator reference should probably result in the same
	 * results as passing in this instance.   The API contract doesn't attempt to say anything more
	 * on this subject.
	 * @param sleepTime Time to wait, in milliseconds for job execution to reach a "final" state.
	 * @return waiter instance
	 */
	public JobExecutionWaiter createWaiter(long executionId, JobOperator jobOp, long sleepTime);
}
