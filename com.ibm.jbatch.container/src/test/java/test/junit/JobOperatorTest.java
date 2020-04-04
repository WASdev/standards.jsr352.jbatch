/*
 * Copyright 2014 International Business Machines Corp.
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
package test.junit;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.JobInstance;

import org.junit.BeforeClass;
import org.junit.Test;

public class JobOperatorTest {
	static JobOperator jobOp = null;	
	
	private static int sleepTime = 2000;
	
	@BeforeClass
	public static void init() {
		jobOp = BatchRuntime.getJobOperator();
	}
	
	@Test
	public void testJobParameters() throws Exception {
		Properties origParams = new Properties();

		String sleepPropName = "sleepTime";
		String sleepPropVal = "100";
		origParams.setProperty(sleepPropName, sleepPropVal);
		long execId = jobOp.start("simpleJob", origParams);
		Thread.sleep(sleepTime);
		JobExecution je = jobOp.getJobExecution(execId);
		
		assertEquals("batch status", BatchStatus.COMPLETED, je.getBatchStatus());
		
		Properties obtainedParams = jobOp.getParameters(execId);
		assertEquals("Unexpected number of job parameters", 1, obtainedParams.size());
		assertEquals("Unexpected sleepTime parameter value", sleepPropVal, obtainedParams.getProperty(sleepPropName));

	}
	
	// Bug 5806
	@Test
	public void testJobParametersFromJobExecutionsFromInstance() throws Exception {
		Properties origParams = new Properties();

		String sleepPropName = "sleepTime";
		String sleepPropVal = "100";
		origParams.setProperty(sleepPropName, sleepPropVal);

		long execId = jobOp.start("simpleJob", origParams);
		Thread.sleep(sleepTime);

		JobInstance ji = jobOp.getJobInstance(execId);
		JobExecution je = jobOp.getJobExecutions(ji).get(0);
		assertEquals("batch status", BatchStatus.COMPLETED, je.getBatchStatus());
		
		Properties obtainedParams = je.getJobParameters();
		assertEquals("Unexpected number of job parameters", 1, obtainedParams.size());
		assertEquals("Unexpected sleepTime parameter value", sleepPropVal, obtainedParams.getProperty(sleepPropName));

	}
}
