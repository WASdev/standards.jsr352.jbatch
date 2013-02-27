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

import javax.batch.operations.JobOperator.BatchStatus;
import javax.batch.runtime.JobExecution;

import com.ibm.jbatch.tck.utils.JobOperatorBridge;

import org.junit.Before;
import org.testng.Reporter;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class JobLevelPropertiesTests {

	private JobOperatorBridge jobOp = null;

	private int PROPERTIES_COUNT = 3;
	
	private String FOO_VALUE = "bar";

	/**
	 * @testName: testJobLevelPropertiesCount
	 * @assertion: Section 5.1.3 Job Level Properties
	 * @test_Strategy: set a list of properties to job should add them to the job context properties
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test @org.junit.Test
	public void testJobLevelPropertiesCount() throws Exception {
		
		String METHOD = "testJobLevelPropertiesCount";
		
		try {
			Reporter.log("starting job");
			JobExecution jobExec = jobOp.startJobAndWaitForResult("job_level_properties_count");
	
			Reporter.log("Job Status = " + jobExec.getBatchStatus());
			assertWithMessage("Job completed", jobExec.getBatchStatus().equals(BatchStatus.COMPLETED));
			Reporter.log("job completed");
			
			int count = jobExec.getExitStatus() != null ? Integer.parseInt(jobExec.getExitStatus()) : 0;
		
			assertWithMessage("Properties count", count  == PROPERTIES_COUNT);
			
			Reporter.log("Job batchlet return code is the job.properties size " + count);
		} catch (Exception e) {
            handleException(METHOD, e);
        }
	}
	
	/**
	 * @testName: testJobLevelPropertiesPropertyValue
	 * @assertion: Section 5.1.3 Job Level Properties
	 * @test_Strategy: set a job property value should equal value set on job context property 
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test @org.junit.Test
	public void testJobLevelPropertiesPropertyValue() throws Exception {
		
		String METHOD = "testJobLevelPropertiesPropertyValue";

		try {
			Reporter.log("starting job");
			JobExecution jobExec = jobOp.startJobAndWaitForResult("job_level_properties_value");
	
			Reporter.log("Job Status = " + jobExec.getBatchStatus());
			assertWithMessage("Job completed", jobExec.getBatchStatus().equals(BatchStatus.COMPLETED));
			Reporter.log("job completed");
			
			assertWithMessage("Property value", FOO_VALUE.equals(jobExec.getExitStatus()));
			
			Reporter.log("Job batchlet return code is the job property foo value " + FOO_VALUE);
		} catch (Exception e) {
            handleException(METHOD, e);
        }
	}
	
	/**
	 * @testName: testJobLevelPropertiesEmpty
	 * @assertion: Section 5.1.3 Job Level Properties
	 * @test_Strategy: set a job property value should equal value set on job context property 
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test @org.junit.Test
	public void testJobLevelPropertiesEmpty() throws Exception {
		
		String METHOD = "testJobLevelPropertiesEmpty";
		
		try {
			Reporter.log("starting job");
			JobExecution jobExec = jobOp.startJobAndWaitForResult("job_level_properties_count_zero");
	
			Reporter.log("Job Status = " + jobExec.getBatchStatus());
			assertWithMessage("Job completed", jobExec.getBatchStatus().equals(BatchStatus.COMPLETED));
			Reporter.log("job completed");
			
			int count = jobExec.getExitStatus() != null ? Integer.parseInt(jobExec.getExitStatus()) : 100;
		
			assertWithMessage("Properties count", count == 0);
			
			Reporter.log("Job batchlet return code is the job.properties size " + count);
		} catch (Exception e) {
            handleException(METHOD, e);
        }
	}
	
	/**
	 * @testName: testJobLevelPropertiesShouldNotBeAvailableThroughStepContext
	 * @assertion: Section 5.1.3 Job Level Properties
	 * @test_Strategy: set a job property value should not be available to step context 
	 * 
	 * @throws InterruptedException
	 */
	@Test @org.junit.Test
	public void testJobLevelPropertiesShouldNotBeAvailableThroughStepContext() throws Exception {
		
		String METHOD = "testJobLevelPropertiesShouldNotBeAvailableThroughStepContext";
		
		try {
			Reporter.log("starting job");
			JobExecution jobExec = jobOp.startJobAndWaitForResult("job_level_properties_scope");
	
			Reporter.log("Job Status = " + jobExec.getBatchStatus());
			assertWithMessage("Job completed", jobExec.getBatchStatus().equals(BatchStatus.COMPLETED));
			Reporter.log("job completed");
			
			assertWithMessage("Job Level Property is not available through step context", jobExec.getExitStatus().equals(BatchStatus.COMPLETED.name()));
			Reporter.log("Job batchlet return code is the job.property read through step context (expected value=COMPLETED) " + jobExec.getExitStatus());
		} catch (Exception e) {
            handleException(METHOD, e);
        }
	}
	
	 private static void handleException(String methodName, Exception e) throws Exception {
	        Reporter.log("Caught exception: " + e.getMessage() + "<p>");
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
