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

import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.batch.operations.JobExecutionNotMostRecentException;
import javax.batch.operations.JobExecutionNotRunningException;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.JobStartException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.runtime.JobInstance;

import com.ibm.jbatch.container.jobinstance.RuntimeJobExecution;
import com.ibm.jbatch.container.util.BatchFlowInSplitWorkUnit;
import com.ibm.jbatch.container.util.BatchPartitionWorkUnit;
import com.ibm.jbatch.container.util.BatchWorkUnit;
import com.ibm.jbatch.container.util.FlowInSplitBuilderConfig;
import com.ibm.jbatch.container.util.PartitionsBuilderConfig;
import com.ibm.jbatch.spi.BatchSecurityHelper;
import com.ibm.jbatch.spi.services.IBatchServiceBase;

public interface IBatchKernelService extends IBatchServiceBase {

	IJobExecution getJobExecution(long executionId) throws NoSuchJobExecutionException;

	IJobExecution restartJob(long executionID) throws JobRestartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException, NoSuchJobExecutionException;

	IJobExecution restartJob(long executionID, Properties overrideJobParameters) throws JobRestartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException, NoSuchJobExecutionException;

	IJobExecution startJob(String jobXML) throws JobStartException;

	IJobExecution startJob(String jobXML, Properties jobParameters) throws JobStartException;

	void stopJob(long executionID) throws NoSuchJobExecutionException, JobExecutionNotRunningException;

	void jobExecutionDone(RuntimeJobExecution jobExecution);

	int getJobInstanceCount(String jobName);

	JobInstance getJobInstance(long instanceId);

	BatchSecurityHelper getBatchSecurityHelper();

    List<BatchPartitionWorkUnit> buildNewParallelPartitions(PartitionsBuilderConfig config) throws JobRestartException, JobStartException;

    List<BatchPartitionWorkUnit> buildOnRestartParallelPartitions(PartitionsBuilderConfig config) throws JobRestartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException;

    void startGeneratedJob(BatchWorkUnit batchWork);

    void restartGeneratedJob(BatchWorkUnit batchWork) throws JobRestartException;
	
	boolean isExecutionRunning(long executionId);

	BatchFlowInSplitWorkUnit buildNewFlowInSplitWorkUnit(
			FlowInSplitBuilderConfig config);

	BatchFlowInSplitWorkUnit buildOnRestartFlowInSplitWorkUnit(
			FlowInSplitBuilderConfig config); 


}
