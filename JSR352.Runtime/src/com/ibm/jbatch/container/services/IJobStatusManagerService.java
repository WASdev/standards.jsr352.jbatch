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

import java.util.List;

import javax.batch.operations.JobOperator.BatchStatus;

import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.status.JobStatus;
import com.ibm.jbatch.container.status.StepStatus;
import com.ibm.jbatch.spi.services.IBatchServiceBase;

public interface IJobStatusManagerService extends IBatchServiceBase {

	/**
	 * This method creates an entry for a new job instance
	 * 
	 */
	public JobStatus createJobStatus(long jobInstanceId) throws BatchContainerServiceException;

	public void updateJobStatus(JobStatus jobStatus);
		
	/**
     * Returns the JobStatus for a given jobInstance id
     * @param jobId
     * @return
     * @throws BatchContainerServiceException
     */
    public abstract JobStatus getJobStatus(long jobInstanceId) throws BatchContainerServiceException;
    
    public abstract JobStatus getJobStatusFromExecutionId(long executionId) throws BatchContainerServiceException;

    public void updateJobBatchStatus(long jobInstanceId, BatchStatus batchStatus) throws BatchContainerServiceException;
    public void updateJobExecutionStatus(long jobInstanceId, BatchStatus batchStatus, String exitStatus) throws BatchContainerServiceException;
    public void updateJobStatusFromJSLStop(long jobInstanceId, String restartOn) throws BatchContainerServiceException;
    public void updateJobStatusWithNewExecution(long jobInstanceId, long newExecutionId) throws BatchContainerServiceException;
	
    /**
     * Updates the current step of the job, updates the stepstatus to be IN SETUP
     * and sends the update to the scheduler
     * @param currentStepName
     * @throws BatchContainerServiceException
     */
    public abstract void updateJobCurrentStep(long jobInstanceId, String currentStepName)  throws BatchContainerServiceException;

    /**
     * Creates an entry for the step in the stepstatus table during jobsetup
     * @param stepId
     * @throws BatchContainerServiceException
     */
    public abstract StepStatus createStepStatus(long stepExecutionId) throws BatchContainerServiceException;
    
    public abstract void updateStepStatus(long stepExecutionId, StepStatus newStepStatus) throws BatchContainerServiceException;
    
    /**
     * Returns a list of all jobstatus currently in the DB
     * @return
     * @throws BatchContainerServiceException
     */
    public abstract List<JobStatus> getAllJobStatus() throws BatchContainerServiceException;
    
    /**
     * Gets the status for the given step belonging to the current job
     * @param stepId
     * @return
     * @throws BatchContainerServiceException
     */
    public abstract StepStatus getStepStatus(long jobInstanceId, String stepId) throws BatchContainerServiceException ;


	//------------------------------------------------------------------------------	

    /**
     * Returns the current job status 
     * @return
     * @throws BatchContainerServiceException
     */ 
    // No concept of "current" yet.
    // public abstract JobStatus getJobStatus() throws BatchContainerServiceException;

    
	

	
	/**
	 * Updates all columns of the JobStatus row in the DB with the values in the passed
	 * object and optionally send the update to the Scheduler and other listeners.
	 * @param ljsu
	 * @throws BatchContainerServiceException
	 */
//	public abstract void updateJobStatus(
	//		JobStatus ljsu,boolean notifyListeners) throws BatchContainerServiceException;

	/**
	 * Updates all columns of the JobStatus row in the DB with the values in the passed
	 * object and sends the update to the Scheduler
	 * @param ljsu
	 * @throws BatchContainerServiceException
	 */
	//public abstract void updateJobStatus(
		//	JobStatus ljsu) throws BatchContainerServiceException;
	
	
		
	
	/**
	 * Updates the job status to be suspended and updates the suspend until
	 * to the given time. Sets the return code to denote that its  suspended and 
	 * sends an update to the scheduler
	 * @param suspendUntilTime
	 * @param status
	 * @throws BatchContainerServiceException
	 */
	//public abstract void updateJobSuspendUntil( String suspendUntilTime, int status)  throws BatchContainerServiceException;
	
	
	/**
	 * Deletes the job status entry from the DB
	 * @throws BatchContainerServiceException
	 */
	//public abstract void deleteJobStatus( ) throws BatchContainerServiceException;
	
	/**
	 * Deletes the status for the specified job
	 * @param jobId
	 * @throws BatchContainerServiceException
	 */
	//public abstract void  deleteJobStatus( String jobId, String bjeeName) throws BatchContainerServiceException;
	

	/**
	 * Update the entire stepstatus row in the DB with the given object, updates
	 * the current step related attributes of the cached job status with the given object
	 * and finally sends an update to the scheduler
	 * @param st
	 * @throws BatchContainerServiceException
	 */
	//public abstract void updateStepStatus(StepStatus st) throws BatchContainerServiceException;
	
	/**
	 * Delete all step status entries for the current job
	 * @throws BatchContainerServiceException
	 */
	//public abstract void deleteStepStatus() throws BatchContainerServiceException;
	

	/**
	 * Updates the resultcode of the current job and sends an update to the scheduler
	 * @param resultCode
	 * @throws BatchContainerServiceException
	 */
	//public abstract void updateJobResults( int resultCode) throws BatchContainerServiceException;

	/**
	 * Updates the current step return code of the cached job status
	 * Updates the step table with the return code
	 * send the job status to the scheduler
	 * @param name
	 * @param rc
	 * @param status
	 * @throws BatchContainerServiceException
	 */
//	public abstract void updateJobStepReturnCode( String name, int rc, int status) throws BatchContainerServiceException;
	
	/**
	 * Updates the stepdata of the current job and sends an update to the scheduler
	 * @param stepData
	 * @throws BatchContainerServiceException
	 */
	//public abstract void updateStepData( byte[] stepData) throws BatchContainerServiceException;
	
	/**
	 * Updates the context data of the current job and sends an update to the scheduler
	 * @param stepData 
	 * @param stepRetries
	 * @param stepTime
	 * @param recordMetrics
	 * @throws BatchContainerServiceException
	 */
	//public abstract void updateContextData( byte[] stepData, long stepRetries, long stepTime, byte[] recordMetrics) throws BatchContainerServiceException;

	
	/**
	 * Returns the jobstatus for the given job ids
	 * @param jobIdList
	 * @return
	 * @throws BatchContainerServiceException
	 */
	//public abstract List<JobStatus> getJobStatus(List<String> jobIdList) throws BatchContainerServiceException;
	
	/**
	 * Returns the jobstatus list of jobs with the given id across all endpoints
	 * @param jobId
	 * @return
	 * @throws BatchContainerServiceException
	 */
//	public abstract List<JobStatus> getJobStatus(String jobId) throws BatchContainerServiceException;
	/**
	 * Deletes the DB entry for the current job
	 * @throws BatchContainerServiceException
	 */
//	public abstract void purgeJobStatus() throws BatchContainerServiceException;


	/**
	 * Creates a DB entry for the current job
	 * @param jobStatus
	 * @throws BatchContainerServiceException
	 */
//	public abstract void createJobStatus(JobStatus jobStatus) throws BatchContainerServiceException;


//	public abstract void registerListener( IJobStatusListener listener) throws BatchContainerServiceException;
	
//	public abstract void deregisterListener(String listenerId);
	
}
