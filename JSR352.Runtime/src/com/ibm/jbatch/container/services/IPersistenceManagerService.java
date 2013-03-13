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
package com.ibm.jbatch.container.services;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;

import com.ibm.jbatch.container.context.impl.JobContextImpl;
import com.ibm.jbatch.container.context.impl.StepContextImpl;
import com.ibm.jbatch.container.exception.PersistenceException;
import com.ibm.jbatch.container.jobinstance.RuntimeJobExecutionHelper;
import com.ibm.jbatch.container.jobinstance.StepExecutionImpl;
import com.ibm.jbatch.container.jsl.Navigator;
import com.ibm.jbatch.container.status.JobStatus;
import com.ibm.jbatch.container.status.StepStatus;
import com.ibm.jbatch.spi.services.IBatchServiceBase;

public interface IPersistenceManagerService extends IBatchServiceBase {

	public static final int JOB_STATUS_STORE_ID = 0;
	public static final int STEP_STATUS_STORE_ID = 1;
	public static final int CHECKPOINT_STORE_ID = 2;
	
	public static final int SUBMITTEDJOBS_STORE_ID = 10;
	public static final int LOGICAL_TX_STORE_ID = 11;
	public static final int PJM_JOBCONTEXT_STORE_ID  = 12;
	/**
	 * (Required) Its purpose is to insert data into a particular store.
	 * @param storeDestination The target store destination. One of 
	 * IPersistenceManagerService.JOB_STATUS_STORE_ID, 
	 * IPersistenceManagerService.STEP_STATUS_STORE_ID, 
	 * IPersistenceManagerService.CHECKPOINT_STORE_ID
	 * @param key A unique key under which the data should be stored. 
	 * @param value A serializable object that contains the actual payload. One of
	 * JobStatus, StepStatus or CheckpointData 
	 */
	public void createData(int storeDestination , IPersistenceDataKey key, Serializable value) throws PersistenceException;
	
	/**
	 * (Required) Its purpose is to fetch data using the given key from a particular store
	 * @param storeDestination The store from which the data should be retrieved. One of
	 * IPersistenceManagerService.JOB_STATUS_STORE_ID, 
	 * IPersistenceManagerService.STEP_STATUS_STORE_ID, 
	 * IPersistenceManagerService.CHECKPOINT_STORE_ID
	 * @key Key to retrieve data with. The key may correspond to one or more data items. 
	 * In other words if some of the fields of the key are null then they should be treated as 
	 * wild cards.   
	 * @return A list containing the data items that match the input key filter. 
	 * Depending on the type of the store the List should contain either 
	 * JobStatus, StepStatus or CheckpointData objects
	 * @throws PersistenceException
	 */
	
	public List getData(int storeDestination, IPersistenceDataKey key) throws PersistenceException;

	
	/**
	 * (Required) Its purpose is to update one or more fields of a data item identified by the 
	 * key from the given data store.
	 * @param storeDestination The store from which the data should be updated. One of
	 * IPersistenceManagerService.JOB_STATUS_STORE_ID, 
	 * IPersistenceManagerService.STEP_STATUS_STORE_ID, 
	 * IPersistenceManagerService.CHECKPOINT_STORE_ID  
	 * @key Key to identify the data with. The key will correspond to exactly one data item.
	 * @value The object containing only the delta of the existing value, 
	 * in other words if the data item has 3 fields and only 2 fields need to be updated 
	 * the value would have valid entries for the 2 fields that have changed 
	 * while the unchanged field will be NULL 
	 * @throws PersistenceException
	 */
	
	public void updateData(int storeDestination, IPersistenceDataKey key, Serializable value) throws PersistenceException;

	   
    /**
     * (Required) Its purpose is to delete the data items corresponding to the 
     * given key from the given store.
     * @param storeDestination The store from which the data should be deleted. One of
     * IPersistenceManagerService.JOB_STATUS_STORE_ID, 
     * IPersistenceManagerService.STEP_STATUS_STORE_ID, 
     * IPersistenceManagerService.CHECKPOINT_STORE_ID  
     * @key Key to identify the data with. The key may correspond to one or more data items. 
     * In other words if some of the fields of the key are null then they should be treated as wild cards. 
     * @throws PersistenceException
     */
    public void deleteData(int storeDestination, IPersistenceDataKey key) throws PersistenceException;
    
    /**
     * JOB OPERATOR ONLY METHODS
     */
    
    public void jobOperatorCreateJobInstanceData(long key, String jobNameValue, String apptag);
    
	public int jobOperatorGetJobInstanceCount(String jobName);
	
	public HashMap jobOperatorGetJobInstanceData();
	
	public List<Long> jobOperatorgetJobInstanceIds(String jobName, int start, int count);

	public Set<String> jobOperatorgetJobNames();

	public void jobOperatorCreateExecutionData(long key,
			Timestamp createTime, Timestamp starttime, Timestamp endtime,
			Timestamp updateTime, Properties parms, long instanceID,
			String batchstatus, String exitstatus);
	
