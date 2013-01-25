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
package jsr352.tck.tests.jslxml;

import java.net.URL;
import java.util.Properties;

import javax.batch.runtime.JobExecution;

import jsr352.tck.specialized.MetricsStepListener;
import jsr352.tck.specialized.MySkipListener;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import jsr352.tck.utils.IOHelper;
import jsr352.tck.utils.JobOperatorBridge;

import static jsr352.tck.utils.AssertionUtils.assertWithMessage;

public class ChunkStopOnEndOnChkptTest {
    
    private static JobOperatorBridge jobOp = null;
        
    
    public static void setup(String[] args, Properties props) throws Exception {
        jobOp = new JobOperatorBridge();
    }
    
    @BeforeClass
    public static void setUp() throws Exception {
        jobOp = new JobOperatorBridge();
    }
    
    /* cleanup */
	public void  cleanup()
	{		
	
	}
    
    /*
     * Obviously would be nicer to have more granular tests for some of this function,
     * but here we're going a different route and saying, if it's going to require
     * restart it will have some complexity, so let's test a few different functions
     * in one longer restart scenario.
     */
    
    /*
	 * @testName: testChunkRestart
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
    @Test
    public void testChunkRestart() throws Exception {
        
        Properties jobParams = new Properties();
        jobParams.put("execution.number", "1");
        jobParams.put("readrecord.fail", "12");
        jobParams.put("app.arraysize", "30");
        jobParams.put("app.chunksize", "7");
        jobParams.put("app.commitinterval", "10");
        
        URL jobXMLURL = this.getClass().getResource("/chunkStopOnEndOn.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        
        JobExecution execution1 = jobOp.startJobAndWaitForResult(jobXML, jobParams);
        assertWithMessage("Testing execution #1", "FAILED", execution1.getStatus());
        assertWithMessage("Testing execution #1", "FAILED", execution1.getExitStatus());
        
         long jobInstanceId = execution1.getInstanceId();
        //TODO - we think this will change so we restart by instanceId, for now the draft spec
        // says to restart by execution Id.
        long lastExecutionId = execution1.getExecutionId();

        {
            Properties jobParametersOverride = new Properties();
            jobParametersOverride.put("execution.number", "2");
            jobParams.put("app.arraysize", "30");
            jobParams.put("app.chunksize", "7");
            jobParams.put("app.commitinterval", "10");
            JobExecution exec = jobOp.restartJobAndWaitForResult(jobInstanceId, jobParametersOverride);
            lastExecutionId = exec.getExecutionId();
            assertWithMessage("Testing execution #2", "COMPLETED", exec.getStatus());
            assertWithMessage("Testing execution #2", "COMPLETED", exec.getExitStatus());
            assertWithMessage("Testing execution #2", jobInstanceId, exec.getInstanceId());  
        }

    }
    
    /*
	 * @testName: testChunkRestartInterval5commit20
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
    @Test
    public void testChunkRestartInterval5commit20() throws Exception {
        
        Properties jobParams = new Properties();
        jobParams.put("execution.number", "1");
        jobParams.put("readrecord.fail", "12");
        jobParams.put("app.arraysize", "30");
        jobParams.put("app.writepoints", "0,5,10,15,20,25,30");
        
        URL jobXMLURL = this.getClass().getResource("/chunkinterval5commitinterval7.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        
        JobExecution execution1 = jobOp.startJobAndWaitForResult(jobXML, jobParams);
        assertWithMessage("Testing execution #1", "FAILED", execution1.getStatus());
        assertWithMessage("Testing execution #1", "FAILED", execution1.getExitStatus());
        
         long jobInstanceId = execution1.getInstanceId();
        //TODO - we think this will change so we restart by instanceId, for now the draft spec
        // says to restart by execution Id.
        long lastExecutionId = execution1.getExecutionId();

        {
            Properties jobParametersOverride = new Properties();
            jobParametersOverride.put("execution.number", "2");
            jobParams.put("app.arraysize", "30");
            jobParams.put("app.writepoints", "0,5,10,15,20,25,30");
            JobExecution exec = jobOp.restartJobAndWaitForResult(jobInstanceId, jobParametersOverride);
            lastExecutionId = exec.getExecutionId();
            assertWithMessage("Testing execution #2", "COMPLETED", exec.getStatus());
            assertWithMessage("Testing execution #2", "COMPLETED", exec.getExitStatus());
            assertWithMessage("Testing execution #2", jobInstanceId, exec.getInstanceId());  
        }

    }
    
    /*
   	 * @testName: testChunkRestartChunk5commit3
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testChunkRestartChunk5commit3() throws Exception {
        
        Properties jobParams = new Properties();
        jobParams.put("execution.number", "1");
        jobParams.put("readrecord.fail", "12");
        jobParams.put("app.arraysize", "30");
        jobParams.put("app.writepoints", "0,3,6,9,12,15,18,21,24,27,30");
        
        URL jobXMLURL = this.getClass().getResource("/chunksize5commitinterval3.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        
        JobExecution execution1 = jobOp.startJobAndWaitForResult(jobXML, jobParams);
        assertWithMessage("Testing execution #1", "FAILED", execution1.getStatus());
        assertWithMessage("Testing execution #1", "FAILED", execution1.getExitStatus());
        
         long jobInstanceId = execution1.getInstanceId();
        //TODO - we think this will change so we restart by instanceId, for now the draft spec
        // says to restart by execution Id.
        long lastExecutionId = execution1.getExecutionId();

        {
            Properties jobParametersOverride = new Properties();
            jobParametersOverride.put("execution.number", "2");
            jobParametersOverride.put("app.arraysize", "30");
            jobParametersOverride.put("app.writepoints", "9,12,15,18,21,24,27,30");
            JobExecution exec = jobOp.restartJobAndWaitForResult(jobInstanceId, jobParametersOverride);
            lastExecutionId = exec.getExecutionId();
            assertWithMessage("Testing execution #2", "COMPLETED", exec.getStatus());
            assertWithMessage("Testing execution #2", "COMPLETED", exec.getExitStatus());
            assertWithMessage("Testing execution #2", jobInstanceId, exec.getInstanceId());  
        }

    }
    
    /*
   	 * @testName: testChunkRestartChunk5commit8
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testChunkRestartChunk5commit8() throws Exception {
        
        Properties jobParams = new Properties();
        jobParams.put("execution.number", "1");
        jobParams.put("readrecord.fail", "12");
        jobParams.put("app.arraysize", "30");
        jobParams.put("app.chunksize", "5");
        jobParams.put("app.commitinterval", "8");
        
        URL jobXMLURL = this.getClass().getResource("/chunksize5commitinterval8.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        
        JobExecution execution1 = jobOp.startJobAndWaitForResult(jobXML, jobParams);
        assertWithMessage("Testing execution #1", "FAILED", execution1.getStatus());
        assertWithMessage("Testing execution #1", "FAILED", execution1.getExitStatus());
        
         long jobInstanceId = execution1.getInstanceId();
        //TODO - we think this will change so we restart by instanceId, for now the draft spec
        // says to restart by execution Id.
        long lastExecutionId = execution1.getExecutionId();

        {
            Properties jobParametersOverride = new Properties();
            jobParametersOverride.put("execution.number", "2");
            jobParams.put("app.arraysize", "30");
            jobParams.put("app.chunksize", "5");
            jobParams.put("app.commitinterval", "8");
            JobExecution exec = jobOp.restartJobAndWaitForResult(jobInstanceId, jobParametersOverride);
            lastExecutionId = exec.getExecutionId();
            assertWithMessage("Testing execution #2", "COMPLETED", exec.getStatus());
            assertWithMessage("Testing execution #2", "COMPLETED", exec.getExitStatus());
            assertWithMessage("Testing execution #2", jobInstanceId, exec.getInstanceId());  
        }

    }
    
    /*
   	 * @testName: testChunkRestartChunk5commit5
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testChunkRestartChunk5commit5() throws Exception {
        
        Properties jobParams = new Properties();
        jobParams.put("execution.number", "1");
        jobParams.put("readrecord.fail", "12");
        jobParams.put("app.arraysize", "30");
        jobParams.put("app.writepoints", "0,5,10,15,20,25,30");
        jobParams.put("app.commitinterval", "5");
        
        URL jobXMLURL = this.getClass().getResource("/chunksize5commitinterval5.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        
        JobExecution execution1 = jobOp.startJobAndWaitForResult(jobXML, jobParams);
        assertWithMessage("Testing execution #1", "FAILED", execution1.getStatus());
        assertWithMessage("Testing execution #1", "FAILED", execution1.getExitStatus());
        
         long jobInstanceId = execution1.getInstanceId();
        //TODO - we think this will change so we restart by instanceId, for now the draft spec
        // says to restart by execution Id.
        long lastExecutionId = execution1.getExecutionId();

        {
            Properties jobParametersOverride = new Properties();
            jobParametersOverride.put("execution.number", "2");
            jobParametersOverride.put("app.arraysize", "30");
            jobParametersOverride.put("app.writepoints", "10,15,20,25,30");
       
            JobExecution exec = jobOp.restartJobAndWaitForResult(jobInstanceId, jobParametersOverride);
            lastExecutionId = exec.getExecutionId();
            assertWithMessage("Testing execution #2", "COMPLETED", exec.getStatus());
            assertWithMessage("Testing execution #2", "COMPLETED", exec.getExitStatus());
            assertWithMessage("Testing execution #2", jobInstanceId, exec.getInstanceId());  
        }

    }
    
    /*
   	 * @testName: testChunkRestartCustomCheckpoint
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testChunkRestartCustomCheckpoint() throws Exception {
        
        Properties jobParams = new Properties();
        jobParams.put("execution.number", "1");
        jobParams.put("readrecord.fail", "12");
        jobParams.put("app.arraysize", "30");
        jobParams.put("app.writepoints", "0,4,9,13,15,20,22,27,30");
        
        URL jobXMLURL = this.getClass().getResource("/chunkCustomCheckpoint.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        
        JobExecution execution1 = jobOp.startJobAndWaitForResult(jobXML, jobParams);
        assertWithMessage("Testing execution #1", "FAILED", execution1.getStatus());
        assertWithMessage("Testing execution #1", "FAILED", execution1.getExitStatus());
        
         long jobInstanceId = execution1.getInstanceId();
        //TODO - we think this will change so we restart by instanceId, for now the draft spec
        // says to restart by execution Id.
        long lastExecutionId = execution1.getExecutionId();

        {
            Properties jobParametersOverride = new Properties();
            jobParametersOverride.put("execution.number", "2");
            jobParametersOverride.put("app.arraysize", "30");
            jobParametersOverride.put("app.writepoints", "9,13,15,20,22,27,30");
            JobExecution exec = jobOp.restartJobAndWaitForResult(jobInstanceId, jobParametersOverride);
            lastExecutionId = exec.getExecutionId();
            assertWithMessage("Testing execution #2", "COMPLETED", exec.getStatus());
            assertWithMessage("Testing execution #2", "COMPLETED", exec.getExitStatus());
            assertWithMessage("Testing execution #2", jobInstanceId, exec.getInstanceId());  
        }

    }
    
    /*
   	 * @testName: testChunkTimeBasedDefaultCheckpoint
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testChunkTimeBasedDefaultCheckpoint() throws Exception {
               
        Properties jobParams = new Properties();
        jobParams.put("execution.number", "1");
        jobParams.put("readrecord.fail", "31");
        jobParams.put("app.arraysize", "30");
        
        URL jobXMLURL = this.getClass().getResource("/chunkTimeBasedDefaultCheckpoint.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        
        JobExecution execution1 = jobOp.startJobAndWaitForResult(jobXML, jobParams);
        assertWithMessage("Testing execution #1", "COMPLETED", execution1.getStatus());
        assertWithMessage("Testing execution #1", "COMPLETED", execution1.getExitStatus());
    }
        
    /*
   	 * @testName: testChunkRestartTimeBasedCheckpoint
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testChunkRestartTimeBasedCheckpoint() throws Exception {
        
        Properties jobParams = new Properties();
        jobParams.put("execution.number", "1");
        jobParams.put("readrecord.fail", "12");
        jobParams.put("app.arraysize", "30");
        
        URL jobXMLURL = this.getClass().getResource("/chunkTimeBasedCheckpoint.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        
        JobExecution execution1 = jobOp.startJobAndWaitForResult(jobXML, jobParams);
        assertWithMessage("Testing execution #1", "FAILED", execution1.getStatus());
        assertWithMessage("Testing execution #1", "FAILED", execution1.getExitStatus());
        
        
         long jobInstanceId = execution1.getInstanceId();
        //TODO - we think this will change so we restart by instanceId, for now the draft spec
        // says to restart by execution Id.
        long lastExecutionId = execution1.getExecutionId();

        {
            Properties jobParametersOverride = new Properties();
            jobParametersOverride.put("execution.number", "2");
            jobParametersOverride.put("app.arraysize", "30");
            JobExecution exec = jobOp.restartJobAndWaitForResult(jobInstanceId, jobParametersOverride);
            lastExecutionId = exec.getExecutionId();
            assertWithMessage("Testing execution #2", "COMPLETED", exec.getStatus());
            assertWithMessage("Testing execution #2", "COMPLETED", exec.getExitStatus());
            assertWithMessage("Testing execution #2", jobInstanceId, exec.getInstanceId());  
        }
        

    }
    
    /*
   	 * @testName: testChunkRestartTimeBasedDefaultCheckpoint
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testChunkRestartTimeBasedDefaultCheckpoint() throws Exception {
        
        Properties jobParams = new Properties();
        jobParams.put("execution.number", "1");
        jobParams.put("readrecord.fail", "21");
        jobParams.put("app.arraysize", "30");
        
        URL jobXMLURL = this.getClass().getResource("/chunkTimeBasedDefaultCheckpoint.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        
        JobExecution execution1 = jobOp.startJobAndWaitForResult(jobXML, jobParams);
        assertWithMessage("Testing execution #1", "FAILED", execution1.getStatus());
        assertWithMessage("Testing execution #1", "FAILED", execution1.getExitStatus());
        
        
         long jobInstanceId = execution1.getInstanceId();
        //TODO - we think this will change so we restart by instanceId, for now the draft spec
        // says to restart by execution Id.
        long lastExecutionId = execution1.getExecutionId();

        {
            Properties jobParametersOverride = new Properties();
            jobParametersOverride.put("execution.number", "2");
            jobParametersOverride.put("app.arraysize", "30");
            JobExecution exec = jobOp.restartJobAndWaitForResult(jobInstanceId, jobParametersOverride);
            lastExecutionId = exec.getExecutionId();
            assertWithMessage("Testing execution #2", "COMPLETED", exec.getStatus());
            assertWithMessage("Testing execution #2", "COMPLETED", exec.getExitStatus());
            assertWithMessage("Testing execution #2", jobInstanceId, exec.getInstanceId());  
        }
        

    }
    
    /*
   	 * @testName: testItemTimeCustomCheckpoint
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testItemTimeCustomCheckpoint() throws Exception {
        
        Properties jobParams = new Properties();
        jobParams.put("execution.number", "2");
        jobParams.put("readrecord.fail", "21");
        jobParams.put("app.arraysize", "30");
        
        URL jobXMLURL = this.getClass().getResource("/chunkItemTimeCustomCheckpoint.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        
        JobExecution execution1 = jobOp.startJobAndWaitForResult(jobXML, jobParams);
        assertWithMessage("Testing execution #1", "COMPLETED", execution1.getStatus());
        assertWithMessage("Testing execution #1", "COMPLETED", execution1.getExitStatus());
        
    }
    
    /*
   	 * @testName: testChunkSkipRead
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testChunkSkipRead() throws Exception {
        
        Properties jobParams = new Properties();
        jobParams.put("execution.number", "1");
        jobParams.put("readrecord.fail", "1,3");
        jobParams.put("app.arraysize", "30");
        
        URL jobXMLURL = this.getClass().getResource("/chunkSkipInitialTest.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        
        JobExecution execution1 = jobOp.startJobAndWaitForResult(jobXML, jobParams);
        assertWithMessage("Testing execution #1", "COMPLETED", execution1.getStatus());
        assertWithMessage("Testing execution #1", MySkipListener.GOOD_EXIT_STATUS, execution1.getExitStatus());
        //assertWithMessage("Testing execution #1", "COMPLETED", execution1.getExitStatus());
        
    }
    
    /*
   	 * @testName: testChunkSkipProcess
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testChunkSkipProcess() throws Exception {
        
        Properties jobParams = new Properties();
        jobParams.put("execution.number", "1");
        jobParams.put("processrecord.fail", "7,13");
        jobParams.put("app.arraysize", "30");
        
        URL jobXMLURL = this.getClass().getResource("/chunkSkipInitialTest.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        
        JobExecution execution1 = jobOp.startJobAndWaitForResult(jobXML, jobParams);
        assertWithMessage("Testing execution #1", "COMPLETED", execution1.getStatus());
        assertWithMessage("Testing execution #1", "COMPLETED", execution1.getExitStatus());
        
    }
    
    /*
   	 * @testName: testChunkSkipWrite
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testChunkSkipWrite() throws Exception {
        
        Properties jobParams = new Properties();
        jobParams.put("execution.number", "1");
        jobParams.put("writerecord.fail", "1,3");
        jobParams.put("app.arraysize", "30");
        
        URL jobXMLURL = this.getClass().getResource("/chunkSkipInitialTest.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        
        JobExecution execution1 = jobOp.startJobAndWaitForResult(jobXML, jobParams);
        assertWithMessage("Testing execution #1", "COMPLETED", execution1.getStatus());
        assertWithMessage("Testing execution #1", "COMPLETED", execution1.getExitStatus());
        
    }
    
    /*
   	 * @testName: testChunkSkipReadExceedSkip
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testChunkSkipReadExceedSkip() throws Exception {
        
        Properties jobParams = new Properties();
        jobParams.put("execution.number", "1");
        jobParams.put("readrecord.fail", "1,2");
        jobParams.put("app.arraysize", "30");
        
        URL jobXMLURL = this.getClass().getResource("/chunkSkipExceededTest.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        
        JobExecution execution1 = jobOp.startJobAndWaitForResult(jobXML, jobParams);
        assertWithMessage("Testing execution #1", "FAILED", execution1.getStatus());
        assertWithMessage("Testing execution #1", MySkipListener.GOOD_EXIT_STATUS, execution1.getExitStatus());
        
    }
    
    /*
   	 * @testName: testChunkSkipProcessExceedSkip
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testChunkSkipProcessExceedSkip() throws Exception {
        
        Properties jobParams = new Properties();
        jobParams.put("execution.number", "1");
        jobParams.put("processrecord.fail", "5,7");
        jobParams.put("app.arraysize", "30");
        
        URL jobXMLURL = this.getClass().getResource("/chunkSkipExceededTest.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        
        JobExecution execution1 = jobOp.startJobAndWaitForResult(jobXML, jobParams);
        assertWithMessage("Testing execution #1", "FAILED", execution1.getStatus());
        assertWithMessage("Testing execution #1", "FAILED", execution1.getExitStatus());
        
    }
    
    /*
   	 * @testName: testChunkSkipWriteExceedSkip
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testChunkSkipWriteExceedSkip() throws Exception {
        
        Properties jobParams = new Properties();
        jobParams.put("execution.number", "1");
        jobParams.put("writerecord.fail", "2,8");
        jobParams.put("app.arraysize", "30");
        
        URL jobXMLURL = this.getClass().getResource("/chunkSkipExceededTest.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        
        JobExecution execution1 = jobOp.startJobAndWaitForResult(jobXML, jobParams);
        assertWithMessage("Testing execution #1", "FAILED", execution1.getStatus());
        assertWithMessage("Testing execution #1", "FAILED", execution1.getExitStatus());
        
    }
    
    /*
   	 * @testName: testChunkSkipReadNoSkipChildEx
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testChunkSkipReadNoSkipChildEx() throws Exception {
        
        Properties jobParams = new Properties();
        jobParams.put("execution.number", "1");
        jobParams.put("readrecord.fail", "1,2,3");
        jobParams.put("app.arraysize", "30");
        
        URL jobXMLURL = this.getClass().getResource("/chunkSkipNoSkipChildExTest.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        
        JobExecution execution1 = jobOp.startJobAndWaitForResult(jobXML, jobParams);
        assertWithMessage("Testing execution #1", "FAILED", execution1.getStatus());
        assertWithMessage("Testing execution #1", MySkipListener.GOOD_EXIT_STATUS, execution1.getExitStatus());
        
    }
    
    /*
   	 * @testName: testChunkRetryRead
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testChunkRetryRead() throws Exception {
        
        Properties jobParams = new Properties();
        jobParams.put("execution.number", "1");
        jobParams.put("readrecord.fail", "8,13,22");
        jobParams.put("app.arraysize", "30");
        
        URL jobXMLURL = this.getClass().getResource("/chunkRetryInitialTest.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        
        JobExecution execution1 = jobOp.startJobAndWaitForResult(jobXML, jobParams);
        assertWithMessage("Testing execution #1", "COMPLETED", execution1.getStatus());
        assertWithMessage("Testing execution #1", "COMPLETED", execution1.getExitStatus());
        
    }
    
    /*
   	 * @testName: testMetrics
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testMetrics() throws Exception {
        
        Properties jobParams = new Properties();
        jobParams.put("execution.number", "1");
        jobParams.put("readrecord.fail", "40");
        jobParams.put("app.arraysize", "30");
        jobParams.put("app.chunksize", "7");
        jobParams.put("app.commitinterval", "10");
        jobParams.put("numberOfSkips", "0");
        jobParams.put("ReadProcessWrite", "READ");

        
        URL jobXMLURL = this.getClass().getResource("/testChunkMetrics.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        
        JobExecution execution1 = jobOp.startJobAndWaitForResult(jobXML, jobParams);
        assertWithMessage("Testing metrics", MetricsStepListener.GOOD_EXIT_STATUS_READ, execution1.getExitStatus());
        assertWithMessage("Testing execution #1", "COMPLETED", execution1.getStatus());
        
    }
    
    /*
   	 * @testName: testMetricsInApp
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testMetricsInApp() throws Exception {
        
        Properties jobParams = new Properties();
        jobParams.put("execution.number", "1");
        jobParams.put("readrecord.fail", "40");
        jobParams.put("app.arraysize", "30");
        jobParams.put("app.chunksize", "7");
        jobParams.put("app.commitinterval", "10");
        jobParams.put("numberOfSkips", "0");
        jobParams.put("ReadProcessWrite", "READ");
        
        URL jobXMLURL = this.getClass().getResource("/testChunkMetrics.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        
        JobExecution execution1 = jobOp.startJobAndWaitForResult(jobXML, jobParams);
        assertWithMessage("Testing execution #1", "COMPLETED", execution1.getStatus());
        assertWithMessage("Testing metrics", MetricsStepListener.GOOD_EXIT_STATUS_READ, execution1.getExitStatus());
        assertWithMessage("Testing execution #1", "COMPLETED", execution1.getStatus());
    }
    
    /*
   	 * @testName: testMetricsSkipRead
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testMetricsSkipRead() throws Exception {
        
        Properties jobParams = new Properties();
        jobParams.put("execution.number", "1");
        jobParams.put("readrecord.fail", "1,3");
        jobParams.put("app.arraysize", "30");
        jobParams.put("numberOfSkips", "2");
        jobParams.put("ReadProcessWrite", "READ_SKIP");

        
        URL jobXMLURL = this.getClass().getResource("/testMetricsSkipCount.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        
        JobExecution execution1 = jobOp.startJobAndWaitForResult(jobXML, jobParams);
        assertWithMessage("Testing execution #1", "COMPLETED", execution1.getStatus());
        assertWithMessage("Testing execution #1", MetricsStepListener.GOOD_EXIT_STATUS_READ, execution1.getExitStatus());
        
    }
    
    /*
   	 * @testName: testMetricsSkipProcess
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Ignore
    public void testMetricsSkipProcess() throws Exception {

        Properties jobParams = new Properties();
        jobParams.put("execution.number", "1");
        jobParams.put("processrecord.fail", "7,13");
        jobParams.put("app.arraysize", "30");
        jobParams.put("numberOfSkips", "2");
        jobParams.put("ReadProcessWrite", "PROCESS");
        
        URL jobXMLURL = this.getClass().getResource("/testMetricsSkipCount.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        
        JobExecution execution1 = jobOp.startJobAndWaitForResult(jobXML, jobParams);
        assertWithMessage("Testing execution #1", "COMPLETED", execution1.getStatus());
        assertWithMessage("Testing execution #1", MetricsStepListener.GOOD_EXIT_STATUS_PROCESS, execution1.getExitStatus());
        
    }
}
