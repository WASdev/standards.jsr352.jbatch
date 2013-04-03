package test.junit;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.JobInstance;

import org.junit.BeforeClass;
import org.junit.Test;

import test.utils.TestSecurityHelper;

import com.ibm.jbatch.spi.BatchSPIManager;

public class TestsWithSecurityHelper {

	static JobOperator jobOp = null;
	static BatchSPIManager spiMgr = null;
	static Map<Integer, TestSecurityHelper> helperPool = new HashMap<Integer, TestSecurityHelper>();
	static TestSecurityHelper adminHelper;
	static int NUM_HELPERS = 3;

	private void registerHelper(int i) {
		TestSecurityHelper helper = helperPool.get(i % NUM_HELPERS );
		spiMgr.registerBatchSecurityHelper(helper);
	}

	@BeforeClass 
	public static void setup() {
		jobOp = BatchRuntime.getJobOperator();
		for (int i = 0; i < NUM_HELPERS; i++) {
			helperPool.put(i, new TestSecurityHelper("Test.Helper." + i));
		}
		spiMgr = BatchSPIManager.getInstance();
		adminHelper = new TestSecurityHelper(true, "AdminTestHelper");
	}

	@Test
	public void getJobInstancesAndGetJobInstanceCount() throws Exception {
		int COUNT = 5;
		int instanceCounts[] = new int[NUM_HELPERS];
		int adminInstanceCount = 0;
		for (int i = 0; i < NUM_HELPERS; i++) {
			registerHelper(i);
			instanceCounts[i] = jobOp.getJobInstanceCount("runtimejunit.alwaysFails1");
		}
		spiMgr.registerBatchSecurityHelper(adminHelper);
		adminInstanceCount = jobOp.getJobInstanceCount("runtimejunit.alwaysFails1");
		
		for (int i = 0; i < COUNT * NUM_HELPERS; i++) {
			registerHelper(i % NUM_HELPERS);
			jobOp.start("alwaysFails1", null);
		}
		Thread.sleep(2000);

		for (int i = 0; i < NUM_HELPERS; i++) {
			registerHelper(i);
			int diff = jobOp.getJobInstanceCount("runtimejunit.alwaysFails1") - instanceCounts[i];
			assertEquals("Checking difference in instance count", COUNT, diff);
			{
				List<JobInstance> jobInstances = jobOp.getJobInstances("runtimejunit.alwaysFails1", 0, COUNT);
				assertEquals("Checking instances list size i = " + i, COUNT, jobInstances.size());
			} 
			{  
				List<JobInstance> jobInstances = jobOp.getJobInstances("runtimejunit.alwaysFails1", COUNT, instanceCounts[i]);
				assertEquals("Checking instances list size i = " + i, instanceCounts[i], jobInstances.size());
			}
			{  
				List<JobInstance> jobInstances = jobOp.getJobInstances("runtimejunit.alwaysFails1", COUNT, instanceCounts[i] + 1);
				assertEquals("Checking instances list size i = " + i, instanceCounts[i],  jobInstances.size());
			}
			{  
				List<JobInstance> jobInstances = jobOp.getJobInstances("runtimejunit.alwaysFails1", 0, Integer.MAX_VALUE);
				assertEquals("Checking instances list size i = " + i, instanceCounts[i] + COUNT,  jobInstances.size());
			}
		}
		
		spiMgr.registerBatchSecurityHelper(adminHelper);
		int diff = jobOp.getJobInstanceCount("runtimejunit.alwaysFails1") - adminInstanceCount;
		assertEquals("Checking difference in instance count", COUNT * NUM_HELPERS, diff);
		{
			List<JobInstance> jobInstances = jobOp.getJobInstances("runtimejunit.alwaysFails1", 0, COUNT);
			assertEquals("Checking instances list size", COUNT, jobInstances.size());
		} 
		{  
			List<JobInstance> jobInstances = jobOp.getJobInstances("runtimejunit.alwaysFails1", COUNT * NUM_HELPERS, adminInstanceCount);
			assertEquals("Checking instances list size", adminInstanceCount, jobInstances.size());
		}
		{  
			List<JobInstance> jobInstances = jobOp.getJobInstances("runtimejunit.alwaysFails1", COUNT * NUM_HELPERS, adminInstanceCount + 1);
			assertEquals("Checking instances list size", adminInstanceCount,  jobInstances.size());
		}
		{  
			List<JobInstance> jobInstances = jobOp.getJobInstances("runtimejunit.alwaysFails1", 0, Integer.MAX_VALUE);
			assertEquals("Checking instances list size", adminInstanceCount + COUNT * NUM_HELPERS,  jobInstances.size());
		}
	}
}
