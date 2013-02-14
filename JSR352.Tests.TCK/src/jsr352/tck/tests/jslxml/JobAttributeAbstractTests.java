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
package jsr352.tck.tests.jslxml;

import static jsr352.tck.utils.AssertionUtils.assertWithMessage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import javax.batch.operations.JobOperator.BatchStatus;
import javax.batch.operations.exception.JobStartException;
import javax.batch.runtime.JobExecution;

import jsr352.tck.utils.JobOperatorBridge;

import org.junit.Before;
import org.testng.Reporter;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class JobAttributeAbstractTests {

	private JobOperatorBridge jobOp = null;
	
	private static final String JOB_FILE = "job_attributes_test";
	
	/**
	 * @testName: testJobAttributeAbstractTrue
	 * @assertion: Section 5.1 job attribute restartable
	 * @test_Strategy: set abstract true should not allow job to start
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test @org.junit.Test 
	public void testJobAttributeAbstractTrue() throws FileNotFoundException, IOException, InterruptedException {
		
		Properties jobParams = new Properties();
		jobParams.setProperty("abstract", "true");

		JobExecution jobExec = null;
		Reporter.log("starting job, should fail because abstract is true");
		try {
			jobExec = jobOp.startJobAndWaitForResult(JOB_FILE, jobParams);
		} catch (JobStartException jse) {
			
			Reporter.log("JobStartException = " + jse.getLocalizedMessage());
		}
		assertWithMessage("Job should fails to start", jobExec == null);
		Reporter.log("Job should fails to start");
	}

	/**
	 * @testName: testJobAttributeAbstractFalse
	 * @assertion: Section 5.1 job attribute restartable
	 * @test_Strategy: set abstract false should allow job to start
	 * 
	 * @throws JobStartException
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test @org.junit.Test
	public void testJobAttributeAbstractFalse() throws JobStartException, FileNotFoundException, IOException, InterruptedException {
		
		Properties jobParams = new Properties();
		jobParams.setProperty("abstract", "false");

		Reporter.log("starting job");
		JobExecution jobExec = jobOp.startJobAndWaitForResult(JOB_FILE, jobParams);
		Reporter.log("Job Status = " + jobExec.getBatchStatus());
		
		assertWithMessage("Job completed", jobExec.getBatchStatus().equals(BatchStatus.COMPLETED));
		Reporter.log("Job completed");
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
