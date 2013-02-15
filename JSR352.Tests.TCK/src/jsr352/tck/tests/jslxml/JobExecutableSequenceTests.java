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

public class JobExecutableSequenceTests {

	private JobOperatorBridge jobOp = null;
	
	/**
	 * @testName: testJobExecutableSequenceToUnknown
	 * @assertion: Section 5.3 Flow
	 * @test_Strategy: 1. setup a job consisting of 3 steps (step1 next to step2, step2 next to unknown, step3 unreachable
	 * 				   2. start job 
	 * 				   3. job should fail because it shouldn't be able to transition to unknown
	 * 
	 * @throws JobStartException
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws InterruptedException
	 */
    @Test(enabled = false)
    @org.junit.Test
	public void testJobExecutableSequenceToUnknown() throws Exception {

    	String METHOD = "testJobExecutableSequenceToUnknown";
    	
    	try {
    	
			Reporter.log("starting job");
			JobExecution jobExec = null;
			try {
				jobExec = jobOp.startJobAndWaitForResult("job_executable_sequence_invalid", null);
			} catch (Exception e) {
				Reporter.log("Job Failed with error: " + e.getLocalizedMessage());
			}
			Reporter.log("Job Status = " + jobExec.getBatchStatus());
			
			assertWithMessage("Job should have failed with unknown steps ", jobExec.getBatchStatus().equals(BatchStatus.FAILED));
			Reporter.log("job failed");
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
