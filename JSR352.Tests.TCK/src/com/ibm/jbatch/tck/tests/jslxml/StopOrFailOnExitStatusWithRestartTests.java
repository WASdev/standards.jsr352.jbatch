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

import java.util.Properties;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;

import org.junit.BeforeClass;
import org.testng.Reporter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ibm.jbatch.tck.utils.JobOperatorBridge;
import com.ibm.jbatch.tck.utils.TCKJobExecutionWrapper;

public class StopOrFailOnExitStatusWithRestartTests {

	private static JobOperatorBridge jobOp;

	private void begin(String str) {
		Reporter.log("Begin test method: " + str+"<p>");
	}

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

	public static void cleanup() throws Exception {
	}

	/*
	 * @testName: testInvokeJobWithUserStopAndRestart
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
	@Test
	@org.junit.Test
	public void testInvokeJobWithUserStopAndRestart() throws Exception {

		String METHOD = "testInvokeJobWithUserStopAndRestart";
		begin(METHOD);

		final String DEFAULT_SLEEP_TIME = "5000";

		try {
			Reporter.log("Locate job XML file: job_batchlet_longrunning.xml<p>");

			Reporter.log("Create job parameters for execution #1:<p>");
			Properties overrideJobParams = new Properties();
			Reporter.log("run.indefinitely=true<p>");
			overrideJobParams.setProperty("run.indefinitely" , "true");

			Reporter.log("Invoke startJobWithoutWaitingForResult for execution #1<p>");
			TCKJobExecutionWrapper execution1 = jobOp.startJobWithoutWaitingForResult("job_batchlet_longrunning", overrideJobParams);

			long execID = execution1.getExecutionId(); 
			Reporter.log("StopRestart: Started job with execId=" + execID + "<p>");

			int sleepTime = Integer.parseInt(System.getProperty("StopOrFailOnExitStatusWithRestartTests.testInvokeJobWithUserStop.sleep",DEFAULT_SLEEP_TIME));
			Reporter.log("Sleep " +  sleepTime  + "<p>");
			Thread.sleep(sleepTime); 

			Reporter.log("execution #1 JobExecution getBatchStatus()="+execution1.getBatchStatus()+"<p>");
			assertWithMessage("Hopefully job isn't finished already, if it is fail the test and use a longer sleep time within the batch step-related artifact.",
					BatchStatus.STARTED, execution1.getBatchStatus());

			Reporter.log("Invoke stopJobAndWaitForResult");
			jobOp.stopJobAndWaitForResult(execution1);

			Reporter.log("execution #1 JobExecution getBatchStatus()="+execution1.getBatchStatus()+"<p>");
			assertWithMessage("The stop should have taken effect by now, even though the batchlet artifact had control at the time of the stop, it should have returned control by now.", 
					BatchStatus.STOPPED, execution1.getBatchStatus());  

			Reporter.log("execution #1 JobExecution getBatchStatus()="+execution1.getExitStatus()+"<p>");
			assertWithMessage("If this assert fails with an exit status of STOPPED, try increasing the sleep time. It's possible" +
					"the JobOperator stop is being issued before the Batchlet has a chance to run.", "BATCHLET CANCELED BEFORE COMPLETION", execution1.getExitStatus());

			Reporter.log("Create job parameters for execution #2:<p>");
			Reporter.log("run.indefinitely=false<p>");
			overrideJobParams.setProperty("run.indefinitely" , "false");


			Reporter.log("Invoke restartJobAndWaitForResult with executionId: " + execution1.getInstanceId() + "<p>");
			JobExecution execution2 = jobOp.restartJobAndWaitForResult(execution1.getExecutionId(),overrideJobParams);

			Reporter.log("execution #2 JobExecution getBatchStatus()="+execution2.getBatchStatus()+"<p>");
			assertWithMessage("If the restarted job hasn't completed yet then try increasing the sleep time.", 
					BatchStatus.COMPLETED, execution2.getBatchStatus());

			Reporter.log("execution #2 JobExecution getExitStatus()="+execution2.getExitStatus()+"<p>");
			assertWithMessage("If this fails, the reason could be that step 1 didn't run the second time," + 
					"though it should since it won't have completed successfully the first time.", 
					"GOOD.STEP.GOOD.STEP", execution2.getExitStatus());
		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}

	/*
	 * @testName: testInvokeJobWithUncaughtExceptionFailAndRestart
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
	@Test
	@org.junit.Test
	public void testInvokeJobWithUncaughtExceptionFailAndRestart() throws Exception {
		String METHOD = "testInvokeJobWithUncaughtExceptionFailAndRestart";
		begin(METHOD);

		try {
			Reporter.log("Locate job XML file: job_batchlet_longrunning.xml<p>");

			Reporter.log("Create job parameters for execution #1:<p>");
			Properties jobParameters = new Properties();
			Reporter.log("throw.exc.on.number.3=true<p>");
			jobParameters.setProperty("throw.exc.on.number.3" , "true");  // JSL default is 'false'

			Reporter.log("Invoke startJobAndWaitForResult");
			TCKJobExecutionWrapper firstJobExecution = jobOp.startJobAndWaitForResult("job_batchlet_longrunning", jobParameters);

			Reporter.log("Started job with execId=" + firstJobExecution.getExecutionId()+"<p>");       

			Reporter.log("execution #1 JobExecution getBatchStatus()="+firstJobExecution.getBatchStatus()+"<p>");
			Reporter.log("execution #1 JobExecution getExitStatus()="+firstJobExecution.getExitStatus()+"<p>");
			assertWithMessage("If the job hasn't failed yet then try increasing the sleep time.", BatchStatus.FAILED, firstJobExecution.getBatchStatus());    
			assertObjEquals("FAILED", firstJobExecution.getExitStatus());

			Reporter.log("Create job parameters for execution #2:<p>");
			Properties overrideJobParams = new Properties();
			Reporter.log("throw.exc.on.number.3=false<p>");
			Reporter.log("run.indefinitely=false<p>");
			overrideJobParams.setProperty("throw.exc.on.number.3" , "false");
			overrideJobParams.setProperty("run.indefinitely" , "false");

			Reporter.log("Invoke restartJobAndWaitForResult with executionId: " + firstJobExecution.getInstanceId() + "<p>");
			JobExecution secondJobExecution = jobOp.restartJobAndWaitForResult(firstJobExecution.getExecutionId(),overrideJobParams);

			Reporter.log("execution #2 JobExecution getBatchStatus()="+secondJobExecution.getBatchStatus()+"<p>");
			assertWithMessage("If the restarted job hasn't completed yet then try increasing the sleep time.", 
					BatchStatus.COMPLETED, secondJobExecution.getBatchStatus());

			Reporter.log("execution #2 JobExecution getExitStatus()="+secondJobExecution.getExitStatus()+"<p>");
			assertWithMessage("If this fails with only \"GOOD.STEP\", the reason could be that step 1 didn't run the second time," + 
					"though it should since it won't have completed successfully the first time.", 
					"GOOD.STEP.GOOD.STEP", secondJobExecution.getExitStatus());
		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}



	private static void handleException(String methodName, Exception e) throws Exception {
		Reporter.log("Caught exception: " + e.getMessage()+"<p>");
		Reporter.log(methodName + " failed<p>");
		throw e;
	}
}
