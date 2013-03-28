package test.junit;

import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.batch.operations.JobOperator;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;

import org.junit.BeforeClass;
import org.junit.Test;

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
			List<JobInstance> exe = jobOp.getJobInstances(jn, 0, 2000);
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
	public void twoRestartsConsecutively() throws Exception {
		JobOperator jo = BatchRuntime.getJobOperator();
		long exec1Id = jo.start("runtimejunit1", null);
		JobExecution exec1 = jo.getJobExecution(exec1Id);
		Thread.sleep(5000);
		assertEquals(BatchStatus.FAILED, exec1.getBatchStatus());
		
		long exec2Id = jo.restart(exec1Id, null);
		JobExecution exec2 = jo.getJobExecution(exec2Id);
		Thread.sleep(5000);
		assertEquals(BatchStatus.FAILED, exec2.getBatchStatus());
		
		long exec3Id = jo.restart(exec2Id, null);
		long exec4Id = jo.restart(exec2Id, null);
		
	}
}