	public Timestamp jobOperatorQueryJobExecutionTimestamp(long key, String timetype);
	
	public String jobOperatorQueryJobExecutionStatus(long key, String requestedStatus);
	
	public long jobOperatorQueryJobExecutionJobInstanceId(long executionID);
	
	public void jobExecutionStatusStringUpdate(long key, String statusToUpdate, String statusString, Timestamp updatets);
	
	public void jobExecutionTimestampUpdate(long key, String timestampToUpdate, Timestamp ts);
	
	public void stepExecutionCreateStepExecutionData(String stepExecutionKey, long rootExecutionID, StepContextImpl stepContext, List<String> containment);
	
	//public long stepExecutionQueryID(String key);
	
	public List<StepExecution<?>> getStepExecutionIDListQueryByJobID(long execid);
	
	public void jobOperatorUpdateBatchStatusWithUPDATETSonly(long key, String statusToUpdate, String statusString, Timestamp updatets);
	
	public void jobOperatorUpdateBatchStatusWithSTATUSandUPDATETSonly(long key, String statusToUpdate, String statusString, Timestamp updatets);
	
	public IJobExecution jobOperatorGetJobExecution(long jobExecutionId);

	public List<IJobExecution> jobOperatorGetJobExecutionsByJobInstanceID(long jobInstanceID);

	public Properties getParameters(long instanceId);

	public List<IJobExecution> jobOperatorGetJobExecutions(long jobInstanceId);

	public StepExecution getStepExecutionObjQueryByStepID(long stepexecutionId);

	public Set<Long> jobOperatorGetRunningExecutions(String jobName);
	
	public String getJobCurrentTag(long jobInstanceId);
	
	public void purge(String apptag);
	
	public List<JobStatus> getJobStatusFromExecution(long executionId);
	
	public long getJobInstanceIdByExecutionId(long executionId);
	
	// JOBINSTANCEDATA
	/**
	 * Creates a JobIntance
	 * 
	 * @param name the job id from job.xml
	 * @param apptag the application tag that owns this job
	 * @param jobXml the resolved job xml
	 * @param jobParameters parameters used for this job
	 * @return the job instance
	 */
	public JobInstance createJobInstance(String name, String apptag, String jobXml, Properties jobParameters);
	
	// EXECUTIONINSTANCEDATA
	/**
	 * Create a JobExecution
	 * 
	 * @param jobNavigator resolved job navigator
	 * @param jobInstance the parent job instance
	 * @param jobParameters the parent job instance parameters
	 * @param jobContext the job context for this job execution
	 * @return the RuntimeJobExecutionHelper class for this JobExecution
	 */
	public RuntimeJobExecutionHelper createJobExecution(Navigator jobNavigator, JobInstance jobInstance, Properties jobParameters, JobContextImpl jobContext);
	
	// STEPEXECUTIONINSTANCEDATA
	/**
	 * Create a StepExecution
	 * 
	 * @param jobExecId the parent JobExecution id
	 * @param stepContext the step context for this step execution
	 * @return the StepExecution
	 */
	public StepExecutionImpl createStepExecution(long jobExecId, StepContextImpl stepContext, List<String> containment);
	
	/**
	 * Update a StepExecution
	 * 
	 * @param jobExecId the parent JobExecution id
	 * @param stepContext the step context for this step execution
	 */
	public void updateStepExecution(long jobExecId, StepContextImpl stepContext, List<String> containment);
	

	
	// JOB_STATUS
	/**
	 * Create a JobStatus
	 * 
	 * @param jobInstanceId the parent job instance id
	 * @return the JobStatus
	 */
	public JobStatus createJobStatus(long jobInstanceId);
	
	/**
	 * Get a JobStatus
	 * 
	 * @param instanceId the parent job instance id
	 * @return the JobStatus
	 */
	public JobStatus getJobStatus(long instanceId);
	
	/**
	 * Update a JobStatus
	 * 
	 * @param instanceId the parent job instance id
	 * @param jobStatus the job status to be updated
	 */
	public void updateJobStatus(long instanceId, JobStatus jobStatus);
	
	// STEP_STATUS
	/**
	 * Create a StepStatus
	 * 
	 * @param stepExecId the parent step execution id
	 * @return the StepStatus
	 */
	public StepStatus createStepStatus(long stepExecId);
	
	/**
	 * Get a StepStatus
	 * 
	 * The parent job instance id and this step name from the job xml
	 * are used to determine if the current step execution have previously run.
	 * 
	 * @param instanceId the parent job instance id
	 * @param stepName the step name
	 * @return the StepStatus
	 */
	public StepStatus getStepStatus(long instanceId, String stepName);
	
	/**
	 * Update a StepStatus
	 * 
	 * @param stepExecutionId the parent step execution id
	 * @param stepStatus the step status to be updated
	 */
	public void updateStepStatus(long stepExecutionId, StepStatus stepStatus);
	
	
	/**
	 * Get the application name from an execution id.
	 * 
	 * @param jobExecutionId the job execution id
	 * @return the application name
	 */
	public String getTagName(long jobExecutionId);
}
