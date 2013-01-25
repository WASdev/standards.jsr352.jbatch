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

import javax.batch.runtime.JobExecution;


import org.junit.BeforeClass;
import org.junit.Test;

import jsr352.tck.utils.IOHelper;
import jsr352.tck.utils.JobOperatorBridge;

import static jsr352.tck.utils.AssertionUtils.assertWithMessage;

public class BatchletRestartStateMachineTest {
    
    private static JobOperatorBridge jobOp = null;
        
    public static void setup(String[] args, Properties props) throws Exception {
        jobOp = new JobOperatorBridge();
    }
    
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
    public void testMultiPartRestart() throws Exception {
        
        Properties jobParams = new Properties();
        jobParams.put("execution.number", "1");
            
        URL jobXMLURL = this.getClass().getResource("/batchletRestartStateMachine.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());
        
        JobExecution execution1 = jobOp.startJobAndWaitForResult(jobXML, jobParams);
        assertWithMessage("Testing execution #1", "STOPPED", execution1.getStatus());
        assertWithMessage("Testing execution #1", "EXECUTION.1", execution1.getExitStatus());
        
        long jobInstanceId = execution1.getInstanceId();
        //TODO - we think this will change so we restart by instanceId, for now the draft spec
        // says to restart by execution Id.
        long lastExecutionId = execution1.getExecutionId();
        
        for (int i = 2; i < 6; i++) {
            String execString = new Integer(i).toString();
            Properties jobParametersOverride = new Properties();
            jobParametersOverride.put("execution.number", execString);
            JobExecution exec = jobOp.restartJobAndWaitForResult(jobInstanceId, jobParametersOverride);
            lastExecutionId = exec.getExecutionId();
            assertWithMessage("Testing execution #" + i, "STOPPED", exec.getStatus());
            assertWithMessage("Testing execution #" + i, "EXECUTION." + execString, exec.getExitStatus());
            assertWithMessage("Testing execution #" + i, jobInstanceId, exec.getInstanceId());  
        }
        
        // Last execution should succeed
        Properties jobParametersOverride = new Properties();
        jobParametersOverride.put("execution.number", "6");
        JobExecution exec = jobOp.restartJobAndWaitForResult(jobInstanceId, jobParametersOverride);
        assertWithMessage("Testing execution #6", "COMPLETED", exec.getStatus());
        assertWithMessage("Testing execution #6", "EXECUTION.6", exec.getExitStatus());
        assert(jobInstanceId == exec.getInstanceId());  

    }
    
}
