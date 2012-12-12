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
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
/**
 * 
 */
public class CheckpointData implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private long _jobInstanceId;
	private String _batchDataStreamName;
	private String _stepName;
	private byte[] _restartToken;
	
	public CheckpointData (
			long jobInstanceId,
		String stepname,
		String batchDataStreamName) {
		if(stepname != null && batchDataStreamName != null) {
			_jobInstanceId = jobInstanceId;
			_batchDataStreamName = batchDataStreamName;
			_stepName = stepname;
			try {
				_restartToken = new String("NOTSET").getBytes("UTF8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException("Doesn't support UTF-8", e);
			}
		} else {
			throw new RuntimeException("Invalid parameters to CheckpointData jobInstanceId: " + _jobInstanceId + 
					" BDS: " + batchDataStreamName + " stepName: " + stepname);
		}
	}

	public long getjobInstanceId() {
		return _jobInstanceId;
	}

	public void setjobInstanceId(long id) {
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

	public byte[] getRestartToken() {
		return _restartToken;
	}

	public void setRestartToken(byte[] token) {
		_restartToken = token;
	}
	
	public String toString() {
		String restartString = null;
		try {
			restartString = new String(this._restartToken, "UTF8");
		} catch (UnsupportedEncodingException e) {
			restartString = "<bytes not UTF-8>";
		}
		return " jobInstanceId: " + _jobInstanceId + " stepId: " + this._stepName + " bdsName: " + this._batchDataStreamName +
		" restartToken: [UTF8-bytes: " + restartString;
		
	}
	
	
	
}

