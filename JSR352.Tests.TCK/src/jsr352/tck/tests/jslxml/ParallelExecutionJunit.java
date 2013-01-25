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

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;


import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.batch.tck.spi.JobEndCallback;
import com.ibm.batch.tck.spi.JobEndCallbackManager;
import jsr352.tck.utils.IOHelper;
import jsr352.tck.utils.ServiceGateway;

public class ParallelExecutionJunit {
    
    private final static Logger logger = Logger.getLogger(ParallelExecutionJunit.class.getName());
    
    private static final int WAIT_TIME = 15000; 

    private static JobOperator jobOp = BatchRuntime.getJobOperator();
    private JobEndCallbackManager callbackMgr = ServiceGateway.getServices().getCallbackManager();

    class ParallelJobEndCallbackImpl implements JobEndCallback{
    	
    	Object waitObj = null;
    	
    	ParallelJobEndCallbackImpl(Object waitObj) {
    		this.waitObj = waitObj;
    	}
    	

        @Override
        public void done(long jobExecutionId) {
            if (waitObj != null) {
                synchronized (waitObj) {
                    waitObj.notify();
                }
            }
        }
    }
    

   
    public static void setup(String[] args, Properties props) throws Exception {
 
    }
    
    @BeforeClass
    public static void setUp() throws Exception {
    	 
    }

    @AfterClass
    public static void cleanup() throws Exception {
    }


    private void begin(String str) {
        logger.fine("Begin test method: " + str);
    }
    
    /*
   	 * @testName: testInvokeJobWithOnePartitionedStep
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testInvokeJobWithOnePartitionedStep() throws Exception {
        String METHOD = "testInvokeJobWithOnePartitionedStep";
        begin(METHOD);
        URL jobXMLURL = this.getClass().getResource("/job_partitioned_1step.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());

        Object waitObj = new Object();
        JobEndCallback callback = new ParallelJobEndCallbackImpl(waitObj);
        callbackMgr.registerJobEndCallback(callback);
        Long executionId = jobOp.start(jobXML, null);
        synchronized (waitObj) {
        	waitObj.wait(WAIT_TIME);
		}

        assert("COMPLETED" == jobOp.getJobExecution(executionId).getStatus());
        callbackMgr.deregisterJobEndCallback(callback);
    }

    
 
    
    /*
   	 * @testName: testStopRunningPartitionedStep
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testStopRunningPartitionedStep() throws Exception {
        String METHOD = "testStopRunningPartitionedStep";
        begin(METHOD);
        URL jobXMLURL = this.getClass().getResource("/job_batchlet_longrunning_partitioned.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());

        Object waitObj = new Object();
        
        Properties overrideJobParams = new Properties();
        overrideJobParams.setProperty("run.indefinitely" , "true");
        
        JobEndCallback callback = new ParallelJobEndCallbackImpl(waitObj);
        callbackMgr.registerJobEndCallback(callback);
        Long executionId = jobOp.start(jobXML, overrideJobParams);
        Long jobInstanceId = jobOp.getJobExecution(executionId).getInstanceId();
        //Sleep long enough for parallel steps to fan out
        Thread.sleep(1800);
        
        jobOp.stop(jobInstanceId);
        
        synchronized (waitObj) {
        	waitObj.wait(WAIT_TIME);
		}

        assert("STOPPED" == jobOp.getJobExecution(executionId).getStatus());
        callbackMgr.deregisterJobEndCallback(callback);
    }

    /*
   	 * @testName: testInvokeJobSimpleSplit
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testInvokeJobSimpleSplit() throws Exception {
        String METHOD = "testInvokeJobSimpleSplit";
        begin(METHOD);
    	URL jobXMLURL = this.getClass().getResource("/job_split_batchlet_4steps.xml");
    	String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());

        Object waitObj = new Object();
        JobEndCallback callback = new ParallelJobEndCallbackImpl(waitObj);
        callbackMgr.registerJobEndCallback(callback);
        Long executionId = jobOp.start(jobXML, null);
        synchronized (waitObj) {
        	
        	waitObj.wait(WAIT_TIME);
		}

        assert("COMPLETED" == jobOp.getJobExecution(executionId).getStatus());
        callbackMgr.deregisterJobEndCallback(callback);
    }
    
    /*
   	 * @testName: testPartitionedPlanCollectorAnalyzer
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testPartitionedPlanCollectorAnalyzer() throws Exception {
        String METHOD = "testPartitionedPlanCollectorAnalyzer";
        begin(METHOD);
        URL jobXMLURL = this.getClass().getResource("/job_partitioned_artifacts.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());

        Object waitObj = new Object();
        
        Properties overrideJobParams = new Properties();
        
        //append "CA" to expected exit status for each partition
        overrideJobParams.setProperty("numPartitionsProp" , "3"); 
        
        JobEndCallback callback = new ParallelJobEndCallbackImpl(waitObj);
        callbackMgr.registerJobEndCallback(callback);
        Long executionId = jobOp.start(jobXML, overrideJobParams);
        synchronized (waitObj) {
        	
        	waitObj.wait(WAIT_TIME);
		}

        assert("COMPLETED" == jobOp.getJobExecution(executionId).getStatus());
        
        assert("CACACA" == jobOp.getJobExecution(executionId).getExitStatus());
        callbackMgr.deregisterJobEndCallback(callback);
    }
    
    
}
