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

import static com.ibm.jbatch.tck.utils.AssertionUtils.assertObjEquals;

import java.net.URL;
import java.util.Properties;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator.BatchStatus;
import javax.batch.runtime.JobExecution;

import com.ibm.jbatch.tck.utils.JobOperatorBridge;

import org.junit.BeforeClass;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ExecutionTests {
    
    private final static Logger logger = Logger.getLogger(ExecutionTests.class.getName());

    private static JobOperatorBridge jobOp;

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

    @AfterClass
    public static void cleanup() throws Exception {
    }

    private void begin(String str) {
        Reporter.log("Begin test method: " + str);
    }
    
    /*
	 * @testName: testInvokeJobWithOneBatchletStep
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
    @Test
    @org.junit.Test  
    public void testInvokeJobWithOneBatchletStep() throws Exception {
        String METHOD = "testInvokeJobWithOneBatchletStep";
        begin(METHOD);
        
        try {
        	Reporter.log("Locate job XML file: job_batchlet_1step.xml<p>");
	        URL jobXMLURL = this.getClass().getResource("/job_batchlet_1step.xml");

	
	        Reporter.log("Invoking startJobAndWaitForResult for Execution #1<p>");
	        JobExecution jobExec = jobOp.startJobAndWaitForResult("job_batchlet_1step");
	
	        Reporter.log("execution #1 JobExecution getBatchStatus()="+jobExec.getBatchStatus()+"<p>");
	        assertObjEquals(BatchStatus.COMPLETED, jobExec.getBatchStatus());
        } catch (Exception e) {
        	handleException(METHOD, e);
        }
    }

    /*
   	 * @testName: testInvokeJobWithTwoStepSequenceOfBatchlets
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    @org.junit.Test
    public void testInvokeJobWithTwoStepSequenceOfBatchlets() throws Exception {
        String METHOD = "testInvokeJobWithTwoStepSequenceOfBatchlets";
        begin(METHOD);
        
        try {
        	Reporter.log("Locate job XML file: job_batchlet_2steps.xml<p>");
	        URL jobXMLURL = this.getClass().getResource("/job_batchlet_2steps.xml");

	        
	        Reporter.log("Invoking startJobAndWaitForResult for Execution #1<p>");
	        JobExecution jobExec = jobOp.startJobAndWaitForResult("job_batchlet_2steps");
	        
	        Reporter.log("execution #1 JobExecution getBatchStatus()="+jobExec.getBatchStatus()+"<p>");
	        assertObjEquals(BatchStatus.COMPLETED, jobExec.getBatchStatus());
        } catch (Exception e) {
        	handleException(METHOD, e);
        }
    }

    /*
   	 * @testName: testInvokeJobWithFourStepSequenceOfBatchlets
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    @org.junit.Test
    public void testInvokeJobWithFourStepSequenceOfBatchlets() throws Exception {
        String METHOD = "testInvokeJobWithFourStepSequenceOfBatchlets";
        begin(METHOD);
        
        try {
        	Reporter.log("Locate job XML file: job_batchlet_4steps.xml<p>");
	        URL jobXMLURL = this.getClass().getResource("/job_batchlet_4steps.xml");

	        
	        Reporter.log("Invoking startJobAndWaitForResult for Execution #1<p>");
	        JobExecution jobExec = jobOp.startJobAndWaitForResult("job_batchlet_4steps");
	        
	        Reporter.log("execution #1 JobExecution getBatchStatus()="+jobExec.getBatchStatus()+"<p>");
	        assertObjEquals(BatchStatus.COMPLETED, jobExec.getBatchStatus());
        } catch (Exception e) {
        	handleException(METHOD, e);
        }
    }

    /*
   	 * @testName: testInvokeJobWithNextElement
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    @org.junit.Test  
    public void testInvokeJobWithNextElement() throws Exception {
        String METHOD = "testInvokeJobWithNextElement";
        begin(METHOD);
        
        try {
        	Reporter.log("Locate job XML file: job_batchlet_nextElement.xml<p>");
	        URL jobXMLURL = this.getClass().getResource("/job_batchlet_nextElement.xml");

	        
	        Reporter.log("Invoking startJobAndWaitForResult for Execution #1<p>");
	        JobExecution jobExec = jobOp.startJobAndWaitForResult("job_batchlet_nextElement");
	        
	        Reporter.log("execution #1 JobExecution getBatchStatus()="+jobExec.getBatchStatus()+"<p>");
	        assertObjEquals(BatchStatus.COMPLETED, jobExec.getBatchStatus());
        } catch (Exception e) {
        	handleException(METHOD, e);
        }
    }
    
    /*
   	 * @testName: testInvokeJobWithFailElement
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    @org.junit.Test  
    public void testInvokeJobWithFailElement() throws Exception {
        String METHOD = "testInvokeJobWithFailElement";
        begin(METHOD);
        
        try {
        	Reporter.log("Locate job XML file: job_batchlet_failElement.xml<p>");
	        URL jobXMLURL = this.getClass().getResource("/job_batchlet_failElement.xml");

	        
	        Reporter.log("Invoking startJobAndWaitForResult for Execution #1<p>");
	        JobExecution jobExec = jobOp.startJobAndWaitForResult("job_batchlet_failElement");
	        
	        Reporter.log("execution #1 JobExecution getExitStatus()="+jobExec.getExitStatus()+"<p>");
	        Reporter.log("execution #1 JobExecution getBatchStatus()="+jobExec.getBatchStatus()+"<p>");
	        assertObjEquals("TEST_FAIL", jobExec.getExitStatus());
	        assertObjEquals(BatchStatus.FAILED, jobExec.getBatchStatus());
        } catch (Exception e) {
        	handleException(METHOD, e);
        }
    }
    
    /*
   	 * @testName: testInvokeJobWithStopElement
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    @org.junit.Test  
    public void testInvokeJobWithStopElement() throws Exception {
        String METHOD = "testInvokeJobWithStopElement";
        begin(METHOD);
        
        try {
        	Reporter.log("Locate job XML file: job_batchlet_stopElement.xml<p>");
	        URL jobXMLURL = this.getClass().getResource("/job_batchlet_stopElement.xml");

	        
	        Reporter.log("Invoking startJobAndWaitForResult for Execution #1<p>");
	        JobExecution jobExec = jobOp.startJobAndWaitForResult("job_batchlet_stopElement");
	        
	        Reporter.log("execution #1 JobExecution getBatchStatus()="+jobExec.getBatchStatus()+"<p>");
	        assertObjEquals(BatchStatus.STOPPED, jobExec.getBatchStatus());
        } catch (Exception e) {
        	handleException(METHOD, e);
        }
    }
    
    /*
   	 * @testName: testInvokeJobWithEndElement
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    @org.junit.Test  
    public void testInvokeJobWithEndElement() throws Exception {
        String METHOD = "testInvokeJobWithEndElement";
        begin(METHOD);
        
        try {
        	Reporter.log("Locate job XML file: job_batchlet_endElement.xml<p>");
	        URL jobXMLURL = this.getClass().getResource("/job_batchlet_endElement.xml");

	        
	        Reporter.log("Invoking startJobAndWaitForResult for Execution #1<p>");
	        JobExecution jobExec = jobOp.startJobAndWaitForResult("job_batchlet_endElement");
	        
	        Reporter.log("execution #1 JobExecution getExitStatus()="+jobExec.getExitStatus()+"<p>");
	        Reporter.log("execution #1 JobExecution getBatchStatus()="+jobExec.getBatchStatus()+"<p>");
	        assertObjEquals("TEST_ENDED", jobExec.getExitStatus());
	        assertObjEquals(BatchStatus.COMPLETED, jobExec.getBatchStatus());
        } catch (Exception e) {
        	handleException(METHOD, e);
        }
    }
    
    /*
   	 * @testName: testInvokeJobSimpleChunk
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    @org.junit.Test
    public void testInvokeJobSimpleChunk() throws Exception {
    	String METHOD = "testInvokeJobSimpleChunk";
    	begin(METHOD);
    	
    	try {
    		Reporter.log("Locate job XML file: job_chunk_simple.xml<p>");
	    	URL jobXMLURL = ExecutionTests.class.getResource("/job_chunk_simple.xml");

	    	
	    	Reporter.log("Invoking startJobAndWaitForResult for Execution #1<p>");
	    	JobExecution jobExec = jobOp.startJobAndWaitForResult("job_chunk_simple");
	    	
	    	Reporter.log("execution #1 JobExecution getBatchStatus()="+jobExec.getBatchStatus()+"<p>");
	    	assertObjEquals(BatchStatus.COMPLETED, jobExec.getBatchStatus());
    	} catch (Exception e) {
        	handleException(METHOD, e);
        }
    }
    
    /*
   	 * @testName: testInvokeJobChunkWithFullAttributes
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    @org.junit.Test
    public void testInvokeJobChunkWithFullAttributes() throws Exception {
    	String METHOD = "testInvokeJobChunkWithFullAttributes";
    	begin(METHOD);
    	
    	try {
    		Reporter.log("Locate job XML file: job_chunk_full_attributes.xml<p>");
	    	URL jobXMLURL = this.getClass().getResource("/job_chunk_full_attributes.xml");

	    	
	    	Reporter.log("Invoking startJobAndWaitForResult for Execution #1<p>");
	    	JobExecution jobExec = jobOp.startJobAndWaitForResult("job_chunk_full_attributes");
	    	
	    	Reporter.log("execution #1 JobExecution getBatchStatus()="+jobExec.getBatchStatus()+"<p>");
	    	assertObjEquals(BatchStatus.COMPLETED, jobExec.getBatchStatus());
    	} catch (Exception e) {
        	handleException(METHOD, e);
        }
    }

    /*
   	 * @testName: testCheckpoint
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    @org.junit.Test
    public void testCheckpoint() throws Exception {
    	String METHOD = "testCheckpoint";
    	begin(METHOD);
    	
    	try {
    		Reporter.log("Locate job XML file: job_chunk_checkpoint.xml<p>");
	    	URL jobXMLURL = this.getClass().getResource("/job_chunk_checkpoint.xml");

	    	
	    	Reporter.log("Invoking startJobAndWaitForResult for Execution #1<p>");
	    	JobExecution jobExec = jobOp.startJobAndWaitForResult("job_chunk_checkpoint");
	    	
	    	Reporter.log("execution #1 JobExecution getBatchStatus()="+jobExec.getBatchStatus()+"<p>");
	    	assertObjEquals(BatchStatus.COMPLETED, jobExec.getBatchStatus());
    	} catch (Exception e) {
        	handleException(METHOD, e);
        }
    }
    
    /*
   	 * @testName: testSimpleFlow
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    @org.junit.Test
    public void testSimpleFlow() throws Exception {
    	String METHOD = "testSimpleFlow";
    	begin(METHOD);
    	
    	try {
    		Reporter.log("Locate job XML file: job_flow_batchlet_4steps.xml<p>");
	    	URL jobXMLURL = this.getClass().getResource("/job_flow_batchlet_4steps.xml");

	    	
	    	Reporter.log("Invoking startJobAndWaitForResult for Execution #1<p>");
	    	JobExecution jobExec = jobOp.startJobAndWaitForResult("job_flow_batchlet_4steps");
	    	
	    	Reporter.log("execution #1 JobExecution getBatchStatus()="+jobExec.getBatchStatus()+"<p>");
	    	assertObjEquals(BatchStatus.COMPLETED, jobExec.getBatchStatus());
    	} catch (Exception e) {
        	handleException(METHOD, e);
        }
    	
    }

    /*
   	 * @testName: testInvokeJobWithUserStop
   	 * @assertion: The batch status of a job is set to stopped after it is stopped through the job operator
   	 * 
   	 * @test_Strategy: Issue a job that runs in an infinite loop. Issue a job operator stop and verify the 
   	 * batch status.
   	 */
    @Test
    @org.junit.Test
    public void testInvokeJobWithUserStop() throws Exception {
        String METHOD = "testInvokeJobWithUserStop";
        begin(METHOD);
        
        try {
        	Reporter.log("Locate job XML file: job_batchlet_longrunning.xml<p>");
	        URL jobXMLURL = ExecutionTests.class.getResource("/job_batchlet_longrunning.xml");

	        
	        Reporter.log("Create job parameters for execution #1:<p>");
	        Properties jobParameters = new Properties();
	        Reporter.log("run.indefinitely=true<p>");
	        jobParameters.setProperty("run.indefinitely" , "true");
	        
	        Reporter.log("Invoking startJobWithoutWaitingForResult for Execution #1<p>");
	        JobExecution jobExec = jobOp.startJobWithoutWaitingForResult("job_batchlet_longrunning", jobParameters);
	        
	        Reporter.log("Thread.sleep(1000)");
	        Thread.sleep(1000);
	
	        Reporter.log("Invoking stopJobAndWaitForResult for Execution #1<p>");
	        jobOp.stopJobAndWaitForResult(jobExec);
	        
	        Reporter.log("execution #1 JobExecution getBatchStatus()="+jobExec.getBatchStatus()+"<p>");
	        assertObjEquals(BatchStatus.STOPPED, jobExec.getBatchStatus());
        } catch (Exception e) {
        	handleException(METHOD, e);
        }
        
    }
    
    private static void handleException(String methodName, Exception e) throws Exception {
		Reporter.log("Caught exception: " + e.getMessage()+"<p>");
		Reporter.log(methodName + " failed<p>");
		throw e;
	}
    
}
