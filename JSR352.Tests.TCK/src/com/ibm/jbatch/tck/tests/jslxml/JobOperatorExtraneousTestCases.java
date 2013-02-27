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
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator.BatchStatus;
import javax.batch.operations.exception.NoSuchJobInstanceException;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;

import com.ibm.jbatch.tck.utils.JobOperatorBridge;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class JobOperatorExtraneousTestCases {
	
	//PLACE HOLDER FOR JOBOP TESTCASES THAT ARE UNDER CONSIDERATION
	
	private final static Logger logger = Logger.getLogger(JobOperatorTests.class.getName());
	
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
	
    @AfterMethod
    public static void tearDown() throws Exception {
    }
    
    private void begin(String str) {
       Reporter.log("Begin test method: " + str);
    }
    
    private static void handleException(String methodName, Exception e) throws Exception {
		Reporter.log("Caught exception: " + e.getMessage()+"<p>");
		Reporter.log(methodName + " failed<p>");
		throw e;
	}

 
	@Test(enabled = false) @org.junit.Test @Ignore
    public void testJobOperatorGetExecutionsException() throws Exception {
        String METHOD = "testJobOperatorGetExecutionsException";
        begin(METHOD);
        
        try {
        	Reporter.log("Create job parameters for execution #1:<p>");
	        Properties jobParams = new Properties();
	        Reporter.log("execution.number=1<p>");
    		Reporter.log("readrecord.fail=12<p>");
    		Reporter.log("app.writepoints=0,5,10,15,20,25,30<p>");
    		Reporter.log("app.next.writepoints=10,15,20,25,30<p>");
    		Reporter.log("app.commitinterval=5<p>");
	        jobParams.put("execution.number", "1");
	        jobParams.put("readrecord.fail", "12");
	        jobParams.put("app.arraysize", "30");
	        jobParams.put("app.writepoints", "0,5,10,15,20,25,30");
	        jobParams.put("app.next.writepoints", "10,15,20,25,30");
	        jobParams.put("app.commitinterval", "5");
	        
	        Reporter.log("Locate job XML file: chunksize5commitinterval5.xml<p>");
	        URL jobXMLURL = this.getClass().getResource("/chunksize5commitinterval5.xml");

	        
	        Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
	        JobExecution execution1 = jobOp.startJobAndWaitForResult("chunksize5commitinterval5", jobParams);
	        
	        Reporter.log("execution #1 JobExecution getBatchStatus()="+execution1.getBatchStatus()+"<p>");
	        Reporter.log("execution #1 JobExecution getExitStatus()="+execution1.getExitStatus()+"<p>");
	        assertWithMessage("Testing execution #1", BatchStatus.FAILED, execution1.getBatchStatus());
	        assertWithMessage("Testing execution #1", BatchStatus.FAILED, execution1.getExitStatus());
	        
	         long jobInstanceId = execution1.getInstanceId();
	         long lastExecutionId = execution1.getExecutionId();
	         Reporter.log("Got Job instance id: " + jobInstanceId + "<p>");
		        Reporter.log("Got Job execution id: " + lastExecutionId + "<p>");
		        
	         {
	        	 Reporter.log("Create job parameters for execution #2:<p>");
	             Properties jobParametersOverride = new Properties();
	             Reporter.log("execution.number=2<p>");
		    		Reporter.log("app.arraysize=30<p>");
		    		Reporter.log("app.writepoints=10,15,20,25,30<p>");
	             jobParametersOverride.put("execution.number", "2");
	             jobParametersOverride.put("app.arraysize", "30");
	             jobParametersOverride.put("app.writepoints", "10,15,20,25,30");
	        
	             Reporter.log("Invoke restartJobAndWaitForResult for execution id: " + lastExecutionId + "<p>");
	             JobExecution exec = jobOp.restartJobAndWaitForResult(lastExecutionId);
	             lastExecutionId = exec.getExecutionId();
	             
	             Reporter.log("execution #2 JobExecution getBatchStatus()="+exec.getBatchStatus()+"<p>");
			     Reporter.log("execution #2 JobExecution getExitStatus()="+exec.getExitStatus()+"<p>");
			     Reporter.log("execution #2 Job instance id="+exec.getInstanceId()+"<p>");
	             assertWithMessage("Testing execution #2", BatchStatus.COMPLETED, exec.getBatchStatus());
	             assertWithMessage("Testing execution #2", BatchStatus.COMPLETED, exec.getExitStatus());
	             assertWithMessage("Testing execution #2", jobInstanceId, exec.getInstanceId());  
	         }
	         
	         try {
	        	 List<JobExecution> instances = jobOp.getExecutions(null);
	         }
	         catch (Exception e){
	        	 Reporter.log("Testing for NoSuchJobInstanceException obj, got: " + e.getClass() + "<p>");
	        	 assertWithMessage("Exception caught is an instanceof NoSuchJobInstanceException", e instanceof NoSuchJobInstanceException);
	         }
        } catch (Exception e) {
    		handleException(METHOD, e);
    	}
      
    }
    
	
	@Test(enabled = false) @org.junit.Test @Ignore
    public void testJobOperatorGetParametersException() throws Exception {
        String METHOD = "testJobOperatorGetParametersException";
        begin(METHOD);
        
        try {
        	Reporter.log("Create job parameters for execution #1:<p>");
	        Properties jobParams = new Properties();
	        Reporter.log("execution.number=1<p>");
    		Reporter.log("readrecord.fail=12<p>");
    		Reporter.log("app.arraysize=30<p>");
    		Reporter.log("app.writepoints=0,5,10,15,20,25,30<p>");
    		Reporter.log("app.next.writepoints=10,15,20,25,30<p>");
    		Reporter.log("app.commitinterval=5<p>");
	        jobParams.put("execution.number", "1");
	        jobParams.put("readrecord.fail", "12");
	        jobParams.put("app.arraysize", "30");
	        jobParams.put("app.writepoints", "0,5,10,15,20,25,30");
	        jobParams.put("app.next.writepoints", "10,15,20,25,30");
	        jobParams.put("app.commitinterval", "5");
	        
	        Reporter.log("Locate job XML file: chunksize5commitinterval5.xml<p>");
	        URL jobXMLURL = this.getClass().getResource("/chunksize5commitinterval5.xml");

	        
	        Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
	        JobExecution execution1 = jobOp.startJobAndWaitForResult("chunksize5commitinterval5", jobParams);
	        
	        Reporter.log("execution #1 JobExecution getBatchStatus()="+execution1.getBatchStatus()+"<p>");
	        Reporter.log("execution #1 JobExecution getExitStatus()="+execution1.getExitStatus()+"<p>");
	        assertWithMessage("Testing execution #1", BatchStatus.FAILED, execution1.getBatchStatus());
	        assertWithMessage("Testing execution #1", BatchStatus.FAILED, execution1.getExitStatus());
	        
	         long jobInstanceId = execution1.getInstanceId();
	         long lastExecutionId = execution1.getExecutionId();
	         JobExecution exec = null;
	         Reporter.log("Got Job instance id: " + jobInstanceId + "<p>");
		     Reporter.log("Got Job execution id: " + lastExecutionId + "<p>");
	         {
	        	 Reporter.log("Create job parameters for execution #2:<p>");
	             Properties jobParametersOverride = new Properties();
	             Reporter.log("execution.number=2<p>");
		    	 Reporter.log("app.arraysize=30<p>");
		    	 Reporter.log("app.writepoints=10,15,20,25,30<p>");
	             jobParametersOverride.put("execution.number", "2");
	             jobParametersOverride.put("app.arraysize", "30");
	             jobParametersOverride.put("app.writepoints", "10,15,20,25,30");
	        
	             Reporter.log("Invoke restartJobAndWaitForResult for execution id: " + lastExecutionId + "<p>");
	             exec = jobOp.restartJobAndWaitForResult(lastExecutionId);
	             lastExecutionId = exec.getExecutionId();
	             Reporter.log("execution #2 JobExecution getBatchStatus()="+exec.getBatchStatus()+"<p>");
			     Reporter.log("execution #2 JobExecution getExitStatus()="+exec.getExitStatus()+"<p>");
			     Reporter.log("execution #2 Job instance id="+exec.getInstanceId()+"<p>");
	             assertWithMessage("Testing execution #2", BatchStatus.COMPLETED, exec.getBatchStatus());
	             assertWithMessage("Testing execution #2", BatchStatus.COMPLETED, exec.getExitStatus());
	             assertWithMessage("Testing execution #2", jobInstanceId, exec.getInstanceId());  
	         }
	  	
	 		try{
	 			Properties newprops = jobOp.getParameters(null);
	 		}
	 		catch (Exception e){
	 			Reporter.log("Testing that exception caught is an instanceof NoSuchJobInstanceException, got: " + e.getClass() + "<p>");
	 			assertWithMessage("Exception caught is an instanceof NoSuchJobInstanceException", e instanceof NoSuchJobInstanceException);
	 		}
        } catch (Exception e) {
    		handleException(METHOD, e);
    	}
 		
 	}
	

	
	/*
	   @Ignore
	   
	    public void testJobOperatorGetStepExecutions() throws FileNotFoundException, IOException, JobStartException {
	    	
	        String METHOD = "testJobOp";
	        begin(METHOD);
	        URL jobXMLURL = this.getClass().getResource("/job_batchlet_4steps.xml");


	        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);
	        
	        List<StepExecution> steps = jobOp.getJobSteps(jobExec.getExecutionId());
	        
	        assertObjEquals(4, steps.size());
	        
			for (StepExecution step : steps) {
				// make sure all steps finish successfully
				showStepState(step);
				assertTrue("Testing retrieval of a StepExecution obj", step instanceof StepExecution);
			}

	        assertObjEquals(new String (BatchStatus.COMPLETED), jobExec.getBatchStatus());
	    
	    }
	    */
    
    private void showStepState(StepExecution step) {
    	
    	
		Reporter.log("---------------------------");
		Reporter.log("getStepName(): " + step.getName() + " - ");
		Reporter.log("getJobExecutionId(): " + step.getJobExecutionId() + " - ");
		//System.out.print("getStepExecutionId(): " + step.getStepExecutionId() + " - ");
		Metric[] metrics = step.getMetrics();
		
		for (int i = 0; i < metrics.length; i++) {
			Reporter.log(metrics[i].getName() + ": " + metrics[i].getValue() + " - ");
		}
		
		Reporter.log("getStartTime(): " + step.getStartTime() + " - ");
		Reporter.log("getEndTime(): " + step.getEndTime() + " - ");
		//System.out.print("getLastUpdateTime(): " + step.getLastUpdateTime() + " - ");
		Reporter.log("getBatchStatus(): " + step.getBatchStatus() + " - ");
		Reporter.log("getExitStatus(): " + step.getExitStatus());
		Reporter.log("---------------------------");
    }
}
