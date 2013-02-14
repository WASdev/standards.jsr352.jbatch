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

import static jsr352.tck.utils.AssertionUtils.assertObjEquals;

import java.util.Properties;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator.BatchStatus;
import javax.batch.runtime.JobExecution;

import jsr352.tck.specialized.BatchletUsingStepContextImpl;
import jsr352.tck.utils.JobOperatorBridge;

import org.junit.BeforeClass;
import org.testng.Reporter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ExecuteTests {
	
    private final static Logger logger = Logger.getLogger(ExecuteTests.class.getName());
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
    
    /* cleanup */
	public void  cleanup()
	{		
	
	}
	
	 private static void handleException(String methodName, Exception e) throws Exception {
			Reporter.log("Caught exception: " + e.getMessage()+"<p>");
			Reporter.log(methodName + " failed<p>");
			throw e;
		}

    /*
	 * @testName: testMyStepContextBatchlet
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
    @Test
    @org.junit.Test
    public void testMyStepContextBatchlet() throws Exception { 
    	
    	String METHOD = "testMyStepContextBatchlet";
    	
    	try {
	
	    	Reporter.log("Invoke startJobAndWaitForResult<p>");
	    	
	        JobExecution jobExec = jobOp.startJobAndWaitForResult("test_batchlet_stepCtx"); 
	
	        Reporter.log("EXPECTED JobExecution getExitStatus()="+BatchletUsingStepContextImpl.GOOD_JOB_EXIT_STATUS+"<p>");
	        Reporter.log("ACTUAL JobExecution getExitStatus()="+jobExec.getExitStatus()+"<p>");
	        Reporter.log("EXPECTED JobExecution getBatchStatus()=COMPLETED<p>");
	        Reporter.log("ACTUAL JobExecution getBatchStatus()="+jobExec.getBatchStatus()+"<p>");
	        assertObjEquals(BatchletUsingStepContextImpl.GOOD_JOB_EXIT_STATUS, jobExec.getExitStatus());
	        assertObjEquals(BatchStatus.COMPLETED, jobExec.getBatchStatus());
    	} catch (Exception e) {
    		handleException(METHOD, e);
    	}

    }
    
}
