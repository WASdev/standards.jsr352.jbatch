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

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import javax.batch.runtime.JobExecution;

import jsr352.tck.utils.IOHelper;
import jsr352.tck.utils.JobOperatorBridge;

import org.junit.BeforeClass;
import org.junit.Test;

public class StopFailExitStatusMatchingWithRestart {

    private static JobOperatorBridge jobOp;

    private Set<Long> completedExecutions = new HashSet<Long>();

    private final static Logger logger = Logger.getLogger(StopFailExitStatusMatchingWithRestart.class.getName());
   
    private int threadWaitTime = Integer.parseInt(System.getProperty("junit.thread.sleep.time", "500"));

    private void begin(String str) {
        logger.fine("Begin test method: " + str);
    }

    @BeforeClass
    public static void setUp() throws Exception {
        jobOp = new JobOperatorBridge();
    }

    @Test
    public void testUserStopResultsInStoppingStatus() throws Exception {
        String METHOD = "testUserStopResultsInStoppingStatus";
        begin(METHOD);

        URL jobXMLURL = ExecutionJunit.class.getResource("/job_batchlet_longrunning.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());

        Properties overrideJobParams = new Properties();
        overrideJobParams.setProperty("run.indefinitely" , "true");
        
        JobExecution execution = jobOp.startJobWithoutWaitingForResult(jobXML, overrideJobParams);

        long execID = execution.getExecutionId(); 
        logger.fine("StopRestart: Started job with execId=" + execID);

        Thread.sleep(threadWaitTime); 

        assertEquals("Hopefully job isn't finished already, if it is fail the test and use a longer sleep time within the batch step-related artifact.",
                "STARTED", execution.getStatus());

        jobOp.stopJobWithoutWaitingForResult(execution.getInstanceId());                                      

        assertEquals("Hopefully job isn't stopped already, if it is fail the test and use a longer sleep time within the batch step-related artifact.",
                "STOPPING", execution.getStatus());
    }
    
    @Test
    public void testInvokeJobWithUserStopAndRestart() throws Exception {

        String METHOD = "testInvokeJobWithUserStopAndRestart";
        begin(METHOD);

        URL jobXMLURL = ExecutionJunit.class.getResource("/job_batchlet_longrunning.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());

        Properties overrideJobParams = new Properties();
        overrideJobParams.setProperty("run.indefinitely" , "true");
        
        JobExecution execution1 = jobOp.startJobWithoutWaitingForResult(jobXML, overrideJobParams);

        long execID = execution1.getExecutionId(); 
        long jobInstanceId = execution1.getInstanceId();
        logger.fine("StopRestart: Started job with execId=" + execID);

        Thread.sleep(threadWaitTime); 

        assertEquals("Hopefully job isn't finished already, if it is fail the test and use a longer sleep time within the batch step-related artifact.",
                "STARTED", execution1.getStatus());

        jobOp.stopJobAndWaitForResult(execution1);
    
        assertEquals("The stop should have taken effect by now, even though the batchlet artifact had control at the time of the stop, it should have returned control by now.", 
                "STOPPED", execution1.getStatus());  
        
        assertEquals("BATCHLET CANCELED BEFORE COMPLETION", execution1.getExitStatus());

        overrideJobParams.setProperty("run.indefinitely" , "false");
        
        JobExecution execution2 = jobOp.restartJobAndWaitForResult(jobInstanceId, overrideJobParams);
                        
        assertEquals("If the restarted job hasn't completed yet then try increasing the sleep time.", 
                "COMPLETED", execution2.getStatus());

        assertEquals("If this fails, the reason could be that step 1 didn't run the second time," + 
                "though it should since it won't have completed successfully the first time.", 
                "GOOD.STEP.GOOD.STEP", execution2.getExitStatus());
    }

    @Test
    public void testInvokeJobWithUncaughtExceptionFailAndRestart() throws Exception {
        String METHOD = "testInvokeJobWithUncaughtExceptionFailAndRestart";
        begin(METHOD);

        URL jobXMLURL = ExecutionJunit.class.getResource("/job_batchlet_longrunning.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());

        Properties jobParameters = new Properties();
        jobParameters.setProperty("throw.exc.on.number.3" , "true");  // JSL default is 'false'

        JobExecution firstJobExecution = jobOp.startJobAndWaitForResult(jobXML, jobParameters);
        
        long jobInstanceId = firstJobExecution.getInstanceId();
        		
        logger.fine("Started job with execId=" + firstJobExecution.getExecutionId());       

        assertEquals("If the job hasn't failed yet then try increasing the sleep time.", "FAILED", firstJobExecution.getStatus());              
        assertEquals("FAILED", firstJobExecution.getExitStatus());

        Properties overrideJobParams = new Properties();
        overrideJobParams.setProperty("throw.exc.on.number.3" , "false");
        overrideJobParams.setProperty("run.indefinitely" , "false");

        JobExecution secondJobExecution = jobOp.restartJobAndWaitForResult(jobInstanceId, overrideJobParams);
        
        assertEquals("If the restarted job hasn't completed yet then try increasing the sleep time.", 
                "COMPLETED", secondJobExecution.getStatus());

        assertEquals("If this fails with only \"GOOD.STEP\", the reason could be that step 1 didn't run the second time," + 
                "though it should since it won't have completed successfully the first time.", 
                "GOOD.STEP.GOOD.STEP", secondJobExecution.getExitStatus());
    }
    
    /*
     * Obviously would be nicer to have more granular tests for some of this function,
     * but here we're going a different route and saying, if it's going to require
     * restart it will have some complexity, so let's test a few different functions
     * in one longer restart scenario.
     */
    
    @Test
    public void testStopOnEndOn() throws Exception {
        
        Properties jobParams = new Properties();
        jobParams.setProperty("execution.number", "1");
        jobParams.setProperty("step1.stop", "ES.STEP1");
        jobParams.setProperty("step1.next", "ES.XXX");
        jobParams.setProperty("step2.fail", "ES.STEP2");
        jobParams.setProperty("step2.next", "ES.XXX");
        
        URL jobXMLURL = this.getClass().getResource("/batchletStopOnEndOn.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        
        JobExecution execution1 = jobOp.startJobAndWaitForResult(jobXML, jobParams);
        assertEquals("Testing execution #1", "STOPPED", execution1.getStatus());
        assertEquals("Testing execution #1", "STOPPED", execution1.getExitStatus());
        
        long jobInstanceId = execution1.getInstanceId();
        //TODO - we think this will change so we restart by instanceId, for now the draft spec
        // says to restart by execution Id.
        long lastExecutionId = execution1.getExecutionId();

        {
            Properties jobParametersOverride = new Properties();
            jobParametersOverride.setProperty("execution.number", "2");
            jobParametersOverride.setProperty("step1.stop", "ES.STOP");
            jobParametersOverride.setProperty("step1.next", "ES.STEP1");
            JobExecution exec = jobOp.restartJobAndWaitForResult(jobInstanceId, jobParametersOverride);
            lastExecutionId = exec.getExecutionId();
            assertEquals("Testing execution #2", "FAILED", exec.getStatus());
            assertEquals("Testing execution #2", "SUCCESS", exec.getExitStatus());
            assertEquals("Testing execution #2", jobInstanceId, exec.getInstanceId());  
        }

        {
            Properties jobParametersOverride = new Properties();
            jobParametersOverride.setProperty("execution.number", "3");
            jobParametersOverride.setProperty("step1.stop", "ES.STOP");
            jobParametersOverride.setProperty("step1.next", "ES.STEP1");
            jobParametersOverride.setProperty("step2.fail", "ES.FAIL");
            jobParametersOverride.setProperty("step2.next", "ES.STEP2");
            JobExecution exec = jobOp.restartJobAndWaitForResult(jobInstanceId, jobParametersOverride);
            lastExecutionId = exec.getExecutionId();
            assertEquals("Testing execution #3", "COMPLETED", exec.getStatus());
            assertEquals("Testing execution #3", "COMPLETED", exec.getExitStatus());
            assertEquals("Testing execution #3", jobInstanceId, exec.getInstanceId());  
        }

    }

}
