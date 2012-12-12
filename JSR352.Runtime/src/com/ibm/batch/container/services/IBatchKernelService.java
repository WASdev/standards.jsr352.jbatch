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
package com.ibm.batch.container.services;

import java.util.List;
import java.util.Properties;

import javax.batch.operations.exception.JobExecutionNotRunningException;
import javax.batch.operations.exception.NoSuchJobExecutionException;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.StepExecution;

import com.ibm.batch.container.jobinstance.RuntimeJobExecutionImpl;

public interface IBatchKernelService extends IBatchServiceBase {

	JobExecution getJobExecution(long executionId);
	
	StepExecution getStepExecution(long jobExecutionId, long stepExecutionId);

	//AJM: void setJobExecution(Long executionID, JobExecution execution);

	JobExecution restartJob(long jobInstanceId);

	JobExecution startJob(String jobXML);

	JobExecution startJob(String jobXML, Properties jobParameters);

    void stopJob(long jobInstanceId) throws NoSuchJobExecutionException, JobExecutionNotRunningException;

    JobExecution restartJob(long jobInstanceId, Properties overrideJobParameters);

    void jobExecutionDone(RuntimeJobExecutionImpl jobExecution);
    
    List<StepExecution> getJobSteps(long jobExecutionId);
    
    List<Long> getExecutionIds(long jobInstance);
    
    int getJobInstanceCount(String jobName);
    
}
