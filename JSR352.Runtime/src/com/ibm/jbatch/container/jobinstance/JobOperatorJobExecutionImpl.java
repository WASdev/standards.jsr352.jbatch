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
import java.util.logging.Logger;

import javax.batch.runtime.BatchStatus;

import com.ibm.jbatch.container.context.impl.JobContextImpl;
import com.ibm.jbatch.container.services.IJobExecution;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.services.IPersistenceManagerService.TimestampType;
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
	String jobName = null;
	private JobContextImpl jobContext = null;

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public void setJobContext(JobContextImpl jobContext) {
		this.jobContext = jobContext;
	}

	public JobOperatorJobExecutionImpl(long executionId, long instanceId) {
		this.executionID = executionId;
		this.instanceID = instanceId;
	}

	@Override
	public BatchStatus getBatchStatus() {

		BatchStatus batchStatus  = null;

		if (this.jobContext != null){
			batchStatus = this.jobContext.getBatchStatus();
			logger.finest("Returning batch status of: " + batchStatus + " from JobContext.");
		}
		else {
			// old job, retrieve from the backend
			batchStatus = BatchStatus.valueOf(_persistenceManagementService.jobOperatorQueryJobExecutionBatchStatus(executionID));
			logger.finest("Returning batch status of: " + batchStatus + " from JobContext.");
		}
		return batchStatus;
	}

	@Override
	public Date getCreateTime() {

		if (this.jobContext == null) {
			createTime = _persistenceManagementService.jobOperatorQueryJobExecutionTimestamp(executionID, TimestampType.CREATE);
		}

		if (createTime != null){
			return new Date(createTime.getTime());
		}
		else return createTime;
	}

	@Override
	public Date getEndTime() {


		if (this.jobContext == null) {
			endTime = _persistenceManagementService.jobOperatorQueryJobExecutionTimestamp(executionID, TimestampType.END);
		}

		if (endTime != null){
			return new Date(endTime.getTime());
		}
		else return endTime;
	}

	@Override
	public long getExecutionId() {
		return executionID;
	}

	@Override
	public String getExitStatus() {

		if (this.jobContext != null){
			return this.jobContext.getExitStatus();
		}
		else {
			exitStatus = _persistenceManagementService.jobOperatorQueryJobExecutionExitStatus(executionID);
			return exitStatus;
		}

	}

	@Override
	public Date getLastUpdatedTime() {

		if (this.jobContext == null) {
			this.updateTime = _persistenceManagementService.jobOperatorQueryJobExecutionTimestamp(executionID, TimestampType.LAST_UPDATED);
		}

		if (updateTime != null) {
			return new Date(this.updateTime.getTime());
		}
		else return updateTime;
	}

	@Override
	public Date getStartTime() {

		if (this.jobContext == null) {
			startTime = _persistenceManagementService.jobOperatorQueryJobExecutionTimestamp(executionID, TimestampType.STARTED);
		}

		if (startTime != null){
			return new Date(startTime.getTime());
		}
		else return startTime;
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
		return jobName;
	}

	@Override
	public String getTagName() {
		return _persistenceManagementService.getTagName(executionID);
	}

	@Override
	public long getInstanceId() {
		return instanceID;
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("createTime=" + createTime);
		buf.append(",batchStatus=" + batchStatus);
		buf.append(",exitStatus=" + exitStatus);
		buf.append(",jobName=" + jobName);
		buf.append(",instanceId=" + instanceID);
		buf.append(",executionId=" + executionID);
		return buf.toString();
	}

}
