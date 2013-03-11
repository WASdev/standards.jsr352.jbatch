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
package com.ibm.jbatch.tck.tests.jslxml;

import static com.ibm.jbatch.tck.utils.AssertionUtils.assertWithMessage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import javax.batch.operations.JobStartException;
import javax.batch.operations.JobOperator.BatchStatus;
import javax.batch.runtime.JobExecution;

import com.ibm.jbatch.tck.utils.JobOperatorBridge;

import org.junit.Before;
import org.junit.Ignore;
import org.testng.Reporter;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class FlowTransitioningTests {

	private JobOperatorBridge jobOp = null;
	
	/**
	 * @testName: testFlowTransitionToStep
	 * @assertion: Section 5.3 Flow
	 * @test_Strategy: 1. setup a job consisting of one flow (w/ 3 steps) and one step
	 * 				   2. start job 
	 * 				   3. create a list of step id's as they are processed
	 * 				   4. return the list from step 3 as job exit status
	 * 				   5. compare that list to our transition list
	 * 		           6. verify that in fact we transition from each step within the flow, then to the flow "next" step
	 * 
	 * @throws JobStartException
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test @org.junit.Test
	public void testFlowTransitionToStep() throws Exception {

		String METHOD = "testFlowTransitionToStep";
		
		try {

			String[] transitionList = {"flow1step1", "flow1step2", "flow1step3", "step1"};
			Reporter.log("starting job");
			JobExecution jobExec = jobOp.startJobAndWaitForResult("flow_transition_to_step", null);
			Reporter.log("Job Status = " + jobExec.getBatchStatus());
			
			String[] jobTransitionList = jobExec.getExitStatus().split(",");
			assertWithMessage("transitioned to exact number of steps", jobTransitionList.length == transitionList.length);
			for (int i = 0; i < jobTransitionList.length; i++) {
				assertWithMessage("Flow transitions", transitionList[i].equals(jobTransitionList[i].trim())  );
			}
			
			assertWithMessage("Job completed", jobExec.getBatchStatus().equals(BatchStatus.COMPLETED));
			Reporter.log("Job completed");
		} catch (Exception e) {
    		handleException(METHOD, e);
    	}
	}
	
	/**
	 * @testName: testFlowTransitionToStepOutOfScope
	 * @assertion: Section 5.3 Flow
	 * @test_Strategy: 1. setup a job consisting of one flow (w/ 3 steps) and one step
	 * 				   2. start job 
	 * 				   3. this job should fail because the flow step flow1step2 next to outside the flow
	 * 
	 * 	<flow id="flow1">
	 *		<step id="flow1step1" next="flow1step2">
	 *			<batchlet ref="flowTransitionToStepTestBatchlet"/>
	 *		</step>
	 *		<step id="flow1step2" next="step1">
	 *			<batchlet ref="flowTransitionToStepTestBatchlet"/>
	 *		</step>
	 *		<step id="flow1step3">
	 *			<batchlet ref="flowTransitionToStepTestBatchlet"/>
	 *		</step>
	 *	</flow>
	
	 *	<step id="step1">
	 *		<batchlet ref="flowTransitionToStepTestBatchlet"/>
	 * 	</step>
	 *
	 * @throws JobStartException
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws InterruptedException
	 */
    @Test(enabled = false)
    @org.junit.Test
    @Ignore
	public void testFlowTransitionToStepOutOfScope() throws Exception {
    	
    	String METHOD = " testFlowTransitionToStepOutOfScope";

    	try {
    	
			Reporter.log("starting job");
			JobExecution jobExec = null;
			try {
				jobExec = jobOp.startJobAndWaitForResult("flow_transition_to_step_out_of_scope", null);
			} catch (JobStartException e) {
				Reporter.log("job failed to start " + e.getLocalizedMessage());
			}
			
			Reporter.log("Job Status = " + jobExec.getBatchStatus());
			
			assertWithMessage("Job should have failed because of out of scope execution elements.", jobExec.getBatchStatus().equals(BatchStatus.FAILED));
    	} catch (Exception e) {
    		handleException(METHOD, e);
    	}
	}
	
	/**
	 * @testName: testFlowTransitionToDecision
	 * @assertion: Section 5.3 Flow
	 * @test_Strategy: 1. setup a job consisting of one flow (w/ 3 steps) and one decision
	 * 				   2. start job 
	 * 				   3. flow will transition to decider which will change the exit status
	 * 				   4. compare that the exit status set by the decider matches that of the job
	 * 
	 * @throws JobStartException
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test @org.junit.Test
	public void testFlowTransitionToDecision() throws Exception {

		String METHOD = "testFlowTransitionToDecision";
		
		try {
			String exitStatus = "ThatsAllFolks";
			// based on our decider exit status
			/*
			<decision id="decider1" ref="flowTransitionToDecisionTestDecider">
				<end exit-status="ThatsAllFolks" on="DECIDER_EXIT_STATUS*VERY GOOD INVOCATION" />
			</decision>
			*/
			Reporter.log("starting job");
			JobExecution jobExec = jobOp.startJobAndWaitForResult("flow_transition_to_decision", null);
			Reporter.log("Job Status = " + jobExec.getBatchStatus());
			
			assertWithMessage("Job Exit Status is from decider", exitStatus, jobExec.getExitStatus());
			assertWithMessage("Job completed", jobExec.getBatchStatus().equals(BatchStatus.COMPLETED));
			Reporter.log("Job completed");
		} catch (Exception e) {
    		handleException(METHOD, e);
    	}
	}
	
	/**
	 * @testName: testFlowTransitionWithinFlow
	 * @assertion: Section 5.3 Flow
	 * @test_Strategy: 1. setup a job consisting of one flow (w/ 3 steps and 1 decision)
	 * 				   2. start job 
	 * 				   3. within the flow step1 will transition to decider then to step2 and finally step3.
	 * 				   4. create a list of step id's as they are processed
	 * 				   4. return the list from step 3 as job exit status
	 * 				   5. compare that list to our transition list
	 * 		           6. verify that in fact we transition from each step within the flow, then to the flow "next" step
	 * 
	 * @throws JobStartException
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test @org.junit.Test
	public void testFlowTransitionWithinFlow() throws Exception {

		String METHOD = "testFlowTransitionWithinFlow";
		
		try {
			String[] transitionList = {"flow1step1", "flow1step2", "flow1step3"};
			Reporter.log("starting job");
			JobExecution jobExec = jobOp.startJobAndWaitForResult("flow_transition_within_flow", null);
			Reporter.log("Job Status = " + jobExec.getBatchStatus());
			
			String[] jobTransitionList = jobExec.getExitStatus().split(",");
			assertWithMessage("transitioned to exact number of steps", jobTransitionList.length == transitionList.length);
			for (int i = 0; i < jobTransitionList.length; i++) {
				assertWithMessage("Flow transitions", transitionList[i].equals(jobTransitionList[i].trim())  );
			}
					
			assertWithMessage("Job completed", jobExec.getBatchStatus().equals(BatchStatus.COMPLETED));
			Reporter.log("Job completed");
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
