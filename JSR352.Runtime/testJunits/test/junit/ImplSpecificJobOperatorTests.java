package test.junit;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import javax.batch.operations.JobOperator;
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
	@Ignore("Didn't bother putting the expected catches in here so will just produce a failure")
	public void twoRestartsConsecutively() throws Exception {
		long exec1Id = jobOp.start("alwaysFails1", null);
		JobExecution exec1 = jobOp.getJobExecution(exec1Id);
		Thread.sleep(5000);
		assertEquals(BatchStatus.FAILED, exec1.getBatchStatus());
		
		long exec2Id = jobOp.restart(exec1Id, null);
		JobExecution exec2 = jobOp.getJobExecution(exec2Id);
		Thread.sleep(5000);
		assertEquals(BatchStatus.FAILED, exec2.getBatchStatus());
		
		long exec3Id = jobOp.restart(exec2Id, null);
		long exec4Id = jobOp.restart(exec2Id, null);
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
}
