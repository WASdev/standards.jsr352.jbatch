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
import java.util.Properties;
import java.util.Stack;
import java.util.concurrent.LinkedBlockingQueue;

import javax.batch.operations.exception.JobExecutionNotRunningException;
import javax.batch.operations.exception.JobRestartException;
import javax.batch.operations.exception.JobStartException;
import javax.batch.operations.exception.NoSuchJobExecutionException;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;

import com.ibm.jbatch.container.jobinstance.ParallelJobExecution;
import com.ibm.jbatch.container.jobinstance.RuntimeJobExecutionImpl;
import com.ibm.jbatch.container.util.PartitionDataWrapper;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.jbatch.spi.services.IBatchServiceBase;

public interface IBatchKernelService extends IBatchServiceBase {

	JobExecution getJobExecution(long executionId);

	StepExecution getStepExecution(long stepExecutionId);

	//AJM: void setJobExecution(Long executionID, JobExecution execution);

	JobExecution restartJob(long executionID) throws JobRestartException;

	JobExecution restartJob(long executionID, Properties overrideJobParameters) throws JobRestartException;

	JobExecution startJob(String jobXML) throws JobStartException;

	JobExecution startJob(String jobXML, Properties jobParameters) throws JobStartException;

	void stopJob(long executionID) throws NoSuchJobExecutionException, JobExecutionNotRunningException;

	// should be removed once props are gone JobExecution restartJob(long jobInstanceId);

	void jobExecutionDone(RuntimeJobExecutionImpl jobExecution);

	//List<StepExecution> getJobSteps(long jobExecutionId);

	List<Long> getExecutionIds(long jobInstance);

	int getJobInstanceCount(String jobName);

	JobInstance getJobInstance(long instanceId);

	List<StepExecution> getStepExecutions(long executionId);

	List<ParallelJobExecution> startParallelJobs(List<JSLJob> jobModels, Properties[] partitionProperties, LinkedBlockingQueue<PartitionDataWrapper> analyzerQueue, Stack<String> subJobExitStatusQueue);

	List<ParallelJobExecution> restartParallelJobs(List<JSLJob> jobModels, Properties[] partitionProperties, LinkedBlockingQueue<PartitionDataWrapper> analyzerQueue, Stack<String> subJobExitStatusQueue)
			throws JobRestartException;
}
