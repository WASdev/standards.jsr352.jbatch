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
package com.ibm.ws.batch.container.checkpoint;

import com.ibm.batch.container.services.IPersistenceDataKey;

public class CheckpointDataKey implements IPersistenceDataKey {
	
	private long _jobInstanceId;
	private String _batchDataStreamName;
	private String _stepName;
	
	public CheckpointDataKey(long jobId) {
		this._jobInstanceId = jobId;
		
	}
	
	public CheckpointDataKey(long jobId, String stepName, String bdsName) {
		this._jobInstanceId = jobId;
		this._stepName = stepName;
		this._batchDataStreamName = bdsName;
	}
	
	public long getJobInstanceId() {
		return _jobInstanceId;
	}
	public void setJobInstanceId(long id) {
		_jobInstanceId = id;
	}
	public String getBatchDataStreamName() {
		return _batchDataStreamName;
	}
	public void setBatchDataStreamName(String dataStreamName) {
		_batchDataStreamName = dataStreamName;
	}
	public String getStepName() {
		return _stepName;
	}
	public void setStepName(String name) {
		_stepName = name;
	}


	public String getCommaSeparatedKey() {
		return _jobInstanceId + "," + _stepName + "," + _batchDataStreamName;
	}

	public String toString() {
		return this.getKeyPrimitive();
	}

	@Override
	public String getKeyPrimitive() {
		return _jobInstanceId + "," + _stepName + "," + _batchDataStreamName; 
}
}
