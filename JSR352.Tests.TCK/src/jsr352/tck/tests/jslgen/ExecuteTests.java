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

import java.util.Properties;
import java.util.logging.Logger;

import javax.batch.runtime.JobExecution;

import jsr352.tck.specialized.BatchletUsingStepContextImpl;

import org.junit.BeforeClass;
import org.junit.Test;

import jsr352.tck.utils.JSLBuilder;
import jsr352.tck.utils.JobOperatorBridge;

public class ExecuteTests {
	
    private final static Logger logger = Logger.getLogger(ExecuteTests.class.getName());
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
	 * @testName: testMyStepContextBatchlet
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
    @Test
    public void testMyStepContextBatchlet() throws Exception { 
    	JSLBuilder builder = new JSLBuilder();
    	builder.addBatchletStep("step1", "BatchletUsingStepContextImpl");        

        JobExecution jobExec = jobOp.startJobAndWaitForResult(builder.getJSL()); 

        assert(BatchletUsingStepContextImpl.GOOD_JOB_EXIT_STATUS == jobExec.getExitStatus());
        assert("COMPLETED" == jobExec.getStatus());

    }
    
}
