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
package com.ibm.jbatch.tck.tests.jslxml;

import static com.ibm.jbatch.tck.utils.AssertionUtils.assertWithMessage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import javax.batch.operations.JobRestartException;
import javax.batch.operations.JobStartException;
import javax.batch.operations.NoSuchJobException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.operations.JobOperator.BatchStatus;
import javax.batch.runtime.JobExecution;

import com.ibm.jbatch.tck.utils.JobOperatorBridge;

import org.junit.Before;
import org.testng.Reporter;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class JobAttributeRestartTests {

	private JobOperatorBridge jobOp = null;

	private long TIMEOUT = 5000L;

	/**
	 * @testName: testJobAttributeRestartableTrue
	 * @assertion: Section 5.1 job attribute restartable
	 * @test_Strategy: set restartable true should allow job to restart
	 * 
	 * @throws JobStartException
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws JobRestartException 
	 * @throws NoSuchJobException 
	 * @throws NoSuchJobExecutionException 
	 * @throws JobInstanceAlreadyCompleteException 
	 */
	@Test @org.junit.Test
	public void testJobAttributeRestartableTrue() throws Exception {

		String METHOD = "testJobAttributeRestartableTrue";

		try {
			Reporter.log("starting job");
			Properties jobParams = new Properties();
			Reporter.log("execution.number=1<p>");
			jobParams.put("execution.number", "1");
			JobExecution jobExec = jobOp.startJobAndWaitForResult("job_attributes_restart_true_test", jobParams);

			Reporter.log("Job Status = " + jobExec.getBatchStatus());
			assertWithMessage("Job failed ", BatchStatus.FAILED, jobExec.getBatchStatus());

			Reporter.log("restarting job");
			Properties restartParams = new Properties();
			Reporter.log("execution.number=2<p>");
			restartParams.put("execution.number", "2");
			JobExecution newJobExec = jobOp.restartJobAndWaitForResult(jobExec.getExecutionId(), restartParams);

			Reporter.log("Job Status = " + newJobExec.getBatchStatus());
			assertWithMessage("Job completed", BatchStatus.COMPLETED, newJobExec.getBatchStatus());
			Reporter.log("job completed");
		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}



	private static void handleException(String methodName, Exception e) throws Exception {
		Reporter.log("Caught exception: " + e.getMessage()+"<p>");
		Reporter.log(methodName + " failed<p>");
		throw e;
	}

	public void setup(String[] args, Properties props) throws Exception {

		String METHOD = "setup";

		try {
			jobOp = new JobOperatorBridge();
		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}

	/* cleanup */
	public void  cleanup()
	{		

	}

	@BeforeTest
	@Before
	public void beforeTest() throws ClassNotFoundException {
		jobOp = new JobOperatorBridge(); 
	}

	@AfterTest
	public void afterTest() {
		jobOp = null;
	}

}
