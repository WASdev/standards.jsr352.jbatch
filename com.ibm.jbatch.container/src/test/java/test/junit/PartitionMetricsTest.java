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
import java.util.Properties;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.chunk.AbstractItemReader;
import jakarta.batch.api.chunk.AbstractItemWriter;
import jakarta.batch.api.chunk.ItemProcessor;
import jakarta.batch.api.chunk.listener.AbstractChunkListener;
import jakarta.batch.api.listener.AbstractStepListener;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.Metric;
import jakarta.batch.runtime.Metric.MetricType;
import jakarta.batch.runtime.StepExecution;
import jakarta.batch.runtime.context.StepContext;
import jakarta.inject.Inject;

import org.junit.BeforeClass;
import org.junit.Test;

public class PartitionMetricsTest {
	static JobOperator jobOp = null;	

	private static int sleepTime = 3000;

	@BeforeClass
	public static void init() {
		jobOp = BatchRuntime.getJobOperator();
	}

	@Test
	public void testRestartAfterRestartAfterComplete() throws Exception {
		Properties origParams = new Properties();
		origParams.setProperty("step1Size", "10");
		origParams.setProperty("step2Size", "5");
		origParams.setProperty("stepListener.forceFailure", "true");

		long execId = jobOp.start("partitionMetrics", origParams);
		Thread.sleep(sleepTime);
		assertEquals("Didn't fail as expected", BatchStatus.FAILED, jobOp.getJobExecution(execId).getBatchStatus());

		//Now run again, since we failed on step 2 and allow-restart-if-complete = true, we'll get a different execution for
		// step 1.

		Properties restartParams = new Properties();
		restartParams.setProperty("step1Size", "25");
		restartParams.setProperty("step2Size", "6");
		restartParams.setProperty("stepListener.forceFailure", "false");
		restartParams.setProperty("chunkListener.forceFailure", "true");

		long restartExecId = jobOp.restart(execId, restartParams);
		Thread.sleep(sleepTime);
		assertEquals("Didn't fail as expected", BatchStatus.FAILED, jobOp.getJobExecution(restartExecId).getBatchStatus());


		restartParams.setProperty("chunkListener.forceFailure", "false");

		long restartExecId2 = jobOp.restart(restartExecId, restartParams);
		Thread.sleep(sleepTime);
		assertEquals("Didn't complete", BatchStatus.COMPLETED, jobOp.getJobExecution(restartExecId2).getBatchStatus());

	}


	@Test
	public void testMetricsRestartAfterComplete() throws Exception {
		Properties origParams = new Properties();
		origParams.setProperty("step1Size", "10");
		origParams.setProperty("step2Size", "5");
		origParams.setProperty("stepListener.forceFailure", "true");

		long execId = jobOp.start("partitionMetrics", origParams);
		Thread.sleep(sleepTime);
		assertEquals("Didn't fail as expected", BatchStatus.FAILED, jobOp.getJobExecution(execId).getBatchStatus());

		//Now run again, since we failed on step 2 and allow-restart-if-complete = true, we'll get a different execution for
		// step 1.

		Properties restartParams = new Properties();
		restartParams.setProperty("step1Size", "25");
		restartParams.setProperty("step2Size", "6");
		restartParams.setProperty("stepListener.forceFailure", "false");

		long restartExecId = jobOp.restart(execId, restartParams);
		Thread.sleep(sleepTime);
		assertEquals("Didn't complete successfully", BatchStatus.COMPLETED, jobOp.getJobExecution(restartExecId).getBatchStatus());

		StepExecution step1Exec1 = null;  StepExecution step1Exec2 = null; 

		for (StepExecution se : jobOp.getStepExecutions(execId)) {
			if (se.getStepName().equals("step1")) {
				step1Exec1 = se;
			}
		}

		for (StepExecution se : jobOp.getStepExecutions(restartExecId)) {
			if (se.getStepName().equals("step1")) {
				step1Exec2 = se;
			}
		}

		Metric[] metrics = step1Exec1.getMetrics();

		// 3 partitions of 10 elements - for each partition, 6 will be written and 4 will be filtered, this will be 2 chunks (item-count=5) + 1 zero-item chunk
		assertEquals("commit count", 9, getMetricVal(metrics, Metric.MetricType.COMMIT_COUNT));
		assertEquals("filter count", 12, getMetricVal(metrics, Metric.MetricType.FILTER_COUNT));
		assertEquals("read count", 30, getMetricVal(metrics, Metric.MetricType.READ_COUNT));
		assertEquals("write count", 18, getMetricVal(metrics, Metric.MetricType.WRITE_COUNT));

		Metric[] metrics2 = step1Exec2.getMetrics();

		// 3 partitions of 25 elements - for each partition, 15 will be written and 10 will be filtered, this will be 5 chunks (item-count=5) + 1 zero-item chunk
		assertEquals("commit count", 18, getMetricVal(metrics2, Metric.MetricType.COMMIT_COUNT));
		assertEquals("filter count", 30, getMetricVal(metrics2, Metric.MetricType.FILTER_COUNT));
		assertEquals("read count", 75, getMetricVal(metrics2, Metric.MetricType.READ_COUNT));
		assertEquals("write count", 45, getMetricVal(metrics2, Metric.MetricType.WRITE_COUNT));			
	}
	
	

