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

import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;

import com.ibm.jbatch.tck.artifacts.reusable.MyPersistentUserData;
import com.ibm.jbatch.tck.utils.JobOperatorBridge;

import org.junit.BeforeClass;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class StepExecutionTests {

	private final static Logger logger = Logger.getLogger(StepExecutionTests.class.getName());

	private static JobOperatorBridge jobOp;

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
	public static void setUp()throws Exception {
		jobOp = new JobOperatorBridge();
	}

	@AfterClass
	public static void cleanup() throws Exception {
	}

	private void begin(String str) {
		Reporter.log("Begin test method: " + str + "<p>");
	}

	/*
	 * @testName: testOneStepExecutionStatus
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
	@Test
	@org.junit.Test
	public void testOneStepExecutionStatus() throws Exception {

		String METHOD = "testOneStepExecutionStatus";
		begin(METHOD);

		try {
			Reporter.log("Locate job XML file: job_batchlet_1step.xml<p>");

			Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
			JobExecution jobExec = jobOp.startJobAndWaitForResult("job_batchlet_1step");

			Reporter.log("Obtaining StepExecutions for execution id: " + jobExec.getExecutionId() + "<p>");
			List<StepExecution<?>> steps = jobOp.getStepExecutions(jobExec.getExecutionId());

			assertObjEquals(1, steps.size());

			for (StepExecution step : steps) {
				// make sure all steps finish successfully
				showStepState(step);
				Reporter.log("Step status = " + step.getBatchStatus() + "<p>");
				assertObjEquals(BatchStatus.COMPLETED, step.getBatchStatus());
			}

			Reporter.log("Job execution status = " + jobExec.getBatchStatus() + "<p>");
			assertObjEquals(BatchStatus.COMPLETED, jobExec.getBatchStatus());
		} catch (Exception e) {
			handleException(METHOD, e);
		}

	}

	/*
	 * @testName: testFourStepExecutionStatus
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
	@Test
	@org.junit.Test
	public void testFourStepExecutionStatus() throws Exception {

		String METHOD = "testFourStepExecutionStatus";
		begin(METHOD);

		try {
			Reporter.log("Locate job XML file: job_batchlet_4steps.xml<p>");

			Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
			JobExecution jobExec = jobOp.startJobAndWaitForResult("job_batchlet_4steps");

			Reporter.log("Obtaining StepExecutions for execution id: " + jobExec.getExecutionId() + "<p>");
			List<StepExecution<?>> steps = jobOp.getStepExecutions(jobExec.getExecutionId());
			assertObjEquals(4, steps.size());

			for (StepExecution step : steps) {
				// check that each step completed successfully
				showStepState(step);
				Reporter.log("Step status = " + step.getBatchStatus() + "<p>");
				assertObjEquals(BatchStatus.COMPLETED, step.getBatchStatus());
			}
			Reporter.log("Job execution status = " + jobExec.getBatchStatus() + "<p>");
			assertObjEquals(BatchStatus.COMPLETED, jobExec.getBatchStatus());
		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}

	/*
	 * @testName: testFailedStepExecutionStatus
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
	@Test
	@org.junit.Test  
	public void testFailedStepExecutionStatus() throws Exception {
		String METHOD = "testFailedStepExecutionStatus";
		begin(METHOD);

		try {
			Reporter.log("Locate job XML file: job_batchlet_failElement.xml<p>");

			Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
			JobExecution jobExec = jobOp.startJobAndWaitForResult("job_batchlet_failElement");

			Reporter.log("Obtaining StepExecutions for execution id: " + jobExec.getExecutionId() + "<p>");
			List<StepExecution<?>> steps = jobOp.getStepExecutions(jobExec.getExecutionId());
			assertObjEquals(1, steps.size());
			for (StepExecution step : steps) {
				// check that each step completed successfully
				// TODO: shouldn't the step status be failed here ???
				showStepState(step);
			}

			Reporter.log("Job execution getExitStatus()="+jobExec.getExitStatus()+"<p>");
			Reporter.log("Job execution getBatchStatus()="+jobExec.getBatchStatus()+"<p>");
			assertObjEquals("TEST_FAIL", jobExec.getExitStatus());
			assertObjEquals(BatchStatus.FAILED, jobExec.getBatchStatus());
		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}

	/*
	 * @testName: testStoppedStepExecutionStatus
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
	@Test
	@org.junit.Test  
	public void testStoppedStepExecutionStatus() throws Exception {
		String METHOD = "testStoppedStepExecutionStatus";
		begin(METHOD);

		try {
			Reporter.log("Locate job XML file: job_batchlet_stopElement.xml<p>");

			Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
			JobExecution jobExec = jobOp.startJobAndWaitForResult("job_batchlet_stopElement");

			Reporter.log("Obtaining StepExecutions for execution id: " + jobExec.getExecutionId() + "<p>");
			List<StepExecution<?>> steps = jobOp.getStepExecutions(jobExec.getExecutionId());
			assertObjEquals(1, steps.size());
			for (StepExecution step : steps) {
				// check that each step completed successfully
				// TODO: shouldn't the step status be stopped here ???
				showStepState(step);
			}

			Reporter.log("Job execution getBatchStatus()="+jobExec.getBatchStatus()+"<p>");
			assertObjEquals(BatchStatus.STOPPED, jobExec.getBatchStatus());
		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}

	/*
	 * @testName: testPersistedStepData
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
	@Test
	@org.junit.Test 
	public void testPersistedStepData() throws Exception {
		String METHOD = "testPersistedStepData";
		begin(METHOD);

		try {
			Reporter.log("Locate job XML file: job_batchlet_persistedData.xml<p>");

			Reporter.log("Create job parameters for execution #1:<p>");
			Properties jobParameters = new Properties();
			Reporter.log("force.failure=true<p>");
			jobParameters.setProperty("force.failure" , "true");

			Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
			JobExecution jobExec = jobOp.startJobAndWaitForResult("job_batchlet_persistedData", jobParameters);

			Reporter.log("execution #1 JobExecution getBatchStatus()="+jobExec.getBatchStatus()+"<p>");
			assertObjEquals(BatchStatus.FAILED, jobExec.getBatchStatus());

			//This job should only have one step.
			Reporter.log("Obtaining StepExecutions for execution id: " + jobExec.getExecutionId() + "<p>");
			List<StepExecution<?>> steps = jobOp.getStepExecutions(jobExec.getExecutionId());
			StepExecution stepExec = steps.get(0);
			assertObjEquals(1, steps.size());

			Reporter.log("execution #1 StepExecution getBatchStatus()="+jobExec.getBatchStatus()+"<p>");
			assertObjEquals(BatchStatus.FAILED, stepExec.getBatchStatus());
			assertObjEquals(4, ((MyPersistentUserData)stepExec.getUserPersistentData()).getData());

			//jobParameters.setProperty("force.failure" , "false");
			Reporter.log("Invoke restartJobAndWaitForResult with execution id: " + jobExec.getExecutionId() + "<p>");
			JobExecution restartedJobExec = jobOp.restartJobAndWaitForResult(jobExec.getExecutionId(),jobParameters);

			//This job should only have one step.

			steps = jobOp.getStepExecutions(restartedJobExec.getExecutionId());
			stepExec = steps.get(0);

			Reporter.log("execution #1 StepExecution getBatchStatus()="+jobExec.getBatchStatus()+"<p>");
			assertObjEquals(BatchStatus.COMPLETED, stepExec.getBatchStatus());
			assertObjEquals(5, ((MyPersistentUserData)stepExec.getUserPersistentData()).getData());		

		} catch (Exception e) {
			handleException(METHOD, e);
		}

	}


	/*
	 * @testName: testStepContainment
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
	@Test
	@org.junit.Test  
	public void testStepContainment() throws Exception {
		String METHOD = "testStepContainment";
		begin(METHOD);

		try {
			Reporter.log("Locate job XML file: job_batchlet_failElement.xml<p>");

			Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
			JobExecution jobExec = jobOp.startJobAndWaitForResult("job_batchlet_failElement");

			Reporter.log("Obtaining StepExecutions for execution id: " + jobExec.getExecutionId() + "<p>");
			List<StepExecution<?>> steps = jobOp.getStepExecutions(jobExec.getExecutionId());
			assertObjEquals(1, steps.size());
			for (StepExecution step : steps) {
				// check that each step completed successfully
				// TODO: shouldn't the step status be failed here ???
				showStepState(step);
			}

			String[] containment = null;
			for (StepExecution step : steps) {
				if (step.getStepName().equals("step1")) {
					containment = step.getStepContainment();
				}
			}

			Reporter.log("StepExecution.getStepContainment()="+getStepContainmentCSV(containment)+"<p>");
			assertObjEquals("", getStepContainmentCSV(containment));
		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}

	/*
	 * @testName: testStepInFlowContainment
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
	@Test
	@org.junit.Test  
	public void testStepInFlowContainment() throws Exception {
		String METHOD = "testStepInFlowContainment";
		begin(METHOD);

		try {
			Reporter.log("Locate job XML file: job_batchlet_failElement.xml<p>");
			Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
			JobExecution jobExec = jobOp.startJobAndWaitForResult("flow_transition_to_step");

			Reporter.log("Obtaining StepExecutions for execution id: " + jobExec.getExecutionId() + "<p>");
			List<StepExecution<?>> steps = jobOp.getStepExecutions(jobExec.getExecutionId());
			assertObjEquals(4, steps.size());
			for (StepExecution step : steps) {
				// check that each step completed successfully
				// TODO: shouldn't the step status be failed here ???
				showStepState(step);
			}

			String[] containment = null;
			for (StepExecution step : steps) {
				if (step.getStepName().equals("flow1step3")) {
					containment = step.getStepContainment();
				}
			}

			Reporter.log("StepExecution.getStepContainment()="+getStepContainmentCSV(containment)+"<p>");
			assertObjEquals("flow1", getStepContainmentCSV(containment));

		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}

	/*
	 * @testName: testStepInFlowInSplitContainment
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
	@Test
	@org.junit.Test  
	public void testStepInFlowInSplitContainment() throws Exception {
		String METHOD = "testStepInFlowInSplitContainment";
		begin(METHOD);

		try {
			Reporter.log("Locate job XML file: job_batchlet_failElement.xml<p>");
			Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
			JobExecution jobExec = jobOp.startJobAndWaitForResult("job_split_batchlet_4steps");

			Reporter.log("Obtaining StepExecutions for execution id: " + jobExec.getExecutionId() + "<p>");
			List<StepExecution<?>> steps = jobOp.getStepExecutions(jobExec.getExecutionId());
			assertObjEquals(4, steps.size());
			for (StepExecution step : steps) {
				// check that each step completed successfully
				// TODO: shouldn't the step status be failed here ???
				showStepState(step);
			}

			String[] containment = null;
			for (StepExecution step : steps) {
				if (step.getStepName().equals("step1")) {
					containment = step.getStepContainment();
				}
			}

			Reporter.log("StepExecution.getStepContainment()="+getStepContainmentCSV(containment)+"<p>");
			assertObjEquals("split1,flow1", getStepContainmentCSV(containment));

		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}

	/*
	 * @testName: testPartitionedStepContainment
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
	@Test
	@org.junit.Test  
	public void testPartitionedStepContainment() throws Exception {
		String METHOD = "testPartitionedStepContainment";
		begin(METHOD);

		try {
			Reporter.log("Locate job XML file: job_batchlet_longrunning_partitioned_inflow.xml<p>");
			Reporter.log("Create job parameters<p>");
			Properties overrideJobParams = new Properties();
			Reporter.log("run.indefinitely=false<p>");
			overrideJobParams.setProperty("run.indefinitely" , "false");

			Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
			JobExecution jobExec = jobOp.startJobAndWaitForResult("job_batchlet_longrunning_partitioned_inflow", overrideJobParams);

			Reporter.log("Obtaining StepExecutions for execution id: " + jobExec.getExecutionId() + "<p>");
			List<StepExecution<?>> steps = jobOp.getStepExecutions(jobExec.getExecutionId());
			assertObjEquals(3, steps.size());
			for (StepExecution step : steps) {
				// check that each step completed successfully
				// TODO: shouldn't the step status be failed here ???
				showStepState(step);
			}

			String[] containment = null;
			for (StepExecution step : steps) {
				if (step.getStepName().equals("step1")) {
					containment = step.getStepContainment();
				}
			}

			Reporter.log("StepExecution.getStepContainment()="+getStepContainmentCSV(containment)+"<p>");
			assertObjEquals("flow1", getStepContainmentCSV(containment));
		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}

	private void showStepState(StepExecution step) {


		Reporter.log("---------------------------<p>");
		Reporter.log("getStepName(): " + step.getStepName() + " - ");
		Reporter.log("getJobExecutionId(): " + step.getExecutionId() + " - ");
		//System.out.print("getStepExecutionId(): " + step.getStepExecutionId() + " - ");
		Metric[] metrics = step.getMetrics();

		for (int i = 0; i < metrics.length; i++) {
			Reporter.log(metrics[i].getType() + ": " + metrics[i].getValue() + " - ");
		}

		Reporter.log("getStartTime(): " + step.getStartTime() + " - ");
		Reporter.log("getEndTime(): " + step.getEndTime() + " - ");
		//System.out.print("getLastUpdateTime(): " + step.getLastUpdateTime() + " - ");
		Reporter.log("getBatchStatus(): " + step.getBatchStatus() + " - ");
		Reporter.log("getExitStatus(): " + step.getExitStatus()+"<p>");
		Reporter.log("---------------------------<p>");
	}

	private static void handleException(String methodName, Exception e) throws Exception {
		Reporter.log("Caught exception: " + e.getMessage()+"<p>");
		Reporter.log(methodName + " failed<p>");
		throw e;
	}

	public static String getStepContainmentCSV(String[] stepContainment) {

		if (stepContainment == null) {
			return "";
		}

		StringBuilder strBuilder = new StringBuilder();

		for (int i = 0; i < stepContainment.length; i++) {
			strBuilder.append(stepContainment[i]);

			if (i < stepContainment.length - 1) {
				strBuilder.append(",");
			}

		}
		return strBuilder.toString();
	}
}
