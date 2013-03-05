/*
 * Copyright 2012 International Business Machines Corp.
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
package com.ibm.jbatch.tck.tests.jslxml;

import static com.ibm.jbatch.tck.utils.AssertionUtils.assertWithMessage;

import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.batch.operations.JobOperator.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;

import org.junit.BeforeClass;
import org.testng.Reporter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ibm.jbatch.tck.artifacts.specialized.MetricsStepListener;
import com.ibm.jbatch.tck.utils.JobOperatorBridge;

public class MetricsTests {

	private static JobOperatorBridge jobOp = null;

	public static void setup(String[] args, Properties props) throws Exception {
		String METHOD = "setup";

		try {
			jobOp = new JobOperatorBridge();
		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}

	@BeforeMethod
	@BeforeClass
	public static void setUp() throws Exception {
		jobOp = new JobOperatorBridge();
	}

	/* cleanup */
	public void cleanup() {

	}

	/*
	 * Obviously would be nicer to have more granular tests for some of this
	 * function, but here we're going a different route and saying, if it's
	 * going to require restart it will have some complexity, so let's test a
	 * few different functions in one longer restart scenario.
	 */

	/*
	 * @testName: testMetricsInApp
	 * 
	 * 
	 * @assertion: Section 7.1 Job Metrics - Ensure Metrics are available to Batch Artifacts during job execution
	 * @test_Strategy: Batch Artifact reads a known number of items - test that those reads are reflected 
	 *                 in the read count and accessible at job execution time to the Batch Artifact
	 * 
	 */
	@Test
	@org.junit.Test
	public void testMetricsInApp() throws Exception {
		String METHOD = "testMetricsInApp";

		try {
			Reporter.log("Create job parameters for execution #1:<p>");
			Properties jobParams = new Properties();
			Reporter.log("execution.number=1<p>");
			Reporter.log("readrecord.fail=40<p>");
			Reporter.log("app.arraysize=30<p>");
			Reporter.log("app.chunksize=7<p>");
			Reporter.log("app.commitinterval=10<p>");
			Reporter.log("numberOfSkips=0<p>");
			Reporter.log("ReadProcessWrite=READ<p>");
			jobParams.put("execution.number", "1");
			jobParams.put("readrecord.fail", "40");
			jobParams.put("app.arraysize", "30");
			jobParams.put("app.chunksize", "7");
			jobParams.put("app.commitinterval", "10");
			jobParams.put("numberOfSkips", "0");
			jobParams.put("ReadProcessWrite", "READ");
			jobParams.put("app.writepoints", "0,7,14,21,28,30");
			jobParams.put("app.next.writepoints", "7,14,21,28,30");

			Reporter.log("Locate job XML file: testChunkMetrics.xml<p>");
			URL jobXMLURL = this.getClass()
					.getResource("/testChunkMetrics.xml");


			Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
			JobExecution execution1 = jobOp.startJobAndWaitForResult("testChunkMetrics",
					jobParams);
			Reporter.log("execution #1 JobExecution getBatchStatus()="
					+ execution1.getBatchStatus() + "<p>");
			Reporter.log("execution #1 JobExecution getExitStatus()="
					+ execution1.getExitStatus() + "<p>");
			assertWithMessage("Testing execution #1", BatchStatus.COMPLETED,
					execution1.getBatchStatus());
			assertWithMessage("Testing metrics",
					MetricsStepListener.GOOD_EXIT_STATUS_READ,
					execution1.getExitStatus());
		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}

	/*
	 * @testName: testMetricsSkipRead
	 * 
	 * @assertion: Section 7.1 Job Metrics - Skip Read Count
	 * @test_Strategy: Force Batch Artifact to skip a known number of reads - test that those skips are reflected in the skip read count
	 * 
	 */
	@Test
	@org.junit.Test
	public void testMetricsSkipRead() throws Exception {

		String METHOD = "testMetricsSkipRead";

		try {
			Reporter.log("Create job parameters for execution #1:<p>");
			Properties jobParams = new Properties();
			Reporter.log("execution.number=1<p>");
			Reporter.log("readrecord.fail=1,3<p>");
			Reporter.log("app.arraysize=30<p>");
			Reporter.log("numberOfSkips=2<p>");
			Reporter.log("ReadProcessWrite=READ_SKIP<p>");
			jobParams.put("execution.number", "1");
			jobParams.put("readrecord.fail", "1,3,4,12");
			jobParams.put("app.arraysize", "30");
			jobParams.put("numberOfSkips", "4");
			jobParams.put("ReadProcessWrite", "READ_SKIP");

			Reporter.log("Locate job XML file: testMetricsSkipCount.xml<p>");
			URL jobXMLURL = this.getClass().getResource(
					"/testMetricsSkipCount.xml");


			Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
			JobExecution execution1 = jobOp.startJobAndWaitForResult("testMetricsSkipCount",
					jobParams);

			Reporter.log("execution #1 JobExecution getBatchStatus()="
					+ execution1.getBatchStatus() + "<p>");
			Reporter.log("execution #1 JobExecution getExitStatus()="
					+ execution1.getExitStatus() + "<p>");
			assertWithMessage("Testing execution #1", BatchStatus.COMPLETED,
					execution1.getBatchStatus());
			assertWithMessage("Testing execution #1",
					MetricsStepListener.GOOD_EXIT_STATUS_READ,
					execution1.getExitStatus());
			
			List<StepExecution> stepExecutions = jobOp
					.getStepExecutions(execution1.getExecutionId());
			
			StepExecution step = null;
			String stepNameTest = "step1";

			for (StepExecution stepEx : stepExecutions) {
				if (stepNameTest.equals(stepEx.getName())) {
					step = stepEx;
				}
			}

			Reporter.log("execution #1 JobExecution getBatchStatus()="
					+ execution1.getBatchStatus() + "<p>");
			assertWithMessage("Testing execution #1", BatchStatus.COMPLETED,
					execution1.getBatchStatus());
			
			Metric[] metrics = step.getMetrics();

			Reporter.log("Testing the read count for execution #1<p>");
			for (int i = 0; i < metrics.length; i++) {
				if (metrics[i].getName().equals(Metric.MetricName.READSKIPCOUNT)) {
					System.out.println("AJM: in test, found metric: " + metrics[i].getName());
					assertWithMessage(
							"Testing the read skip count for execution #1", 4L,
							metrics[i].getValue());
				}
			}
		} catch (Exception e) {
			handleException(METHOD, e);
		}

	}
	
	/*
	 * @testName: testMetricsSkipWrite
	 * 
	 * @assertion: Section 7.1 Job Metrics - Skip Write Count
	 * @test_Strategy: Force Batch Artifact to skip a known number of writes - test that those skips are reflected in the skip write count
	 * 
	 */
	@Test
	@org.junit.Test
	public void testMetricsSkipWrite() throws Exception {

		String METHOD = "testMetricsSkipWrite";

		try {
			Reporter.log("Create job parameters for execution #1:<p>");
			Properties jobParams = new Properties();
			Reporter.log("execution.number=1<p>");
			Reporter.log("readrecord.fail=1,3<p>");
			Reporter.log("app.arraysize=30<p>");
			Reporter.log("numberOfSkips=2<p>");
			Reporter.log("ReadProcessWrite=WRITE_SKIP<p>");
			jobParams.put("execution.number", "1");
			jobParams.put("writerecord.fail", "1,3,4");
			jobParams.put("app.arraysize", "30");
			jobParams.put("numberOfSkips", "3");
			jobParams.put("ReadProcessWrite", "WRITE_SKIP");

			Reporter.log("Locate job XML file: testMetricsSkipWriteCount.xml<p>");
			URL jobXMLURL = this.getClass().getResource(
					"/testMetricsSkipWriteCount.xml");


			Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
			JobExecution execution1 = jobOp.startJobAndWaitForResult("testMetricsSkipWriteCount",
					jobParams);

			Reporter.log("execution #1 JobExecution getBatchStatus()="
					+ execution1.getBatchStatus() + "<p>");
			Reporter.log("execution #1 JobExecution getExitStatus()="
					+ execution1.getExitStatus() + "<p>");
			assertWithMessage("Testing execution #1", BatchStatus.COMPLETED,
					execution1.getBatchStatus());
			
			List<StepExecution> stepExecutions = jobOp
					.getStepExecutions(execution1.getExecutionId());
			
			StepExecution step = null;
			String stepNameTest = "step1";

			for (StepExecution stepEx : stepExecutions) {
				if (stepNameTest.equals(stepEx.getName())) {
					step = stepEx;
				}
			}

			Reporter.log("execution #1 JobExecution getBatchStatus()="
					+ execution1.getBatchStatus() + "<p>");
			assertWithMessage("Testing execution #1", BatchStatus.COMPLETED,
					execution1.getBatchStatus());
			
			Metric[] metrics = step.getMetrics();

			Reporter.log("Testing the write skip count for execution #1<p>");
			for (int i = 0; i < metrics.length; i++) {
				if (metrics[i].getName().equals(Metric.MetricName.WRITESKIPCOUNT)) {
					System.out.println("AJM: in test, found metric: " + metrics[i].getName());
					assertWithMessage(
							"Testing the write skip count for execution #1", 3L,
							metrics[i].getValue());
				}
			}
		} catch (Exception e) {
			handleException(METHOD, e);
		}

	}

	/*
	 * @testName: testMetricsSkipProcess
	 * 
	 * @assertion: Section 7.1 Job Metrics - Skip Process Count
	 * @test_Strategy: Force Batch Artifact to skip a known number of processing - test that those skips are reflected in the skip process count
	 * 
	 */
	@Test
	@org.junit.Test
	public void testMetricsSkipProcess() throws Exception {
		String METHOD = "testMetricsSkipProcess";

		try {
			Reporter.log("Create job parameters for execution #1:<p>");
			Properties jobParams = new Properties();
			Reporter.log("execution.number=1<p>");
			Reporter.log("readrecord.fail=7,13<p>");
			Reporter.log("app.arraysize=30<p>");
			Reporter.log("numberOfSkips=2<p>");
			Reporter.log("ReadProcessWrite=PROCESS<p>");
			jobParams.put("execution.number", "1");
			jobParams.put("processrecord.fail", "7,13");
			jobParams.put("app.arraysize", "30");
			jobParams.put("numberOfSkips", "2");
			jobParams.put("ReadProcessWrite", "PROCESS");

			Reporter.log("Locate job XML file: testMetricsSkipCount.xml<p>");
			URL jobXMLURL = this.getClass().getResource(
					"/testMetricsSkipCount.xml");


			Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
			JobExecution execution1 = jobOp.startJobAndWaitForResult("testMetricsSkipCount",
					jobParams);

			Reporter.log("execution #1 JobExecution getBatchStatus()="
					+ execution1.getBatchStatus() + "<p>");
			Reporter.log("execution #1 JobExecution getExitStatus()="
					+ execution1.getExitStatus() + "<p>");
			assertWithMessage("Testing execution #1", BatchStatus.COMPLETED,
					execution1.getBatchStatus());
			assertWithMessage("Testing execution #1",
					MetricsStepListener.GOOD_EXIT_STATUS_PROCESS,
					execution1.getExitStatus());
			
			List<StepExecution> stepExecutions = jobOp
					.getStepExecutions(execution1.getExecutionId());
			
			StepExecution step = null;
			String stepNameTest = "step1";

			for (StepExecution stepEx : stepExecutions) {
				if (stepNameTest.equals(stepEx.getName())) {
					step = stepEx;
				}
			}

			Reporter.log("execution #1 JobExecution getBatchStatus()="
					+ execution1.getBatchStatus() + "<p>");
			assertWithMessage("Testing execution #1", BatchStatus.COMPLETED,
					execution1.getBatchStatus());
			
			Metric[] metrics = step.getMetrics();

			Reporter.log("Testing the read count for execution #1<p>");
			for (int i = 0; i < metrics.length; i++) {
				if (metrics[i].getName().equals(Metric.MetricName.PROCESSSKIPCOUNT)) {
					System.out.println("AJM: in test, found metric: " + metrics[i].getName());
					assertWithMessage(
							"Testing the read count for execution #1", 2L,
							metrics[i].getValue());
				}
			}
		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}
	
	/*
	 * @testName: testReadMetric
	 * 
	 * @assertion: Section 7.1 Job Metrics - Read Count
	 * @test_Strategy: Batch Artifact reads a known number of items - test that those reads are reflected in the read count
	 * 
	 */
	@Test
	@org.junit.Test
	public void testReadMetric() throws Exception {
		String METHOD = "testReadMetric";

		try {
			Reporter.log("Create job parameters for execution #1:<p>");
			Properties jobParams = new Properties();
			Reporter.log("execution.number=1<p>");
			Reporter.log("readrecord.fail=40<p>");
			Reporter.log("app.arraysize=30<p>");
			Reporter.log("app.chunksize=7<p>");
			Reporter.log("app.commitinterval=10<p>");
			Reporter.log("numberOfSkips=0<p>");
			Reporter.log("ReadProcessWrite=READ<p>");
			jobParams.put("execution.number", "1");
			jobParams.put("readrecord.fail", "-1");
			jobParams.put("app.arraysize", "30");
		
			Reporter.log("Locate job XML file: testChunkMetrics.xml<p>");
			URL jobXMLURL = this.getClass()
					.getResource("/testMetricCount.xml");


			Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
			JobExecution execution1 = jobOp.startJobAndWaitForResult("testMetricCount",
					jobParams);
			Reporter.log("execution #1 JobExecution getBatchStatus()="
					+ execution1.getBatchStatus() + "<p>");
			Reporter.log("execution #1 JobExecution getExitStatus()="
					+ execution1.getExitStatus() + "<p>");
			
			List<StepExecution> stepExecutions = jobOp
					.getStepExecutions(execution1.getExecutionId());
			
			StepExecution step = null;
			String stepNameTest = "step1Metric";

			for (StepExecution stepEx : stepExecutions) {
				if (stepNameTest.equals(stepEx.getName())) {
					step = stepEx;
				}
			}

			Reporter.log("execution #1 JobExecution getBatchStatus()="
					+ execution1.getBatchStatus() + "<p>");
			assertWithMessage("Testing execution #1", BatchStatus.COMPLETED,
					execution1.getBatchStatus());
			
			Metric[] metrics = step.getMetrics();

			Reporter.log("Testing the read count for execution #1<p>");
			for (int i = 0; i < metrics.length; i++) {
				if (metrics[i].getName().equals(Metric.MetricName.READCOUNT)) {
					System.out.println("AJM: in test, found metric: " + metrics[i].getName());
					assertWithMessage(
							"Testing the read count for execution #1", 9L,
							metrics[i].getValue());
				}
			}

		} catch (Exception e) {
			handleException(METHOD, e);
		}

	}
	
	/*
	 * @testName: testWriteMetric
	 * 
	 * @assertion: Section 7.1 Job Metrics - Write Count
	 * @test_Strategy: Batch Artifact writes a known number of items - test that those writes are reflected in the write count
	 * 
	 */
	@Test
	@org.junit.Test
	public void testWriteMetric() throws Exception {
		String METHOD = "testWriteMetric";

		try {
			Reporter.log("Create job parameters for execution #1:<p>");
			Properties jobParams = new Properties();
		
			Reporter.log("Locate job XML file: testChunkMetrics.xml<p>");
			URL jobXMLURL = this.getClass()
					.getResource("/testMetricCount.xml");


			Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
			JobExecution execution1 = jobOp.startJobAndWaitForResult("testMetricCount",
					jobParams);
			Reporter.log("execution #1 JobExecution getBatchStatus()="
					+ execution1.getBatchStatus() + "<p>");
			Reporter.log("execution #1 JobExecution getExitStatus()="
					+ execution1.getExitStatus() + "<p>");
			
			List<StepExecution> stepExecutions = jobOp
					.getStepExecutions(execution1.getExecutionId());
			
			StepExecution step = null;
			String stepNameTest = "step1Metric";

			for (StepExecution stepEx : stepExecutions) {
				if (stepNameTest.equals(stepEx.getName())) {
					step = stepEx;
				}
			}

			Reporter.log("execution #1 JobExecution getBatchStatus()="
					+ execution1.getBatchStatus() + "<p>");
			assertWithMessage("Testing execution #1", BatchStatus.COMPLETED,
					execution1.getBatchStatus());
			
			Metric[] metrics = step.getMetrics();

			Reporter.log("Testing the read count for execution #1<p>");
			for (int i = 0; i < metrics.length; i++) {
				if (metrics[i].getName().equals(Metric.MetricName.WRITECOUNT)) {
					System.out.println("AJM: in test, found metric: " + metrics[i].getName());
					assertWithMessage(
							"Testing the write count for execution #1", 9L,
							metrics[i].getValue());
				}
			}
			
		} catch (Exception e) {
			handleException(METHOD, e);
		}

	}

	/*
	 * @testName: testMetricsFilterCount
	 * 
	 * @assertion: Section 7.1 Job Metrics - Filter Count
	 * @test_Strategy: Batch Artifact filters a known number of items while processing - test that those filter actions are reflected in the filter count
	 * 
	 */
	@Test
	@org.junit.Test
	public void testMetricsFilterCount() throws Exception {

		String METHOD = "testMetricsFilterCount";

		try {

			Reporter.log("Create job parameters for execution #1:<p>");
			Properties jobParams = new Properties();
			jobParams.put("app.processFilterItem", "3");
			Reporter.log("app.processFilterItem=3<p>");

			Reporter.log("Locate job XML file: testMetricsFilterCount.xml<p>");
			URL jobXMLURL = this.getClass().getResource(
					"/testMetricsFilterCount.xml");


			Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
			JobExecution execution1 = jobOp.startJobAndWaitForResult("testMetricsFilterCount",
					jobParams);

			Reporter.log("Obtaining StepExecutions for execution id: "
					+ execution1.getExecutionId() + "<p>");
			List<StepExecution> stepExecutions = jobOp
					.getStepExecutions(execution1.getExecutionId());

			StepExecution tempstep = null;
			StepExecution step = null;
			String stepNameTest = "step1FM";

			for (StepExecution stepEx : stepExecutions) {
				if (stepNameTest.equals(stepEx.getName())) {
					step = stepEx;
				}
			}

			Reporter.log("execution #1 JobExecution getBatchStatus()="
					+ execution1.getBatchStatus() + "<p>");
			assertWithMessage("Testing execution #1", BatchStatus.COMPLETED,
					execution1.getBatchStatus());
			Metric[] metrics = step.getMetrics();

			Reporter.log("Testing the filter count for execution #1<p>");
			for (int i = 0; i < metrics.length; i++) {
				if (metrics[i].getName().equals(Metric.MetricName.FILTERCOUNT)) {
					assertWithMessage(
							"Testing the filter count for execution #1", 1L,
							metrics[i].getValue());
				}
			}
		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}

	/*
	 * @testName: testMetricsCommitCount
	 * 
	 * @assertion: Section 7.1 Job Metrics - Commit Count
	 * @test_Strategy: Batch Artifact read/process/writes a known number of items and all are committed - test that those commits are reflected in the commit count
	 * 
	 */
	@Test
	@org.junit.Test
	public void testMetricsCommitCount() throws Exception {

		String METHOD = "testMetricsCommitCount";

		try {
			Reporter.log("Create job parameters for execution #1:<p>");
			Properties jobParams = new Properties();
			jobParams.put("app.processFilterItem", "3");
			Reporter.log("app.processFilterItem=3<p>");

			Reporter.log("Locate job XML file: testMetricsCommitCount.xml<p>");
			URL jobXMLURL = this.getClass().getResource(
					"/testMetricsCommitCount.xml");


			Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
			JobExecution execution1 = jobOp.startJobAndWaitForResult("testMetricsCommitCount",
					jobParams);

			Reporter.log("Obtaining StepExecutions for execution id: "
					+ execution1.getExecutionId() + "<p>");
			List<StepExecution> stepExecutions = jobOp
					.getStepExecutions(execution1.getExecutionId());

			StepExecution tempstep = null;
			StepExecution step = null;
			String stepNameTest = "step1CCM";

			for (StepExecution stepEx : stepExecutions) {
				if (stepNameTest.equals(stepEx.getName())) {
					step = stepEx;
				}
			}

			Reporter.log("execution #1 JobExecution getBatchStatus()="
					+ execution1.getBatchStatus() + "<p>");
			assertWithMessage("Testing execution #1", BatchStatus.COMPLETED,
					execution1.getBatchStatus());
			Metric[] metrics = step.getMetrics();

			Reporter.log("Testing the commit count for execution #1<p>");
			for (int i = 0; i < metrics.length; i++) {
				if (metrics[i].getName().equals(Metric.MetricName.COMMITCOUNT)) {
					assertWithMessage(
							"Testing the commit count for execution #1", 4L,
							metrics[i].getValue());
				}
			}
		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}

	
	/*
	 * @testName: testMetricsCommitCount
	 * 
	 * @assertion: Section 7.1 Job Metrics - Commit Count
	 * @test_Strategy: Batch Artifact read/process/writes a known number of items and all are committed - test that those commits are reflected in the commit count
	 * 
	 */
	@Test
	@org.junit.Test
	public void testMetricsStepTimestamps() throws Exception {

		String METHOD = "testMetricsCommitCount";

		try {
			Reporter.log("Create job parameters for execution #1:<p>");
			Properties jobParams = new Properties();
			jobParams.put("app.processFilterItem", "3");
			Reporter.log("app.processFilterItem=3<p>");

			Reporter.log("Locate job XML file: testMetricsCommitCount.xml<p>");
			URL jobXMLURL = this.getClass().getResource(
					"/testMetricsCommitCount.xml");

			long time = System.currentTimeMillis();
			Date ts = new Date(time);

			Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
			JobExecution execution1 = jobOp.startJobAndWaitForResult("testMetricsCommitCount",
					jobParams);

			Reporter.log("Obtaining StepExecutions for execution id: "
					+ execution1.getExecutionId() + "<p>");
			List<StepExecution> stepExecutions = jobOp
					.getStepExecutions(execution1.getExecutionId());

			StepExecution tempstep = null;
			StepExecution step = null;
			String stepNameTest = "step1CCM";

			for (StepExecution stepEx : stepExecutions) {
				if (stepNameTest.equals(stepEx.getName())) {
					step = stepEx;
				}
			}

			Reporter.log("execution #1 JobExecution getBatchStatus()="
					+ execution1.getBatchStatus() + "<p>");
			assertWithMessage("Testing execution #1", BatchStatus.COMPLETED,
					execution1.getBatchStatus());
			
			
			assertWithMessage("Start time of step occurs after start time of test", ts.compareTo(step.getStartTime()) < 0);
			assertWithMessage("End time of step occurs after start time of step", step.getEndTime().compareTo(step.getStartTime()) > 0);
			assertWithMessage("End time of step occurs after start time of test", step.getEndTime().compareTo(ts) > 0);
		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}
	
	/*
	 * @testName: testMetricsCommitCount
	 * 
	 * @assertion: Section 7.1 Job Metrics - Commit Count
	 * @test_Strategy: Batch Artifact read/process/writes a known number of items and all are committed - test that those commits are reflected in the commit count
	 * 
	 */
	@Test
	@org.junit.Test
	public void testMetricsJobExecutionTimestamps() throws Exception {

		String METHOD = "testMetricsCommitCount";

		try {
			Reporter.log("Create job parameters for execution #1:<p>");
			Properties jobParams = new Properties();
			jobParams.put("app.processFilterItem", "3");
			Reporter.log("app.processFilterItem=3<p>");

			Reporter.log("Locate job XML file: testMetricsCommitCount.xml<p>");
			URL jobXMLURL = this.getClass().getResource(
					"/testMetricsCommitCount.xml");

			long time = System.currentTimeMillis();
			Date ts = new Date(time);

			Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
			JobExecution execution1 = jobOp.startJobAndWaitForResult("testMetricsCommitCount",
					jobParams);


			
			Reporter.log("execution #1 JobExecution getBatchStatus()="
					+ execution1.getBatchStatus() + "<p>");
			assertWithMessage("Testing execution #1", BatchStatus.COMPLETED,
					execution1.getBatchStatus());
			
			System.out.println("AJM: testcase start time: " + ts);
			System.out.println("AJM: job create time: " + execution1.getCreateTime());
			System.out.println("AJM: job start time: " + execution1.getStartTime());
			System.out.println("AJM: job last updated time: " + execution1.getLastUpdatedTime());
			System.out.println("AJM: job end time: " + execution1.getEndTime());
			
			assertWithMessage("Start time of job occurs after start time of test", ts.compareTo(execution1.getStartTime()) < 0);
			assertWithMessage("Create time of job occurs before start time of job", execution1.getCreateTime().compareTo(execution1.getStartTime()) < 0);
			assertWithMessage("End time of job occurs after start time of job", execution1.getEndTime().compareTo(execution1.getStartTime()) > 0);
			assertWithMessage("Last Updated time of job occurs after start time of job", execution1.getLastUpdatedTime().compareTo(execution1.getStartTime()) > 0);
			assertWithMessage("End time of job occurs after start time of test", execution1.getEndTime().compareTo(ts) > 0);
		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}

	private static void handleException(String methodName, Exception e)
			throws Exception {
		Reporter.log("Caught exception: " + e.getMessage() + "<p>");
		Reporter.log(methodName + " failed<p>");
		throw e;
	}

}