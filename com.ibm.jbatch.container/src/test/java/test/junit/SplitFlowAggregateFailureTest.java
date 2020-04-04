/*
 * Copyright 2014 International Business Machines Corp.
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
package test.junit;

import java.util.List;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.StepExecution;
import jakarta.batch.runtime.context.StepContext;
import jakarta.inject.Inject;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import test.utils.TCKCandidate;


public class SplitFlowAggregateFailureTest {

	private static JobOperator jobOp;
	private static int sleepTime = 2000;
	
	@BeforeClass
	public static void setup() throws Exception {
		jobOp = BatchRuntime.getJobOperator();
	}

	@Test
	@TCKCandidate("If we think this is well-specified enough")
	public void testFailureAfterOnlyOneFlowFails() throws Exception {
		
		long theExecId = jobOp.start("splitFlowAggregateFailure", null);
		Thread.sleep(sleepTime);
		
		// Check job COMPLETED since some validation is crammed into the execution.
		JobExecution je = jobOp.getJobExecution(theExecId);
		assertEquals("Test failure", "FAILED", je.getBatchStatus().toString());

		List<StepExecution> stepExecutions = jobOp.getStepExecutions(theExecId);
		assertEquals("Number StepExecutions", 2, stepExecutions.size());

		for (StepExecution se : stepExecutions) {
			if (se.getStepName().equals("flow1step1")) {
				assertEquals("First flow should complete successfully", BatchStatus.COMPLETED, se.getBatchStatus());
			} else if (se.getStepName().equals("flow2step1")) {
				assertEquals("Second flow should fail", BatchStatus.FAILED, se.getBatchStatus());
			} else {
				throw new Exception("Unexpected step: " + se.getStepName());
			}
		}
	}

	/**
	 * Test artifacts below
	 */
	public static class SFB extends AbstractBatchlet {

		@Inject StepContext sc;

		@Override
		public String process() throws Exception {
			if (sc.getStepName().equals("flow1step1")) {
				return "COMPLETED";
			} else {
				throw new Exception("Fail on step name: " + sc.getStepName());
			} 
		}
	}

}
