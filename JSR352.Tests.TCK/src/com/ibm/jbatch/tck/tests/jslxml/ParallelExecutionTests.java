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

import org.junit.BeforeClass;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ibm.jbatch.tck.utils.JobOperatorBridge;
import com.ibm.jbatch.tck.utils.TCKJobExecutionWrapper;


public class ParallelExecutionTests {

	private final static Logger logger = Logger.getLogger(ParallelExecutionTests.class.getName());

	private static final String TIME_TO_SLEEP_BEFORE_ISSUING_STOP = "1900"; 

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

	@AfterClass
	public static void cleanup() throws Exception {
	}


	private void begin(String str) {
		Reporter.log("Begin test method: " + str + "<p>");
	}

	/*
	 * @testName: testInvokeJobWithOnePartitionedStep
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
	@Test
	@org.junit.Test
	public void testInvokeJobWithOnePartitionedStep() throws Exception {
		String METHOD = "testInvokeJobWithOnePartitionedStep";
		begin(METHOD);

		try {
			Reporter.log("Locate job XML file: job_partitioned_1step.xml<p>");

			Reporter.log("Invoke startJobAndWaitForResult<p>");
			JobExecution jobExecution = jobOp.startJobAndWaitForResult("job_partitioned_1step");

			Reporter.log("JobExecution getBatchStatus()="+jobExecution.getBatchStatus()+"<p>");
			assertObjEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}

	/*
	 * @testName: testInvokeJobWithOnePartitionedStepExitStatus
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
	@Test
	@org.junit.Test
	public void testInvokeJobWithOnePartitionedStepExitStatus() throws Exception {
		String METHOD = "testInvokeJobWithOnePartitionedStepExitStatus";
		begin(METHOD);

		String DEFAULT_SLEEP_TIME = "2000";

		try {
			Properties jobParameters = new Properties();

			String sleepTime = System.getProperty("ParallelExecutionTests.testInvokeJobWithOnePartitionedStepExitStatus.sleep",DEFAULT_SLEEP_TIME);
			jobParameters.put("sleep.time", sleepTime);
			
			Reporter.log("Invoke startJobAndWaitForResult<p>");
			JobExecution jobExecution = jobOp.startJobAndWaitForResult("job_partitioned_1step_exitStatusTest",jobParameters);

			Reporter.log("JobExecution getBatchStatus()="+jobExecution.getBatchStatus()+"<p>");
			assertObjEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());

			List<StepExecution> stepExecutions = jobOp.getStepExecutions(jobExecution.getExecutionId());
			assertObjEquals(1, stepExecutions.size());

			for (StepExecution stepEx : stepExecutions) {
				assertObjEquals("STEP EXIT STATUS: 10", stepEx.getExitStatus());
				assertObjEquals(BatchStatus.COMPLETED, stepEx.getBatchStatus());
			}

			assertObjEquals("JOB EXIT STATUS: 10", jobExecution.getExitStatus());
			assertObjEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());

		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}

	/*
	 * @testName: testStopRunningPartitionedStep
	 * 
	 * @assertion: Issuing a JobOperator.stop() will stop all partitioned
	 * threads
	 * 
	 * @test_Strategy: A partitioned batchlet is run in an infinite loop with 4
	 * instances. The Test verifies that the job returns with STOPPED status
	 * instead of running forever.
	 */
	@Test
	@org.junit.Test
	public void testStopRunningPartitionedStep() throws Exception {
		String METHOD = "testStopRunningPartitionedStep";
		begin(METHOD);

		try {
			Reporter.log("Locate job XML file: job_batchlet_longrunning_partitioned.xml<p>");

			Reporter.log("Create job parameters<p>");
			Properties overrideJobParams = new Properties();
			Reporter.log("run.indefinitely=true<p>");
			overrideJobParams.setProperty("run.indefinitely" , "true");

			Reporter.log("Invoke startJobWithoutWaitingForResult<p>");
			JobExecution jobExecution =  jobOp.startJobWithoutWaitingForResult("job_batchlet_longrunning_partitioned", overrideJobParams);

			//Sleep long enough for parallel steps to fan out
			int sleepTime = Integer.parseInt(System.getProperty("ParallelExecutionTests.testStopRunningPartitionedStep.sleep",TIME_TO_SLEEP_BEFORE_ISSUING_STOP));
			Reporter.log("Sleep for " + TIME_TO_SLEEP_BEFORE_ISSUING_STOP);
			Thread.sleep(sleepTime);


			Reporter.log("Invoke stopJobAndWaitForResult<p>");
			jobOp.stopJobAndWaitForResult(jobExecution);

			JobExecution jobExec2 = jobOp.getJobExecution(jobExecution.getExecutionId());
			Reporter.log("JobExecution getBatchStatus()=" + jobExec2.getBatchStatus() + "<p>");
			assertObjEquals(BatchStatus.STOPPED, jobExec2.getBatchStatus());

		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}

	/*
	 * @testName: testStopRestartRunningPartitionedStep
	 * 
	 * @assertion: A stopped partitioned step can be restarted to completion.
	 * 
	 * @test_Strategy: A partitioned batchlet is run in an infinite loop with 4
	 * instances. The Test verifies that the job returns with STOPPED status
	 * instead of running forever. The job is restarted and each partition must 
	 * restart and run to completion.
	 */
	@Test
	@org.junit.Test()
	public void testStopRestartRunningPartitionedStep() throws Exception {
		String METHOD = "testStopRestartRunningPartitionedStep";
		begin(METHOD);

		try {
			Reporter.log("Locate job XML file: job_batchlet_longrunning_partitioned.xml<p>");

			Reporter.log("Create job parameters<p>");
			Properties jobParams = new Properties();
			Reporter.log("run.indefinitely=true<p>");
			jobParams.setProperty("run.indefinitely", "true");

			Reporter.log("Invoke startJobWithoutWaitingForResult<p>");
			JobExecution origJobExecution = jobOp.startJobWithoutWaitingForResult("job_batchlet_longrunning_partitioned", jobParams);

			// Sleep long enough for parallel steps to fan out
			int sleepTime = Integer.parseInt(System.getProperty("ParallelExecutionTests.testStopRestartRunningPartitionedStep.sleep",TIME_TO_SLEEP_BEFORE_ISSUING_STOP));
			Reporter.log("Sleep for " + TIME_TO_SLEEP_BEFORE_ISSUING_STOP);
			Thread.sleep(sleepTime);

			Reporter.log("Invoke stopJobAndWaitForResult<p>");
			jobOp.stopJobAndWaitForResult(origJobExecution);

			JobExecution jobExec2 = jobOp.getJobExecution(origJobExecution.getExecutionId());
			Reporter.log("JobExecution getBatchStatus()=" + jobExec2.getBatchStatus() + "<p>");
			assertObjEquals(BatchStatus.STOPPED, jobExec2.getBatchStatus());

			Reporter.log("Create restart job parameters<p>");
			Properties restartJobParams = new Properties();
			Reporter.log("run.indefinitely=true<p>");
			restartJobParams.setProperty("run.indefinitely", "false");

			Reporter.log("Invoke restartJobAndWaitForResult<p>");
			JobExecution restartedJobExec = jobOp.restartJobAndWaitForResult(origJobExecution.getExecutionId(), restartJobParams);

			Reporter.log("JobExecution getBatchStatus()=" + restartedJobExec.getBatchStatus() + "<p>");
			assertObjEquals(BatchStatus.COMPLETED, restartedJobExec.getBatchStatus());
		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}

	/*
	 * @testName: testInvokeJobSimpleSplit
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
	@Test
	@org.junit.Test
	public void testInvokeJobSimpleSplit() throws Exception {
		String METHOD = "testInvokeJobSimpleSplit";
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
	 * @testName: testPartitionedPlanCollectorAnalyzerReducerComplete
	 * 
	 * @assertion: Section 8.7. Partitioned artifact and Chunk processing order.
	 * Partition mapper can generate a plan that will determine partition
	 * instances and properties.
	 * 
	 * @test_Strategy: A chunk is partitioned into exactly 3 partitions, using
	 * the mapper, each with their own checkpointing. The collector, analyzer,
	 * and reducer, each append to the exit status to verify that they are
	 * called in the correct order. The persistent data is used to remember how
	 * many times the step has been run. If the data is not persisted
	 */
	@Test
	@org.junit.Test
	public void testPartitionedPlanCollectorAnalyzerReducerComplete() throws Exception {
		String METHOD = "testPartitionedPlanCollectorAnalyzerReducerComplete";
		begin(METHOD);

		try {
			Reporter.log("Locate job XML file: job_partitioned_artifacts.xml<p>");

			Reporter.log("Create Job parameters for Execution #1<p>");
			Properties jobParams = new Properties();
			Reporter.log("numPartitionsProp=3<p>");
			//append "CA" to expected exit status for each partition
			jobParams.setProperty("numPartitionsProp" , "3"); 

			Reporter.log("Invoke startJobAndWaitForResult<p>");
			JobExecution execution = jobOp.startJobAndWaitForResult("job_partitioned_artifacts", jobParams);

			Reporter.log("Execution exit status = " +  execution.getExitStatus()+"<p>");
			assertObjEquals("nullBeginCACACABeforeAfter", execution.getExitStatus());

			Reporter.log("Execution status = " + execution.getBatchStatus()+"<p>");
			assertObjEquals(BatchStatus.COMPLETED,execution.getBatchStatus());
		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}

	/*
	 * @testName: testZeroBasedPartitionedPlanCollectorAnalyzerReducerRollback
	 * 
	 * @assertion: Section 8.7. Partitioned artifact and Chunk processing order
	 * verifies that Rollback() is called on the reducer in case of a failure in
	 * any partition. Also verifies that the collector and analyzer are always
	 * called at the end of a partition even in the case or a failure, and
	 * partition properties are passed to partitions.
	 * 
	 * @test_Strategy: A mapper is used to generate exactly 3 partitions, with
	 * one partition marked to fail. Each partition appends to the exit status
	 * through the collector, analyzer, and finally the reducer.
	 */
	@Test
	@org.junit.Test
	public void testZeroBasedPartitionedPlanCollectorAnalyzerReducerRollback() throws Exception {
		String METHOD = "testZeroBasedPartitionedPlanCollectorAnalyzerReducerRollback";
		begin(METHOD);

		try {
			Reporter.log("Locate job XML file: job_partitioned_artifacts.xml<p>");

			Reporter.log("Create Job parameters for Execution #1<p>");
			Properties jobParams = new Properties();
			Reporter.log("numPartitionsProp=3<p>");
			Reporter.log("failThisPartition=0<p>");
			//append "CA" to expected exit status for each partition
			jobParams.setProperty("numPartitionsProp" , "3"); 
			jobParams.setProperty("failThisPartition" , "0"); //Remember we are 0 based

			Reporter.log("Invoke startJobAndWaitForResult<p>");
			JobExecution execution = jobOp.startJobAndWaitForResult("job_partitioned_artifacts", jobParams);

			Reporter.log("Execution exit status = " +  execution.getExitStatus()+"<p>");
			assertObjEquals("nullBeginCACACARollbackAfter", execution.getExitStatus());

			Reporter.log("Execution status = " + execution.getBatchStatus()+"<p>");
			assertObjEquals(BatchStatus.FAILED,execution.getBatchStatus());
		} catch (Exception e) {
			handleException(METHOD, e);
		}

	}

	/*
	 * @testName: testPartitionedCollectorAnalyzerReducerChunkRestartItemCount10
	 * 
	 * @assertion: Section 8.7. Partitioned artifact and Chunk processing order.
	 * and persistent data in partitioned step is actually persisted, and a
	 * completed partition is not restarted.
	 * 
	 * @test_Strategy: A chunk is partitioned into 3 partitions each with their
	 * own checkpointing. The collector, analyzer, and reducer, each append to
	 * the exit status to verify that they are called in the correct order. (The
	 * 'C' in the exit status String represents a call to the (C)ollector and
	 * the 'A' a call to the (A)nalyzer). The persistent data is used to remember 
	 * how many times the step has been run. If the data is not persisted correctly 
	 * the partitioned steps will not be * able to complete because the persisted count will not get incremented One
	 * of the partitions completes on the first attempt. The other two fail and
	 * must be restarted. We verify that the completed partition is not rerun
	 * since it does not append any data to the exit status.
	 */
	@Test
	@org.junit.Test
	public void testPartitionedCollectorAnalyzerReducerChunkRestartItemCount10() throws Exception {

		String METHOD = "testPartitionedCollectorAnalyzerReducerChunkRestartItemCount10";
		try {
			Reporter.log("Create job parameters for execution #1:<p>");
			Properties jobParams = new Properties();
			Reporter.log("readrecord.fail=23<p>");
			Reporter.log("app.arraysize=30<p>");
			Reporter.log("app.writepoints=0,5,10,15,20,25,30<p>");
			Reporter.log("app.next.writepoints=0,5,10,15,20,25,30<p>");
			jobParams.put("readrecord.fail", "23");
			jobParams.put("app.arraysize", "30");
			jobParams.put("app.writepoints", "0,10,20,30");
			jobParams.put("app.next.writepoints", "20,30");

			Reporter.log("Locate job XML file: chunkrestartPartitionedCheckpt10.xml<p>");

			Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
			TCKJobExecutionWrapper execution1 = jobOp.startJobAndWaitForResult("chunkrestartPartitionedCheckpt10", jobParams);

			{ // Use block to reduce copy/paste errors
				Reporter.log("execution #1 JobExecution getBatchStatus()=" + execution1.getBatchStatus() + "<p>");
				Reporter.log("execution #1 JobExecution getExitStatus()=" + execution1.getExitStatus() + "<p>");
				assertWithMessage("Testing execution #1", BatchStatus.FAILED, execution1.getBatchStatus());

				// '2' each from partition that gets through the first two chunks and fails during the third
				// '4' for the partition that gets through all three chunks, plus one call for end of partition
				int CA_PAIRS = 2 + 2 + 4;   
				String CA = "CA"; // This is a (C)ollector(A)nalyzerPair

				String expectedExitStatus = "null"; // verifies clean exit status
				expectedExitStatus += "Begin";    // From PartitionReducer beginPartitionedStep()
				for (int i = 0; i < CA_PAIRS; i++, expectedExitStatus += CA);
				expectedExitStatus += "Rollback"; // From PartitionReducer rollbackPartitionedStep()
				expectedExitStatus += "After";    // From PartitionReducer afterPartitionedStepCompletion()

				// Should be: "nullBeginCACACACACACACACARollbackAfter"
				assertWithMessage("Testing execution #1", expectedExitStatus, execution1.getExitStatus());
			}

			{
				long execution1Id = execution1.getExecutionId();
				long execution1InstanceId = execution1.getInstanceId();
				Reporter.log("Invoke restartJobAndWaitForResult with execution id: " + execution1Id + "<p>");
				TCKJobExecutionWrapper execution2 = jobOp.restartJobAndWaitForResult(execution1Id, jobParams);

				Reporter.log("execution #2 JobExecution getBatchStatus()=" + execution2.getBatchStatus() + "<p>");
				Reporter.log("execution #2 JobExecution getExitStatus()=" + execution2.getExitStatus() + "<p>");
				Reporter.log("execution #2 Job instance id=" + execution2.getInstanceId() + "<p>");
				Reporter.log("execution #2 Job execution id=" + execution2.getExecutionId() + "<p>");
				
				// '2' for each of the two partitions that process chunks #2, #3, and each make one C+A call 
				// at the end of the partition and '0' for the partition already complete.
				int CA_PAIRS = 2 + 2;
				String CA = "CA"; // This is a (C)ollector(A)nalyzerPair

				String expectedExitStatus = "null"; // verifies clean exit status
				expectedExitStatus += "Begin";    // From PartitionReducer beginPartitionedStep()
				for (int i = 0; i < CA_PAIRS; i++, expectedExitStatus += CA);
				expectedExitStatus += "Before"; // From PartitionReducer beforePartitionedStepCompletion()
				expectedExitStatus += "After";    // From PartitionReducer afterPartitionedStepCompletion()

				// Should be: "nullBeginCACACACABeforeAfter"
				assertWithMessage("Testing execution #2 exit status", expectedExitStatus, execution2.getExitStatus());
				assertWithMessage("Testing execution #2 batch status", BatchStatus.COMPLETED, execution2.getBatchStatus());
				assertWithMessage("Testing execution #2 instance ID", execution1InstanceId, execution2.getInstanceId());
			}

		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}
	
	/*
     * @testName: testPartitionedMapperOverrideFalseOnRestart
     * 
     * @assertion: Section 8.7. Partitioned artifact and Chunk processing order.
     * Partition mapper can generate a plan that will determine partition
     * instances and properties.
     * 
     * @test_Strategy: 
     */
    @Test
    @org.junit.Test
    public void testPartitionedMapperOverrideFalseOnRestart() throws Exception {
        String METHOD = "testPartitionedMapperOverrideFalse";
        begin(METHOD);

        try {
            Reporter.log("Locate job XML file: job_partitioned_artifacts.xml<p>");

            Reporter.log("Create Job parameters for Execution #1<p>");
            Properties jobParams = new Properties();
            Reporter.log("numPartitionsProp=3<p>");
            Reporter.log("failThisPartition=0<p>");
            //append "CA" to expected exit status for each partition
            jobParams.setProperty("numPartitionsProp" , "3"); 
            jobParams.setProperty("failThisPartition" , "0"); //Remember we are 0 based
            jobParams.setProperty("partitionsOverride", "false");

            Reporter.log("Invoke startJobAndWaitForResult<p>");
            JobExecution execution = jobOp.startJobAndWaitForResult("job_partitioned_artifacts", jobParams);

            Reporter.log("Execution exit status = " +  execution.getExitStatus()+"<p>");
            assertObjEquals("nullBeginCACACARollbackAfter", execution.getExitStatus());

            Reporter.log("Execution status = " + execution.getBatchStatus()+"<p>");
            assertObjEquals(BatchStatus.FAILED,execution.getBatchStatus());
            
            Reporter.log("Set restart job parameters<p>");
            jobParams.setProperty("numPartitionsProp" , "7"); 
            jobParams.setProperty("failThisPartition" , "5"); //Remember we are 0 based
            jobParams.setProperty("partitionsOverride", "false");
            
            JobExecution execution2 = jobOp.restartJobAndWaitForResult(execution.getExecutionId(), jobParams);
            Reporter.log("Execution exit status = " +  execution2.getExitStatus()+"<p>");
            assertObjEquals("nullBeginCABeforeAfter", execution2.getExitStatus());
            
        } catch (Exception e) {
            handleException(METHOD, e);
        }
    }

    
    /*
     * @testName: testPartitionedMapperOverrideTrueDiffPartitionNumOnRestart
     * 
     * @assertion: 
     * 
     * @test_Strategy: 
     */
    @Test
    @org.junit.Test
    public void testPartitionedMapperOverrideTrueDiffPartitionNumOnRestart() throws Exception {
        String METHOD = "testPartitionedMapperOverrideFalse";
        begin(METHOD);

        try {
            Reporter.log("Locate job XML file: job_partitioned_artifacts.xml<p>");

            Reporter.log("Create Job parameters for Execution #1<p>");
            Properties jobParams = new Properties();
            Reporter.log("numPartitionsProp=3<p>");
            Reporter.log("failThisPartition=0<p>");
            //append "CA" to expected exit status for each partition
            jobParams.setProperty("numPartitionsProp" , "2"); 
            jobParams.setProperty("failThisPartition" , "0"); //Remember we are 0 based
            jobParams.setProperty("partitionsOverride", "false");

            Reporter.log("Invoke startJobAndWaitForResult<p>");
            JobExecution execution = jobOp.startJobAndWaitForResult("job_partitioned_artifacts", jobParams);

            Reporter.log("Execution exit status = " +  execution.getExitStatus()+"<p>");
            assertObjEquals("nullBeginCACARollbackAfter", execution.getExitStatus());

            Reporter.log("Execution status = " + execution.getBatchStatus()+"<p>");
            assertObjEquals(BatchStatus.FAILED,execution.getBatchStatus());
            
            Reporter.log("Set restart job parameters<p>");
            jobParams.setProperty("numPartitionsProp" , "4"); 
            jobParams.setProperty("failThisPartition" , "3"); //Remember we are 0 based
            jobParams.setProperty("partitionsOverride", "true");
            
            JobExecution execution2 = jobOp.restartJobAndWaitForResult(execution.getExecutionId(), jobParams);
            Reporter.log("Execution exit status = " +  execution2.getExitStatus()+"<p>");
            assertObjEquals("nullBeginCACACACARollbackAfter", execution2.getExitStatus());
            
        } catch (Exception e) {
            handleException(METHOD, e);
        }
    }
    
    /*
     * @testName: testPartitionedMapperOverrideTrueSamePartitionNumOnRestart
     * 
     * @assertion: 
     * 
     * @test_Strategy: 
     */
    @Test
    @org.junit.Test
    public void testPartitionedMapperOverrideTrueSamePartitionNumOnRestart() throws Exception {
        String METHOD = "testPartitionedMapperOverrideFalse";
        begin(METHOD);

        try {
            Reporter.log("Locate job XML file: job_partitioned_artifacts.xml<p>");

            Reporter.log("Create Job parameters for Execution #1<p>");
            Properties jobParams = new Properties();
            Reporter.log("numPartitionsProp=3<p>");
            Reporter.log("failThisPartition=0<p>");
            //append "CA" to expected exit status for each partition
            jobParams.setProperty("numPartitionsProp" , "3"); 
            jobParams.setProperty("failThisPartition" , "0"); //Remember we are 0 based
            jobParams.setProperty("partitionsOverride", "false");

            Reporter.log("Invoke startJobAndWaitForResult<p>");
            JobExecution execution = jobOp.startJobAndWaitForResult("job_partitioned_artifacts", jobParams);

            Reporter.log("Execution exit status = " +  execution.getExitStatus()+"<p>");
            assertObjEquals("nullBeginCACACARollbackAfter", execution.getExitStatus());

            Reporter.log("Execution status = " + execution.getBatchStatus()+"<p>");
            assertObjEquals(BatchStatus.FAILED,execution.getBatchStatus());
            
            Reporter.log("Set restart job parameters<p>");
            jobParams.setProperty("numPartitionsProp" , "3"); 
            jobParams.setProperty("failThisPartition" , "1"); //Remember we are 0 based
            jobParams.setProperty("partitionsOverride", "true");
            
            JobExecution execution2 = jobOp.restartJobAndWaitForResult(execution.getExecutionId(), jobParams);
            Reporter.log("Execution exit status = " +  execution2.getExitStatus()+"<p>");
            assertObjEquals("nullBeginCACACARollbackAfter", execution2.getExitStatus());
            
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
