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
package com.ibm.jbatch.container.status;

public class JobOrFlowStatus {
	boolean batchStatusOnly = true;
	private JobOrFlowBatchStatus batchStatus;
	private String exitStatus;
	private String restartOn;
	
	public boolean isBatchStatusOnly() {
		return batchStatusOnly;
	}

	public JobOrFlowStatus(JobOrFlowBatchStatus batchStatus) {
		this.batchStatus = batchStatus;
	}
	
	public JobOrFlowStatus(JobOrFlowBatchStatus batchStatus, String exitStatus) {
		super();
		this.batchStatus = batchStatus;
		this.exitStatus = exitStatus;
		this.batchStatusOnly = false;
	}
	
	public JobOrFlowBatchStatus getBatchStatus() {
		return batchStatus;
	}
	public void setBatchStatus(JobOrFlowBatchStatus batchStatus) {
		this.batchStatus = batchStatus;
	}
	public String getExitStatus() {
		return exitStatus;
	}
	
	public void setExitStatus(String exitStatus) {
		this.exitStatus = exitStatus;
		this.batchStatusOnly = false;
	}
	
	public String getRestartOn() {
		return restartOn;
	}

	public void setRestartOn(String restartOn) {
		this.restartOn = restartOn;
	}
	
	@Override
	public String toString() {
		return "BatchStatusOnly?: " + batchStatusOnly + ", batchStatus = " + batchStatus.name() +
				", exitStatus = " + exitStatus + 
			    ", restartOn = " + restartOn;	
			
		
	}

}
