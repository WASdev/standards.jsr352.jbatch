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
package com.ibm.jbatch.container.status;

import javax.batch.operations.JobOperator.BatchStatus;

/*
 * This is just a holder for batch and exit status-like processing
 * try to force a batch status into the picture.
 * 
 * Rather than have a mirror of batch status like statuses, we use
 * this with a clear understanding that is an internal convenience object,
 * and is NOT exactly mirroring data visibly external to a JobOperator 
 * JobExecution/StepExecution function.
 * This allows us not to have to invent an internal
 * enum which is a lot like the regular BatchStatus.
 */
public class InternalExecutionElementStatus {

	protected BatchStatus batchStatus;
	protected String exitStatus;
	protected String restartOn;

	public String getRestartOn() {
		return restartOn;
	}

	public void setRestartOn(String restartOn) {
		this.restartOn = restartOn;
	}

	/**
	 * Don't set an exit status, batchStatus=COMPLETED
	 */
	public InternalExecutionElementStatus() {
		// OK to use this as a default since we made clear it is not  externally-meaningful.
		this.batchStatus = BatchStatus.COMPLETED;
	}
	
	// On this path, we're defaulting the exit status to the BatchStatus.
	// Again, since this is a placeholder, we shouldn't be too worried.   
	//
	// Presumably we are smart enough to set the exit status correctly in the
	// externally-visible places, and only use this for internal status.
	/**
	 * Set a batch status, default exit status to batch status name
	 */
	public InternalExecutionElementStatus(BatchStatus batchStatus) {
		this.batchStatus = batchStatus;
		this.exitStatus = batchStatus.name();
	}

	/**
	 * Set an exit status status, default batch status to COMPLETED
	 */
	public InternalExecutionElementStatus(String exitStatus) {
		this();
		this.exitStatus = exitStatus;
	}
	
	public InternalExecutionElementStatus(BatchStatus batchStatus, String exitStatus) {
		this.batchStatus = batchStatus;
		this.exitStatus = exitStatus;
	}

	public BatchStatus getBatchStatus() {
		return batchStatus;
	}
	public void setBatchStatus(BatchStatus batchStatus) {
		this.batchStatus = batchStatus;
	}
	public String getExitStatus() {
		return exitStatus;
	}
	public void setExitStatus(String exitStatus) {
		this.exitStatus = exitStatus;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("BatchStatus: " + batchStatus); 
		buf.append("\nExit Status: " + exitStatus);
		return buf.toString();
	}
}
