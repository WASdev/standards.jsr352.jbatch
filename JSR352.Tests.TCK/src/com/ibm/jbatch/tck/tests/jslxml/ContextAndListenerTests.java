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

import static com.ibm.jbatch.tck.utils.AssertionUtils.assertObjEquals;
import static com.ibm.jbatch.tck.utils.AssertionUtils.assertWithMessage;

import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.StepExecution;

import com.ibm.jbatch.tck.utils.JobOperatorBridge;

import org.junit.BeforeClass;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ContextAndListenerTests {

	private final static Logger logger = Logger.getLogger(ContextAndListenerTests.class.getName());
	private static JobOperatorBridge jobOp = null;


	public static void setup(String[] args, Properties props) throws Exception {

		String METHOD = "setup";

		try {
			jobOp = new JobOperatorBridge();
		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}

	@BeforeMethod
	@BeforeClass
	public static void setUp() throws Exception {
		jobOp = new JobOperatorBridge();
	}

	/*
	 * @testName: testExamineJobContextInArtifact
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
	@Test
	@org.junit.Test 
	public void testExamineJobContextInArtifact() throws Exception {

		String METHOD = "testOneArtifactIsJobAndStepListener";

		try {

			Reporter.log("Locate job XML file: oneArtifactIsJobAndStepListener.xml<p>");

			Reporter.log("Create job parameters for execution #1:<p>");
			Properties jobParams = new Properties();
			Reporter.log("app.timeinterval=10<p>");
			jobParams.put("app.timeinterval", "10");

			Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
			JobExecution execution1 = jobOp.startJobAndWaitForResult("JobContextTestBatchlet", jobParams);
		
			
			String testString = "JobName=job1;JobInstanceId=" + jobOp.getJobInstance(execution1.getExecutionId()).getInstanceId() + ";JobExecutionId=" + execution1.getExecutionId();
			Reporter.log("EXPECTED JobExecution getBatchStatus()=COMPLETED<p>");
			Reporter.log("ACTUAL JobExecution getBatchStatus()="+execution1.getBatchStatus()+"<p>");
			Reporter.log("EXPECTED JobExecution getExitStatus()="+testString+"<p>");
			Reporter.log("ACTUAL JobExecution getExitStatus()="+execution1.getExitStatus()+"<p>");
			assertWithMessage("Testing batch status", BatchStatus.COMPLETED, execution1.getBatchStatus());
			assertWithMessage("Testing exit status", testString, execution1.getExitStatus());
		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}
	
	/*
	 * @testName: testExamineJobContextInArtifact
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
	@Test
	@org.junit.Test 
	public void testExamineStepContextInArtifact() throws Exception {

		String METHOD = "testOneArtifactIsJobAndStepListener";

		try {

			Reporter.log("Locate job XML file: oneArtifactIsJobAndStepListener.xml<p>");

			Reporter.log("Create job parameters for execution #1:<p>");
			Properties jobParams = new Properties();
			Reporter.log("app.timeinterval=10<p>");
			jobParams.put("app.timeinterval", "10");

			Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
			JobExecution execution1 = jobOp.startJobAndWaitForResult("StepContextTestBatchlet", jobParams);
		
			List<StepExecution> steps = jobOp.getStepExecutions(execution1.getExecutionId());
			
			assertWithMessage("list of step executions == 1", steps.size() == 1);
			
			String testString = "StepName=step1;StepExecutionId=" + steps.get(0).getStepExecutionId();
			Reporter.log("EXPECTED JobExecution getBatchStatus()=COMPLETED<p>");
			Reporter.log("ACTUAL JobExecution getBatchStatus()="+execution1.getBatchStatus()+"<p>");
			Reporter.log("EXPECTED JobExecution getExitStatus()="+testString+"<p>");
			Reporter.log("ACTUAL JobExecution getExitStatus()="+execution1.getExitStatus()+"<p>");
			assertWithMessage("Testing batch status", BatchStatus.COMPLETED, execution1.getBatchStatus());
			assertWithMessage("Testing exit status", testString, execution1.getExitStatus());
			
		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}
	
	/*
	 * @testName: testOneArtifactIsJobAndStepListener
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
	@Test
	@org.junit.Test 
	public void testOneArtifactIsJobAndStepListener() throws Exception {

		String METHOD = "testOneArtifactIsJobAndStepListener";

		try {
			String expectedStr = "BeforeJob" + 
					"BeforeStep" + "UnusedExitStatusForPartitions" + "AfterStep" +
					"BeforeStep" + "UnusedExitStatusForPartitions" + "AfterStep" + 
					"AfterJob";

			Reporter.log("Locate job XML file: oneArtifactIsJobAndStepListener.xml<p>");

			Reporter.log("Create job parameters for execution #1:<p>");
			Properties jobParams = new Properties();
			Reporter.log("app.timeinterval=10<p>");
			jobParams.put("app.timeinterval", "10");

			Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
			JobExecution execution1 = jobOp.startJobAndWaitForResult("oneArtifactIsJobAndStepListener", jobParams);

			Reporter.log("EXPECTED JobExecution getBatchStatus()=COMPLETED<p>");
			Reporter.log("ACTUAL JobExecution getBatchStatus()="+execution1.getBatchStatus()+"<p>");
			Reporter.log("EXPECTED JobExecution getExitStatus()="+expectedStr+"<p>");
			Reporter.log("ACTUAL JobExecution getExitStatus()="+execution1.getExitStatus()+"<p>");
			assertWithMessage("Testing batch status", BatchStatus.COMPLETED, execution1.getBatchStatus());
			assertWithMessage("Testing exit status", expectedStr, execution1.getExitStatus());
		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}

	/*
	 * @testName: testgetException
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
	@Test
	@org.junit.Test 
	public void testgetException() throws Exception {

		String METHOD = "testgetException";

		try {
			String expectedStr = "MyChunkListener: found instanceof MyParentException";

			Reporter.log("Locate job XML file: oneArtifactIsJobAndStepListener.xml<p>");

			Reporter.log("Create job parameters for execution #1:<p>");
			Properties jobParams = new Properties();
			Reporter.log("fail.immediate=true<p>");

			Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
			JobExecution execution1 = jobOp.startJobAndWaitForResult("job_chunk_getException", jobParams);

			Reporter.log("EXPECTED JobExecution getBatchStatus()=FAILED<p>");
			Reporter.log("ACTUAL JobExecution getBatchStatus()="+execution1.getBatchStatus()+"<p>");
			Reporter.log("EXPECTED JobExecution getExitStatus()="+expectedStr+"<p>");
			Reporter.log("ACTUAL JobExecution getExitStatus()="+execution1.getExitStatus()+"<p>");
			assertWithMessage("Testing batch status", BatchStatus.FAILED, execution1.getBatchStatus());
			assertWithMessage("Testing exit status", expectedStr, execution1.getExitStatus());
		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}

	/*
	 * @testName: testgetExceptionListenerBased
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
	@Test
	@org.junit.Test 
	public void testgetExceptionListenerBased() throws Exception {

		String METHOD = "testgetExceptionListenerBased";

		try {
			String expectedStr = "MyChunkListener: found instanceof MyParentException";

			Reporter.log("Locate job XML file: oneArtifactIsJobAndStepListener.xml<p>");

			Reporter.log("Create job parameters for execution #1:<p>");
			Properties jobParams = new Properties();
			Reporter.log("fail.immediate=true<p>");

			Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
			JobExecution execution1 = jobOp.startJobAndWaitForResult("job_chunk_getExceptionListeners", jobParams);

			Reporter.log("EXPECTED JobExecution getBatchStatus()=FAILED<p>");
			Reporter.log("ACTUAL JobExecution getBatchStatus()="+execution1.getBatchStatus()+"<p>");
			Reporter.log("EXPECTED JobExecution getExitStatus()="+expectedStr+"<p>");
			Reporter.log("ACTUAL JobExecution getExitStatus()="+execution1.getExitStatus()+"<p>");
			assertWithMessage("Testing batch status", BatchStatus.FAILED, execution1.getBatchStatus());
			assertWithMessage("Testing exit status", expectedStr, execution1.getExitStatus());
		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}

    /*
     * @testName: testJobContextIsUniqueForMainThreadAndPartitions
     * 
     * @assertion: FIXME
     * 
     * @test_Strategy: FIXME
     */
    @Test
    @org.junit.Test 
    public void testJobContextIsUniqueForMainThreadAndPartitions() throws Exception {

        String METHOD = "testJobContextIsUniqueForMainThreadAndPartitions";
        begin(METHOD);

        try {
            Reporter.log("Locate job XML file: job_partitioned_1step.xml<p>");

            Reporter.log("Invoke startJobAndWaitForResult<p>");
            JobExecution jobExecution = jobOp.startJobAndWaitForResult("job_partitioned_1step");

            Reporter.log("JobExecution getBatchStatus()="+jobExecution.getBatchStatus()+"<p>");
            assertObjEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
            assertObjEquals("COMPLETED", jobExecution.getExitStatus());
            
        } catch (Exception e) {
            handleException(METHOD, e);
        }
    }
	
    /*
     * @testName: testJobContextIsUniqueForMainThreadAndFlowsInSplits
     * 
     * @assertion: FIXME
     * 
     * @test_Strategy: FIXME
     */
    @Test
    @org.junit.Test 
    public void testJobContextIsUniqueForMainThreadAndFlowsInSplits() throws Exception {

        String METHOD = "testJobContextIsUniqueForMainThreadAndFlowsInSplits";
        begin(METHOD);

        try {
            Reporter.log("Locate job XML file: job_split_batchlet_4steps.xml<p>");

            Reporter.log("Invoke startJobAndWaitForResult<p>");
            JobExecution execution = jobOp.startJobAndWaitForResult("job_split_batchlet_4steps");

            Reporter.log("JobExecution getBatchStatus()="+execution.getBatchStatus()+"<p>");
            assertObjEquals(BatchStatus.COMPLETED, execution.getBatchStatus());
            assertObjEquals("COMPLETED", execution.getExitStatus());
            
        } catch (Exception e) {
            handleException(METHOD, e);
        }
    }

    /*
     * @testName: testStepContextIsUniqueForMainThreadAndPartitions
     * 
     * @assertion: FIXME
     * 
     * @test_Strategy: FIXME
     */
    @Test
    @org.junit.Test
    public void testStepContextIsUniqueForMainThreadAndPartitions() throws Exception {
        String METHOD = "testStepContextIsUniqueForMainThreadAndPartitions";
        begin(METHOD);

        try {
            Reporter.log("Locate job XML file: job_partitioned_1step.xml<p>");

            Reporter.log("Invoke startJobAndWaitForResult<p>");
            JobExecution jobExecution = jobOp.startJobAndWaitForResult("job_partitioned_1step");

            Reporter.log("JobExecution getBatchStatus()=" + jobExecution.getBatchStatus() + "<p>");
            
            assertObjEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
            
            List<StepExecution> stepExecs = jobOp.getStepExecutions(jobExecution.getExecutionId());
            
            //only one step in job
            StepExecution stepExec = stepExecs.get(0);
            
            //verify step context is defaulted because it was never set on main thread.
            assertObjEquals("COMPLETED", stepExec.getExitStatus());
            
        } catch (Exception e) {
            handleException(METHOD, e);
        }
    }

    @AfterClass
    public static void cleanup() throws Exception {
    }

    private static void handleException(String methodName, Exception e) throws Exception {
		Reporter.log("Caught exception: " + e.getMessage()+"<p>");
		Reporter.log(methodName + " failed<p>");
		throw e;
	}

    private void begin(String str) {
        Reporter.log("Begin test method: " + str + "<p>");
    }
}
