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
package com.ibm.jbatch.container.jobinstance;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator.BatchStatus;

import com.ibm.jbatch.container.context.impl.JobContextImpl;
import com.ibm.jbatch.container.services.IJobExecution;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.services.impl.JDBCPersistenceManagerImpl;
import com.ibm.jbatch.container.servicesmanager.ServicesManager;
import com.ibm.jbatch.container.servicesmanager.ServicesManagerImpl;
import com.ibm.jbatch.spi.TaggedJobExecution;

public class JobOperatorJobExecutionImpl implements IJobExecution, TaggedJobExecution {

    private final static String sourceClass = JobOperatorJobExecutionImpl.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);
    
	private static ServicesManager servicesManager = ServicesManagerImpl.getInstance();
    private static IPersistenceManagerService _persistenceManagementService = servicesManager.getPersistenceManagerService();

    private long executionID = 0L;
    private long instanceID = 0L;
    
    Timestamp createTime;
    Timestamp startTime;
    Timestamp endTime;
    Timestamp updateTime;
    Properties parameters;
    String batchStatus;
    String exitStatus;
    Properties jobProperties = null;
    
    private JobContextImpl<?> jobcontext = null;
    
	public JobOperatorJobExecutionImpl(long executionId, long instanceId, JobContextImpl<?> jobContext) {
		this.executionID = executionId;
		this.instanceID = instanceId;
		jobcontext = jobContext;
	}
	
	@Override
	public BatchStatus getBatchStatus() {
		
		BatchStatus batchStatus  = null;
		
		if (this.jobcontext != null){
			batchStatus = this.jobcontext.getBatchStatus();
			if (logger.isLoggable(Level.FINE)) {            
				logger.fine("Returning batch status of: " + batchStatus + " from JobContext.");
			}
		}
		else {
			// old job, retrieve from the backend
			if (_persistenceManagementService instanceof JDBCPersistenceManagerImpl){
				batchStatus = BatchStatus.valueOf(((JDBCPersistenceManagerImpl)_persistenceManagementService).jobOperatorQueryJobExecutionStatus(executionID, JDBCPersistenceManagerImpl.BATCH_STATUS));
			} else {
				throw new UnsupportedOperationException("Only JDBC-based persistence currently supported for this function.");
			}
			if (logger.isLoggable(Level.FINE)) {            
				logger.fine("Returning batch status of: " + batchStatus + " from JobContext.");
			}
		}
		return batchStatus;
	}

	@Override
	public Date getCreateTime() {

		if (this.jobcontext == null) {

			if (_persistenceManagementService instanceof JDBCPersistenceManagerImpl) {
				createTime = ((JDBCPersistenceManagerImpl) _persistenceManagementService)
						.jobOperatorQueryJobExecutionTimestamp(executionID,
								JDBCPersistenceManagerImpl.CREATE_TIME);
			}
		}
		return new Date(createTime.getTime());
	}

	@Override
	public Date getEndTime() {

		
		if (this.jobcontext == null) {
		
			if (_persistenceManagementService instanceof JDBCPersistenceManagerImpl){
				endTime = ((JDBCPersistenceManagerImpl)_persistenceManagementService).jobOperatorQueryJobExecutionTimestamp(executionID, JDBCPersistenceManagerImpl.END_TIME);
			}
		}
		
		return new Date(endTime.getTime());
	}

	@Override
	public long getExecutionId() {
		// TODO Auto-generated method stub
		return executionID;
	}

	@Override
	public String getExitStatus() {
		
		if (this.jobcontext != null){
			return this.jobcontext.getExitStatus();
		}
		else {
			// old job, retrieve from the backend
			if (_persistenceManagementService instanceof JDBCPersistenceManagerImpl){
				exitStatus = ((JDBCPersistenceManagerImpl)_persistenceManagementService).jobOperatorQueryJobExecutionStatus(executionID, JDBCPersistenceManagerImpl.EXIT_STATUS);
			}
			return exitStatus;
		}

	}

	@Override
	public Date getLastUpdatedTime() {
		// if and only if we are accessing the JDBC impl, get the requested timestamp
		/*
		if (_persistenceManagementService instanceof JDBCPersistenceManagerImpl){
			updateTime = ((JDBCPersistenceManagerImpl)_persistenceManagementService).jobOperatorQueryJobExecutionTimestamp(executionID, JDBCPersistenceManagerImpl.UPDATE_TIME);
		}
		*/
		
		if (this.jobcontext == null) {

			if (_persistenceManagementService instanceof JDBCPersistenceManagerImpl) {
				this.updateTime = ((JDBCPersistenceManagerImpl) _persistenceManagementService)
						.jobOperatorQueryJobExecutionTimestamp(executionID,
								JDBCPersistenceManagerImpl.UPDATE_TIME);
			}
		}

		return new Date(this.updateTime.getTime());
	}

	@Override
	public Date getStartTime() {
		
		// if and only if we are accessing the JDBC impl, get the requested timestamp
		/*
		if (_persistenceManagementService instanceof JDBCPersistenceManagerImpl){
			startTime = ((JDBCPersistenceManagerImpl)_persistenceManagementService).jobOperatorQueryJobExecutionTimestamp(executionID, JDBCPersistenceManagerImpl.START_TIME);
		}
		*/
		
		
		if (this.jobcontext == null) {
		
			if (_persistenceManagementService instanceof JDBCPersistenceManagerImpl){
				startTime = ((JDBCPersistenceManagerImpl)_persistenceManagementService).jobOperatorQueryJobExecutionTimestamp(executionID, JDBCPersistenceManagerImpl.START_TIME);
			}
		}
		
		return new Date(startTime.getTime());
	}
	
	@Override
	public Properties getJobParameters() {
		// TODO Auto-generated method stub
		return jobProperties;
	}
	
	// IMPL specific setters
	
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
	
	public void setJobParameters(Properties jProps){
		jobProperties = jProps;
	}

	@Override
	public String getJobName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTagName() {
		return _persistenceManagementService.getTagName(executionID);
	}

	@Override
	public long getInstanceId() {
		return instanceID;
	}

}
