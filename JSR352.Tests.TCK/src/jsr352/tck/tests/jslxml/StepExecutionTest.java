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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

import javax.batch.operations.exception.JobStartException;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.StepExecution;

import jsr352.tck.utils.IOHelper;
import jsr352.tck.utils.JobOperatorBridge;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class StepExecutionTest {
	
	private final static Logger logger = Logger.getLogger(ExecutionJunit.class.getName());
	
    private static JobOperatorBridge jobOp;

    @BeforeClass
    public static void setUp() throws Exception {
    	jobOp = new JobOperatorBridge();
    }
	
    @AfterClass
    public static void tearDown() throws Exception {
    }
    
    private void begin(String str) {
        logger.fine("Begin test method: " + str);
    }
    
    @Test
    public void testOneStepExecutionStatus() throws FileNotFoundException, IOException, JobStartException {
    	
        String METHOD = "testInvokeJobWithOneBatchletStep";
        begin(METHOD);
        URL jobXMLURL = ExecutionJunit.class.getResource("/job_batchlet_1step.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());

        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);
        
        List<StepExecution> steps = jobOp.getJobSteps(jobExec.getExecutionId());
        
        assertEquals(1, steps.size());
        
		for (StepExecution step : steps) {
			// make sure all steps finish successfully
			showStepState(step);
			assertEquals(new String("COMPLETED"), step.getStatus());
		}

        assertEquals(new String ("COMPLETED"), jobExec.getStatus());
    
    }
    
	@Test
	public void testFourStepExecutionStatus() throws FileNotFoundException, IOException, JobStartException {

		String METHOD = "testInvokeJobWithFourStepSequenceOfBatchlets";
		begin(METHOD);
		URL jobXMLURL = ExecutionJunit.class.getResource("/job_batchlet_4steps.xml");
		String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
		JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);

		List<StepExecution> steps = jobOp.getJobSteps(jobExec.getExecutionId());
		assertEquals(4, steps.size());

		for (StepExecution step : steps) {
			// check that each step completed successfully
			showStepState(step);
			assertEquals(new String("COMPLETED"), step.getStatus());
		}
		
		assertEquals(new String("COMPLETED"), jobExec.getStatus());
	}
	
    @Test  
    public void testFailedStepExecutionStatus() throws Exception {
        String METHOD = "testInvokeJobWithFailElement";
        begin(METHOD);
        URL jobXMLURL = ExecutionJunit.class.getResource("/job_batchlet_failElement.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);
        
        List<StepExecution> steps = jobOp.getJobSteps(jobExec.getExecutionId());
		for (StepExecution step : steps) {
			// check that each step completed successfully
			// TODO: shouldn't the step status be failed here ???
			showStepState(step);
		}
		
        assertEquals("TEST_FAIL", jobExec.getExitStatus());
        assertEquals("FAILED", jobExec.getStatus());
    }
 
    @Test  
    public void testStoppedStepExecutionStatus() throws Exception {
        String METHOD = "testInvokeJobWithStopElement";
        begin(METHOD);
        URL jobXMLURL = ExecutionJunit.class.getResource("/job_batchlet_stopElement.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);
        
        List<StepExecution> steps = jobOp.getJobSteps(jobExec.getExecutionId());
		for (StepExecution step : steps) {
			// check that each step completed successfully
			// TODO: shouldn't the step status be stopped here ???
			showStepState(step);
		}
		
        assertEquals(new String ("STOPPED"), jobExec.getStatus());
    }
    
    @Test
    public void testStepExecutionMetrics() {
    	
    }
    
    private void showStepState(StepExecution step) {
		System.out.println("---------------------------");
		//System.out.print("getStepName(): " + step.getStepName() + " - ");
		//System.out.print("getJobExecutionId(): " + step.getJobExecutionId() + " - ");
		//System.out.print("getStepExecutionId(): " + step.getStepExecutionId() + " - ");			
		//System.out.print("getCommitCount(): " + step.getCommitCount() + " - ");
		//System.out.print("getFilterCount(): " + step.getFilterCount() + " - ");
		//System.out.print("getProcessSkipCount(): " + step.getProcessSkipCount() + " - ");
		//System.out.print("getReadCount(): " + step.getReadCount() + " - ");
		//System.out.print("getReadSkipCount(): " + step.getReadSkipCount() + " - ");
		//System.out.print("getRollbackCount(): " + step.getRollbackCount() + " - ");
		//System.out.print("getWriteCount(): " + step.getWriteCount() + " - ");
		//System.out.print("getWriteSkipCount(): " + step.getWriteSkipCount() + " - ");
		//System.out.print("getStartTime(): " + step.getStartTime() + " - ");
		//System.out.print("getEndTime(): " + step.getEndTime() + " - ");
		//System.out.print("getLastUpdateTime(): " + step.getLastUpdateTime() + " - ");
		System.out.print("getBatchStatus(): " + step.getStatus() + " - ");
		System.out.println("getExitStatus(): " + step.getExitStatus());
		System.out.println("---------------------------");
    }
}
