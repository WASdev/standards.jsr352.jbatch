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

import javax.batch.operations.JobOperator.BatchStatus;
import javax.batch.runtime.JobExecution;

import jsr352.tck.utils.JobOperatorBridge;

import org.junit.Before;
import org.testng.Reporter;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class StepLevelPropertiesTests {

	private JobOperatorBridge jobOp = null;

	private int PROPERTIES_COUNT = 3;
	
	private String FOO_VALUE = "bar";
	
	/**
	 * @testName: testStepLevelPropertiesCount
	 * @assertion: Section 5.2.3 Step Level Properties
	 * @test_Strategy: set a list of properties to the step should add them to the step context properties
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test @org.junit.Test
	public void testStepLevelPropertiesCount() throws Exception {
			
		Reporter.log("starting job");
		JobExecution jobExec = jobOp.startJobAndWaitForResult("step_level_properties_count");

		Reporter.log("Job Status = " + jobExec.getBatchStatus());
		assertWithMessage("Job completed", jobExec.getBatchStatus().equals(BatchStatus.COMPLETED));
		Reporter.log("job completed");
		
		int count = jobExec.getExitStatus() != null ? Integer.parseInt(jobExec.getExitStatus()) : 0;
	
		assertWithMessage("Properties count", count == PROPERTIES_COUNT);
		
		Reporter.log("Job batchlet return code is the step.properties size " + count);
	}
	
	/**
	 * @testName: testStepLevelPropertiesPropertyValue
	 * @assertion: Section 5.2.3 Step Level Properties
	 * @test_Strategy: set a step property value should equal value set on step context property 
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test @org.junit.Test
	public void testStepLevelPropertiesPropertyValue() throws Exception {

		Reporter.log("starting job");
		JobExecution jobExec = jobOp.startJobAndWaitForResult("step_level_properties_value");

		Reporter.log("Job Status = " + jobExec.getBatchStatus());
		assertWithMessage("Job completed", jobExec.getBatchStatus().equals(BatchStatus.COMPLETED));
		Reporter.log("job completed");
		
		assertWithMessage("Property value", FOO_VALUE.equals(jobExec.getExitStatus()));
		
		Reporter.log("Job batchlet return code is the step property foo value " + FOO_VALUE);
	}
	
	/**
	 * @testName: testStepLevelPropertiesEmpty
	 * @assertion: Section 5.2.3 Step Level Properties
	 * @test_Strategy: set a step property value should equal value set on step context property 
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test @org.junit.Test
	public void testStepLevelPropertiesEmpty() throws Exception {
	
		Reporter.log("starting job");
		JobExecution jobExec = jobOp.startJobAndWaitForResult("step_level_properties_count_zero");

		Reporter.log("Job Status = " + jobExec.getBatchStatus());
		assertWithMessage("Job completed", jobExec.getBatchStatus().equals(BatchStatus.COMPLETED));
		Reporter.log("job completed");
		
		int count = jobExec.getExitStatus() != null ? Integer.parseInt(jobExec.getExitStatus()) : 100;
	
		assertWithMessage("Properties count", count == 0);
		
		Reporter.log("Job batchlet return code is the step.properties size " + count);
	}
	
	/**
	 * @testName: testStepLevelPropertiesShouldNotBeAvailableThroughJobContext
	 * @assertion: Section 5.2.3 Step Level Properties
	 * @test_Strategy: set a step property value should not be available to job context 
	 * 
	 * @throws InterruptedException
	 */
	@Test @org.junit.Test
	public void testStepLevelPropertiesShouldNotBeAvailableThroughJobContext() throws Exception {
		
		Reporter.log("starting job");
		JobExecution jobExec = jobOp.startJobAndWaitForResult("step_level_properties_scope");

		Reporter.log("Job Status = " + jobExec.getBatchStatus());
		assertWithMessage("Job completed", jobExec.getBatchStatus().equals(BatchStatus.COMPLETED));
		Reporter.log("job completed");
		;
		assertWithMessage("Step Level Property is not available through job context", jobExec.getExitStatus().equals(BatchStatus.COMPLETED.name()));
		Reporter.log("Job batchlet return code is the step.property read through job context (expected value=COMPLETED) " + jobExec.getExitStatus());
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
