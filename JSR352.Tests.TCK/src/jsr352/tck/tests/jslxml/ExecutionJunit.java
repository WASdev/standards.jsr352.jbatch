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
import java.util.logging.Logger;

import javax.batch.runtime.JobExecution;


import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import jsr352.tck.utils.IOHelper;
import jsr352.tck.utils.JobOperatorBridge;


public class ExecutionJunit {
    
    private final static Logger logger = Logger.getLogger(ExecutionJunit.class.getName());

    private static JobOperatorBridge jobOp;

    public static void setup(String[] args, Properties props) throws Exception {
        jobOp = new JobOperatorBridge();
    }
    
    @BeforeClass
    public static void setUp() throws Exception {
        jobOp = new JobOperatorBridge();
    }

    @AfterClass
    public static void cleanup() throws Exception {
    }

    private void begin(String str) {
        logger.fine("Begin test method: " + str);
    }
    
    /*
	 * @testName: testInvokeJobWithOneBatchletStep
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
    @Test  
    public void testInvokeJobWithOneBatchletStep() throws Exception {
        String METHOD = "testInvokeJobWithOneBatchletStep";
        begin(METHOD);
        URL jobXMLURL = this.getClass().getResource("/job_batchlet_1step.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());

        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);

        assert("COMPLETED" == jobExec.getStatus());
    }

    /*
   	 * @testName: testInvokeJobWithTwoStepSequenceOfBatchlets
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testInvokeJobWithTwoStepSequenceOfBatchlets() throws Exception {
        String METHOD = "testInvokeJobWithTwoStepSequenceOfBatchlets";
        begin(METHOD);
        URL jobXMLURL = this.getClass().getResource("/job_batchlet_2steps.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);
        
        assert("COMPLETED" == jobExec.getStatus());
    }

    /*
   	 * @testName: testInvokeJobWithFourStepSequenceOfBatchlets
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testInvokeJobWithFourStepSequenceOfBatchlets() throws Exception {
        String METHOD = "testInvokeJobWithFourStepSequenceOfBatchlets";
        begin(METHOD);
        URL jobXMLURL = this.getClass().getResource("/job_batchlet_4steps.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);
        
        assert("COMPLETED" == jobExec.getStatus());
    }

    /*
   	 * @testName: testInvokeJobWithNextElement
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test  
    public void testInvokeJobWithNextElement() throws Exception {
        String METHOD = "testInvokeJobWithNextElement";
        begin(METHOD);
        URL jobXMLURL = this.getClass().getResource("/job_batchlet_nextElement.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);
        
        assert("COMPLETED" == jobExec.getStatus());
    }
    
    /*
   	 * @testName: testInvokeJobWithFailElement
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test  
    public void testInvokeJobWithFailElement() throws Exception {
        String METHOD = "testInvokeJobWithFailElement";
        begin(METHOD);
        URL jobXMLURL = this.getClass().getResource("/job_batchlet_failElement.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);
        
        assert("TEST_FAIL" == jobExec.getExitStatus());
        assert("FAILED" == jobExec.getStatus());
    }
    
    /*
   	 * @testName: testInvokeJobWithStopElement
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test  
    public void testInvokeJobWithStopElement() throws Exception {
        String METHOD = "testInvokeJobWithStopElement";
        begin(METHOD);
        URL jobXMLURL = this.getClass().getResource("/job_batchlet_stopElement.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);
        
        assert("STOPPED" == jobExec.getStatus());
    }
    
    /*
   	 * @testName: testInvokeJobWithEndElement
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test  
    public void testInvokeJobWithEndElement() throws Exception {
        String METHOD = "testInvokeJobWithEndElement";
        begin(METHOD);
        URL jobXMLURL = this.getClass().getResource("/job_batchlet_endElement.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);
        
        assert("TEST_ENDED" == jobExec.getExitStatus());
        assert("COMPLETED" == jobExec.getStatus());
    }
    
    /*
   	 * @testName: testInvokeJobSimpleChunk
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testInvokeJobSimpleChunk() throws Exception {
    	URL jobXMLURL = ExecutionJunit.class.getResource("/job_chunk_simple.xml");
    	String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());    	
    	JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);
    	
    	assert("COMPLETED" == jobExec.getStatus());
    }
    
    /*
   	 * @testName: testInvokeJobChunkWithFullAttributes
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testInvokeJobChunkWithFullAttributes() throws Exception {
    	URL jobXMLURL = this.getClass().getResource("/job_chunk_full_attributes.xml");
    	String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
    	JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);
    	
    	assert("COMPLETED" == jobExec.getStatus());
    }

    /*
   	 * @testName: testCheckpoint
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testCheckpoint() throws Exception {
    	URL jobXMLURL = this.getClass().getResource("/job_chunk_checkpoint.xml");
    	String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
    	JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);
    	
    	assert("COMPLETED" == jobExec.getStatus());
    }
    
    /*
   	 * @testName: testSimpleFlow
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testSimpleFlow() throws Exception {
    	URL jobXMLURL = this.getClass().getResource("/job_flow_batchlet_4steps.xml");
    	String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
    	JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);
    	
    	assert("COMPLETED" == jobExec.getStatus());
    	
    }

    
}
