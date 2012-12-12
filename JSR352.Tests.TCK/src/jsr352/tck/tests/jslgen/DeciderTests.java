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
package jsr352.tck.tests.jslgen;

import static org.junit.Assert.assertEquals;

import java.util.Properties;
import java.util.logging.Logger;

import javax.batch.runtime.JobExecution;

import jsr352.batch.jsl.Batchlet;
import jsr352.batch.jsl.Decision;
import jsr352.batch.jsl.End;
import jsr352.batch.jsl.Fail;
import jsr352.batch.jsl.JSLProperties;
import jsr352.batch.jsl.Step;
import jsr352.batch.jsl.Stop;
import jsr352.tck.common.StatusConstants;
import jsr352.tck.specialized.DeciderTestsBatchlet;
import jsr352.tck.specialized.DeciderTestsDecider;
import jsr352.tck.utils.JSLBuilder;
import jsr352.tck.utils.JobOperatorBridge;

import org.junit.BeforeClass;
import org.junit.Test;

public class DeciderTests implements StatusConstants {
    private final static Logger logger = Logger.getLogger(DeciderTests.class.getName());
    private static JobOperatorBridge jobOp = null;
    
    @BeforeClass
    public static void setUp() throws Exception {
        jobOp = new JobOperatorBridge();                              
    }

   
    /* 
     * Uses batchlet-level properties.
     */
    private JSLBuilder commonBuilder(String specialExitStatus) {
    	
    	JSLBuilder builder = new JSLBuilder();
    	
    	Step step1 = builder.addBatchletStep("step1", "DeciderBatchlet" );
    	Batchlet b1 = step1.getBatchlet();
    	JSLProperties b1Props = builder.createSingleVariableProperty(DeciderTestsBatchlet.ACTION);
    	b1.setProperties(b1Props);
    	builder.createNextVariableProperty(b1Props, DeciderTestsBatchlet.ACTUAL_VALUE);    	    	
    	
    	Step step2 = builder.addBatchletStep("step2", "DeciderBatchlet" );
    	Batchlet b2 = step2.getBatchlet();
    	JSLProperties b2Props = builder.createSingleProperty(DeciderTestsBatchlet.ACTION, "N/A");
    	b2.setProperties(b2Props);
    	builder.createNextProperty(b2Props, DeciderTestsBatchlet.ACTUAL_VALUE, "N/A");
    	
        step1.setNextFromAttribute("decision1");        
        Decision d = builder.addDecision("decision1", "deciderTestsDecider" );
        builder.addNext(d, "?:Next*", "step2");
        End endNormal = builder.addEnd(d, "1:EndNormal");
        endNormal.setExitStatus("EndNormal");
        End endSpecial = builder.addEnd(d, "1:EndSpecial");
        endSpecial.setExitStatus("EndSpecial");
        Stop stopNormal = builder.addStop(d, "1:StopNorm*");
        stopNormal.setExitStatus("StopNormal");
        Stop stopSpecial = builder.addStop(d, "1:StopSpec??l");
        stopSpecial.setExitStatus("StopSpecial");
        Fail failNormal = builder.addFail(d, "1:FailN*");
        failNormal.setExitStatus("FailNormal");
        Fail failSpecial = builder.addFail(d, "1:FailSpec*");
        failSpecial.setExitStatus("FailSpecial");
        Fail failUnExpected = builder.addFail(d, "*");
        failUnExpected.setExitStatus(UNEXPECTED);
     
        JSLProperties deciderProps = 
        	builder.createSingleProperty(DeciderTestsDecider.SPECIAL_EXIT_STATUS, specialExitStatus);
        d.setProperties(deciderProps);
        
        return builder;
    }
    
    private String buildCommonJSL(String specialExitStatus) {
    	JSLBuilder builder = commonBuilder(specialExitStatus);
    	return builder.getJSL();
    }

    @Test
    public void testDeciderEndNormal() throws Exception {

    	// 1. Here "EndSpecial" is the exit status the decider will return if the step exit status
    	// is the "special" exit status value.  It is set as a property on the decider.
    	String jobXML = buildCommonJSL("EndSpecial"); 
    	        
        Properties jobParameters = new Properties();
    	// 2. Here "EndNormal" is the exit status the decider will return if the step exit status
    	// is the "normal" exit status value.  It is set as a property on the batchlet and passed
        // along to the decider via stepContext.setTransientUserData().
        jobParameters.setProperty(DeciderTestsBatchlet.ACTION, "EndNormal");
    	// 3. This "ACTUAL_VALUE" is a property set on the batchlet.  It will either indicate to end
        // the step with a "normal" or "special" exit status.                
        jobParameters.setProperty(DeciderTestsBatchlet.ACTUAL_VALUE, DeciderTestsBatchlet.NORMAL_VALUE);
        
        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML, jobParameters); 

