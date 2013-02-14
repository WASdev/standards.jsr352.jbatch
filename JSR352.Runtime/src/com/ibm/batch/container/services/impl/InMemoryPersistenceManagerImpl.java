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
package com.ibm.batch.container.services.impl;

import java.sql.Timestamp;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import javax.batch.runtime.JobExecution;
import javax.batch.runtime.StepExecution;

import com.ibm.batch.container.context.impl.StepContextImpl;
import com.ibm.batch.container.exception.BatchContainerServiceException;
import com.ibm.batch.container.services.IPersistenceManagerService;
import com.ibm.batch.container.status.JobStatus;
import com.ibm.batch.container.status.StepStatus;
import com.ibm.ws.batch.container.checkpoint.CheckpointData;

public class InMemoryPersistenceManagerImpl extends AbstractMapBasedPersistenceManagerImpl implements IPersistenceManagerService {

	//private static final String CLASSNAME = InMemoryPersistenceManagerImpl.class.getName();
	//private static Logger logger = Logger.getLogger(InMemoryPersistenceManagerImpl.class.getPackage().getName());;
	//private static boolean _isInited = false;
	
	
	public InMemoryPersistenceManagerImpl() {
		
	}

	protected void _loadDataStores() {
	
		_jobStatusStore = new Hashtable<Long,JobStatus>();
		
		_stepStatusStore = new Hashtable<String,StepStatus>();
		
		_checkpointStore = new Hashtable<String,CheckpointData>();
	}


	protected void _saveStore(int storeId) {
		// DO NOTHING
		
		
	}

	

	public void shutdown() throws BatchContainerServiceException {
		// TODO Auto-generated method stub
		
	}
	
	public String toString() {
	    StringBuffer buf = new StringBuffer(200);
	    buf.append("Job Status");
	    buf.append("----------");
	    for (Entry<Long, JobStatus> e : _jobStatusStore.entrySet()) {
	        buf.append ("Key: " + e.getKey() + " , Value: " + e.getValue());	        
	    }
	    buf.append("----------");
	    buf.append("Step Status");
        buf.append("----------");
        for (Entry<String, StepStatus> e : _stepStatusStore.entrySet()) {
            buf.append ("Key: \n" + e.getKey() + " , Value: " + e.getValue());            
        } 
        buf.append("----------");
	    buf.append("Checkpoint Data");
        buf.append("----------");
        for (Entry<String, CheckpointData> e : _checkpointStore.entrySet()) {
            buf.append ("Key: " + e.getKey() + " , Value: " + e.getValue());            
        } 
	    return buf.toString();
	    
	}


	@Override
	public int jobOperatorGetJobInstanceCount(String jobName) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long jobOperatorQueryJobExecutionJobInstanceId(long executionID) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String jobOperatorQueryJobExecutionStatus(long executionID,
			String requestedStatus) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Timestamp jobOperatorQueryJobExecutionTimestamp(long executionID,
			String timetype) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Long> jobOperatorgetJobInstanceIds(String jobName, int start,
			int count) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> jobOperatorgetJobNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void jobExecutionStatusStringUpdate(long key, String statusToUpdate,
			String statusString, Timestamp updatets) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void jobExecutionTimestampUpdate(long key, String timestampToUpdate,
			Timestamp ts) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void jobOperatorCreateExecutionData(long key, Timestamp createTime,
			Timestamp starttime, Timestamp endtime, Timestamp updateTime,
			Properties parms, long instanceID, String batchstatus,
			String exitstatus) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void jobOperatorCreateJobInstanceData(long key, String jobNameValue) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stepExecutionCreateStepExecutionData(String stepExecutionKey,
			long jobExecutionID, StepContextImpl stepContext) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public JobExecution jobOperatorGetJobExecution(long jobExecutionId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void jobOperatorUpdateBatchStatusWithSTATUSandUPDATETSonly(long key,
			String statusToUpdate, String statusString, Timestamp updatets) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void jobOperatorUpdateBatchStatusWithUPDATETSonly(long key,
			String statusToUpdate, String statusString, Timestamp updatets) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<JobExecution> jobOperatorGetJobExecutionsByJobInstanceID(
			long jobInstanceID) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<JobExecution> jobOperatorGetJobExecutions(long jobInstanceId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Long> jobOperatorGetRunningInstances(String jobName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<StepExecution> getStepExecutionIDListQueryByJobID(long execid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StepExecution getStepExecutionObjQueryByStepID(long stepexecutionId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Properties getParameters(long instanceId) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
