package test.junit;

import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.StepExecution;

import org.junit.BeforeClass;
import org.junit.Test;

public class PartitionPropertyResolverTests {
	static JobOperator jobOp = null;	
	
	private static int sleepTime = 1200;
	
	@BeforeClass
	public static void init() {
		jobOp = BatchRuntime.getJobOperator();
	}
	
	@Test
	public void testCollectorPropertyResolver() throws Exception {
		long execId = jobOp.start("partitionPropertyResolverTest", null);
		Thread.sleep(sleepTime);
		JobExecution je = jobOp.getJobExecution(execId);
		
		assertEquals("batch status", BatchStatus.COMPLETED,je.getBatchStatus());

		List<StepExecution> stepExecutions = jobOp.getStepExecutions(execId);		
		String data = (String) stepExecutions.get(0).getPersistentUserData();
		String[] tokens = data.split(":");
		String[] dataItems = new String[tokens.length - 1]; // ignores before ':'
		for(int i=1; i < tokens.length; i++) {
			dataItems[i - 1] = tokens[i];
		}
		
		for(String s : dataItems) {
			String stepProp = s.substring(s.indexOf('#') + 1, s.indexOf('$'));
			String collectorProp = s.substring(s.indexOf('$') + 1);
			assertEquals("Step Property ", stepProp, collectorProp);
		}
	}
	
	@Test
	public void testMapperPropertyResolver() throws Exception {
		long execId = jobOp.start("partitionPropertyResolverMapperTest", null);
		Thread.sleep(sleepTime);
		JobExecution je = jobOp.getJobExecution(execId);
		
		assertEquals("batch status", BatchStatus.COMPLETED, je.getBatchStatus());
		
		List<StepExecution> stepExec = jobOp.getStepExecutions(execId);
		String stepProp2Data = stepExec.get(0).getExitStatus();
		String partitionAndStepPropData = String.valueOf(stepExec.get(0).getPersistentUserData());
		
		String[] prop2Tokens = stepProp2Data.split(":");
		String stepProp2Value = prop2Tokens[1];
		int partitionsTotal = Integer.parseInt(prop2Tokens[2]);
		
		String[] tokens = partitionAndStepPropData.split("#");
		String[] partitionStepPropValues = new String[tokens.length - 1];
		
		for(int i=1; i < tokens.length; i++) {
			partitionStepPropValues[i - 1] = tokens[i];
		}
		
		int count = 0;
		for(String s : partitionStepPropValues) {
			String stepPropsValue = s.substring(s.indexOf("?") + 1);
			assertEquals("StepProp2's Value ", stepProp2Value, stepPropsValue);

			String partitionStringsValue = s.substring(0, s.indexOf("?"));
			assertEquals("PartitionString's Value ", partitionStringsValue, stepPropsValue);
			count++;
		}

		assertEquals("Paritions seen ", count, partitionsTotal);
	}
}