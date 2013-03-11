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
import static com.ibm.jbatch.tck.utils.AssertionUtils.assertWithMessage;

import java.util.Properties;

import javax.batch.operations.JobOperator.BatchStatus;

import org.junit.BeforeClass;
import org.testng.Reporter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ibm.jbatch.tck.utils.JobOperatorBridge;
import com.ibm.jbatch.tck.utils.TCKJobExecutionWrapper;

public class BatchletRestartStateMachineTests {
    
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
	 * @testName: testMultiPartRestart
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
    @Test
    @org.junit.Test
    public void testMultiPartRestart() throws Exception {
    	
    	String METHOD = "testMultiPartRestart";
    	
    	try {
    		
    		Reporter.log("Create Job parameters for Execution #1<p>");
	        Properties jobParams = new Properties();
	        jobParams.put("execution.number", "1");
	            
	        Reporter.log("Locate job XML file: batchletRestartStateMachine.xml<p>");
	        
	        Reporter.log("Invoking startJobAndWaitForResult for Execution #1<p>");
	        TCKJobExecutionWrapper execution1 = jobOp.startJobAndWaitForResult("batchletRestartStateMachine", jobParams);
	        
	        Reporter.log("EXPECTED Execution #1 JobExecution getBatchStatus()=STOPPED<p>");
	        Reporter.log("ACTUAL Execution #1 JobExecution getBatchStatus()="+execution1.getBatchStatus()+"<p>");
	        Reporter.log("EXPECTED Execution #1 JobExecution getExitStatus()=EXECUTION.1<p>");
	        Reporter.log("ACTUAL Execution #1 JobExecution getExitStatus()="+execution1.getExitStatus()+"<p>");
	        assertWithMessage("Testing execution #1", BatchStatus.STOPPED, execution1.getBatchStatus());
	        assertWithMessage("Testing execution #1", "EXECUTION.1", execution1.getExitStatus());
	        
	        Reporter.log("Get job instance id for Execution #1<p>");
	        long jobInstanceId = execution1.getInstanceId();
	        //TODO - we think this will change so we restart by instanceId, for now the draft spec
	        // says to restart by execution Id.
	        long lastExecutionId = execution1.getExecutionId();
	        
	        TCKJobExecutionWrapper exec = null;
	        
	        for (int i = 2; i < 6; i++) {
	            String execString = new Integer(i).toString();
	            Properties restartJobParameters = new Properties(jobParams);
	            restartJobParameters.put("execution.number", execString);
	            Reporter.log("Invoke restartJobAndWaitForResult<p>");
	            exec = jobOp.restartJobAndWaitForResult(lastExecutionId, restartJobParameters);
	            lastExecutionId = exec.getExecutionId();
	            
		        Reporter.log("EXPECTED Execution #" + i + " JobExecution getBatchStatus()=STOPPED<p>");
	            Reporter.log("EXPECTED Execution #" + i + " JobExecution getExitStatus()=EXECUTION." + execString + "<p>");
		        Reporter.log("EXPECTED Execution #" + i + " job instance id="+jobInstanceId+"<p>");
		        
		        Reporter.log("ACTUAL Execution #" + i + " JobExecution getBatchStatus()="+exec.getBatchStatus()+"<p>");
		        Reporter.log("ACTUAL Execution #" + i + " JobExecution getExitStatus()="+exec.getExitStatus()+"<p>");
		        Reporter.log("ACTUAL Execution #" + i + " job instance id="+exec.getInstanceId()+"<p>");
	            assertWithMessage("Testing execution #" + i, BatchStatus.STOPPED, exec.getBatchStatus());
	            assertWithMessage("Testing execution #" + i, "EXECUTION." + execString, exec.getExitStatus());
	            assertWithMessage("Testing execution #" + i, jobInstanceId, exec.getInstanceId());  
	        }
	        
	        // Last execution should succeed
	        Reporter.log("Create Job parameters for Execution #6<p>");
	        Properties restartJobParameters = new Properties(jobParams);
	        restartJobParameters.put("execution.number", "6");
	        
	        
	        lastExecutionId = exec.getExecutionId();
	        Reporter.log("Invoking restartJobAndWaitForResult for Execution #6<p>");
	        exec = jobOp.restartJobAndWaitForResult(lastExecutionId, restartJobParameters);
	        
	        Reporter.log("EXPECTED Execution #6 JobExecution getBatchStatus()=COMPLETED<p>");
	        Reporter.log("EXPECTED Execution #6 JobExecution getExitStatus()=EXECUTION.6<p>");
	        Reporter.log("EXPECTED Execution #6 job instance id="+jobInstanceId+"<p>");
	        
	        Reporter.log("ACTUAL Execution #6 JobExecution getBatchStatus()="+execution1.getBatchStatus()+"<p>");
	        Reporter.log("ACTUAL Execution #6 JobExecution getExitStatus()="+execution1.getExitStatus()+"<p>");
	        Reporter.log("ACTUAL Execution #6 job instance id="+exec.getInstanceId()+"<p>");
	        assertWithMessage("Testing execution #6", BatchStatus.COMPLETED, exec.getBatchStatus());
	        assertWithMessage("Testing execution #6", "EXECUTION.6", exec.getExitStatus());
	        assertObjEquals(jobInstanceId, exec.getInstanceId());  
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
