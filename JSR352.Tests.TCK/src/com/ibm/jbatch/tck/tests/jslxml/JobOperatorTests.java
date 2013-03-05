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

import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator.BatchStatus;
import javax.batch.operations.exception.NoSuchJobException;
import javax.batch.operations.exception.NoSuchJobInstanceException;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;

import com.ibm.jbatch.tck.utils.JobOperatorBridge;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class JobOperatorTests {
	
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
    
    /* cleanup */
	public void  cleanup()
	{		
	
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

	/*
	 * @testName: testJobOperatorStart
	 * 
	 * @assertion: Section 7.7.9 Job Operator - start
	 * @test_Strategy: start a job that completes successfully with no exceptions thrown.
	 * @throws JobStartException               
	 */
    @Test
    @org.junit.Test
    public void testJobOperatorStart() throws Exception {
    	
        String METHOD = "testJobOperatorStart";
        begin(METHOD);
        
        try {
        	Reporter.log("Locate job XML file: job_batchlet_1step.xml<p>");
	        URL jobXMLURL = this.getClass().getResource("/job_batchlet_1step.xml");

	
	        Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
	        JobExecution jobExec = jobOp.startJobAndWaitForResult("job_batchlet_1step");
	
			Reporter.log("execution #1 JobExecution getBatchStatus()="+jobExec.getBatchStatus()+"<p>");
	        assertObjEquals(BatchStatus.COMPLETED, jobExec.getBatchStatus());
        } catch (Exception e) {
    		handleException(METHOD, e);
    	} 
    
    }
    
	/*
	 * @testName: testJobOperatorRestart
	 * 
	 * @assertion: Section 7.7.9 Job Operator - restart
	 * @test_Strategy: start a job that is configured to fail. Change configuration of job to ensure success. Restart job.
	 *                 Test that job completes successfully and no exceptions are thrown.
	 * @throws JobExecutionAlreadyCompleteException
	 * @throws NoSuchJobExecutionException
	 * @throws JobExecutionNotMostRecentException 
	 * @throws JobRestartException
	 *                 
	 */
    @Test
    @org.junit.Test
    public void testJobOperatorRestart() throws Exception {
    	
        String METHOD = "testJobOperatorRestart";
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
	        assertWithMessage("Testing execution #1", "FAILED", execution1.getExitStatus());
	        
	         long jobInstanceId = execution1.getInstanceId();
	         long lastExecutionId = execution1.getExecutionId();
	         JobExecution exec = null;
	         JobInstance requestedJobInstance = null;
	         Reporter.log("Got Job instance id: " + jobInstanceId + "<p>");
		        Reporter.log("Got Job execution id: " + lastExecutionId + "<p>");
	         {
	        
	             Reporter.log("Invoke restartJobAndWaitForResult for execution id: " + lastExecutionId + "<p>");
	             exec = jobOp.restartJobAndWaitForResult(lastExecutionId, jobParams);
	             requestedJobInstance = jobOp.getJobInstance(exec.getInstanceId());
	             Reporter.log("execution #2 JobExecution getBatchStatus()="+exec.getBatchStatus()+"<p>");
			        Reporter.log("execution #2 JobExecution getExitStatus()="+exec.getExitStatus()+"<p>");
			        Reporter.log("execution #2 Job instance id="+exec.getInstanceId()+"<p>");
	             assertWithMessage("Testing execution #2", BatchStatus.COMPLETED, exec.getBatchStatus());
	             assertWithMessage("Testing execution #2", "COMPLETED", exec.getExitStatus());
	             assertWithMessage("Testing execution #2", jobInstanceId, exec.getInstanceId());  
	         }
	         
        } catch (Exception e) {
    		handleException(METHOD, e);
    	}
    
    }
    
    /*
   	 * @testName: testInvokeJobWithUserStop
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
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
    
	/*
	 * @testName: testJobOperatorGetStepExecutions
	 * 
	 * @assertion: Section 7.7.9 Job Operator - getJobNames
	 * @test_Strategy: start a job that completes successfully. Get the list of all StepException objects associated with job execution id.
	 *                 Test that all objects retrieved are of type StepExecution.
	 * @throws Exception
	 */
    @Test
    @org.junit.Test
    public void testJobOperatorGetStepExecutions() throws Exception {
    	
        String METHOD = "testJobOperatorGetStepExecutions";
        begin(METHOD);
        
        try {
        	Reporter.log("Locate job XML file: job_batchlet_1step.xml<p>");
	        URL jobXMLURL = this.getClass().getResource("/job_batchlet_1step.xml");

	
	        Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
	        JobExecution jobExec = jobOp.startJobAndWaitForResult("job_batchlet_1step");
	        
	        Reporter.log("Obtaining StepExecutions for execution id: " + jobExec.getExecutionId() + "<p>");
	        List<StepExecution> stepExecutions = jobOp.getStepExecutions(jobExec.getExecutionId());
	        
	        assertObjEquals(1, stepExecutions.size());
	        
			for (StepExecution step : stepExecutions) {
				// make sure all steps finish successfully
				showStepState(step);
				Reporter.log("Step status="+step.getBatchStatus()+"<p>");
				assertObjEquals(BatchStatus.COMPLETED, step.getBatchStatus());
			}
	
			Reporter.log("execution #1 JobExecution getBatchStatus()="+jobExec.getBatchStatus()+"<p>");
	        assertObjEquals(BatchStatus.COMPLETED, jobExec.getBatchStatus());
        } catch (Exception e) {
    		handleException(METHOD, e);
    	} 
    
    }
    
	/*
	 * @testName: testJobOpGetJobNames
	 * 
	 * @assertion: Section 7.7.9 Job Operator - getJobNames
	 * @test_Strategy: run a job that completes successfully. Get the list of all known job names and search for the job names that just ran.
	 *                 
	 */
	@Test
	@org.junit.Test
	public void testJobOpGetJobNames() throws Exception {

		String METHOD = "testJobOpGetJobNames";
		begin(METHOD);
		
		try {
			Reporter.log("Locate job XML file: job_batchlet_4steps.xml<p>");
			URL jobXMLURL = this.getClass().getResource("/job_batchlet_4steps.xml");

			
			Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
			JobExecution jobExec = jobOp.startJobAndWaitForResult("job_batchlet_4steps");
	
			 Reporter.log("Obtaining StepExecutions for execution id: " + jobExec.getExecutionId() + "<p>");
			List<StepExecution> stepExecutions = jobOp.getStepExecutions(jobExec.getExecutionId());
			assertObjEquals(4, stepExecutions.size());
			
			
			for (StepExecution step : stepExecutions) {
				// check that each step completed successfully
				showStepState(step);
				Reporter.log("Step status="+step.getBatchStatus()+"<p>");
				assertObjEquals(BatchStatus.COMPLETED, step.getBatchStatus());
			}
			
			Reporter.log("execution #1 JobExecution getBatchStatus()="+jobExec.getBatchStatus()+"<p>");
			assertObjEquals(BatchStatus.COMPLETED, jobExec.getBatchStatus());
			
			List<String> jobNames = jobOp.getJobNames();
			
			for (String jobname : jobNames) {
				Reporter.log("jobname found : " + jobname + " - ");
				if (jobname.equals("job1")){
					assertObjEquals(new String("job1"), jobname);
				}
			}
		} catch (Exception e) {
    		handleException(METHOD, e);
    	}
	}
	
	/*
	 * @testName: testAbandoned
	 * 
	 * @assertion: Section 7.7.9 Job Operator - getJobNames
	 * @test_Strategy: run a job that completes successfully. Abandon the job. Test to ensure the Batch Status for the said job is marked as 'ABANDONED'
	 * @throws Exception
	 * 
	 */
	@Test
	@org.junit.Test
	public void testAbandoned() throws Exception {

		String METHOD = "testAbandoned";
		begin(METHOD);
		
		try {
			Reporter.log("Locate job XML file: job_batchlet_4steps.xml<p>");
			URL jobXMLURL = this.getClass().getResource("/job_batchlet_4steps.xml");

			
			Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
			JobExecution jobExec = jobOp.startJobAndWaitForResult("job_batchlet_4steps");
			assertObjEquals(BatchStatus.COMPLETED, jobExec.getBatchStatus());
			
			jobOp.abandonJobInstance(jobExec);
			
			assertObjEquals(BatchStatus.ABANDONED, jobExec.getBatchStatus());
						
		} catch (Exception e) {
    		handleException(METHOD, e);
    	}
	}
	
	/*
	 * @testName: testJobOpgetJobInstanceCount
	 * 
	 * @assertion: Section 7.7.9 Job Operator - getJobInstanceCount
	 * @test_Strategy: Retrieve the job instance count for a known job name. Run that job. 
	 *                 Retrieve the job instance count for that job again. Test that the count has increased by 1.
	 * @throws Exception
	 * 
	 */
	@Test  
	@org.junit.Test
    public void testJobOpgetJobInstanceCount() throws Exception {
        String METHOD = "testJobOpgetJobInstanceCount";
        begin(METHOD);
        
        try {
       
	        int countTrackerBEFORE = 0;
	        
	        try {
	        	countTrackerBEFORE = jobOp.getJobInstanceCount("chunksize5commitinterval5");
	        } catch (NoSuchJobException e) {
	        	// Can continue.
	        }
	        
	        Reporter.log("Create job parameters for execution #1:<p>");
	        Properties jobParams = new Properties();
	        Reporter.log("execution.number=1<p>");
    		Reporter.log("readrecord.faile=12<p>");
    		Reporter.log("app.arraysize=30<p>");
    		Reporter.log("app.writepoints=0,5,10,15,20,25,30<p>");
    		Reporter.log("app.commitinterval=5<p>");
	        jobParams.put("execution.number", "1");
	        jobParams.put("readrecord.fail", "12");
	        jobParams.put("app.arraysize", "30");
	        jobParams.put("app.writepoints", "0,5,10,15,20,25,30");
	        jobParams.put("app.commitinterval", "5");
	        
	        Reporter.log("Locate job XML file: chunksize5commitinterval5.xml<p>");
	        URL jobXMLURL = this.getClass().getResource("/chunksize5commitinterval5.xml");
	        
	        Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
	        JobExecution execution1 = jobOp.startJobAndWaitForResult("chunksize5commitinterval5", jobParams);
	        
	        Reporter.log("execution #1 JobExecution getBatchStatus()="+execution1.getBatchStatus()+"<p>");
	        Reporter.log("execution #1 JobExecution getExitStatus()="+execution1.getExitStatus()+"<p>");
	        assertWithMessage("Testing execution #1", BatchStatus.FAILED, execution1.getBatchStatus());
	        assertWithMessage("Testing execution #1", "FAILED", execution1.getExitStatus());
	        
	         long jobInstanceId = execution1.getInstanceId();
	        //TODO - we think this will change so we restart by instanceId, for now the draft spec
	        // says to restart by execution Id.
	        long lastExecutionId = execution1.getExecutionId();
	        Reporter.log("Got Job instance id: " + jobInstanceId + "<p>");
	        Reporter.log("Got Job execution id: " + lastExecutionId + "<p>");
	        
	        int countTrackerAFTER = jobOp.getJobInstanceCount("chunksize5commitinterval5");
	        
	        assertWithMessage("job count for job1 increased by 1", countTrackerAFTER - countTrackerBEFORE == 1);
	
	        List<String> jobNames = jobOp.getJobNames();
					
			for (String jobname : jobNames) {
				Reporter.log(jobname + " instance count : " + jobOp.getJobInstanceCount(jobname) + " - ");
			}
        } catch (Exception e) {
    		handleException(METHOD, e);
    	}
    }
	
	/*
	 * @testName: testJobOpgetJobInstanceCountException
	 * 
	 * @assertion: Section 7.7.9 Job Operator - getJobInstanceCountException
	 * @test_Strategy: Retrieve the job instance count for a known job name. Run that job. 
	 *                 Retrieve the job instance count for a job name that does not exist. Test that the NoSuchJobInstanceException is returned.
	 * @throws Exception
	 * 
	 */
	@Test  
	@org.junit.Test
    public void testJobOpgetJobInstanceCountException() throws Exception {
        String METHOD = "testJobOpgetJobInstanceCountException";
        begin(METHOD);
       
        try {
	        int countTrackerBEFORE = 0;
	        
	        try {
	        	countTrackerBEFORE = jobOp.getJobInstanceCount("ChunkStopOnEndOn");
	        } catch (NoSuchJobException e) {
	        	// Can continue.
	        }
	        
	        Reporter.log("Create job parameters for execution #1:<p>");
	        Properties jobParams = new Properties();
	        Reporter.log("execution.number=1<p>");
    		Reporter.log("readrecord.faile=12<p>");
    		Reporter.log("app.arraysize=30<p>");
    		Reporter.log("app.writepoints=0,5,10,15,20,25,30<p>");
    		Reporter.log("app.commitinterval=5<p>");
	        jobParams.put("execution.number", "1");
	        jobParams.put("readrecord.fail", "12");
	        jobParams.put("app.arraysize", "30");
	        jobParams.put("app.writepoints", "0,5,10,15,20,25,30");
	        jobParams.put("app.commitinterval", "5");
	        
	        Reporter.log("Locate job XML file: chunksize5commitinterval5.xml<p>");
	        URL jobXMLURL = this.getClass().getResource("/chunksize5commitinterval5.xml");

	        
	        Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
	        JobExecution execution1 = jobOp.startJobAndWaitForResult("chunksize5commitinterval5", jobParams);
	        
	        Reporter.log("execution #1 JobExecution getBatchStatus()="+execution1.getBatchStatus()+"<p>");
	        Reporter.log("execution #1 JobExecution getExitStatus()="+execution1.getExitStatus()+"<p>");
	        assertWithMessage("Testing execution #1", BatchStatus.FAILED, execution1.getBatchStatus());
	        assertWithMessage("Testing execution #1", "FAILED", execution1.getExitStatus());
	        
	         long jobInstanceId = execution1.getInstanceId();
	        //TODO - we think this will change so we restart by instanceId, for now the draft spec
	        // says to restart by execution Id.
	        long lastExecutionId = execution1.getExecutionId();
	        Reporter.log("Got Job instance id: " + jobInstanceId + "<p>");
	        Reporter.log("Got Job execution id: " + lastExecutionId + "<p>");
	        
	        int countTrackerAFTER;
	        
	        try {
	        	countTrackerAFTER = jobOp.getJobInstanceCount("NoSuchJob");
	        } catch (NoSuchJobException noJobEx) {
	        	Reporter.log("Testing for NoSuchJobException obj, got: " + noJobEx.getClass() + "<p>");
	        	assertWithMessage("Testing for NoSuchJobException obj", noJobEx instanceof NoSuchJobException);
	        }
        } catch (Exception e) {
    		handleException(METHOD, e);
    	}
 
    }
	
	/*
	 * @testName: testJobOpgetJobInstances
	 * 
	 * @assertion: Section 7.7.9 Job Operator - getJobInstances
	 * @test_Strategy: start a job which will ensure at least one job instance known to the runtime. 
	 *                 Retrieve a list of job instance ids for the job name just started. Ask for the first 200 found.
	 *                 Test that 
	 * @throws Exception
	 * 
	 */
	@Test  
	@org.junit.Test
    public void testJobOpgetJobInstances() throws Exception {
        String METHOD = " testJobOpgetJobInstances";
        begin(METHOD);
       
        try {
	        int countTrackerBEFORE = 0;
	        
	        try {
	        	countTrackerBEFORE = jobOp.getJobInstanceCount("ChunkStopOnEndOn");
	        } catch (NoSuchJobException e) {
	        	// Can continue.
	        }
	        
	        Reporter.log("Create job parameters for execution #1:<p>");
	        Properties jobParams = new Properties();
	        Reporter.log("execution.number=1<p>");
    		Reporter.log("readrecord.faile=12<p>");
    		Reporter.log("app.arraysize=30<p>");
    		Reporter.log("app.writepoints=0,5,10,15,20,25,30<p>");
    		Reporter.log("app.commitinterval=5<p>");
	        jobParams.put("execution.number", "1");
	        jobParams.put("readrecord.fail", "12");
	        jobParams.put("app.arraysize", "30");
	        jobParams.put("app.writepoints", "0,5,10,15,20,25,30");
	        jobParams.put("app.commitinterval", "5");
	        
	        Reporter.log("Locate job XML file: chunksize5commitinterval5.xml<p>");
	        URL jobXMLURL = this.getClass().getResource("/chunksize5commitinterval5.xml");

	        
	        Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
	        JobExecution execution1 = jobOp.startJobAndWaitForResult("chunksize5commitinterval5", jobParams);
	        

	        Reporter.log("execution #1 JobExecution getBatchStatus()="+execution1.getBatchStatus()+"<p>");
	        Reporter.log("execution #1 JobExecution getExitStatus()="+execution1.getExitStatus()+"<p>");
	        assertWithMessage("Testing execution #1", BatchStatus.FAILED, execution1.getBatchStatus());
	        assertWithMessage("Testing execution #1", "FAILED", execution1.getExitStatus());
	        
	         long jobInstanceId = execution1.getInstanceId();
	        //TODO - we think this will change so we restart by instanceId, for now the draft spec
	        // says to restart by execution Id.
	        long lastExecutionId = execution1.getExecutionId();
	        Reporter.log("Got Job instance id: " + jobInstanceId + "<p>");
	        Reporter.log("Got Job execution id: " + lastExecutionId + "<p>");
	        
	        List<JobInstance> jobInstances = null;
	        
	        try {
	        	jobInstances = jobOp.getJobInstances("chunksize5commitinterval5", 0, 200);
	        } catch (NoSuchJobException noJobEx) {
	        	Reporter.log("Testing for NoSuchJobException obj, got: " + noJobEx.getClass() + "<p>");
	        	assertWithMessage("Testing for NoSuchJobException obj", !(noJobEx instanceof NoSuchJobException));
	        }
	        
        	assertWithMessage("Testing that a list of Job Instances were obtained", jobInstances.size() > 0);

	        /*
	        for (JobInstance jobInstance : jobInstances){
	        	Reporter.log("Found id " + jobInstance.getJobName() + "<p>");
	        	Reporter.log("Job instance id is: " + jobInstance.getInstanceId() + "<p>");
	        	assertWithMessage("Testing that a list of Job Instances were obtained", jobInstance.getInstanceId() > 0);
	        }
	        */
        } catch (Exception e) {
    		handleException(METHOD, e);
    	}
 
    }
	
	/*
	 * @testName: testJobOpgetJobInstancesException
	 * 
	 * @assertion: Section 7.7.9 Job Operator - getJobInstancesException
	 * @test_Strategy: Retrieve a list of job instances for a job name that does not exist. 
	 *                 Test that the NoSuchJobException is thrown.
	 * @throws  Exception 
	 * 
	 */
	@Test  
	@org.junit.Test
    public void testJobOpgetJobInstancesException() throws Exception {
        String METHOD = "testJobOpgetJobInstancesException";
        begin(METHOD);
       
        try {
	        int countTrackerBEFORE = 0;
	        
	        try {
	        	countTrackerBEFORE = jobOp.getJobInstanceCount("ChunkStopOnEndOn");
	        } catch (NoSuchJobException e) {
	        	// Can continue.
	        }
	        
	        Reporter.log("Create job parameters for execution #1:<p>");
	        Properties jobParams = new Properties();
	        Reporter.log("execution.number=1<p>");
    		Reporter.log("readrecord.faile=12<p>");
    		Reporter.log("app.arraysize=30<p>");
    		Reporter.log("app.writepoints=0,5,10,15,20,25,30<p>");
    		Reporter.log("app.commitinterval=5<p>");
	        jobParams.put("execution.number", "1");
	        jobParams.put("readrecord.fail", "12");
	        jobParams.put("app.arraysize", "30");
	        jobParams.put("app.writepoints", "0,5,10,15,20,25,30");
	        jobParams.put("app.commitinterval", "5");
	        
	        
	        Reporter.log("Locate job XML file: /chunksize5commitinterval5.xml<p>");
	        URL jobXMLURL = this.getClass().getResource("/chunksize5commitinterval5.xml");

	        
	        Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
	        JobExecution execution1 = jobOp.startJobAndWaitForResult("chunksize5commitinterval5", jobParams);
	        
	        Reporter.log("execution #1 JobExecution getBatchStatus()="+execution1.getBatchStatus()+"<p>");
	        Reporter.log("execution #1 JobExecution getExitStatus()="+execution1.getExitStatus()+"<p>");
	        assertWithMessage("Testing execution #1", BatchStatus.FAILED, execution1.getBatchStatus());
	        assertWithMessage("Testing execution #1", "FAILED", execution1.getExitStatus());
	        
	         long jobInstanceId = execution1.getInstanceId();
	        //TODO - we think this will change so we restart by instanceId, for now the draft spec
	        // says to restart by execution Id.
	        long lastExecutionId = execution1.getExecutionId();
	        
	        List<JobInstance> jobIds = null;
	        
	        try {
	        	jobIds = jobOp.getJobInstances("NoSuchJob", 0, 12);
	        } catch (NoSuchJobException noJobEx) {
	        	Reporter.log("Testing for NoSuchJobException obj, got: " + noJobEx.getClass() + "<p>");
	        	assertWithMessage("Testing for NoSuchJobException obj", noJobEx instanceof NoSuchJobException);
	        }
        } catch (Exception e) {
    		handleException(METHOD, e);
    	}
 
    }
 
	@Test(enabled = false) @org.junit.Test @Ignore
    public void testJobOperatorGetExecutionsException() throws Exception {
        String METHOD = "testInvokeJobWithStopElement";
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
	        
	             Reporter.log("Invoke restartJobAndWaitForResult for execution id: " + lastExecutionId + "<p>");
	             JobExecution exec = jobOp.restartJobAndWaitForResult(lastExecutionId, jobParams);
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
    
	/*
	 * @testName: testJobOperatorGetParameters
	 * 
	 * @assertion: Section 7.7.9 Job Operator - getParameters
	 * @test_Strategy: Start a job with a set of parameters. Restart the job with a set of override parameters. Once completed, retrieve the 
	 *                 parameters object associated with the job instance. Test that the object retrieved is a Properties object. 
	 *                 Test that the NoSuchJobException is thrown.
	 * @throws Exception
	 * 
	 */
	@Test  
	@org.junit.Test
    public void testJobOperatorGetParameters() throws Exception {
        String METHOD = "testJobOperatorGetParameters";
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
	        assertWithMessage("Testing execution #1", "FAILED", execution1.getExitStatus());
	        
	         long jobInstanceId = execution1.getInstanceId();
	         long lastExecutionId = execution1.getExecutionId();
	         JobExecution exec = null;
	         JobInstance requestedJobInstance = null;
	         Reporter.log("Got Job instance id: " + jobInstanceId + "<p>");
		        Reporter.log("Got Job execution id: " + lastExecutionId + "<p>");
	         {
	        
	             Reporter.log("Invoke restartJobAndWaitForResult for execution id: " + lastExecutionId + "<p>");
	             exec = jobOp.restartJobAndWaitForResult(lastExecutionId, jobParams);
	             requestedJobInstance = jobOp.getJobInstance(exec.getInstanceId());
	             Reporter.log("execution #2 JobExecution getBatchStatus()="+exec.getBatchStatus()+"<p>");
			        Reporter.log("execution #2 JobExecution getExitStatus()="+exec.getExitStatus()+"<p>");
			        Reporter.log("execution #2 Job instance id="+exec.getInstanceId()+"<p>");
	             assertWithMessage("Testing execution #2", BatchStatus.COMPLETED, exec.getBatchStatus());
	             assertWithMessage("Testing execution #2", "COMPLETED", exec.getExitStatus());
	             assertWithMessage("Testing execution #2", jobInstanceId, exec.getInstanceId());  
	         }
	         
	         Properties jobprops = jobOp.getParameters(requestedJobInstance);
	         jobprops.toString();
	 		
	         Reporter.log("Testing retrieval of a Properties obj, got: " + jobprops.getClass() + "<p>");
	 		assertWithMessage("Testing retrieval of a Properties obj", jobprops instanceof Properties);
	 		
	 		Properties newprops = exec.getJobParameters();
	 		Reporter.log("newprops: " + newprops.toString() + "<p>");
	 		Reporter.log("Testing retrieval of a Properties obj, got: " + newprops.getClass() + "<p>");
	 		assertWithMessage("Testing retrieval of a Properties obj", newprops instanceof Properties);
        } catch (Exception e) {
    		handleException(METHOD, e);
    	}
 		
 	}
	
	@Test(enabled = false)
	@org.junit.Test @Ignore
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
	        
	             Reporter.log("Invoke restartJobAndWaitForResult for execution id: " + lastExecutionId + "<p>");
	             exec = jobOp.restartJobAndWaitForResult(lastExecutionId, jobParams);
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
	 * @testName: testJobOperatorGetJobInstance
	 * 
	 * @assertion: Section 7.7.9 Job Operator - getJobInstance
	 * @test_Strategy: Start a job, which is configured to fail. Restart the same job which is configured to complete successfully.
	 *                 Retrieve the Job Instance object for the known job instance id. Test that the object retrieved is an instance of JobInstance
	 *                 
	 * @throws Exception 
	 * 
	 */
	@Test  
	@org.junit.Test
    public void testJobOperatorGetJobInstance() throws Exception {
        String METHOD = "testJobOperatorGetJobInstance";
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
	        assertWithMessage("Testing execution #1", "FAILED", execution1.getExitStatus());
	        
	         long jobInstanceId = execution1.getInstanceId();
	         long lastExecutionId = execution1.getExecutionId();
	         JobExecution exec = null;
	         
	         {
	        
	             Reporter.log("Invoke restartJobAndWaitForResult for execution id: " + lastExecutionId + "<p>");
	             exec = jobOp.restartJobAndWaitForResult(lastExecutionId, jobParams);
	             lastExecutionId = exec.getExecutionId();
	             Reporter.log("execution #2 JobExecution getBatchStatus()="+exec.getBatchStatus()+"<p>");
			     Reporter.log("execution #2 JobExecution getExitStatus()="+exec.getExitStatus()+"<p>");
			     Reporter.log("execution #2 Job instance id="+exec.getInstanceId()+"<p>");
	             assertWithMessage("Testing execution #2", BatchStatus.COMPLETED, exec.getBatchStatus());
	             assertWithMessage("Testing execution #2", "COMPLETED", exec.getExitStatus());
	             assertWithMessage("Testing execution #2", jobInstanceId, exec.getInstanceId());  
	         }
	         
	         JobInstance jobInstance = jobOp.getJobInstance(jobInstanceId);
	         Reporter.log("Job name = " + jobInstance.getJobName() + "<p>");
	         Reporter.log("Testing retrieval of the JobInstance obj<p>");
	 		assertWithMessage("Testing retrieval of the JobInstance obj", jobInstance instanceof JobInstance);
        } catch (Exception e) {
    		handleException(METHOD, e);
    	}
 		
 	}
	
	/*
	 * @testName: testJobOperatorGetJobInstances
	 * 
	 * @assertion: Section 7.7.9 Job Operator - getJobInstances
	 * @test_Strategy: Start a specific job four times, all of which will finish successfully. Retrieve two separate lists of JobExecutions for the job.
	 *                 List 1 will contain JobExecution Objects for job start 1 - 3. List 2 will contain JobExecution Objects for job start 2 - 4.
	 *                 Test that the second and third JobExecution objects of List 1 is equivalent to the first and second JobExecution objects in List 2.
	 *                                   
	 * @throws Exception
	 * 
	 */
	@Test  
	@org.junit.Test
    public void testJobOperatorGetJobInstances() throws Exception {
        String METHOD = "testJobOperatorGetJobInstances";
        begin(METHOD);
        
        try {
        	Reporter.log("Create job parameters for execution #1:<p>");
	        Properties jobParams = new Properties();
	        Reporter.log("execution.number=1<p>");
    		Reporter.log("readrecord.fail=31<p>");
    		Reporter.log("app.arraysize=30<p>");
    		Reporter.log("app.writepoints=0,5,10,15,20,25,30<p>");
    		Reporter.log("app.commitinterval=5<p>");
	        jobParams.put("execution.number", "1");
	        jobParams.put("readrecord.fail", "31");
	        jobParams.put("app.arraysize", "30");
	        jobParams.put("app.writepoints", "0,5,10,15,20,25,30");
	        jobParams.put("app.commitinterval", "5");
	        
	        Reporter.log("Locate job XML file: chunksize5commitinterval5.xml<p>");
	        URL jobXMLURL = this.getClass().getResource("/chunksize5commitinterval5.xml");

	        
	        Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
	        JobExecution execution1 = jobOp.startJobAndWaitForResult("chunksize5commitinterval5", jobParams);
	        
	        Reporter.log("execution #1 JobExecution getBatchStatus()="+execution1.getBatchStatus()+"<p>");
	        Reporter.log("execution #1 JobExecution getExitStatus()="+execution1.getExitStatus()+"<p>");
	        assertWithMessage("Testing execution #1", BatchStatus.COMPLETED, execution1.getBatchStatus());
	        assertWithMessage("Testing execution #1", "COMPLETED", execution1.getExitStatus());
	        
	        Reporter.log("Create job parameters for execution #2:<p>");
	        jobParams = new Properties();
	        Reporter.log("execution.number=2<p>");
	        Reporter.log("readrecord.fail=31<p>");
	    	 Reporter.log("app.arraysize=30<p>");
	    	 Reporter.log("app.writepoints=10,15,20,25,30<p>");
	    	 Reporter.log("app.commitinterval=5<p>");
	        jobParams.put("execution.number", "1");
	        jobParams.put("readrecord.fail", "31");
	        jobParams.put("app.arraysize", "30");
	        jobParams.put("app.writepoints", "0,5,10,15,20,25,30");
	        jobParams.put("app.commitinterval", "5");
	        
	        Reporter.log("Invoke startJobAndWaitForResult for execution #2<p>");
	        JobExecution execution2 = jobOp.startJobAndWaitForResult("chunksize5commitinterval5", jobParams);
	        Reporter.log("execution #2 JobExecution getBatchStatus()="+execution1.getBatchStatus()+"<p>");
	        Reporter.log("execution #2 JobExecution getExitStatus()="+execution1.getExitStatus()+"<p>");
	        assertWithMessage("Testing execution #2", BatchStatus.COMPLETED, execution2.getBatchStatus());
	        assertWithMessage("Testing execution #2", "COMPLETED", execution2.getExitStatus());
	        
	        Reporter.log("Invoke startJobAndWaitForResult for execution #2<p>");
	        JobExecution execution3 = jobOp.startJobAndWaitForResult("chunksize5commitinterval5", jobParams);
	        Reporter.log("execution #3 JobExecution getBatchStatus()="+execution1.getBatchStatus()+"<p>");
	        Reporter.log("execution #3 JobExecution getExitStatus()="+execution1.getExitStatus()+"<p>");
	        assertWithMessage("Testing execution #3", BatchStatus.COMPLETED, execution3.getBatchStatus());
	        assertWithMessage("Testing execution #3", "COMPLETED", execution3.getExitStatus());
	        
	        Reporter.log("Invoke startJobAndWaitForResult for execution #2<p>");
	        JobExecution execution4 = jobOp.startJobAndWaitForResult("chunksize5commitinterval5", jobParams);
	        Reporter.log("execution #4 JobExecution getBatchStatus()="+execution1.getBatchStatus()+"<p>");
	        Reporter.log("execution #4 JobExecution getExitStatus()="+execution1.getExitStatus()+"<p>");
	        assertWithMessage("Testing execution #4", BatchStatus.COMPLETED, execution4.getBatchStatus());
	        assertWithMessage("Testing execution #4", "COMPLETED", execution4.getExitStatus());
	         
	         List <JobInstance> jobInstances012 = jobOp.getJobInstances("chunksize5commitinterval5",0,3);
	         List <JobInstance> jobInstances123 = jobOp.getJobInstances("chunksize5commitinterval5",1,3);
	         
	         for (int i=0; i<3; i++){
	        	 System.out.println("AJM: instance id012["+i+"] = " + jobInstances012.get(i).getInstanceId());
	        	 System.out.println("AJM: instance id123["+i+"] = " + jobInstances123.get(i).getInstanceId());
	         }
	         
	         assertWithMessage("job instances should not be equal", jobInstances012.get(0).getInstanceId()!=jobInstances123.get(0).getInstanceId()); 
	         assertWithMessage("job instances should be equal", jobInstances012.get(1).getInstanceId()==jobInstances123.get(0).getInstanceId()); 
	         assertWithMessage("job instances should be equal", jobInstances012.get(2).getInstanceId()==jobInstances123.get(1).getInstanceId()); 
	         assertWithMessage("job instances should not be equal", jobInstances012.get(2).getInstanceId()!=jobInstances123.get(2).getInstanceId()); 

	         
	         
	         Reporter.log("Size of jobInstancesList = " + jobInstances012.size() + "<p>");
	         Reporter.log("Testing retrieval of the JobInstances list<p>");
	 		assertWithMessage("Testing retrieval of the JobInstances list", jobInstances012.size() > 0);
         } catch (Exception e) {
    		handleException(METHOD, e);
    	}
 		
 	}
	
	/*
	 * @testName: testJobOperatorGetRunningJobInstances
	 * 
	 * @assertion: Section 7.7.9 Job Operator - getJobInstances
	 * @test_Strategy: start a job which will ensure at least one job instance known to the runtime. Job will be long running. Testcase does not wait for job to complete.
	 *                 Retrieve a list of job instance ids for the job name just started that are in running state. Ensure that at least one Job Instance is returned
	 *                 Test that 
	 * @throws Exception
	 * 
	 */
	@Test
	@org.junit.Test
    public void testJobOperatorGetRunningJobExecutions() throws Exception {
        String METHOD = "testJobOperatorGetRunningJobInstances";
        begin(METHOD);
        
        try {
        	Reporter.log("Create job parameters for execution #1:<p>");
	        Properties jobParams = new Properties();
	        Reporter.log("app.timeinterval=10000<p>");
	        jobParams.put("app.timeinterval", "10000");
	        
	        Reporter.log("Locate job XML file: job_batchlet_step_listener.xml<p>");
	        URL jobXMLURL = this.getClass().getResource("/job_batchlet_step_listener.xml");

	        
	        Reporter.log("Invoke startJobWithoutWaitingForResult for execution #1<p>");
	        JobExecution execution1 = jobOp.startJobWithoutWaitingForResult("job_batchlet_step_listener", jobParams);
	        
	         //long jobInstanceId = execution1.getInstanceId();
	         //long lastExecutionId = execution1.getExecutionId();
	         JobExecution exec = null;
	         
	         
			Properties jobParametersOverride = new Properties();
			jobParametersOverride.put("app.timeinterval", "10000");
	
			Reporter.log("Invoke startJobWithoutWaitingForResult<p>");
			exec = jobOp.startJobWithoutWaitingForResult("job_batchlet_step_listener", jobParametersOverride);
	         
	         List<JobExecution> jobExecutions = jobOp.getRunningExecutions("job_batchlet_step_listener");
	         assertWithMessage("Found job instances in the RUNNING state", jobExecutions.size() > 0);
	         
        } catch (Exception e) {
    		handleException(METHOD, e);
    	}
 		
 	}
	
	/*
	 * @testName: testJobOperatorGetRunningJobInstancesExeception
	 * 
	 * @assertion: Section 7.7.9 Job Operator - getJobInstances
	 * @test_Strategy: start a job which will ensure at least one job instance known to the runtime. Job will be long running. Testcase does not wait for job to complete.
	 *                 Retrieve a list of job instance ids for a job name that does not exist in running state. Ensure that NoSuchJobException exception is thrown
	 *                 Test that 
	 * @throws Exception
	 * 
	 */
	@Test
	@org.junit.Test
    public void testJobOperatorGetRunningJobExecutionsExeception() throws Exception {
        String METHOD = "testJobOperatorGetRunningJobInstancesExeception";
        begin(METHOD);
        
        try {
        	Reporter.log("Create job parameters for execution #1:<p>");
	        Properties jobParams = new Properties();
	        Reporter.log("app.timeinterval=10000<p>");
	        jobParams.put("app.timeinterval", "10000");
	        
	        Reporter.log("Locate job XML file: job_batchlet_step_listener.xml<p>");
	        URL jobXMLURL = this.getClass().getResource("/job_batchlet_step_listener.xml");

	        
	        Reporter.log("Invoke startJobWithoutWaitingForResult for execution #1<p>");
	        JobExecution execution1 = jobOp.startJobWithoutWaitingForResult("job_batchlet_step_listener", jobParams);
	        
	         //long jobInstanceId = execution1.getInstanceId();
	         //long lastExecutionId = execution1.getExecutionId();
	         JobExecution exec = null;
	         
	         
			Properties jobParametersOverride = new Properties();
			jobParametersOverride.put("app.timeinterval", "10000");
	
			Reporter.log("Invoke startJobWithoutWaitingForResult");
			exec = jobOp.startJobWithoutWaitingForResult("job_batchlet_step_listener", jobParametersOverride);
			//lastExecutionId = exec.getExecutionId();

			try {
				Reporter.log("Check for an instance of a non-existent job<p>");
				jobOp.getRunningExecutions("JOBNAMEDOESNOTEXIST");
			}
			catch (Exception e) {
				Reporter.log("Test that exception caught is an instanceof NoSuchJobExecutionException<p>");
				assertWithMessage("Exception caught is an instanceof NoSuchJobExecutionException", e instanceof NoSuchJobException);
			}
        } catch (Exception e) {
    		handleException(METHOD, e);
    	}
         
 	}
	
	/*
	 * @testName: testJobOperatorGetJobExecution
	 * 
	 * @assertion: Section 7.7.9 Job Operator - getJobExecution
	 * @test_Strategy: start a job which will run to successful completion.
	 *                 Retrieve a JobExecution object using the execution ID returned by the start command.
	 *                 Ensure the object returned is an instance of JobExecution
	 *                  
	 * @throws Exception 
	 * 
	 */
	@Test 
	@org.junit.Test
    public void testJobOperatorGetJobExecution() throws Exception {
        String METHOD = "testJobOperatorGetJobExecution";
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
	        assertWithMessage("Testing execution #1", "FAILED", execution1.getExitStatus());
	        
	         long jobInstanceId = execution1.getInstanceId();
	         long lastExecutionId = execution1.getExecutionId();
	         JobExecution exec = null;
	         
	         {
	        
	             Reporter.log("Invoke restartJobAndWaitForResult for execution id: " + lastExecutionId + "<p>");
	             exec = jobOp.restartJobAndWaitForResult(lastExecutionId, jobParams);
	             lastExecutionId = exec.getExecutionId();
	             Reporter.log("execution #2 JobExecution getBatchStatus()="+exec.getBatchStatus()+"<p>");
			     Reporter.log("execution #2 JobExecution getExitStatus()="+exec.getExitStatus()+"<p>");
			     Reporter.log("execution #2 Job instance id="+exec.getInstanceId()+"<p>");
	             assertWithMessage("Testing execution #2", BatchStatus.COMPLETED, exec.getBatchStatus());
	             assertWithMessage("Testing execution #2", "COMPLETED", exec.getExitStatus());
	             assertWithMessage("Testing execution #2", jobInstanceId, exec.getInstanceId());  
	         }
	         
	         Reporter.log("Testing retrieval of a JobExecution obj");
	         JobExecution jobEx = jobOp.getJobExecution(lastExecutionId);
	         Reporter.log("Status retreived from JobExecution obj: " + jobEx.getBatchStatus() + "<p>");
	 		assertWithMessage("Testing retrieval of a JobExecution obj", jobEx instanceof JobExecution);
        } catch (Exception e) {
    		handleException(METHOD, e);
    	}
 		
 	}
	
	/*
	 * @testName: testJobOperatorGetJobExecutions
	 * 
	 * @assertion: Section 7.7.9 Job Operator - getJobExecutions
	 * @test_Strategy: start a job which will run to successful completion.
	 *                 Retrieve a JobExecution object using the execution ID returned by the start command.
	 *                 Ensure the object returned is an instance of JobExecution
	 *                  
	 * @throws Exception 
	 * 
	 */
	@Test 
	@org.junit.Test
    public void testJobOperatorGetJobExecutions() throws Exception {
        String METHOD = "testJobOperatorGetJobExecutions";
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
	        assertWithMessage("Testing execution #1", "FAILED", execution1.getExitStatus());
	        
	         long jobInstanceId = execution1.getInstanceId();
	         JobInstance jobInstance = jobOp.getJobInstance(jobInstanceId);
	         long lastExecutionId = execution1.getExecutionId();
	         JobExecution exec = null;
	         
	         {
	        
	             Reporter.log("Invoke restartJobAndWaitForResult for execution id: " + lastExecutionId + "<p>");
	             exec = jobOp.restartJobAndWaitForResult(lastExecutionId, jobParams);
	             lastExecutionId = exec.getExecutionId();
	             
	             Reporter.log("execution #2 JobExecution getBatchStatus()="+exec.getBatchStatus()+"<p>");
			     Reporter.log("execution #2 JobExecution getExitStatus()="+exec.getExitStatus()+"<p>");
			     Reporter.log("execution #2 Job instance id="+exec.getInstanceId()+"<p>");
	             assertWithMessage("Testing execution #2", BatchStatus.COMPLETED, exec.getBatchStatus());
	             assertWithMessage("Testing execution #2", "COMPLETED", exec.getExitStatus());
	             assertWithMessage("Testing execution #2", jobInstanceId, exec.getInstanceId());  
	         }
	         
	         Reporter.log("Testing retrieval of a Properties obj<p>");
	         List<JobExecution> jobExecutions = jobOp.getJobExecutions(jobInstance);
	         assertWithMessage("Testing list size of JobExecutions", jobExecutions.size() == 2);
	         for (JobExecution je : jobExecutions){
	        	 Reporter.log("Status retreived from JobExecution obj: " + je.getBatchStatus() + "<p>");
	        	 assertWithMessage("Testing retrieval of a JobExecution obj", je instanceof JobExecution);
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
