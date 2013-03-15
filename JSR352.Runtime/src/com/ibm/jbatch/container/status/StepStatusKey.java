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

public class StepStatusKey {
	
	private long _jobInstanceId;
	
	/**
	 * Not public since we don't want the instance id getter public,
	 * Only for use in trace.
	 * 
	 * @return 
	 */
	private String getStepId() {
		return _stepId;
	}

	private String _stepId;
	
	public StepStatusKey(long jobInstanceId, String stepId) {		
		_jobInstanceId = jobInstanceId;
		_stepId = stepId;
	}
	
	/**
	 * Note this is the only getter method, to enforce consistency
	 * in getting the instance ID as key.
	 * 
	 * @return jobInstanceId
	 */
	public long getDatabaseKey() {
		return _jobInstanceId;
	}
	
	public String toString() {		
	    return Long.toString(_jobInstanceId) + ":" + getStepId();
	}
}
