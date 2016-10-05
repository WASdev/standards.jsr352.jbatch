/*
 * Copyright 2013 International Business Machines Corp.
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
package com.ibm.jbatch.container.services.impl;

import javax.batch.operations.JobExecutionIsRunningException;
import javax.batch.operations.JobSecurityException;
import javax.batch.operations.NoSuchJobInstanceException;
import javax.batch.runtime.BatchRuntime;

import com.ibm.jbatch.container.api.impl.JobOperatorImpl;
import com.ibm.jbatch.spi.BatchJobUtil;

public class RuntimeBatchJobUtil implements BatchJobUtil {

	@Override
	public void purgeOwnedRepositoryData(String tag) {
		
		JobOperatorImpl jobOperator = (JobOperatorImpl) BatchRuntime.getJobOperator();
		jobOperator.purge(tag);
	}

	@Override
	public boolean deleteJobInstance(long jobInstanceId)
			throws NoSuchJobInstanceException, JobSecurityException,
			JobExecutionIsRunningException {
		JobOperatorImpl jobOperator = (JobOperatorImpl) BatchRuntime.getJobOperator();
		return jobOperator.deleteJobInstance(jobInstanceId);
	}
}
