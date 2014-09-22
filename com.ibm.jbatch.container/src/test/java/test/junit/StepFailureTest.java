/**
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
package test.junit;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.StepExecution;

import org.junit.BeforeClass;
import org.junit.Test;

public class StepFailureTest {

	static JobOperator jobOp = null;
	
	int normalSleepTime = 1200;

	@BeforeClass 
	public static void setup() {
		jobOp = BatchRuntime.getJobOperator();
	}

	/*
	 * This test just shows we can still execute a single step and end normally
	 */
	@Test
	public void testEndNormallyExecuteOne() throws Exception {
		JobOperator jo = BatchRuntime.getJobOperator();
		long exec1Id = jo.start("nextOnStep1Failure", null);
		Thread.sleep(normalSleepTime);
		JobExecution je = jo.getJobExecution(exec1Id);
		List<StepExecution> steps = jo.getStepExecutions(exec1Id);
		
		assertEquals("Job batch status", BatchStatus.COMPLETED, je.getBatchStatus());
		assertEquals("exit status", "FINISH EARLY", je.getExitStatus());
		assertEquals("Steps executed", 1, steps.size());
		assertEquals("Step failed", BatchStatus.COMPLETED, steps.get(0).getBatchStatus());
	}
	
	/*
	 * Here it gets more interesting:  we fail the first step then go on to execute the 2dn.
	 */
	@Test
	public void testEndNormallyExecuteBoth() throws Exception {
		JobOperator jo = BatchRuntime.getJobOperator();
		Properties props = new Properties();
		props.put("forceFailure", "true");
		long exec1Id = jo.start("nextOnStep1Failure", props);
		Thread.sleep(normalSleepTime);
		JobExecution je = jo.getJobExecution(exec1Id);
		Map<String, StepExecution> steps = new HashMap<String,StepExecution>();
		for (StepExecution se : jo.getStepExecutions(exec1Id)) { steps.put(se.getStepName(), se); }
		
		assertEquals("Job batch status", BatchStatus.COMPLETED, je.getBatchStatus());
		assertEquals("exit status", "COMPLETED", je.getExitStatus());
		assertEquals("Steps executed", 2, steps.size());
		assertEquals("Step failed", BatchStatus.FAILED, steps.get("step1").getBatchStatus());
		assertEquals("Step failed", BatchStatus.COMPLETED, steps.get("step2").getBatchStatus());
	}
	
	@Test
	public void testStopOnStep1Failure() throws Exception {
		JobOperator jo = BatchRuntime.getJobOperator();
		Properties props = new Properties();
		props.put("forceFailure", "true");
		long exec1Id = jo.start("stopOnStep1Failure", props);
		Thread.sleep(normalSleepTime);
		JobExecution je = jo.getJobExecution(exec1Id);
		List<StepExecution> steps = jo.getStepExecutions(exec1Id);
		
		assertEquals("Job batch status", BatchStatus.STOPPED, je.getBatchStatus());
		assertEquals("exit status", "IT.STOPPED", je.getExitStatus());
		assertEquals("Steps executed", 1, steps.size());
		assertEquals("Step failed", BatchStatus.FAILED, steps.get(0).getBatchStatus());
	}
	
	@Test
	public void testStopOnStep1FailureDefaultExitStatus() throws Exception {
		JobOperator jo = BatchRuntime.getJobOperator();
		Properties props = new Properties();
		props.put("forceFailure", "true");
		long exec1Id = jo.start("stopOnStep1FailureDefaultExitStatus", props);
		Thread.sleep(normalSleepTime);
		JobExecution je = jo.getJobExecution(exec1Id);
		List<StepExecution> steps = jo.getStepExecutions(exec1Id);
		
		assertEquals("Job batch status", BatchStatus.STOPPED, je.getBatchStatus());
		assertEquals("exit status", "STOPPED", je.getExitStatus());
		assertEquals("Steps executed", 1, steps.size());
		assertEquals("Step failed", BatchStatus.FAILED, steps.get(0).getBatchStatus());
	}
	
	@Test
	public void testEndOnStep1Failure() throws Exception {
		JobOperator jo = BatchRuntime.getJobOperator();
		Properties props = new Properties();
		props.put("forceFailure", "true");
		long exec1Id = jo.start("endOnStep1Failure", props);
		Thread.sleep(normalSleepTime);
		JobExecution je = jo.getJobExecution(exec1Id);
		List<StepExecution> steps = jo.getStepExecutions(exec1Id);
		
		assertEquals("Job batch status", BatchStatus.COMPLETED, je.getBatchStatus());
		assertEquals("exit status", "ALL DONE", je.getExitStatus());
		assertEquals("Steps executed", 1, steps.size());
		assertEquals("Step failed", BatchStatus.FAILED, steps.get(0).getBatchStatus());
	}

	@Test
	public void testFailOnStep1Failure() throws Exception {
		JobOperator jo = BatchRuntime.getJobOperator();
		Properties props = new Properties();
		props.put("forceFailure", "true");
		long exec1Id = jo.start("failOnStep1Failure", props);
		Thread.sleep(normalSleepTime);
		JobExecution je = jo.getJobExecution(exec1Id);
		List<StepExecution> steps = jo.getStepExecutions(exec1Id);
		
		assertEquals("Job batch status", BatchStatus.FAILED, je.getBatchStatus());
		assertEquals("exit status", "WE FAILED", je.getExitStatus());
		assertEquals("Steps executed", 1, steps.size());
		assertEquals("Step failed", BatchStatus.FAILED, steps.get(0).getBatchStatus());
	}
}
