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
import java.util.logging.Logger;

import javax.batch.runtime.JobExecution;

import jsr352.tck.utils.IOHelper;
import jsr352.tck.utils.JobOperatorBridge;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


public class ExecutionJunit {
    
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
    public void testInvokeJobWithOneBatchletStep() throws Exception {
        String METHOD = "testInvokeJobWithOneBatchletStep";
        begin(METHOD);
        URL jobXMLURL = ExecutionJunit.class.getResource("/job_batchlet_1step.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());

        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);

        assertEquals(new String ("COMPLETED"), jobExec.getStatus());
    }

    @Test
    public void testInvokeJobWithTwoStepSequenceOfBatchlets() throws Exception {
        String METHOD = "testInvokeJobWithTwoStepSequenceOfBatchlets";
        begin(METHOD);
        URL jobXMLURL = ExecutionJunit.class.getResource("/job_batchlet_2steps.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);
        
        assertEquals(new String ("COMPLETED"), jobExec.getStatus());
    }

    @Test
    public void testInvokeJobWithFourStepSequenceOfBatchlets() throws Exception {
        String METHOD = "testInvokeJobWithFourStepSequenceOfBatchlets";
        begin(METHOD);
        URL jobXMLURL = ExecutionJunit.class.getResource("/job_batchlet_4steps.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);
        
        assertEquals(new String ("COMPLETED"), jobExec.getStatus());
    }


    @Test  
    public void testInvokeJobWithNextElement() throws Exception {
        String METHOD = "testInvokeJobWithNextElement";
        begin(METHOD);
        URL jobXMLURL = ExecutionJunit.class.getResource("/job_batchlet_nextElement.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);
        
        assertEquals(new String ("COMPLETED"), jobExec.getStatus());
    }
    @Test  
    public void testInvokeJobWithFailElement() throws Exception {
        String METHOD = "testInvokeJobWithFailElement";
        begin(METHOD);
        URL jobXMLURL = ExecutionJunit.class.getResource("/job_batchlet_failElement.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);
        
        assertEquals("TEST_FAIL", jobExec.getExitStatus());
        assertEquals("FAILED", jobExec.getStatus());
    }
    @Test  
    public void testInvokeJobWithStopElement() throws Exception {
        String METHOD = "testInvokeJobWithStopElement";
        begin(METHOD);
        URL jobXMLURL = ExecutionJunit.class.getResource("/job_batchlet_stopElement.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);
        
        assertEquals(new String ("STOPPED"), jobExec.getStatus());
    }
    @Test  
    public void testInvokeJobWithEndElement() throws Exception {
        String METHOD = "testInvokeJobWithEndElement";
        begin(METHOD);
        URL jobXMLURL = ExecutionJunit.class.getResource("/job_batchlet_endElement.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);
        
        assertEquals("TEST_ENDED", jobExec.getExitStatus());
        assertEquals("COMPLETED", jobExec.getStatus());
    }
    
    @Test
    public void testInvokeJobSimpleChunk() throws Exception {
    	URL jobXMLURL = ExecutionJunit.class.getResource("/job_chunk_simple.xml");
    	String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());    	
    	JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);
    	
    	assertEquals(new String ("COMPLETED"), jobExec.getStatus());
    }
    
    @Test
    public void testInvokeJobChunkWithFullAttributes() throws Exception {
    	URL jobXMLURL = ExecutionJunit.class.getResource("/job_chunk_full_attributes.xml");
    	String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
    	JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);
    	
    	assertEquals(new String ("COMPLETED"), jobExec.getStatus());
    }

    @Test
    public void testCheckpoint() throws Exception {
    	URL jobXMLURL = ExecutionJunit.class.getResource("/job_chunk_checkpoint.xml");
    	String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
    	JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);
    	
    	assertEquals(new String ("COMPLETED"), jobExec.getStatus());
    }
    
    @Test
    public void testSimpleFlow() throws Exception {
    	URL jobXMLURL = ExecutionJunit.class.getResource("/job_flow_batchlet_4steps.xml");
    	String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
    	JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);
    	
    	assertEquals(new String ("COMPLETED"), jobExec.getStatus());
    	
    }

    
}
