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

import static org.junit.Assert.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.batch.operations.JobOperator;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.ibm.jbatch.container.jobinstance.StepExecutionImpl;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.servicesmanager.ServicesManagerImpl;

public class ImplSpecificJobOperatorTests {

	static JobOperator jobOp = null;

	@BeforeClass 
	public static void setup() {
		jobOp = BatchRuntime.getJobOperator();
	}

	@Test
	public void testGetStepExecutionsBadExecutionId() throws Exception {
		boolean seen1 = false; 
		try {
			List<StepExecution> e = jobOp.getStepExecutions(Long.MAX_VALUE);
		} catch (NoSuchJobExecutionException e) {
			seen1 = true;
		}
		if (!seen1) throw new IllegalStateException("FAIL");
	}

	@Test
	public void testStopBadExecutionId() throws Exception {
		boolean seen2 = false; 
		try {
			jobOp.stop(Long.MAX_VALUE);
		} catch (NoSuchJobExecutionException e) {
			seen2 = true;
		}
		if (!seen2) throw new IllegalStateException("FAIL");
	}

	@Test
	public void test19928() throws Exception {
		for (String jn : jobOp.getJobNames()) {
			System.out.println("JN: " + jn);
			List<JobInstance> exe = jobOp.getJobInstances(jn, 0, 200);
			if (exe != null) {
				for (JobInstance ji : exe) {
					System.out.println("JI: " + ji.getInstanceId());
					for (JobExecution je : jobOp.getJobExecutions(ji)) { 
						System.out.println("JE: " + je.getExecutionId());
						if (je.getJobName() != null) {
							System.out.println("SKSK: ");
						} else {
							throw new RuntimeException("saw null for je: " + je.getExecutionId() 
									+ ", ji: " + ji.getInstanceId() + ", jn= " + jn);
						}
					}
				}
			}
		}
	}
	
	@Test
	//@Ignore("If the timing of the sleeps becomes a problem feel free to ignore this test.")
	public void testRestartWhileStillRunning() throws Exception {
		Properties props = new Properties();
		props.put("sleepTime", "100");  // short sleep, could have omitted
		long exec1Id = jobOp.start("alwaysFails1", props);
		JobExecution exec1 = jobOp.getJobExecution(exec1Id);
		Thread.sleep(1500); // Sleep to give exec 1 a chance to finish
		assertEquals(BatchStatus.FAILED, exec1.getBatchStatus());
		
		props.put("sleepTime", "100000");  // sleep long enough to kick off 2 then 3
		long exec2Id = jobOp.restart(exec1Id, props);
		JobExecution exec2 = jobOp.getJobExecution(exec2Id);
		Thread.sleep(1500);
		
		boolean seenExc = false;
		try {
			long exec3Id = jobOp.restart(exec2Id, props);
			JobExecution exec3 = jobOp.getJobExecution(exec3Id);
		} catch(JobRestartException e) {
			seenExc = true;
		}
		assertTrue("Didn't catch JobRestartException on execution #3", seenExc);
	}
	
	@Test
	public void testRestartBadExecutionId() throws Exception {
		boolean seen1 = false;
		JobOperator jo = BatchRuntime.getJobOperator();
		long exec1Id = jo.start("alwaysFails1", null);
		try {
			jo.restart(Long.MAX_VALUE, null);
		} catch (NoSuchJobExecutionException e) {
			seen1 = true;
		}
		if (!seen1) throw new IllegalStateException("FAIL");
	}
	
	@Test
	public void testGetterOnJustCreatedStepExecution() throws Exception {
		StepExecutionImpl e = new StepExecutionImpl(0, 0);
		Date start = e.getStartTime();
		Date end = e.getEndTime();
	}
	
	@Test
	@Ignore("Only works if you happen to have instance '6269' but we'll leave the logic to exercise getMostRecentStepExecutionsForJobInstance")
	public void testStepExecution() throws Exception {
		IPersistenceManagerService ps = ServicesManagerImpl.getInstance().getPersistenceManagerService();
		Map<String, StepExecution> steps = ps.getMostRecentStepExecutionsForJobInstance(6269);
	
		String[] strings = new String[steps.size()];
		for (String name : steps.keySet().toArray(strings)) {
			StepExecutionImpl step = (StepExecutionImpl)steps.get(name);
			System.out.println("-------------------------------");
			System.out.println("SKSK: " + step.getStepName());
			System.out.println("SKSK: " + step.getStepExecutionId());
			System.out.println("SKSK: " + step.getJobExecutionId());
			System.out.println("SKSK: " + step.getExitStatus());
			System.out.println("SKSK: " + step.getStartTime());
			System.out.println("SKSK: " + step.getEndTime());
		}
		
	}
	
	@Test
	public void testNullAndEmptyJobParameters() throws Exception {
		JobOperator jo = BatchRuntime.getJobOperator();
		long exec1Id = jo.start("alwaysFails1", null);
		Properties exec1Props = jo.getParameters(exec1Id);
		assertNull("Expecting null parameters", exec1Props);
		
		Properties emptyProps = new Properties();
		long exec2Id = jo.start("alwaysFails1", emptyProps);
		Properties exec2Props = jo.getParameters(exec2Id);
		assertEquals("Expecting empty parameters", 0, exec2Props.entrySet().size());
	}
}
