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

import static jsr352.tck.utils.AssertionUtils.assertWithMessage;

import java.net.URL;
import java.util.Properties;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator.BatchStatus;
import javax.batch.runtime.JobExecution;

import jsr352.tck.utils.JobOperatorBridge;

import org.junit.BeforeClass;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ContextAndListenerTests {

    private final static Logger logger = Logger.getLogger(ContextAndListenerTests.class.getName());
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

    /*
	 * @testName: testOneArtifactIsJobAndStepListener
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
    @Test
    @org.junit.Test 
    public void testOneArtifactIsJobAndStepListener() throws Exception {

    	String METHOD = "testOneArtifactIsJobAndStepListener";
    	
    	try {
	        String expectedStr = "BeforeJob" + 
	                                 "BeforeStep" + "AfterStep" +
	                                 "BeforeStep" + "AfterStep" + 
	                             "AfterJob";
	        
	        Reporter.log("Locate job XML file: oneArtifactIsJobAndStepListener.xml<p>");
	        URL jobXMLURL = this.getClass().getResource("/oneArtifactIsJobAndStepListener.xml");

	
	        Reporter.log("Create job parameters for execution #1:<p>");
	        Properties jobParams = new Properties();
	        Reporter.log("app.timeinterval=10<p>");
	        jobParams.put("app.timeinterval", "10");
	        
	        Reporter.log("Invoke startJobAndWaitForResult for execution #1<p>");
	        JobExecution execution1 = jobOp.startJobAndWaitForResult("oneArtifactIsJobAndStepListener", jobParams);
	        
	        Reporter.log("EXPECTED JobExecution getBatchStatus()=COMPLETED<p>");
	        Reporter.log("ACTUAL JobExecution getBatchStatus()="+execution1.getBatchStatus()+"<p>");
	        Reporter.log("EXPECTED JobExecution getExitStatus()="+expectedStr+"<p>");
	        Reporter.log("ACTUAL JobExecution getExitStatus()="+execution1.getExitStatus()+"<p>");
	        assertWithMessage("Testing batch status", BatchStatus.COMPLETED, execution1.getBatchStatus());
	        assertWithMessage("Testing exit status", expectedStr, execution1.getExitStatus());
    	} catch (Exception e) {
    		handleException(METHOD, e);
    	}
    }

    @AfterClass
    public static void cleanup() throws Exception {
        jobOp.destroy();
    }
    
    private static void handleException(String methodName, Exception e) throws Exception {
  		Reporter.log("Caught exception: " + e.getMessage()+"<p>");
  		Reporter.log(methodName + " failed<p>");
  		throw e;
  	}
}
