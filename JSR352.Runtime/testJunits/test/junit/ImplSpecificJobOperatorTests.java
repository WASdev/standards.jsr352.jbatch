package test.junit;

import java.util.List;

import javax.batch.operations.JobOperator;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.runtime.BatchRuntime;
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
}