        assertEquals("EndNormal", jobExec.getExitStatus());
        assertEquals("COMPLETED", jobExec.getStatus());
    }
    
    
    @Test
    public void testDeciderEndSpecial() throws Exception {

    	String jobXML = buildCommonJSL("EndSpecial");
    	
        Properties jobParameters = new Properties();        
        jobParameters.setProperty(DeciderTestsBatchlet.ACTION, "EndNormal");
        // 1. This is the only test parameter that differs from testDeciderEndNormal().
        jobParameters.setProperty(DeciderTestsBatchlet.ACTUAL_VALUE, DeciderTestsBatchlet.SPECIAL_VALUE);
        
        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML, jobParameters); 

        // 2. And the job exit status differs accordingly.
        assertEquals("EndSpecial", jobExec.getExitStatus());
        assertEquals("COMPLETED", jobExec.getStatus());
    }

    // See the first two test methods for an explanation of parameter values.
    @Test
    public void testDeciderStopNormal() throws Exception {
    	String jobXML = buildCommonJSL("StopSpecial");
    	
        Properties jobParameters = new Properties();        
        jobParameters.setProperty(DeciderTestsBatchlet.ACTION, "StopNormal");
        jobParameters.setProperty(DeciderTestsBatchlet.ACTUAL_VALUE, DeciderTestsBatchlet.NORMAL_VALUE);
        
        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML, jobParameters); 

        assertEquals("StopNormal", jobExec.getExitStatus());
        assertEquals("STOPPED", jobExec.getStatus());
    }

    // See the first two test methods for an explanation of parameter values.
    @Test
    public void testDeciderStopSpecial() throws Exception {
    	String jobXML = buildCommonJSL("StopSpecial");
    	
        Properties jobParameters = new Properties();        
        jobParameters.setProperty(DeciderTestsBatchlet.ACTION, "StopNormal");
        jobParameters.setProperty(DeciderTestsBatchlet.ACTUAL_VALUE, DeciderTestsBatchlet.SPECIAL_VALUE);
        
        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML, jobParameters); 

        assertEquals("StopSpecial", jobExec.getExitStatus());
        assertEquals("STOPPED", jobExec.getStatus());
    }

    // See the first two test methods for an explanation of parameter values.
    @Test
    public void testDeciderFailNormal() throws Exception {
    	String jobXML = buildCommonJSL("FailSpecial");
    	
        Properties jobParameters = new Properties();        
        jobParameters.setProperty(DeciderTestsBatchlet.ACTION, "FailNormal");
        jobParameters.setProperty(DeciderTestsBatchlet.ACTUAL_VALUE, DeciderTestsBatchlet.NORMAL_VALUE);
        
        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML, jobParameters); 

        assertEquals("FailNormal", jobExec.getExitStatus());
        assertEquals("FAILED", jobExec.getStatus());
    }
    
    // See the first two test methods for an explanation of parameter values.
    @Test
    public void testDeciderFailSpecial() throws Exception {
    	String jobXML = buildCommonJSL("FailSpecial");
    	
        Properties jobParameters = new Properties();        
        jobParameters.setProperty(DeciderTestsBatchlet.ACTION, "FailNormal");
        jobParameters.setProperty(DeciderTestsBatchlet.ACTUAL_VALUE, DeciderTestsBatchlet.SPECIAL_VALUE);
        
        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML, jobParameters); 

        assertEquals("FailSpecial", jobExec.getExitStatus());
        assertEquals("FAILED", jobExec.getStatus());
    }
    
    @Test
    public void testDeciderNextNormal() throws Exception {
    	JSLBuilder builder = commonBuilder("NextSpecial");

    	Properties jobParameters = new Properties();        
        jobParameters.setProperty(DeciderTestsBatchlet.ACTION, "NextNormal");
        jobParameters.setProperty(DeciderTestsBatchlet.ACTUAL_VALUE, DeciderTestsBatchlet.NORMAL_VALUE);
        
        builder.createSingleJobListener("deciderTestsJobListener");
        String jobXML = builder.getJSL();
        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML, jobParameters); 
    	
        assertEquals(GOOD_JOB_EXIT_STATUS, jobExec.getExitStatus());
        assertEquals("COMPLETED", jobExec.getStatus());
    }
    
    @Test
    public void testDeciderNextSpecial() throws Exception {
    	JSLBuilder builder = commonBuilder("NextSpecial");

    	Properties jobParameters = new Properties();        
        jobParameters.setProperty(DeciderTestsBatchlet.ACTION, "NextNormal");
        jobParameters.setProperty(DeciderTestsBatchlet.ACTUAL_VALUE, DeciderTestsBatchlet.SPECIAL_VALUE);
        
        builder.createSingleJobListener("deciderTestsJobListener");
        String jobXML = builder.getJSL();
        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML, jobParameters); 
    	
        // This actually exits with the exact same status as the "...NextNormal" test.
        assertEquals(GOOD_JOB_EXIT_STATUS, jobExec.getExitStatus());
        assertEquals("COMPLETED", jobExec.getStatus());
    }   

}
