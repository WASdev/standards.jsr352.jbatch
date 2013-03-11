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
import static com.ibm.jbatch.tck.utils.AssertionUtils.assertObjEquals;

import java.net.URL;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator.BatchStatus;
import javax.batch.runtime.JobExecution;

import com.ibm.jbatch.tck.utils.JobOperatorBridge;
import com.ibm.jbatch.tck.utils.TCKJobExecutionWrapper;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.testng.Reporter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class StopOrFailOnExitStatusWithRestartTests {

    private static JobOperatorBridge jobOp;

    private Set<Long> completedExecutions = new HashSet<Long>();

    private final static Logger logger = Logger.getLogger(StopOrFailOnExitStatusWithRestartTests.class.getName());
   
    private int threadWaitTime = Integer.parseInt(System.getProperty("junit.thread.sleep.time", "500"));

    private void begin(String str) {
        Reporter.log("Begin test method: " + str+"<p>");
    }

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
    
    public static void cleanup() throws Exception {
    }

    /*
   	 * @testName: testInvokeJobWithUserStopAndRestart
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    @org.junit.Test
    public void testInvokeJobWithUserStopAndRestart() throws Exception {

        String METHOD = "testInvokeJobWithUserStopAndRestart";
        begin(METHOD);

        try {
        	Reporter.log("Locate job XML file: job_batchlet_longrunning.xml<p>");
	        URL jobXMLURL = this.getClass().getResource("/job_batchlet_longrunning.xml");

	
	        Reporter.log("Create job parameters for execution #1:<p>");
	        Properties overrideJobParams = new Properties();
	        Reporter.log("run.indefinitely=true<p>");
	        overrideJobParams.setProperty("run.indefinitely" , "true");
	        
	        Reporter.log("Invoke startJobWithoutWaitingForResult for execution #1<p>");
	        TCKJobExecutionWrapper execution1 = jobOp.startJobWithoutWaitingForResult("job_batchlet_longrunning", overrideJobParams);
	
	        long execID = execution1.getExecutionId(); 
	        Reporter.log("StopRestart: Started job with execId=" + execID + "<p>");
	
	        Reporter.log("Sleep " + threadWaitTime + "<p>");
	        Thread.sleep(threadWaitTime); 
	
	        Reporter.log("execution #1 JobExecution getBatchStatus()="+execution1.getBatchStatus()+"<p>");
	        assertWithMessage("Hopefully job isn't finished already, if it is fail the test and use a longer sleep time within the batch step-related artifact.",
	                BatchStatus.STARTED, execution1.getBatchStatus());
	
	        Reporter.log("Invoke stopJobAndWaitForResult");
	        jobOp.stopJobAndWaitForResult(execution1);
	    
	        Reporter.log("execution #1 JobExecution getBatchStatus()="+execution1.getBatchStatus()+"<p>");
	        assertWithMessage("The stop should have taken effect by now, even though the batchlet artifact had control at the time of the stop, it should have returned control by now.", 
	                BatchStatus.STOPPED, execution1.getBatchStatus());  
	        
	        Reporter.log("execution #1 JobExecution getBatchStatus()="+execution1.getExitStatus()+"<p>");
	        assertObjEquals("BATCHLET CANCELED BEFORE COMPLETION", execution1.getExitStatus());
	
	        Reporter.log("Create job parameters for execution #2:<p>");
	        Reporter.log("run.indefinitely=false<p>");
	        overrideJobParams.setProperty("run.indefinitely" , "false");
	                
	        
	        Reporter.log("Invoke restartJobAndWaitForResult with executionId: " + execution1.getInstanceId() + "<p>");
	        JobExecution execution2 = jobOp.restartJobAndWaitForResult(execution1.getExecutionId(),overrideJobParams);
	                        
	        Reporter.log("execution #2 JobExecution getBatchStatus()="+execution2.getBatchStatus()+"<p>");
	        assertWithMessage("If the restarted job hasn't completed yet then try increasing the sleep time.", 
	                BatchStatus.COMPLETED, execution2.getBatchStatus());
	
	        Reporter.log("execution #2 JobExecution getExitStatus()="+execution2.getExitStatus()+"<p>");
	        assertWithMessage("If this fails, the reason could be that step 1 didn't run the second time," + 
	                "though it should since it won't have completed successfully the first time.", 
	                "GOOD.STEP.GOOD.STEP", execution2.getExitStatus());
        } catch (Exception e) {
    		handleException(METHOD, e);
    	}
    }

    /*
   	 * @testName: testInvokeJobWithUncaughtExceptionFailAndRestart
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    @org.junit.Test
    public void testInvokeJobWithUncaughtExceptionFailAndRestart() throws Exception {
        String METHOD = "testInvokeJobWithUncaughtExceptionFailAndRestart";
        begin(METHOD);

        try {
        	Reporter.log("Locate job XML file: job_batchlet_longrunning.xml<p>");
	
	        Reporter.log("Create job parameters for execution #1:<p>");
	        Properties jobParameters = new Properties();
	        Reporter.log("throw.exc.on.number.3=true<p>");
	        jobParameters.setProperty("throw.exc.on.number.3" , "true");  // JSL default is 'false'
	
	        Reporter.log("Invoke startJobAndWaitForResult");
	        TCKJobExecutionWrapper firstJobExecution = jobOp.startJobAndWaitForResult("job_batchlet_longrunning", jobParameters);
	        
	        Reporter.log("Started job with execId=" + firstJobExecution.getExecutionId()+"<p>");       
	
	        Reporter.log("execution #1 JobExecution getBatchStatus()="+firstJobExecution.getBatchStatus()+"<p>");
	        Reporter.log("execution #1 JobExecution getExitStatus()="+firstJobExecution.getExitStatus()+"<p>");
	        assertWithMessage("If the job hasn't failed yet then try increasing the sleep time.", BatchStatus.FAILED, firstJobExecution.getBatchStatus());    
	        assertObjEquals("FAILED", firstJobExecution.getExitStatus());
	
	        Reporter.log("Create job parameters for execution #2:<p>");
	        Properties overrideJobParams = new Properties();
	        Reporter.log("throw.exc.on.number.3=false<p>");
	        Reporter.log("run.indefinitely=false<p>");
	        overrideJobParams.setProperty("throw.exc.on.number.3" , "false");
	        overrideJobParams.setProperty("run.indefinitely" , "false");
	      
	        Reporter.log("Invoke restartJobAndWaitForResult with executionId: " + firstJobExecution.getInstanceId() + "<p>");
	        JobExecution secondJobExecution = jobOp.restartJobAndWaitForResult(firstJobExecution.getExecutionId(),overrideJobParams);
	        
	        Reporter.log("execution #2 JobExecution getBatchStatus()="+secondJobExecution.getBatchStatus()+"<p>");
	        assertWithMessage("If the restarted job hasn't completed yet then try increasing the sleep time.", 
	                BatchStatus.COMPLETED, secondJobExecution.getBatchStatus());
	
	        Reporter.log("execution #2 JobExecution getExitStatus()="+secondJobExecution.getExitStatus()+"<p>");
	        assertWithMessage("If this fails with only \"GOOD.STEP\", the reason could be that step 1 didn't run the second time," + 
	                "though it should since it won't have completed successfully the first time.", 
	                "GOOD.STEP.GOOD.STEP", secondJobExecution.getExitStatus());
        } catch (Exception e) {
    		handleException(METHOD, e);
    	}
    }
    
    /*
     * Obviously would be nicer to have more granular tests for some of this function,
     * but here we're going a different route and saying, if it's going to require
     * restart it will have some complexity, so let's test a few different functions
     * in one longer restart scenario.
     */
    /*
   	 * @testName: testStopOnEndOn
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
	@Test(enabled = false)
	@org.junit.Test
	@Ignore
    public void testStopOnEndOn() throws Exception {
		
		String METHOD = "testStopOnEndOn";
        
		try {
			Reporter.log("Create job parameters for execution #1:<p>");
	        Properties jobParams = new Properties();
	        Reporter.log("execution.number=1<p>");
    		Reporter.log("step1.stop=ES.STEP1<p>");
    		Reporter.log("step1.next=ES.XXX<p>");
    		Reporter.log("step2.fail=ES.STEP2<p>");
    		Reporter.log("step2.next=ES.XXX<p>");
	        jobParams.setProperty("execution.number", "1");
	        jobParams.setProperty("step1.stop", "ES.STEP1");
	        jobParams.setProperty("step1.next", "ES.XXX");
	        jobParams.setProperty("step2.fail", "ES.STEP2");
	        jobParams.setProperty("step2.next", "ES.XXX");
	        
	        Reporter.log("Locate job XML file: batchletStopOnEndOn.xml<p>");
	        URL jobXMLURL = this.getClass().getResource("/batchletStopOnEndOn.xml");

	        
	        Reporter.log("Invoke startJobAndWaitForResult");
	        TCKJobExecutionWrapper execution1 = jobOp.startJobAndWaitForResult("batchletStopOnEndOn", jobParams);
	        
	        Reporter.log("execution #1 JobExecution getBatchStatus()="+execution1.getBatchStatus()+"<p>");
	        Reporter.log("execution #1 JobExecution getExitStatus()="+execution1.getExitStatus()+"<p>");
	        assertWithMessage("Testing execution #1", BatchStatus.STOPPED, execution1.getBatchStatus());
	        assertWithMessage("Testing execution #1", "STOPPED", execution1.getExitStatus());
	        
	        long jobInstanceId = execution1.getInstanceId();
	        //TODO - we think this will change so we restart by instanceId, for now the draft spec
	        // says to restart by execution Id.
	        long lastExecutionId = execution1.getExecutionId();
	
	        {
	        	Reporter.log("Create job parameters for execution #3:<p>");
	            Properties restartJobParameters = new Properties();
	            Reporter.log("execution.number=2<p>");
	    		Reporter.log("step1.stop=ES.STOP<p>");
	    		Reporter.log("step1.next=ES.STEP1<p>");
	            restartJobParameters.setProperty("execution.number", "2");
	            restartJobParameters.setProperty("step1.stop", "ES.STOP");
	            restartJobParameters.setProperty("step1.next", "ES.STEP1");
	            Reporter.log("Invoke restartJobAndWaitForResult with executionId: " + lastExecutionId + "<p>");
	            //JobExecution exec = jobOp.restartJobAndWaitForResult(jobInstanceId);
	            TCKJobExecutionWrapper exec = jobOp.restartJobAndWaitForResult(lastExecutionId,restartJobParameters);
	            lastExecutionId = exec.getExecutionId();
	            Reporter.log("execution #2 JobExecution getBatchStatus()="+exec.getBatchStatus()+"<p>");
		        Reporter.log("execution #2 JobExecution getExitStatus()="+exec.getExitStatus()+"<p>");
		        Reporter.log("execution #2 Job instance id="+exec.getInstanceId()+"<p>");
	            assertWithMessage("Testing execution #2", BatchStatus.FAILED, exec.getBatchStatus());
	            assertWithMessage("Testing execution #2", "SUCCESS", exec.getExitStatus());
	            assertWithMessage("Testing execution #2", jobInstanceId, exec.getInstanceId());  
	        }
	
	        {
	        	Reporter.log("Create job parameters for execution #3:<p>");
	            Properties restartJobParameters = new Properties();
	            Reporter.log("execution.number=3<p>");
	    		Reporter.log("step1.stop=ES.STOP<p>");
	    		Reporter.log("step1.next=ES.STEP1<p>");
	    		Reporter.log("step2.fail=ES.FAIL<p>");
	    		Reporter.log("step2.next=ES.STEP2<p>");
	            restartJobParameters.setProperty("execution.number", "3");
	            restartJobParameters.setProperty("step1.stop", "ES.STOP");
	            restartJobParameters.setProperty("step1.next", "ES.STEP1");
	            restartJobParameters.setProperty("step2.fail", "ES.FAIL");
	            restartJobParameters.setProperty("step2.next", "ES.STEP2");
	            Reporter.log("Invoke restartJobAndWaitForResult with executionId: " + lastExecutionId + "<p>");
	          //JobExecution exec = jobOp.restartJobAndWaitForResult(jobInstanceId);
	            TCKJobExecutionWrapper exec = jobOp.restartJobAndWaitForResult(lastExecutionId,restartJobParameters);
	            lastExecutionId = exec.getExecutionId();
	            Reporter.log("execution #3 JobExecution getBatchStatus()="+exec.getBatchStatus()+"<p>");
		        Reporter.log("execution #3 JobExecution getExitStatus()="+exec.getExitStatus()+"<p>");
		        Reporter.log("execution #3 Job instance id="+exec.getInstanceId()+"<p>");
	            assertWithMessage("Testing execution #3", BatchStatus.COMPLETED, exec.getBatchStatus());
	            assertWithMessage("Testing execution #3", "COMPLETED", exec.getExitStatus());
	            assertWithMessage("Testing execution #3", jobInstanceId, exec.getInstanceId());  
	        }
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
