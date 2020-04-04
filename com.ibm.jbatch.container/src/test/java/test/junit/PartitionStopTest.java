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

import static org.junit.Assert.assertEquals;

import java.util.List;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.StepExecution;

import org.junit.BeforeClass;
import org.junit.Test;

public class PartitionStopTest {
	
	private static JobOperator jobOp = null;
	private static int sleepTime = 1000;
	
	@BeforeClass
	public static void init() {
		jobOp = BatchRuntime.getJobOperator();
	}

	@Test
	public void testStopForJob() throws Exception {
		long execID = jobOp.start("partitionStopTest", null);
		Thread.sleep(sleepTime);
		jobOp.stop(execID);
		
		Thread.sleep(sleepTime);
		JobExecution je = jobOp.getJobExecution(execID);
		assertEquals("Job BatchStatus: ", BatchStatus.STOPPED, je.getBatchStatus());
	}

	@Test
	public void testStopForStep() throws Exception {
		long execID = jobOp.start("partitionStopTest", null);
		Thread.sleep(sleepTime);
		jobOp.stop(execID);
		
		Thread.sleep(sleepTime);
		List<StepExecution> steps = jobOp.getStepExecutions(execID);
		for(StepExecution se : steps) {
			assertEquals("Step BatchStatus: ", BatchStatus.STOPPED, se.getBatchStatus());
		}
	}
}
