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

import javax.batch.runtime.BatchStatus;

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
    
    /*
     * A side effect of this method is that it nulls out the 'restartOn' value from the previous execution gets zeroed out.
     * 
     * Also sets BatchStatus to STARTING
     */
    public void updateJobStatusWithNewExecution(long jobInstanceId, long newExecutionId) throws BatchContainerServiceException;
	

    public abstract void updateJobCurrentStep(long jobInstanceId, String currentStepName)  throws BatchContainerServiceException;

    /**
     * Creates an entry for the step in the stepstatus table during jobsetup
     * @param stepId
     * @throws BatchContainerServiceException
     */
    public abstract StepStatus createStepStatus(long stepExecutionId) throws BatchContainerServiceException;
    
    public abstract void updateStepStatus(long stepExecutionId, StepStatus newStepStatus) throws BatchContainerServiceException;


    public abstract StepStatus getStepStatus(long jobInstanceId, String stepId) throws BatchContainerServiceException ;


	
}
