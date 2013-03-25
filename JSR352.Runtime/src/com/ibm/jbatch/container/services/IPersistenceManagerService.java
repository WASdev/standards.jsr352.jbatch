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

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;

import com.ibm.jbatch.container.context.impl.StepContextImpl;
import com.ibm.jbatch.container.jobinstance.RuntimeJobContextJobExecutionBridge;
import com.ibm.jbatch.container.jobinstance.StepExecutionImpl;
import com.ibm.jbatch.container.persistence.CheckpointData;
import com.ibm.jbatch.container.persistence.CheckpointDataKey;
import com.ibm.jbatch.container.status.JobStatus;
import com.ibm.jbatch.container.status.StepStatus;
import com.ibm.jbatch.spi.services.IBatchServiceBase;

public interface IPersistenceManagerService extends IBatchServiceBase {
	
    /**
     * JOB OPERATOR ONLY METHODS
     */
    
	public int jobOperatorGetJobInstanceCount(String jobName);
	
	public Map<Long, String> jobOperatorGetExternalJobInstanceData();
	
	public List<Long> jobOperatorGetJobInstanceIds(String jobName, int start, int count);

	public Timestamp jobOperatorQueryJobExecutionTimestamp(long key, String timetype);
	
	public String jobOperatorQueryJobExecutionStatus(long key, String requestedStatus);
	
	public long jobOperatorQueryJobExecutionJobInstanceId(long executionID) throws NoSuchJobExecutionException;
	
	public void jobExecutionStatusStringUpdate(long key, String statusToUpdate, String statusString, Timestamp updatets);
	
	public void jobExecutionTimestampUpdate(long key, String timestampToUpdate, Timestamp ts);
	
	public List<StepExecution> getStepExecutionIDListQueryByJobID(long execid);
	
	public void jobOperatorUpdateBatchStatusWithUPDATETSonly(long key, String statusToUpdate, String statusString, Timestamp updatets);
	
	public void jobOperatorUpdateBatchStatusWithSTATUSandUPDATETSonly(long key, String statusToUpdate, String statusString, Timestamp updatets);
	
	public IJobExecution jobOperatorGetJobExecution(long jobExecutionId);

	public Properties getParameters(long executionId);

	public List<IJobExecution> jobOperatorGetJobExecutions(long jobInstanceId);

	public Set<Long> jobOperatorGetRunningExecutions(String jobName);
	
	public String getJobCurrentTag(long jobInstanceId);
	
	public void purge(String apptag);
	
	public JobStatus getJobStatusFromExecution(long executionId);
	
	public long getJobInstanceIdByExecutionId(long executionId) throws NoSuchJobExecutionException;
	
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
	public JobInstance createJobInstance(String name, String apptag, String jobXml);
	
	// EXECUTIONINSTANCEDATA
	/**
	 * Create a JobExecution
	 * 
	 * @param jobInstance the parent job instance
	 * @param jobParameters the parent job instance parameters
	 * @param batchStatus the current BatchStatus
	 * @return the RuntimeJobContextJobExecutionBridge class for this JobExecution
	 */
	RuntimeJobContextJobExecutionBridge createJobExecution(JobInstance jobInstance, Properties jobParameters, BatchStatus batchStatus);
	
	// STEPEXECUTIONINSTANCEDATA
	/**
	 * Create a StepExecution
	 * 
	 * @param jobExecId the parent JobExecution id
	 * @param stepContext the step context for this step execution
	 * @return the StepExecution
	 */
	public StepExecutionImpl createStepExecution(long jobExecId, StepContextImpl stepContext);
	
	/**
	 * Update a StepExecution
	 * 
	 * @param jobExecId the parent JobExecution id
	 * @param stepContext the step context for this step execution
	 */
	public void updateStepExecution(long jobExecId, StepContextImpl stepContext);
	

	
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
	
	
    public void updateCheckpointData(CheckpointDataKey key, CheckpointData value);

	CheckpointData getCheckpointData(CheckpointDataKey key);

	void createCheckpointData(CheckpointDataKey key, CheckpointData value);

	long getMostRecentExecutionId(long jobInstanceId);

	JobInstance createSubJobInstance(String name, String apptag);

}
