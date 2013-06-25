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

import javax.batch.runtime.JobExecution;

/**
 * Waiter to wait for a JobExecution to reach a "final" state, (i.e. to block until it does).
 */
public interface JobExecutionWaiter {
	/**
	 * The waiter instance is associated with an execution id via the factory create method.	
	 * 
	 * <p>
	 * This method blocks and only returns when the JobExecution reaches one of these states,
	 * which we refer to here as a 'final' state (By 'state' we mean "batch status").  The JSR 352 
	 * specification doesn't formally define a set of 'final' states or use this term explicitly, so 
	 * we list here what we consider to be the 'final' states for the TCK purposes.  
	 * 
	 * <p>
	 * Final states:
	 * <ul>
	 *  <li>ABANDONED
	 *  <li>COMPLETED
	 *  <li>FAILED
	 *  <li>STOPPED
	 * </ul>
	 * 
	 * <p>(This seems like an obvious, uncontroversial interpretation):
	 * 
	 * @return JobExecution instance (based on JobExecution specified in factory create method).
	 * @throws JobExecutionTimeoutException Thrown when JobExecution hasn't reached final state after 
	 * the timeout specified in the factory create method.
	 * 
	 * @see JobExecutionWaiterFactory#createWaiter JobExecutionWaiterFactory.createWaiter(...)
	 */
	JobExecution awaitTermination() throws JobExecutionTimeoutException;
}
