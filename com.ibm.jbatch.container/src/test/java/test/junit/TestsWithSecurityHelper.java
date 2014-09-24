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

import javax.batch.operations.JobOperator;
import javax.batch.operations.NoSuchJobException;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.JobInstance;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.ibm.jbatch.spi.*;

public class TestsWithSecurityHelper {

	static JobOperator jobOp = null;
	static BatchSPIManager spiMgr = null;
	static Map<Integer, SimpleSecurityHelper> helperPool = new HashMap<Integer, SimpleSecurityHelper>();
	static SimpleSecurityHelper adminHelper;
	static int NUM_HELPERS = 3;

	private void registerHelper(int i) {
		SimpleSecurityHelper helper = helperPool.get(i % NUM_HELPERS );
		spiMgr.registerBatchSecurityHelper(helper);
	}

	@BeforeClass 
	public static void setup() {
		jobOp = BatchRuntime.getJobOperator();
		for (int i = 0; i < NUM_HELPERS; i++) {
			helperPool.put(i, new SimpleSecurityHelper("Test.Helper." + i));
		}
		spiMgr = BatchSPIManager.getInstance();
		adminHelper = new SimpleSecurityHelper(true, "AdminTestHelper");
	}

	@Test
	@Ignore("This is only useful with a job repository that has had this test already run against it")
	public void getJobInstancesAndGetJobInstanceCount() throws Exception {
		int COUNT = 5;
		int instanceCounts[] = new int[NUM_HELPERS];
		int adminInstanceCount = 0;
		for (int i = 0; i < NUM_HELPERS; i++) {
			registerHelper(i);
			try {
				instanceCounts[i] = jobOp.getJobInstanceCount("runtimejunit.alwaysFails1");
			} catch (NoSuchJobException e) {
				instanceCounts[i] = 0;
			}
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

	private static class SimpleSecurityHelper implements BatchSecurityHelper {

	final static String defaultTag = "internal.default.tag.for.SimpleSecurityHelper";
	
	private String currentTag;
	private boolean isAdmin = false;
	
	SimpleSecurityHelper(boolean isAdmin, String currentTag) {
		this.isAdmin = isAdmin;
		this.currentTag = currentTag;
	}
	
	SimpleSecurityHelper(String currentTag) {
		this(false, currentTag);
	}
	
	@Override
	public String getCurrentTag() {
		if (currentTag == null) {
			return defaultTag;
		} else {
			return currentTag;
		}
	}

	@Override
	public boolean isAdmin(String tag) {
		return isAdmin;
	}

}
}
