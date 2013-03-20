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
import java.util.concurrent.BlockingQueue;

import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.batch.operations.JobExecutionNotMostRecentException;
import javax.batch.operations.JobExecutionNotRunningException;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.JobStartException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.runtime.JobInstance;

import com.ibm.jbatch.container.jobinstance.RuntimeJobContextJobExecutionBridge;
import com.ibm.jbatch.container.util.BatchWorkUnit;
import com.ibm.jbatch.container.util.PartitionDataWrapper;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.jbatch.spi.BatchSecurityHelper;
import com.ibm.jbatch.spi.services.IBatchServiceBase;

public interface IBatchKernelService extends IBatchServiceBase {

	IJobExecution getJobExecution(long executionId);

	IJobExecution restartJob(long executionID) throws JobRestartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException;

	IJobExecution restartJob(long executionID, Properties overrideJobParameters) throws JobRestartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException;

	IJobExecution startJob(String jobXML) throws JobStartException;

	IJobExecution startJob(String jobXML, Properties jobParameters) throws JobStartException;

	void stopJob(long executionID) throws NoSuchJobExecutionException, JobExecutionNotRunningException;

	void jobExecutionDone(RuntimeJobContextJobExecutionBridge jobExecution);

	int getJobInstanceCount(String jobName);

	JobInstance getJobInstance(long instanceId);

	BatchSecurityHelper getBatchSecurityHelper();

    List<BatchWorkUnit> buildNewParallelJobs(List<JSLJob> jobModels, Properties[] partitionProperties,
            BlockingQueue<PartitionDataWrapper> analyzerQueue, BlockingQueue<BatchWorkUnit> completedQueue, 
            RuntimeJobContextJobExecutionBridge rootJobExecution) throws JobRestartException, JobStartException;

    List<BatchWorkUnit> buildRestartableParallelJobs(List<JSLJob> jobModels, Properties[] partitionProperties,
            BlockingQueue<PartitionDataWrapper> analyzerQueue, BlockingQueue<BatchWorkUnit> completedQueue, 
            RuntimeJobContextJobExecutionBridge rootJobExecution) throws JobRestartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException;

    void startGeneratedJob(BatchWorkUnit batchWork);

    void restartGeneratedJob(BatchWorkUnit batchWork) throws JobRestartException;

    BatchWorkUnit buildNewBatchWorkUnit(JSLJob jobModel, Properties partitionProps,
            BlockingQueue<PartitionDataWrapper> analyzerQueue,
            BlockingQueue<BatchWorkUnit> completedQueue, RuntimeJobContextJobExecutionBridge rootJobExecution) throws JobStartException;

    BatchWorkUnit buildRestartableBatchWorkUnit(JSLJob jobModel, Properties partitionProps,
            BlockingQueue<PartitionDataWrapper> analyzerQueue,
            BlockingQueue<BatchWorkUnit> completedQueue, RuntimeJobContextJobExecutionBridge rootJobExecution) throws JobRestartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException;


}
