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
package com.ibm.batch.container.jobinstance;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Properties;

import javax.batch.runtime.JobExecution;

import com.ibm.batch.container.context.impl.JobContextImpl;
import com.ibm.batch.container.services.IPersistenceManagerService;
import com.ibm.batch.container.services.ServicesManager;
import com.ibm.batch.container.services.ServicesManager.ServiceType;
import com.ibm.batch.container.services.impl.JDBCPersistenceManagerImpl;

public class JobOperatorJobExecutionImpl implements JobExecution {

	private static ServicesManager servicesManager = ServicesManager.getInstance();
    private static IPersistenceManagerService _persistenceManagementService = 
        (IPersistenceManagerService)servicesManager.getService(ServiceType.PERSISTENCE_MANAGEMENT_SERVICE);

    private long executionID = 0L;
    private long instanceID = 0L;
    
    Timestamp createTime;
    Timestamp startTime;
    Timestamp endTime;
    Timestamp updateTime;
    Properties parameters;
    String batchStatus;
    String exitStatus;
    
    private JobContextImpl<?> jobcontext = null;
    
	public JobOperatorJobExecutionImpl(long instanceId, long executionId, JobContextImpl<?> jobContext) {
		this.executionID = executionId;
		this.instanceID = instanceId;
		jobcontext = jobContext;
	}
	
	@Override
	public String getStatus() {
		
		//if (_persistenceManagementService instanceof JDBCPersistenceManagerImpl){
		//	batchStatus = ((JDBCPersistenceManagerImpl)_persistenceManagementService).jobOperatorQueryJobExecutionStatus(executionID, JDBCPersistenceManagerImpl.BATCH_STATUS);
		//}
		
		return this.jobcontext.getBatchStatus();
	}

	@Override
	public Timestamp getCreateTime() {
		// if and only if we are accessing the JDBC impl, get the requested timestamp
		if (_persistenceManagementService instanceof JDBCPersistenceManagerImpl){
			createTime = ((JDBCPersistenceManagerImpl)_persistenceManagementService).jobOperatorQueryJobExecutionTimestamp(executionID, JDBCPersistenceManagerImpl.CREATE_TIME);
		}
		
		return createTime;
	}

	@Override
	public Timestamp getEndTime() {
		// if and only if we are accessing the JDBC impl, get the requested timestamp
		if (_persistenceManagementService instanceof JDBCPersistenceManagerImpl){
			endTime = ((JDBCPersistenceManagerImpl)_persistenceManagementService).jobOperatorQueryJobExecutionTimestamp(executionID, JDBCPersistenceManagerImpl.END_TIME);
		}
		
		return endTime;
	}

	@Override
	public long getExecutionId() {
		// TODO Auto-generated method stub
		return executionID;
	}

	@Override
	public String getExitStatus() {
		//if (_persistenceManagementService instanceof JDBCPersistenceManagerImpl){
		//	exitStatus = ((JDBCPersistenceManagerImpl)_persistenceManagementService).jobOperatorQueryJobExecutionStatus(executionID, JDBCPersistenceManagerImpl.EXIT_STATUS);
		//}
		
		return this.jobcontext.getExitStatus();
	}

	// keep?
	public long getInstanceId() {
		// TODO Auto-generated method stub
		return instanceID;
	}

	@Override
	public Date getLastUpdatedTime() {
		// if and only if we are accessing the JDBC impl, get the requested timestamp
		if (_persistenceManagementService instanceof JDBCPersistenceManagerImpl){
			updateTime = ((JDBCPersistenceManagerImpl)_persistenceManagementService).jobOperatorQueryJobExecutionTimestamp(executionID, JDBCPersistenceManagerImpl.UPDATE_TIME);
		}
		
		return updateTime;
	}

	@Override
	public Date getStartTime() {
		
		// if and only if we are accessing the JDBC impl, get the requested timestamp
		if (_persistenceManagementService instanceof JDBCPersistenceManagerImpl){
			startTime = ((JDBCPersistenceManagerImpl)_persistenceManagementService).jobOperatorQueryJobExecutionTimestamp(executionID, JDBCPersistenceManagerImpl.START_TIME);
		}
		
		return startTime;
	}
	
	public void setBatchStatus(String status) {
		batchStatus = status;
	}

	public void setCreateTime(Timestamp ts) {
		createTime = ts;
	}

	public void setEndTime(Timestamp ts) {
		endTime = ts;
	}

	public void setExecutionId(long id) {
		executionID = id;
	}
	
	public void setJobInstanceId(long jobInstanceID){
		instanceID = jobInstanceID;
	}

	public void setExitStatus(String status) {
		exitStatus = status;
	
	}

	public void setInstanceId(long id) {
		instanceID = id;
	}

	public void setLastUpdateTime(Timestamp ts) {
		updateTime = ts;
	}

	public void setStartTime(Timestamp ts) {
		startTime = ts;
	}

	@Override
	public Properties getJobParameters() {
		// TODO Auto-generated method stub
		return null;
	}

}
