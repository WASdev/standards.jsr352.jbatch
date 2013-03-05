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

import java.util.Properties;

import javax.batch.operations.JobOperator.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;

import com.ibm.jbatch.tck.artifacts.specialized.MyItemReadListenerImpl;
import com.ibm.jbatch.tck.artifacts.specialized.MySkipProcessListener;
import com.ibm.jbatch.tck.artifacts.specialized.MySkipReadListener;
import com.ibm.jbatch.tck.artifacts.specialized.MySkipWriteListener;
import com.ibm.jbatch.tck.utils.JobOperatorBridge;

import org.junit.BeforeClass;
import org.testng.Reporter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ChunkTests {

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
     * @testName: testChunkOnErrorListener
     * 
     * @assertion: Test will finish in FAILED status, with the onError chunk listener invoked
     * 
     * @test_Strategy: Test that the ChunkListener.onError method is driven for an exception occurring
     * 		during the read-write-process batch loop
     */
    @Test
    @org.junit.Test
    public void testChunkOnErrorListener() throws Exception {
    	
    	String METHOD = "testChunkOnErrorListener";
    	
    	try {
    		Reporter.log("Create job parameters for execution #1:<p>");
    		Properties jobParams = new Properties();
    		
    		Reporter.log("execution.number=1<p>");
            Reporter.log("readrecord.fail=5<p>");
            Reporter.log("app.arraysize=30<p>");
            jobParams.put("execution.number", "1");
            jobParams.put("readrecord.fail", "5");
            jobParams.put("app.writepoints", "0,10");
            jobParams.put("app.arraysize", "30");

            Reporter.log("Locate job XML file: chunkListenerTest.xml<p>");

            Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
            JobExecution execution1 = jobOp.startJobAndWaitForResult("chunkListenerTest", jobParams);
            
            Reporter.log("execution #1 JobExecution getBatchStatus()=" + execution1.getBatchStatus() + "<p>");
            Reporter.log("execution #1 JobExecution getExitStatus()=" + execution1.getExitStatus() + "<p>");
            assertWithMessage("Testing execution #1", BatchStatus.FAILED, execution1.getBatchStatus());
            assertWithMessage("Testing execution #1", "Chunk onError invoked", execution1.getExitStatus());
          
        } catch (Exception e) {
            handleException(METHOD, e);
        }
    }
    

    /*
     * @testName: testChunkRestartItemCount7
     * @assertion: first job started will finish as FAILED. Restart of job will finish successfully with COMPLETED.
     *             5.2.1.1 - Reader, 5.2.1.1.1 - Reader Properties,
     *             5.2.1.2 - Processor
     *             5.2.1.3 - Writer, 5.2.1.3.1 - Writer Properties
     *             5.2.1 - Chunk, item-count
     *             5.2.1 - Chunk item checkpointing
     * 
     * @test_Strategy: start a job configured to a item-count of 7 configured to fail on the 12 item read. Restart job and 
     *                 test that the processing begins at last recorded check point (item 7) prior to previous failure
     */
    @Test
    @org.junit.Test
    public void testChunkRestartItemCount7() throws Exception {
        String METHOD = "testChunkRestartItemCount7";

        try {

            Reporter.log("Create job parameters for execution #1:<p>");
            Reporter.log("execution.number=1<p>");
            Reporter.log("readrecord.fail=12<p>");
            Reporter.log("app.arraysize=30<p>");
            Reporter.log("app.chunksize=7<p>");
            Reporter.log("app.commitinterval=10<p>");
            Properties jobParams = new Properties();
            jobParams.put("execution.number", "1");
            jobParams.put("readrecord.fail", "12");
            jobParams.put("app.arraysize", "30");
            jobParams.put("app.checkpoint.position" , "0");
            jobParams.put("app.writepoints", "0,7,14,21,28,30");
            jobParams.put("app.next.writepoints", "7,14,21,28,30");

            Reporter.log("Locate job XML file: chunkStopOnEndOn.xml<p>");

            Reporter.log("Invoke startJobAndWaitForResult<p>");
            JobExecution execution1 = jobOp.startJobAndWaitForResult("chunkStopOnEndOn", jobParams);
            Reporter.log("execution #1 JobExecution getBatchStatus()=" + execution1.getBatchStatus() + "<p>");
            Reporter.log("execution #1 JobExecution getExitStatus()=" + execution1.getExitStatus() + "<p>");
            assertWithMessage("Testing execution #1", BatchStatus.FAILED, execution1.getBatchStatus());
            assertWithMessage("Testing execution #1", "FAILED", execution1.getExitStatus());

            long jobInstanceId = execution1.getInstanceId();
            // TODO - we think this will change so we restart by instanceId, for
            // now the draft spec
            // says to restart by execution Id.
            long lastExecutionId = execution1.getExecutionId();
            Reporter.log("Got Job instance id: " + jobInstanceId + "<p>");
            Reporter.log("Got Job execution id: " + lastExecutionId + "<p>");
            {
                Reporter.log("Create job parameters for execution #2:<p>");
                Reporter.log("execution.number=2<p>");
                Reporter.log("app.arraysize=30<p>");
                Reporter.log("app.chunksize=7<p>");
                Reporter.log("app.commitinterval=10<p>");
                Properties jobParametersOverride = new Properties();
                jobParametersOverride.put("execution.number", "2");
                jobParametersOverride.put("app.arraysize", "30");
                jobParams.put("app.checkpoint.position" , "7");
                Reporter.log("Invoke restartJobAndWaitForResult with executionId: " + lastExecutionId + "<p>");
                JobExecution exec = jobOp.restartJobAndWaitForResult(lastExecutionId, jobParams);
                lastExecutionId = exec.getExecutionId();
                Reporter.log("execution #2 JobExecution getBatchStatus()=" + exec.getBatchStatus() + "<p>");
                Reporter.log("execution #2 JobExecution getExitStatus()=" + exec.getExitStatus() + "<p>");
                Reporter.log("execution #2 Job instance id=" + exec.getInstanceId() + "<p>");
                assertWithMessage("Testing execution #2", BatchStatus.COMPLETED, exec.getBatchStatus());
                assertWithMessage("Testing execution #2", "COMPLETED", exec.getExitStatus());
                assertWithMessage("Testing execution #2", jobInstanceId, exec.getInstanceId());
            }
        } catch (Exception e) {
            handleException(METHOD, e);
        }

    }

    /*
     * @testName: testChunkRestartItemCount10
     * @assertion: first job started will finish as FAILED. Restart of job will finish successfully with COMPLETED.
     *             5.2.1.1 - Reader, 5.2.1.1.1 - Reader Properties,
     *             5.2.1.2 - Processor
     *             5.2.1.3 - Writer, 5.2.1.3.1 - Writer Properties
     *             5.2.1 - Chunk, item-count
     *             5.2.1 - Chunk item checkpointing
     * 
     * @test_Strategy: start a job configured to a item-count of 10 configured to fail on the 12 item read. Restart job and 
     *                 test that the processing begins at last recorded check point (item 10) prior to previous failure
     */
    @Test
    @org.junit.Test
    public void testChunkRestartItemCount10() throws Exception {

        String METHOD = "testChunkRestartItemCount10";
        try {
            Reporter.log("Create job parameters for execution #1:<p>");
            Properties jobParams = new Properties();
            Reporter.log("execution.number=1<p>");
            Reporter.log("readrecord.fail=12<p>");
            Reporter.log("app.arraysize=30<p>");
            Reporter.log("app.writepoints=0,5,10,15,20,25,30<p>");
            Reporter.log("app.next.writepoints=0,5,10,15,20,25,30<p>");
            jobParams.put("execution.number", "1");
            jobParams.put("readrecord.fail", "12");
            jobParams.put("app.arraysize", "30");
            jobParams.put("app.checkpoint.position" , "0");
            jobParams.put("app.writepoints", "0,10,20,30");
            jobParams.put("app.next.writepoints", "10,20,30");

            Reporter.log("Locate job XML file: chunkrestartCheckpt10.xml<p>");

            Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
            JobExecution execution1 = jobOp.startJobAndWaitForResult("chunkrestartCheckpt10", jobParams);
            Reporter.log("execution #1 JobExecution getBatchStatus()=" + execution1.getBatchStatus() + "<p>");
            Reporter.log("execution #1 JobExecution getExitStatus()=" + execution1.getExitStatus() + "<p>");
            assertWithMessage("Testing execution #1", BatchStatus.FAILED, execution1.getBatchStatus());
            assertWithMessage("Testing execution #1", "FAILED", execution1.getExitStatus());

            long jobInstanceId = execution1.getInstanceId();
            // TODO - we think this will change so we restart by instanceId, for
            // now the draft spec
            // says to restart by execution Id.
            long lastExecutionId = execution1.getExecutionId();
            Reporter.log("Got Job instance id: " + jobInstanceId + "<p>");
            Reporter.log("Got Job execution id: " + lastExecutionId + "<p>");

            {
                Reporter.log("Create job parameters for execution #2:<p>");
                Properties jobParametersOverride = new Properties();
                Reporter.log("execution.number=2<p>");
                Reporter.log("app.arraysize=30<p>");
                Reporter.log("app.writepoints=0,5,10,15,20,25,30<p>");
                jobParametersOverride.put("execution.number", "2");
                jobParams.put("app.arraysize", "30");
                jobParams.put("app.checkpoint.position" , "10");
                jobParams.put("app.writepoints", "0,5,10,15,20,25,30");
                Reporter.log("Invoke restartJobAndWaitForResult with execution id: " + lastExecutionId + "<p>");
                JobExecution exec = jobOp.restartJobAndWaitForResult(lastExecutionId, jobParams);
                lastExecutionId = exec.getExecutionId();
                Reporter.log("execution #2 JobExecution getBatchStatus()=" + exec.getBatchStatus() + "<p>");
                Reporter.log("execution #2 JobExecution getExitStatus()=" + exec.getExitStatus() + "<p>");
                Reporter.log("execution #2 Job instance id=" + exec.getInstanceId() + "<p>");
                assertWithMessage("Testing execution #2", BatchStatus.COMPLETED, exec.getBatchStatus());
                assertWithMessage("Testing execution #2", "COMPLETED", exec.getExitStatus());
                assertWithMessage("Testing execution #2", jobInstanceId, exec.getInstanceId());
            }
        } catch (Exception e) {
            handleException(METHOD, e);
        }

    }

    /*
     * @testName: testChunkRestartChunk5
     * @assertion: first job started will finish as FAILED. Restart of job will finish successfully with COMPLETED. 
     *             5.2.1.1 - Reader, 5.2.1.1.1 - Reader Properties,
     *             5.2.1.2 - Processor
     *             5.2.1.3 - Writer, 5.2.1.3.1 - Writer Properties
     *             5.2.1 - Chunk, item-count
     *             5.2.1 - Chunk item checkpointing/restart
     * 
     * @test_Strategy: start a job configured to a item-count of 5 configured to fail on the 12 item read. Restart job and 
     *                 test that the processing begins at last recorded check point (item 10) prior to previous failure
     */
    @Test
    @org.junit.Test
    public void testChunkRestartChunk5() throws Exception {

        String METHOD = "testChunkRestartChunk5";
        try {
            Reporter.log("Create job parameters for execution #1:<p>");
            Properties jobParams = new Properties();
            Reporter.log("execution.number=1<p>");
            Reporter.log("readrecord.fail=12<p>");
            Reporter.log("app.arraysize=30<p>");
            Reporter.log("app.writepoints=0,3,6,9,12,15,18,21,24,27,30<p>");
            Reporter.log("app.next.writepoints=9,12,15,18,21,24,27,30<p>");
            jobParams.put("execution.number", "1");
            jobParams.put("readrecord.fail", "12");
            jobParams.put("app.arraysize", "30");
            jobParams.put("app.checkpoint.position" , "0");
            jobParams.put("app.writepoints", "0,5,10,15,20,25,30");
            jobParams.put("app.next.writepoints", "10,15,20,25,30");

            Reporter.log("Locate job XML file: chunksize5commitinterval3.xml<p>");

            Reporter.log("Invoke startJobAndWaitForResul for execution #1<p>");
            JobExecution execution1 = jobOp.startJobAndWaitForResult("chunksize5commitinterval3", jobParams);
            Reporter.log("execution #1 JobExecution getBatchStatus()=" + execution1.getBatchStatus() + "<p>");
            Reporter.log("execution #1 JobExecution getExitStatus()=" + execution1.getExitStatus() + "<p>");
            assertWithMessage("Testing execution #1", BatchStatus.FAILED, execution1.getBatchStatus());
            assertWithMessage("Testing execution #1", "FAILED", execution1.getExitStatus());

            long jobInstanceId = execution1.getInstanceId();
            // TODO - we think this will change so we restart by instanceId, for
            // now the draft spec
            // says to restart by execution Id.
            long lastExecutionId = execution1.getExecutionId();
            Reporter.log("Got Job instance id: " + jobInstanceId + "<p>");
            Reporter.log("Got Job execution id: " + lastExecutionId + "<p>");
            {
                Reporter.log("Create job parameters for execution #2:<p>");
                Properties jobParametersOverride = new Properties(jobParams);
                
                Reporter.log("execution.number=2<p>");
                Reporter.log("app.arraysize=30<p>");
                
                jobParametersOverride.put("execution.number", "2");
                jobParametersOverride.put("app.checkpoint.position" , "10");
                jobParametersOverride.put("app.arraysize", "30");
                Reporter.log("Invoke restartJobAndWaitForResult with execution id: " + lastExecutionId + "<p>");
                JobExecution exec = jobOp.restartJobAndWaitForResult(lastExecutionId, jobParametersOverride);
                lastExecutionId = exec.getExecutionId();
                Reporter.log("execution #2 JobExecution getBatchStatus()=" + exec.getBatchStatus() + "<p>");
                Reporter.log("execution #2 JobExecution getExitStatus()=" + exec.getExitStatus() + "<p>");
                Reporter.log("execution #2 Job instance id=" + exec.getInstanceId() + "<p>");
                assertWithMessage("Testing execution #2", BatchStatus.COMPLETED, exec.getBatchStatus());
                assertWithMessage("Testing execution #2", "COMPLETED", exec.getExitStatus());
                assertWithMessage("Testing execution #2", jobInstanceId, exec.getInstanceId());
            }
        } catch (Exception e) {
            handleException(METHOD, e);
        }

    }

    /*
     * @testName: testChunkDefaultItemCount
     * @assertion: job will finish successfully with COMPLETED and buffer size = default value of 10 is recognized
     *             5.2.1.1 - Reader, 5.2.1.1.1 - Reader Properties,
     *             5.2.1.2 - Processor
     *             5.2.1.3 - Writer, 5.2.1.3.1 - Writer Properties
     *             5.2.1 - Chunk, item-count default value
     *             5.2.1 - Chunk item checkpointing/restart
     * 
     * @test_Strategy: start a job with no item-count specified. 
     *                 Batch artifact checks that the checkpointing occurs at the default item-count (10). Test that the 
     *                 job completes successfully. 
     */
    @Test
    @org.junit.Test
    public void testChunkDefaultItemCount() throws Exception {
        String METHOD = "testChunkDefaultItemCount";

        try {
            Reporter.log("Create job parameters for execution #1:<p>");
            Properties jobParams = new Properties();
            Reporter.log("execution.number=1<p>");
            Reporter.log("readrecord.fail=40<p>");
            Reporter.log("app.arraysize=30<p>");
            jobParams.put("execution.number", "1");
            jobParams.put("app.checkpoint.position" , "0");
            jobParams.put("readrecord.fail", "40");
            jobParams.put("app.arraysize", "30");

            Reporter.log("Locate job XML file: chunksizeDEFAULTcommitIntervalDEFAULT.xml<p>");

            Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
            JobExecution execution1 = jobOp.startJobAndWaitForResult("chunksizeDEFAULTcommitIntervalDEFAULT", jobParams);
            Reporter.log("execution #1 JobExecution getBatchStatus()=" + execution1.getBatchStatus() + "<p>");
            Reporter.log("execution #1 JobExecution getExitStatus()=" + execution1.getExitStatus() + "<p>");
            assertWithMessage("Testing execution #1", BatchStatus.COMPLETED, execution1.getBatchStatus());
            assertWithMessage("Testing execution #1", "buffer size = 10", execution1.getExitStatus());
        } catch (Exception e) {
            handleException(METHOD, e);
        }

    }

    /*
     * @testName: testChunkRestartCustomCheckpoint
     * @assertion: first job start will finish with FAILED. restart of job will finish successfully with COMPLETED.
     *             5.2.1.1 - Reader, 5.2.1.1.1 - Reader Properties,
     *             5.2.1.2 - Processor
     *             5.2.1.3 - Writer, 5.2.1.3.1 - Writer Properties
     *             5.2.1 - Chunk, item-count, checkpoint-policy
     *             5.2.1 - Chunk item checkpointing/restart
     *             5.2.1.7 - Custom Checkpoint Algorithm
     *             5.2.1.7.1 - Custom Checkpoint Algorithm Properties
     * 
     * @test_Strategy: start a job with item-count specified, checkpoint-policy set to 'custom' and configured to fail on the 12 item read. Restart job.
     *                  Batch artifact enforces that the checkpointing occurs at the custom defined checkpointing points and that 
     *                  reading/writing resumes at last good custom defined checkpoint.
     *                  test that the job completes successfully.
     */
    @Test
    @org.junit.Test
    public void testChunkRestartCustomCheckpoint() throws Exception {
        String METHOD = "testChunkRestartCustomCheckpoint";

        try {
            Reporter.log("Create job parameters for execution #1:<p>");
            Properties jobParams = new Properties();
            Reporter.log("execution.number=2<p>");
            Reporter.log("readrecord.fail=12<p>");
            Reporter.log("app.arraysize=30<p>");
            Reporter.log("app.writepoints=0,4,9,13,15,20,22,27,30<p>");
            Reporter.log("app.next.writepoints=9,13,15,20,22,27,30<p>");
            jobParams.put("execution.number", "1");
            jobParams.put("readrecord.fail", "12");
            jobParams.put("app.checkpoint.position" , "0");
            jobParams.put("app.arraysize", "30");
            jobParams.put("app.writepoints", "0,4,9,13,15,20,22,27,30");
            jobParams.put("app.next.writepoints", "9,13,15,20,22,27,30");

            Reporter.log("Locate job XML file: chunkCustomCheckpoint.xml<p>");

            Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
            JobExecution execution1 = jobOp.startJobAndWaitForResult("chunkCustomCheckpoint", jobParams);
            Reporter.log("execution #1 JobExecution getBatchStatus()=" + execution1.getBatchStatus() + "<p>");
            Reporter.log("execution #1 JobExecution getExitStatus()=" + execution1.getExitStatus() + "<p>");
            assertWithMessage("Testing execution #1", BatchStatus.FAILED, execution1.getBatchStatus());
            assertWithMessage("Testing execution #1", "FAILED", execution1.getExitStatus());

            long jobInstanceId = execution1.getInstanceId();
            // TODO - we think this will change so we restart by instanceId, for
            // now the draft spec
            // says to restart by execution Id.
            long lastExecutionId = execution1.getExecutionId();
            Reporter.log("Got Job instance id: " + jobInstanceId + "<p>");
            Reporter.log("Got Job execution id: " + lastExecutionId + "<p>");
            {
                Reporter.log("Create job parameters for execution #2:<p>");
                Properties jobParametersOverride = new Properties(jobParams);
                Reporter.log("execution.number=2<p>");
                Reporter.log("app.arraysize=30<p>");
                Reporter.log("app.writepoints=9,13,15,20,22,27,30<p>");
                jobParametersOverride.put("execution.number", "2");
                jobParametersOverride.put("app.checkpoint.position" , "9");
                jobParametersOverride.put("app.arraysize", "30");
                jobParametersOverride.put("app.writepoints", "9,13,15,20,22,27,30");
                Reporter.log("Invoke restartJobAndWaitForResult with execution id: " + lastExecutionId + "<p>");
                JobExecution exec = jobOp.restartJobAndWaitForResult(lastExecutionId, jobParametersOverride);
                lastExecutionId = exec.getExecutionId();
                Reporter.log("execution #2 JobExecution getBatchStatus()=" + exec.getBatchStatus() + "<p>");
                Reporter.log("execution #2 JobExecution getExitStatus()=" + exec.getExitStatus() + "<p>");
                Reporter.log("execution #2 Job instance id=" + exec.getInstanceId() + "<p>");
                assertWithMessage("Testing execution #2", BatchStatus.COMPLETED, exec.getBatchStatus());
                assertWithMessage("Testing execution #2", "COMPLETED", exec.getExitStatus());
                assertWithMessage("Testing execution #2", jobInstanceId, exec.getInstanceId());
            }
        } catch (Exception e) {
            handleException(METHOD, e);
        }

    }

    /*
     * @testName: testChunkTimeBasedDefaultCheckpoint
     * @assertion: job will finish successfully with COMPLETED and the default time-limit of 10 seconds recognized
     *             5.2.1.1 - Reader, 5.2.1.1.1 - Reader Properties,
     *             5.2.1.2 - Processor
     *             5.2.1.3 - Writer, 5.2.1.3.1 - Writer Properties
     *             5.2.1 - Chunk, item-count, time-limit default
     *             5.2.1 - Chunk item checkpointing
     * 
     * @test_Strategy: start a job with item-count specified at a value greater than the read data set. time-limit not specified so as to default to 10. 
     *                  Batch artifact enforces that the checkpointing occurs at the default time-limit boundary (10 seconds) .
     *                  test that the job completes successfully.     
     */
    @Test
    @org.junit.Test
    public void testChunkTimeBasedDefaultCheckpoint() throws Exception {
        String METHOD = "testChunkTimeBasedDefaultCheckpoint";

        try {
            Reporter.log("Create job parameters for execution #1:<p>");
            Properties jobParams = new Properties();
            Reporter.log("execution.number=1<p>");
            Reporter.log("readrecord.fail=31<p>");
            Reporter.log("app.arraysize=30<p>");
            jobParams.put("execution.number", "1");
            jobParams.put("readrecord.fail", "31");
            jobParams.put("app.arraysize", "30");

            Reporter.log("Locate job XML file: chunkTimeBasedDefaultCheckpoint.xml<p>");

            Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
            JobExecution execution1 = jobOp.startJobAndWaitForResult("chunkTimeBasedDefaultCheckpoint", jobParams);
            Reporter.log("execution #1 JobExecution getBatchStatus()=" + execution1.getBatchStatus() + "<p>");
            Reporter.log("execution #1 JobExecution getExitStatus()=" + execution1.getExitStatus() + "<p>");
            assertWithMessage("Testing execution #1", BatchStatus.COMPLETED, execution1.getBatchStatus());
            assertWithMessage("Testing execution #1", "TRUE: 0", execution1.getExitStatus());
        } catch (Exception e) {
            handleException(METHOD, e);
        }
    }

    /*
     * @testName: testChunkTimeBased10Seconds
     * @assertion: job will finish successfully with COMPLETED and the time-limit of 10 seconds recognized
     *             5.2.1.1 - Reader, 5.2.1.1.1 - Reader Properties,
     *             5.2.1.2 - Processor
     *             5.2.1.3 - Writer, 5.2.1.3.1 - Writer Properties
     *             5.2.1 - Chunk, item-count, time-limit
     *             5.2.1 - Chunk item checkpointing/restart
     * 
     * @test_Strategy: start a job with item-count specified at a value greater than the read data set. time-limit specified as 10. 
     *                  and configured to fail on the 12 item read. Restart job.
     *                  Batch artifact enforces that the checkpointing occurs at the specified time-limit boundary (10 seconds) and that 
     *                  reading/writing resumes at last good checkpoint.
     *                  test that the job completes successfully.     
     */
    @Test
    @org.junit.Test
    public void testChunkTimeBased10Seconds() throws Exception {
    	
    	 String METHOD = "testChunkTimeBased10Seconds";

    	try {
	        Properties jobParams = new Properties();
	        jobParams.put("execution.number", "1");
	        jobParams.put("readrecord.fail", "31");
	        jobParams.put("app.arraysize", "30");
	
	
	        JobExecution execution1 = jobOp.startJobAndWaitForResult("chunkTimeBasedCheckpoint", jobParams);
	        assertWithMessage("Testing execution #1", BatchStatus.COMPLETED, execution1.getBatchStatus());
	        assertWithMessage("Testing execution #1", "TRUE: 10", execution1.getExitStatus());
	
	        System.out.println("AJM: exit status = " + execution1.getExitStatus());
    	 } catch (Exception e) {
             handleException(METHOD, e);
         }
    }

    /*
     * @testName: testChunkRestartTimeBasedCheckpoint
     * @assertion: first job start will finishas FAILED. Restart of job will finish successfully as COMPLETED
     *             and the time-limit of 10 seconds recognized
     *             5.2.1.1 - Reader, 5.2.1.1.1 - Reader Properties,
     *             5.2.1.2 - Processor
     *             5.2.1.3 - Writer, 5.2.1.3.1 - Writer Properties
     *             5.2.1 - Chunk, item-count, time-limit
     *             5.2.1 - Chunk item checkpointing/restart
     * 
     * @test_Strategy: start a job with item-count specified at a value greater than the read data set. time-limit specified as 10. 
     *                  and configured to fail on the 12 item read. Restart job.
     *                  Batch artifact enforces that the checkpointing occurs at the specified time-limit boundary (10 seconds) and that 
     *                  reading/writing resumes at last good checkpoint.
     *                  test that the job completes successfully.     
     */
    @Test
    @org.junit.Test
    public void testChunkRestartTimeBasedCheckpoint() throws Exception {
        String METHOD = "testChunkRestartTimeBasedCheckpoint";

        try {
            Reporter.log("Create job parameters for execution #1:<p>");
            Properties jobParams = new Properties();
            Reporter.log("execution.number=1<p>");
            Reporter.log("readrecord.fail=12<p>");
            Reporter.log("app.arraysize=30<p>");
            jobParams.put("execution.number", "1");
            jobParams.put("readrecord.fail", "12");
            jobParams.put("app.arraysize", "30");

            Reporter.log("Locate job XML file: chunkTimeBasedCheckpoint.xml<p>");

            Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
            JobExecution execution1 = jobOp.startJobAndWaitForResult("chunkTimeBasedCheckpoint", jobParams);
            Reporter.log("execution #1 JobExecution getBatchStatus()=" + execution1.getBatchStatus() + "<p>");
            Reporter.log("execution #1 JobExecution getExitStatus()=" + execution1.getExitStatus() + "<p>");
            assertWithMessage("Testing execution #1", BatchStatus.FAILED, execution1.getBatchStatus());
            assertWithMessage("Testing execution #1", "FALSE: 0", execution1.getExitStatus());

            long jobInstanceId = execution1.getInstanceId();
            // TODO - we think this will change so we restart by instanceId, for
            // now the draft spec
            // says to restart by execution Id.
            long lastExecutionId = execution1.getExecutionId();
            Reporter.log("Got Job instance id: " + jobInstanceId + "<p>");
            Reporter.log("Got Job execution id: " + lastExecutionId + "<p>");

            {
                Reporter.log("Create job parameters for execution #2:<p>");
                Properties jobParametersOverride = new Properties();
                Reporter.log("execution.number=2<p>");
                ;
                Reporter.log("app.arraysize=30<p>");
                jobParametersOverride.put("execution.number", "2");
                jobParametersOverride.put("app.arraysize", "30");
                Reporter.log("Invoke restartJobAndWaitForResult with execution id: " + lastExecutionId + "<p>");
                JobExecution exec = jobOp.restartJobAndWaitForResult(lastExecutionId);
                lastExecutionId = exec.getExecutionId();
                Reporter.log("execution #2 JobExecution getBatchStatus()=" + exec.getBatchStatus() + "<p>");
                Reporter.log("execution #2 JobExecution getExitStatus()=" + exec.getExitStatus() + "<p>");
                Reporter.log("execution #2 Job instance id=" + exec.getInstanceId() + "<p>");
                assertWithMessage("Testing execution #2", BatchStatus.COMPLETED, exec.getBatchStatus());
                assertWithMessage("Testing execution #2", "TRUE: 10", exec.getExitStatus());
                assertWithMessage("Testing execution #2", jobInstanceId, exec.getInstanceId());
            }
        } catch (Exception e) {
            handleException(METHOD, e);
        }

    }

    /*
     * @testName: testChunkRestartTimeBasedDefaultCheckpoint
     * @assertion: first job start will finishas FAILED. Restart of job will finish successfully as COMPLETED
     *             and the default time-limit of 10 seconds recognized
     *             5.2.1.1 - Reader, 5.2.1.1.1 - Reader Properties,
     *             5.2.1.2 - Processor
     *             5.2.1.3 - Writer, 5.2.1.3.1 - Writer Properties
     *             5.2.1 - Chunk, item-count, time-limit
     *             5.2.1 - Chunk item checkpointing/restart
     * 
     * @test_Strategy: start a job with item-count specified at a value greater than the read data set. time-limit not specified so as to default to 10. 
     *                  and configured to fail on the 12 item read. Restart job.
     *                  Batch artifact enforces that the checkpointing occurs at the default time-limit boundary (10 seconds) and that 
     *                  reading/writing resumes at last good checkpoint.
     *                  test that the job completes successfully.     
     */
    @Test
    @org.junit.Test
    public void testChunkRestartTimeBasedDefaultCheckpoint() throws Exception {

        String METHOD = "testChunkRestartTimeBasedDefaultCheckpoint";
        try {
            Reporter.log("Create job parameters for execution #1:<p>");
            Properties jobParams = new Properties();
            Reporter.log("execution.number=1<p>");
            Reporter.log("readrecord.fail=2<p>");
            Reporter.log("app.arraysize=30<p>");
            jobParams.put("execution.number", "1");
            jobParams.put("readrecord.fail", "2");
            jobParams.put("app.arraysize", "30");

            Reporter.log("Locate job XML file: chunkTimeBasedDefaultCheckpoint.xml<p>");

            Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
            JobExecution execution1 = jobOp.startJobAndWaitForResult("chunkTimeBasedDefaultCheckpoint", jobParams);
            Reporter.log("execution #1 JobExecution getBatchStatus()=" + execution1.getBatchStatus() + "<p>");
            Reporter.log("execution #1 JobExecution getExitStatus()=" + execution1.getExitStatus() + "<p>");
            assertWithMessage("Testing execution #1", BatchStatus.FAILED, execution1.getBatchStatus());
            assertWithMessage("Testing execution #1", "TRUE: 0", execution1.getExitStatus());

            long jobInstanceId = execution1.getInstanceId();
            // TODO - we think this will change so we restart by instanceId, for
            // now the draft spec
            // says to restart by execution Id.
            long lastExecutionId = execution1.getExecutionId();
            Reporter.log("Got Job instance id: " + jobInstanceId + "<p>");
            Reporter.log("Got Job execution id: " + lastExecutionId + "<p>");
            {
                Reporter.log("Create job parameters for execution #2:<p>");
                Properties jobParametersOverride = new Properties();
                Reporter.log("execution.number=2<p>");
                Reporter.log("app.arraysize=30<p>");
                jobParametersOverride.put("execution.number", "2");
                jobParametersOverride.put("app.arraysize", "30");
                Reporter.log("Invoke restartJobAndWaitForResult with execution id: " + lastExecutionId + "<p>");
                JobExecution exec = jobOp.restartJobAndWaitForResult(lastExecutionId);
                lastExecutionId = exec.getExecutionId();
                Reporter.log("execution #2 JobExecution getBatchStatus()=" + exec.getBatchStatus() + "<p>");
                Reporter.log("execution #2 JobExecution getExitStatus()=" + exec.getExitStatus() + "<p>");
                Reporter.log("execution #2 Job instance id=" + exec.getInstanceId() + "<p>");
                assertWithMessage("Testing execution #2", BatchStatus.COMPLETED, exec.getBatchStatus());
                assertWithMessage("Testing execution #2", "TRUE: 0", exec.getExitStatus());
                assertWithMessage("Testing execution #2", jobInstanceId, exec.getInstanceId());
            }
        } catch (Exception e) {
            handleException(METHOD, e);
        }

    }

    /*
     * @testName: testChunkSkipRead
     * @assertion: job will finish successfully as COMPLETED and skippable exceptions will be recognized 
     *             5.2.1.1 - Reader, 5.2.1.1.1 - Reader Properties,
     *             5.2.1.2 - Processor
     *             5.2.1.3 - Writer, 5.2.1.3.1 - Writer Properties
     *             5.2.1 - Chunk, item-count, skip-limit
     *             5.2.1.4 - Exception Handling - skippable-exception-classes
     * 
     * @test_Strategy: start a job with item-count specified at 3.  
     *                  Application is configured to encounter an error on two separate reads, at which point
     *                  a skippable exception is thrown by the application. Batch Application enforces that the exceptions
     *                  were recognized as skippable.
     *                  test that the job completes successfully and that the application recognized the exceptions as skippable     
     */
    @Test
    @org.junit.Test
    public void testChunkSkipRead() throws Exception {

        String METHOD = "testChunkSkipRead";
        try {
            Reporter.log("Create job parameters for execution #1:<p>");
            Properties jobParams = new Properties();
            Reporter.log("execution.number=1<p>");
            Reporter.log("readrecord.fail=1,3<p>");
            Reporter.log("app.arraysize=30<p>");
            jobParams.put("execution.number", "1");
            jobParams.put("readrecord.fail", "1,3");
            jobParams.put("app.arraysize", "30");

            Reporter.log("Locate job XML file: chunkSkipInitialTest.xml<p>");

            Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
            JobExecution execution1 = jobOp.startJobAndWaitForResult("chunkSkipInitialTest", jobParams);
            Reporter.log("execution #1 JobExecution getBatchStatus()=" + execution1.getBatchStatus() + "<p>");
            Reporter.log("execution #1 JobExecution getExitStatus()=" + execution1.getExitStatus() + "<p>");
            assertWithMessage("Testing execution #1", BatchStatus.COMPLETED, execution1.getBatchStatus());
            assertWithMessage("Testing execution #1", MySkipReadListener.GOOD_EXIT_STATUS, execution1.getExitStatus());
        } catch (Exception e) {
            handleException(METHOD, e);
        }

    }

    /*
     * @testName: testChunkSkipProcess
     * @assertion: job will finish successfully as COMPLETED and skippable exceptions will be recognized 
     *             5.2.1.1 - Reader, 5.2.1.1.1 - Reader Properties,
     *             5.2.1.2 - Processor
     *             5.2.1.3 - Writer, 5.2.1.3.1 - Writer Properties
     *             5.2.1 - Chunk, item-count, skip-limit
     *             5.2.1.4 - Exception Handling - skippable-exception-classes
     * 
     * @test_Strategy: start a job with item-count specified as 3. skip-limit is set to 1000.
     *                  Application is configured to encounter an error on two separate processing actions, at which point
     *                  a skippable exception is thrown by the application. Batch Application enforces that the exceptions
     *                  were recognized as skippable.
     *                  test that the job completes successfully and that the application recognized the exception as skippable.     
     */
    @Test
    @org.junit.Test
    public void testChunkSkipProcess() throws Exception {
        String METHOD = "testChunkSkipProcess";
        try {
            Reporter.log("Create job parameters for execution #1:<p>");
            Properties jobParams = new Properties();
            Reporter.log("execution.number=1<p>");
            Reporter.log("processrecord.fail=7,13<p>");
            Reporter.log("app.arraysize=30<p>");
            jobParams.put("execution.number", "1");
            jobParams.put("processrecord.fail", "7,13");
            jobParams.put("app.arraysize", "30");

            Reporter.log("Locate job XML file: chunkSkipInitialTest.xml<p>");

            Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
            JobExecution execution1 = jobOp.startJobAndWaitForResult("chunkSkipInitialTest", jobParams);
            Reporter.log("execution #1 JobExecution getBatchStatus()=" + execution1.getBatchStatus() + "<p>");
            Reporter.log("execution #1 JobExecution getExitStatus()=" + execution1.getExitStatus() + "<p>");
            assertWithMessage("Testing execution #1", BatchStatus.COMPLETED, execution1.getBatchStatus());
            assertWithMessage("Testing execution #1", MySkipProcessListener.GOOD_EXIT_STATUS, execution1.getExitStatus());
        } catch (Exception e) {
            handleException(METHOD, e);
        }
    }

    /*
     * @testName: testChunkSkipWrite
     * @assertion: job will finish successfully as COMPLETED and skippable exceptions will be recognized 
     *             5.2.1.1 - Reader, 5.2.1.1.1 - Reader Properties,
     *             5.2.1.2 - Processor
     *             5.2.1.3 - Writer, 5.2.1.3.1 - Writer Properties
     *             5.2.1 - Chunk, item-count, skip-limit
     *             5.2.1.4 - Exception Handling - skippable-exception-classes
     * 
     * @test_Strategy: start a job with item-count specified as 3. skip-limit set to 1000  
     *                  Application is configured to encounter an error on two separate writes, at which point
     *                  a skippable exception is thrown by the application. Batch Application enforces that the exceptions
     *                  were recognized as skippable.
     *                  test that the job completes successfully and that the application recognized the exceptions as skippable.
     */
    @Test
    @org.junit.Test
    public void testChunkSkipWrite() throws Exception {
        String METHOD = "testChunkSkipWrite";
        try {
            Reporter.log("Create job parameters for execution #1:<p>");
            Properties jobParams = new Properties();
            Reporter.log("execution.number=1<p>");
            Reporter.log("writerecord.fail=1,3<p>");
            Reporter.log("app.arraysize=30<p>");
            jobParams.put("execution.number", "1");
            jobParams.put("writerecord.fail", "1,3");
            jobParams.put("app.arraysize", "30");

            Reporter.log("Locate job XML file: chunkSkipInitialTest.xml<p>");

            Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
            JobExecution execution1 = jobOp.startJobAndWaitForResult("chunkSkipInitialTest", jobParams);

            Reporter.log("execution #1 JobExecution getBatchStatus()=" + execution1.getBatchStatus() + "<p>");
            Reporter.log("execution #1 JobExecution getExitStatus()=" + execution1.getExitStatus() + "<p>");
            assertWithMessage("Testing execution #1", BatchStatus.COMPLETED, execution1.getBatchStatus());
            assertWithMessage("Testing execution #1", MySkipWriteListener.GOOD_EXIT_STATUS, execution1.getExitStatus());
        } catch (Exception e) {
            handleException(METHOD, e);
        }

    }

    /*
     * @testName: testChunkSkipReadExceedSkip
     * @assertion: job will finish as FAILED and exceeded skippable exceptions will be recognized 
     *             5.2.1.1 - Reader, 5.2.1.1.1 - Reader Properties,
     *             5.2.1.2 - Processor
     *             5.2.1.3 - Writer, 5.2.1.3.1 - Writer Properties
     *             5.2.1 - Chunk, item-count, skip-limit
     *             5.2.1.4 - Exception Handling - skippable-exception-classes
     * 
     * @test_Strategy: start a job with item-count specified at a value greater than the read data set and skip-limit set to 1.  
     *                  Application is configured to encounter an error on two separate reads, at which point
     *                  a skippable exception is thrown by the application. Batch Application enforces that the exceptions
     *                  were recognized as skippable and that the second exception exceeded the skip-limit
     *                  test that the job fails but the skip-limit was recognized.     
     */
    @Test
    @org.junit.Test
    public void testChunkSkipReadExceedSkip() throws Exception {
        String METHOD = "testChunkSkipReadExceedSkip";

        try {
            Reporter.log("Create job parameters for execution #1:<p>");
            Properties jobParams = new Properties();
            Reporter.log("execution.number=1<p>");
            Reporter.log("processrecord.fail=1,2<p>");
            Reporter.log("app.arraysize=30<p>");
            jobParams.put("execution.number", "1");
            jobParams.put("readrecord.fail", "1,2");
            jobParams.put("app.arraysize", "30");

            Reporter.log("Locate job XML file: chunkSkipExceededTest.xml<p>");

            Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
            JobExecution execution1 = jobOp.startJobAndWaitForResult("chunkSkipExceededTest", jobParams);

            Reporter.log("execution #1 JobExecution getBatchStatus()=" + execution1.getBatchStatus() + "<p>");
            Reporter.log("execution #1 JobExecution getExitStatus()=" + execution1.getExitStatus() + "<p>");
            assertWithMessage("Testing execution #1", BatchStatus.FAILED, execution1.getBatchStatus());
            assertWithMessage("Testing execution #1", MySkipReadListener.GOOD_EXIT_STATUS, execution1.getExitStatus());
        } catch (Exception e) {
            handleException(METHOD, e);
        }

    }

    /*
     * @testName: testChunkSkipProcessExceedSkip
     * @assertion: job will finish as FAILED and exceeded skippable exceptions will be recognized 
     *             5.2.1.1 - Reader, 5.2.1.1.1 - Reader Properties,
     *             5.2.1.2 - Processor
     *             5.2.1.3 - Writer, 5.2.1.3.1 - Writer Properties
     *             5.2.1 - Chunk, item-count, skip-limit
     *             5.2.1.4 - Exception Handling - skippable-exception-classes
     * 
     * @test_Strategy: start a job with item-count specified at a value greater than the read data set and skip-limit set to 1.  
     *                  Application is configured to encounter an error on two processing actions, at which point
     *                  a skippable exception is thrown by the application. Batch Application enforces that the exceptions
     *                  were recognized as skippable and that the second exception exceeded the skip-limit
     *                  test that the job fails but the skip-limit was recognized.     
     */
    @Test
    @org.junit.Test
    public void testChunkSkipProcessExceedSkip() throws Exception {

        String METHOD = "testChunkSkipProcessExceedSkip";

        try {
            Reporter.log("Create job parameters for execution #1:<p>");
            Properties jobParams = new Properties();
            Reporter.log("execution.number=1<p>");
            Reporter.log("processrecord.fail=5,7<p>");
            Reporter.log("app.arraysize=30<p>");
            jobParams.put("execution.number", "1");
            jobParams.put("processrecord.fail", "5,7");
            jobParams.put("app.arraysize", "30");

            Reporter.log("Locate job XML file: chunkSkipExceededTest.xml<p>");

            Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
            JobExecution execution1 = jobOp.startJobAndWaitForResult("chunkSkipExceededTest", jobParams);

            Reporter.log("execution #1 JobExecution getBatchStatus()=" + execution1.getBatchStatus() + "<p>");
            Reporter.log("execution #1 JobExecution getExitStatus()=" + execution1.getExitStatus() + "<p>");
            assertWithMessage("Testing execution #1", BatchStatus.FAILED, execution1.getBatchStatus());
            assertWithMessage("Testing execution #1", MySkipProcessListener.GOOD_EXIT_STATUS, execution1.getExitStatus());
        } catch (Exception e) {
            handleException(METHOD, e);
        }

    }

    /*
     * @testName: testChunkSkipWriteExceedSkip
     * @assertion: job will finish as FAILED and exceeded skippable exceptions will be recognized 
     *             5.2.1.1 - Reader, 5.2.1.1.1 - Reader Properties,
     *             5.2.1.2 - Processor
     *             5.2.1.3 - Writer, 5.2.1.3.1 - Writer Properties
     *             5.2.1 - Chunk, item-count, skip-limit
     *             5.2.1.4 - Exception Handling - skippable-exception-classes
     * 
     * @test_Strategy: start a job with item-count specified at a value greater than the read data set and skip-limit set to 1.  
     *                  Application is configured to encounter an error on two separate writes, at which point
     *                  a skippable exception is thrown by the application. Batch Application enforces that the exceptions
     *                  were recognized as skippable and that the second exception exceeded the skip-limit
     *                  test that the job fails but the skip-limit was recognized.     
     */
    @Test
    @org.junit.Test
    public void testChunkSkipWriteExceedSkip() throws Exception {
        String METHOD = "testChunkSkipWriteExceedSkip";

        try {
            Reporter.log("Create job parameters for execution #1:<p>");
            Properties jobParams = new Properties();
            Reporter.log("execution.number=1<p>");
            Reporter.log("writerecord.fail=2,8<p>");
            Reporter.log("app.arraysize=30<p>");
            jobParams.put("execution.number", "1");
            jobParams.put("writerecord.fail", "2,8");
            jobParams.put("app.arraysize", "30");

            Reporter.log("Locate job XML file: chunkSkipExceededTest.xml<p>");

            Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
            JobExecution execution1 = jobOp.startJobAndWaitForResult("chunkSkipExceededTest", jobParams);

            Reporter.log("execution #1 JobExecution getBatchStatus()=" + execution1.getBatchStatus() + "<p>");
            Reporter.log("execution #1 JobExecution getExitStatus()=" + execution1.getExitStatus() + "<p>");
            assertWithMessage("Testing execution #1", BatchStatus.FAILED, execution1.getBatchStatus());
            assertWithMessage("Testing execution #1", MySkipWriteListener.GOOD_EXIT_STATUS, execution1.getExitStatus());
        } catch (Exception e) {
            handleException(METHOD, e);
        }

    }

    /*
     * @testName: testChunkSkipReadNoSkipChildEx
     * @assertion: job will finish as FAILED and excluded skippable exceptions will be recognized 
     *             5.2.1.1 - Reader, 5.2.1.1.1 - Reader Properties,
     *             5.2.1.2 - Processor
     *             5.2.1.3 - Writer, 5.2.1.3.1 - Writer Properties
     *             5.2.1 - Chunk, item-count, skip-limit
     *             5.2.1.4 - Exception Handling - skippable-exception-classes
     * 
     * @test_Strategy: start a job with item-count specified at a value greater than the read data set and skip-limit set to 1000.  
     *                  Application is configured to encounter an error on three separate reads.On the first two fails, the application
     *                  throws a skippable exception. On the third fail, the application throws a non-skippable exception.
     *                  The Batch Application enforces that the final exception is non-skippable.
     *                  were recognized as skippable and that the second exception exceeded the skip-limit
     *                  test that the job fails but the final exception was non skippable was recognized.     
     */
    @Test
    @org.junit.Test
    public void testChunkSkipReadNoSkipChildEx() throws Exception {
        String METHOD = "testChunkSkipReadNoSkipChildEx";

        try {
            Reporter.log("Create job parameters for execution #1:<p>");
            Properties jobParams = new Properties();
            Reporter.log("execution.number=1<p>");
            Reporter.log("writerecord.fail=1,2,3<p>");
            Reporter.log("app.arraysize=30<p>");
            jobParams.put("execution.number", "1");
            jobParams.put("readrecord.fail", "1,2,3");
            jobParams.put("app.arraysize", "30");

            Reporter.log("Locate job XML file: chunkSkipNoSkipChildExTest.xml<p>");

            Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
            JobExecution execution1 = jobOp.startJobAndWaitForResult("chunkSkipNoSkipChildExTest", jobParams);

            Reporter.log("execution #1 JobExecution getBatchStatus()=" + execution1.getBatchStatus() + "<p>");
            Reporter.log("execution #1 JobExecution getExitStatus()=" + execution1.getExitStatus() + "<p>");
            assertWithMessage("Testing execution #1", BatchStatus.FAILED, execution1.getBatchStatus());
            assertWithMessage("Testing execution #1", MySkipReadListener.GOOD_EXIT_STATUS, execution1.getExitStatus());
        } catch (Exception e) {
            handleException(METHOD, e);
        }

    }

    /*
     * @testName: testChunkRetryRead
     * @assertion: job will finish successfully as COMPLETED and retryable skippable exceptions will be recognized 
     * 			   5.2.1.1 - Reader, 5.2.1.1.1 - Reader Properties,
     *             5.2.1.2 - Processor
     *             5.2.1.3 - Writer, 5.2.1.3.1 - Writer Properties
     *             5.2.1 - Chunk, item-count, retry-limit
     *             5.2.1.5 - Exception Handling - retry-exception-classes
     * 
     * @test_Strategy: start a job with item-count specified at a value greater than the read data set and retry-limit set to 4.  
     *                  Application is configured to encounter an error on three separate reads, at which point
     *                  a retryable exception is thrown by the application. Batch Application enforces that the exceptions
     *                  were recognized as retryable and that the processing retrys the execution.
     *                  test that the job succeeds.    
     */
    @Test
    @org.junit.Test
    public void testChunkRetryRead() throws Exception {
        String METHOD = "testChunkRetryRead";

        try {
            Reporter.log("Create job parameters for execution #1:<p>");
            Properties jobParams = new Properties();
            Reporter.log("execution.number=1<p>");
            Reporter.log("writerecord.fail=8,13,22<p>");
            Reporter.log("app.arraysize=30<p>");
            jobParams.put("execution.number", "1");
            jobParams.put("readrecord.fail", "8,13,22");
            jobParams.put("app.arraysize", "30");

            Reporter.log("Locate job XML file: chunkRetryInitialTest.xml<p>");

            Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
            JobExecution execution1 = jobOp.startJobAndWaitForResult("chunkRetryInitialTest", jobParams);

            Reporter.log("execution #1 JobExecution getBatchStatus()=" + execution1.getBatchStatus() + "<p>");
            Reporter.log("execution #1 JobExecution getExitStatus()=" + execution1.getExitStatus() + "<p>");
            assertWithMessage("Testing execution #1", BatchStatus.COMPLETED, execution1.getBatchStatus());
            assertWithMessage("Testing execution #1", "COMPLETED", execution1.getExitStatus());
        } catch (Exception e) {
            handleException(METHOD, e);
        }

    }

    /*
     * @testName: testChunkItemListeners
     * @assertion: each job will finish successfully as COMPLETED and the invocation of each type of item listener
     *             will be recognized 
     * 			   5.2.1.1 - Reader, 5.2.1.1.1 - Reader Properties,
     *             5.2.1.2 - Processor, 5.2.2.1 - Processor Properties
     *             5.2.1.3 - Writer, 5.2.1.3.1 - Writer Properties
     *             5.2.1 - Chunk, item-count, retry-limit
     *             5.2.1.5 - Exception Handling - retry-exception-classes
     *             6.2.4 - ItemReadListener
     *             6.2.5 - ItemProcessListener
     *             6.2.6 - ItemWriteListener
     * 
     * @test_Strategy: start 3 separate jobs with item-count specified at a value greater than the read data set.
     *                  Each job is configured to enable an itemreadlistener, and itemprocesslistener and an itemwritelistener 
     *                  batch artifact.
     *                  The Batch Artifact enforces that each listener has been called correctly by the runtime.
     *                  test that each job succeeds and that the appropriate listener was called.    
     */
    @Test
    @org.junit.Test
    public void testChunkItemListeners() throws Exception {
        String METHOD = "testChunkItemListeners";

        try {
            Reporter.log("Create job parameters for execution #1:<p>");
            Properties jobParams = new Properties();
            Reporter.log("execution.number=1<p>");
            Reporter.log("readrecord.fail=31<p>");
            Reporter.log("app.arraysize=30<p>");
            Reporter.log("app.writepoints=0,5,10,15,20,25,30<p>");
            Reporter.log("app.listenertest=READ<p>");
            jobParams.put("execution.number", "1");
            jobParams.put("readrecord.fail", "31");
            jobParams.put("app.arraysize", "30");
            jobParams.put("app.writepoints", "0,5,10,15,20,25,30");
            jobParams.put("app.listenertest", "READ");

            Reporter.log("Locate job XML file: testListeners.xml<p>");

            Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
            JobExecution execution1 = jobOp.startJobAndWaitForResult("testListeners", jobParams);

            Reporter.log("execution #1 JobExecution getBatchStatus()=" + execution1.getBatchStatus() + "<p>");
            Reporter.log("execution #1 JobExecution getExitStatus()=" + execution1.getExitStatus() + "<p>");
            assertWithMessage("Testing execution #1 for the READ LISTENER", BatchStatus.COMPLETED, execution1.getBatchStatus());
            assertWithMessage("Testing execution #1 for the READ LISTENER", MyItemReadListenerImpl.GOOD_EXIT_STATUS,
                    execution1.getExitStatus());

            Reporter.log("Create job parameters for execution #2:<p>");
            jobParams = new Properties();
            Reporter.log("execution.number=1<p>");
            Reporter.log("readrecord.fail=31<p>");
            Reporter.log("app.arraysize=30<p>");
            Reporter.log("app.writepoints=0,5,10,15,20,25,30<p>");
            Reporter.log("app.listenertest=PROCESS<p>");
            jobParams.put("execution.number", "1");
            jobParams.put("readrecord.fail", "31");
            jobParams.put("app.arraysize", "30");
            jobParams.put("app.writepoints", "0,5,10,15,20,25,30");
            jobParams.put("app.listenertest", "PROCESS");

            Reporter.log("Invoke startJobAndWaitForResult for execution #2<p>");
            JobExecution execution2 = jobOp.startJobAndWaitForResult("testListeners", jobParams);
            Reporter.log("execution #2 JobExecution getBatchStatus()=" + execution1.getBatchStatus() + "<p>");
            Reporter.log("execution #2 JobExecution getExitStatus()=" + execution1.getExitStatus() + "<p>");
            assertWithMessage("Testing execution #2 for the PROCESS LISTENER", BatchStatus.COMPLETED, execution2.getBatchStatus());
            assertWithMessage("Testing execution #2 for the PROCESS LISTENER", MyItemReadListenerImpl.GOOD_EXIT_STATUS,
                    execution2.getExitStatus());

            Reporter.log("Create job parameters for execution #3:<p>");
            jobParams = new Properties();
            Reporter.log("execution.number=1<p>");
            Reporter.log("readrecord.fail=31<p>");
            Reporter.log("app.arraysize=30<p>");
            Reporter.log("app.writepoints=0,5,10,15,20,25,30<p>");
            Reporter.log("app.listenertest=WRITE<p>");
            jobParams.put("execution.number", "1");
            jobParams.put("readrecord.fail", "31");
            jobParams.put("app.arraysize", "30");
            jobParams.put("app.writepoints", "0,5,10,15,20,25,30");
            jobParams.put("app.listenertest", "WRITE");

            Reporter.log("Invoke startJobAndWaitForResult for execution #3<p>");
            JobExecution execution3 = jobOp.startJobAndWaitForResult("testListeners", jobParams);
            Reporter.log("execution #3 JobExecution getBatchStatus()=" + execution1.getBatchStatus() + "<p>");
            Reporter.log("execution #3 JobExecution getExitStatus()=" + execution1.getExitStatus() + "<p>");
            assertWithMessage("Testing execution #3 for the WRITE LISTENER", BatchStatus.COMPLETED, execution3.getBatchStatus());
            assertWithMessage("Testing execution #3 for the WRITE LISTENER", MyItemReadListenerImpl.GOOD_EXIT_STATUS,
                    execution3.getExitStatus());
        } catch (Exception e) {
            handleException(METHOD, e);
        }

    }


    private void showStepState(StepExecution step) {

        Reporter.log("---------------------------<p>");
        // System.out.print("getStepName(): " + step.getStepName() + " - ");
        Reporter.log("getJobExecutionId(): " + step.getJobExecutionId() + " - ");
        // System.out.print("getStepExecutionId(): " + step.getStepExecutionId()
        // + " - ");
        Metric[] metrics = step.getMetrics();

        for (int i = 0; i < metrics.length; i++) {
            Reporter.log(metrics[i].getName() + ": " + metrics[i].getValue() + " - ");
        }

        Reporter.log("getStartTime(): " + step.getStartTime() + " - ");
        Reporter.log("getEndTime(): " + step.getEndTime() + " - ");
        // System.out.print("getLastUpdateTime(): " + step.getLastUpdateTime() +
        // " - ");
        Reporter.log("getBatchStatus(): " + step.getBatchStatus() + " - ");
        Reporter.log("getExitStatus(): " + step.getExitStatus());
        Reporter.log("---------------------------<p>");
    }

    private static void handleException(String methodName, Exception e) throws Exception {
        Reporter.log("Caught exception: " + e.getMessage() + "<p>");
        Reporter.log(methodName + " failed<p>");
        throw e;
    }

}
