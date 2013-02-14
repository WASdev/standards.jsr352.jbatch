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

import javax.batch.operations.JobOperator.BatchStatus;
import javax.batch.operations.exception.JobStartException;
import javax.batch.runtime.JobExecution;

import jsr352.tck.utils.JobOperatorBridge;

import org.junit.Before;
import org.testng.Reporter;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class SplitFlowTransitionLoopTests {

	private JobOperatorBridge jobOp = null;
	
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
    @Test
    @org.junit.Test
	public void testSplitFlowTransitionLoopSplitFlowSplit() throws JobStartException {

		Reporter.log("starting job");
		JobExecution jobExec = jobOp.startJobAndWaitForResult("split_flow_transition_loop_splitflowsplit", null);

		Reporter.log("Job Status = " + jobExec.getBatchStatus());
		
		assertWithMessage("Job completed", jobExec.getBatchStatus().equals(BatchStatus.COMPLETED));
		Reporter.log("job completed");
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