	@Test	
	public void testPartitionedRollbackMetric() throws Exception {

		Properties origParams = new Properties();
		// These two don't matter
		origParams.setProperty("step1Size", "15"); origParams.setProperty("step2Size", "20");

		origParams.setProperty("chunkListener.forceFailure", "true");
		long execId = jobOp.start("partitionMetrics", origParams);
		Thread.sleep(sleepTime);
		assertEquals("Didn't fail as expected successfully", BatchStatus.FAILED, jobOp.getJobExecution(execId).getBatchStatus());

		StepExecution step1Exec = null;  StepExecution step2Exec = null; 
		for (StepExecution se : jobOp.getStepExecutions(execId)) {
			if (se.getStepName().equals("step1")) {
				step1Exec = se;
			} else if (se.getStepName().equals("step2")) {
				step2Exec = se;
			}
		}

		Metric[] metrics = step1Exec.getMetrics();

		// 3 partitions - this confirms that the read, filter, write counts get rolled back
		assertEquals("commit count", 0, getMetricVal(metrics, Metric.MetricType.COMMIT_COUNT));
		assertEquals("filter count", 0, getMetricVal(metrics, Metric.MetricType.FILTER_COUNT));
		assertEquals("read count", 0, getMetricVal(metrics, Metric.MetricType.READ_COUNT));
		assertEquals("write count", 0, getMetricVal(metrics, Metric.MetricType.WRITE_COUNT));
		assertEquals("rollback count", 3, getMetricVal(metrics, Metric.MetricType.ROLLBACK_COUNT));

	}


	// 2 steps, complete.  Didn't go ahead and
	// give partitions unique counts but that would be an
	// even more thorough test.
	@Test
	public void testPartitionMetrics() throws Exception {
		validatePartitionedMetrics("partitionMetrics");
	}

	@Test
	public void testNestedSplitFlowPartitionMetrics() throws Exception {
		validatePartitionedMetrics("partitionSplitFlowMetrics");
	}

	
	private void validatePartitionedMetrics(String jslName) throws Exception {
		Properties origParams = new Properties();
		origParams.setProperty("step1Size", "15");
		origParams.setProperty("step2Size", "20");

		long execId = jobOp.start(jslName, origParams);
		Thread.sleep(sleepTime);
		assertEquals("Didn't complete successfully", BatchStatus.COMPLETED, jobOp.getJobExecution(execId).getBatchStatus());
		StepExecution step1Exec = null;  StepExecution step2Exec = null; 
		for (StepExecution se : jobOp.getStepExecutions(execId)) {
			if (se.getStepName().equals("step1")) {
				step1Exec = se;
			} else if (se.getStepName().equals("step2")) {
				step2Exec = se;
			}
		}

		Metric[] metrics = step1Exec.getMetrics();

		// 3 partitions of 15 elements - for each partition, 9 will be written and 6 will be filtered, this will be 3 chunks (item-count=5) + 1 zero-item chunk
		assertEquals("commit count", 12, getMetricVal(metrics, Metric.MetricType.COMMIT_COUNT));
		assertEquals("filter count", 18, getMetricVal(metrics, Metric.MetricType.FILTER_COUNT));
		assertEquals("read count", 45, getMetricVal(metrics, Metric.MetricType.READ_COUNT));
		assertEquals("write count", 27, getMetricVal(metrics, Metric.MetricType.WRITE_COUNT));

		Metric[] metrics2 = step2Exec.getMetrics();

		// 3 partitions of 20 elements - for each partition, 12 will be written and 8 will be filtered, this will be 4 chunks (item-count=5) + 1 zero-item chunk
		assertEquals("commit count", 15, getMetricVal(metrics2, Metric.MetricType.COMMIT_COUNT));
		assertEquals("filter count", 24, getMetricVal(metrics2, Metric.MetricType.FILTER_COUNT));
		assertEquals("read count", 60, getMetricVal(metrics2, Metric.MetricType.READ_COUNT));
		assertEquals("write count", 36, getMetricVal(metrics2, Metric.MetricType.WRITE_COUNT));				
	}

	private long getMetricVal(Metric[] metrics, MetricType type) {
		long retVal = 0L;
		for (Metric m : metrics) {
			if (m.getType().equals(type)) {
				retVal = m.getValue();
			}
		}
		return retVal;
	}

	public static class Reader extends AbstractItemReader {

		@BatchProperty
		String numToRead;

		int i = 0;

		@Override
		public Object readItem() {
			if (i++ <= Integer.parseInt(numToRead) - 1) {
				return i;
			} else {
				return null;
			}
		}
	}

	public static class Processor implements ItemProcessor {

		@Override
		public Object processItem(Object item) {
			Integer i = (Integer)item;
			if (i % 5 == 1 || i % 5 == 3) {
				return null;
			} else {
				return i;
			}
		}
	}

	public static class Writer extends AbstractItemWriter {
		@Override
		public void writeItems(List<Object> items) {
			StringBuilder sb = new StringBuilder("Next chunk is: ");
			for (Object o : items) {
				sb.append(o.toString()).append(",");
			}
			String chunkStr = sb.toString();
			System.out.println(chunkStr.substring(0,chunkStr.length()-1) + "\n");
		}
	}

	public static class ChunkListener extends AbstractChunkListener {

		@BatchProperty
		String forceFailure;

		@Inject StepContext stepCtx;

		@Override
		public void afterChunk() throws Exception {
			if (Boolean.parseBoolean(forceFailure) == true) {
				throw new RuntimeException("Forcing failure for step: " + stepCtx.getStepName());
			}
		}
	}

	public static class StepListener extends AbstractStepListener {

		@BatchProperty
		String forceFailure;

		@Inject StepContext stepCtx;

		@Override
		public void afterStep() throws Exception {
			if (Boolean.parseBoolean(forceFailure) == true) {
				throw new RuntimeException("Forcing failure for step: " + stepCtx.getStepName());
			}
		}
	}

	public static class NoOpBatchlet extends AbstractBatchlet {
		@Override
		public String process() {
			return "true";
		}
	}
}
