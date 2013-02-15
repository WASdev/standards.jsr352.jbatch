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

import static jsr352.tck.utils.AssertionUtils.assertObjEquals;

import java.net.URL;
import java.util.Properties;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator.BatchStatus;
import javax.batch.runtime.JobExecution;

import jsr352.tck.utils.JobOperatorBridge;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Ignore("Ignore EE-only tests from JUnit.")
public class TransactionTests {
    
    private final static Logger logger = Logger.getLogger(TransactionTests.class.getName());

    private static JobOperatorBridge jobOp;

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

    @AfterClass
    public static void cleanup() throws Exception {
    }

    private void begin(String str) {
        Reporter.log("Begin test method: " + str + "<p>");
    }
    

    /*
	 * @testName: testTranRollbackRetryReadSkipRead
	 * 
	 * @assertion: Test will finish in COMPLETED status, with the onRetryReadException and onSkipItem listener invoked.
	 * 
	 * @test_Strategy:
	 * Test that the onRetryReadException listener is invoked when a retryable exception occurs on a read.
	 * The transaction will rollback and the chunk will be retried with an item-count of 1 (one)
	 * Test that the item is skipped, and onSkipReadItem listener is invoked, when the same exception occurs on the retry
	 *
	 */
    @Test
    @org.junit.Test
    public void testTranRollbackRetryReadSkipRead() throws Exception {
    	String METHOD = "testTranRollbackRetryReadSkipRead";
        begin(METHOD);
        
        try {
        
	        Integer initNumbers = 10;
	        Integer forcedFailCountRead = 8;
	        Integer forcedFailCountProcess = 0;
	        Integer forcedFailCountWrite = 0;
	        Integer dummyDelay = 0;
	        Boolean rollback = true;
	        Boolean autoCommit = false;
	        
	        Properties jobParams = new Properties();
	    	
	        jobParams.put("javax.transaction.global.mode", "true");
	        jobParams.put("javax.transaction.global.timeout", "20");
	        jobParams.put("init.numbers.quantity", initNumbers.toString());
	        jobParams.put("forced.fail.count.read", forcedFailCountRead.toString());
	        jobParams.put("forced.fail.count.write", forcedFailCountWrite.toString());
	        jobParams.put("forced.fail.count.process", forcedFailCountProcess.toString());
	        jobParams.put("dummy.delay.seconds", dummyDelay.toString());
	        jobParams.put("rollback", rollback.toString());
	        jobParams.put("auto.commit", autoCommit.toString());
	
	        Reporter.log("Locate job XML file: job_chunk_retryskip_rollback.xml<p>");
	        URL jobXMLURL = TransactionTests.class.getResource("/job_chunk_retryskip_rollback.xml");
	
	
	        Reporter.log("Invoke startJobAndWaitForResult<p>");
	        JobExecution jobExec = jobOp.startJobAndWaitForResult("job_chunk_retryskip_rollback",jobParams);
	        assertObjEquals(BatchStatus.COMPLETED, jobExec.getBatchStatus());
        } catch (Exception e) {
    		handleException(METHOD, e);
    	}
    }
    
    /*
   	 * @testName: testTranRollbackRetryProcessSkipProcess
   	 * 
   	 * @assertion: Test will finish in COMPLETED status, with the onRetryProcessException and onSkipProcess listener invoked.
   	 * 
   	 * @test_Strategy:
   	 * Test that the onRetryProcessException listener is invoked when a retryable exception occurs on a process.
   	 * The transaction will rollback and the chunk will be retried with an item-count of 1 (one)
   	 * Test that the item is skipped, and onSkipProcessItem listener is invoked, when the same exception occurs on the retry
   	 *
   	 */
    @Test
    @org.junit.Test
       public void testTranRollbackRetryProcessSkipProcess() throws Exception {
       	String METHOD = "testTranRollbackRetryProcessSkipProcess";
           begin(METHOD);
           
           try {
	           Integer initNumbers = 10;
	           Integer forcedFailCountRead = 0;
	           Integer forcedFailCountProcess = 8;
	           Integer forcedFailCountWrite = 0;
	           Integer dummyDelay = 0;
	           Boolean rollback = true;
	           Boolean autoCommit = false;
	           
	           Properties jobParams = new Properties();
	       	
	           jobParams.put("javax.transaction.global.mode", "true");
	           jobParams.put("javax.transaction.global.timeout", "20");
	           jobParams.put("init.numbers.quantity", initNumbers.toString());
	           jobParams.put("forced.fail.count.read", forcedFailCountRead.toString());
	           jobParams.put("forced.fail.count.write", forcedFailCountWrite.toString());
	           jobParams.put("forced.fail.count.process", forcedFailCountProcess.toString());
	           jobParams.put("dummy.delay.seconds", dummyDelay.toString());
	           jobParams.put("rollback", rollback.toString());
	           jobParams.put("auto.commit", autoCommit.toString());
	
	           Reporter.log("Locate job XML file: job_chunk_retryskip_rollback.xml<p>");
	           URL jobXMLURL = TransactionTests.class.getResource("/job_chunk_retryskip_rollback.xml");
	
	
	           Reporter.log("Invoke startJobAndWaitForResult<p>");
	           JobExecution jobExec = jobOp.startJobAndWaitForResult("job_chunk_retryskip_rollback",jobParams);
	           assertObjEquals(BatchStatus.COMPLETED, jobExec.getBatchStatus());
           } catch (Exception e) {
       		handleException(METHOD, e);
       	   }
       }
       
       /*
      	 * @testName: testTranRollbackRetryWriteSkipWrite
      	 * 
      	 * @assertion: Test will finish in COMPLETED status, with the onRetryWriteException and onSkipWriteItem listener invoked.
      	 * 
      	 * @test_Strategy:
      	 * Test that the onRetryWriteException listener is invoked when a retryable exception occurs on a write.
      	 * The transaction will rollback and the chunk will be retried with an item-count of 1 (one)
      	 * Test that the item is skipped, and onSkipWriteItem listener is invoked, when the same exception occurs on the retry
      	 *
      	 */
          @Test
          @org.junit.Test
          public void testTranRollbackRetryWriteSkipWrite() throws Exception {
          	String METHOD = "testTranRollbackRetryWriteSkipWrite";
              begin(METHOD);
              
              try {
	              Integer initNumbers = 10;
	              Integer forcedFailCountRead = 0;
	              Integer forcedFailCountProcess = 0;
	              Integer forcedFailCountWrite = 8;
	              Integer dummyDelay = 0;
	              Boolean rollback = true;
	              Boolean autoCommit = false;
	              
	              Properties jobParams = new Properties();
	          	
	              jobParams.put("javax.transaction.global.mode", "true");
	              jobParams.put("javax.transaction.global.timeout", "20");
	              jobParams.put("init.numbers.quantity", initNumbers.toString());
	              jobParams.put("forced.fail.count.read", forcedFailCountRead.toString());
	              jobParams.put("forced.fail.count.write", forcedFailCountWrite.toString());
	              jobParams.put("forced.fail.count.process", forcedFailCountProcess.toString());
	              jobParams.put("dummy.delay.seconds", dummyDelay.toString());
	              jobParams.put("rollback", rollback.toString());
	              jobParams.put("auto.commit", autoCommit.toString());
	
	              Reporter.log("Locate job XML file: job_chunk_retryskip_rollback.xml<p>");
	              URL jobXMLURL = TransactionTests.class.getResource("/job_chunk_retryskip_rollback.xml");
	
	
	              Reporter.log("Invoke startJobAndWaitForResult<p>");
	              JobExecution jobExec = jobOp.startJobAndWaitForResult("job_chunk_retryskip_rollback",jobParams);
	              assertObjEquals(BatchStatus.COMPLETED, jobExec.getBatchStatus());
              } catch (Exception e) {
          		handleException(METHOD, e);
          	}
          }
    
    /*
	 * @testName: testGlobalTranNoExceptions
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
    @Test
    @org.junit.Test  
    public void testGlobalTranNoExceptions() throws Exception {
    	String METHOD = "testGlobalTranNoExceptions";
        begin(METHOD);
    	
        try {
	        Integer initInventory = 99;
	        Integer forcedFailCount = 0;
	        Integer itemCount = 5;
	        Integer dummyDelay = 0;

	
	        Integer expectedInventory = this.calculateGlobalTranExpectedInventory(initInventory, forcedFailCount, itemCount);
	        Integer expectedCompletedOrders = this.calculateExpectedCompleteOrders(initInventory, forcedFailCount, itemCount);
	        
	    	Properties jobParams = new Properties();
	    	Reporter.log("Create job parameters for execution #1:<p>");
	    	Reporter.log("javax.transaction.global.timeout=300<p>");
	    	Reporter.log("commit.interval="+itemCount.toString()+"<p>");
	    	Reporter.log("init.inventory.quantity="+initInventory.toString()+"<p>");
	    	Reporter.log("forced.fail.count="+forcedFailCount.toString()+"<p>");
	    	Reporter.log("dummy.delay.seconds="+dummyDelay.toString()+"<p>");
	    	Reporter.log("expected.inventory="+expectedInventory.toString()+"<p>");
	        jobParams.put("javax.transaction.global.timeout", "300");
	        jobParams.put("commit.interval", itemCount.toString());
	        jobParams.put("init.inventory.quantity", initInventory.toString());
	        jobParams.put("forced.fail.count", forcedFailCount.toString());
	        jobParams.put("dummy.delay.seconds", dummyDelay.toString());
	        jobParams.put("expected.inventory", expectedInventory.toString());
	        
	        
	        Reporter.log("Invoke startJobAndWaitForResult<p>");
	        JobExecution jobExec = jobOp.startJobAndWaitForResult("job_chunk_globaltran",jobParams);
	
	        Reporter.log("execution #1 JobExecution getBatchStatus()="+jobExec.getBatchStatus()+"<p>");
	        Reporter.log("execution #1 JobExecution getExitStatus()="+jobExec.getExitStatus()+"<p>");
	        assertObjEquals("Inventory=" +expectedInventory + " InitialCheckpoint=" + null +" OrderCount="+ expectedCompletedOrders , jobExec.getExitStatus());
	        assertObjEquals(BatchStatus.COMPLETED, jobExec.getBatchStatus());
        } catch (Exception e) {
    		handleException(METHOD, e);
    	}
        
    }

   
    @Test
    @org.junit.Test
    @Ignore
    public void testGlobalTranForcedExceptionWithRollback() throws Exception {
    	String METHOD = "testGlobalTranForcedExceptionWithRollback";
        begin(METHOD);
    	
        try {
	        Integer initInventory = 99;
	        Integer forcedFailCount = 20;
	        Integer itemCount = 9;
	        Integer dummyDelay = 0;
	
	        Integer expectedInventory = TransactionTests.calculateGlobalTranExpectedInventory(initInventory, forcedFailCount, itemCount);
	        Integer expectedCompletedOrders = this.calculateExpectedCompleteOrders(initInventory, forcedFailCount, itemCount);
	        
	    	Properties jobParams = new Properties();
	    	
            Reporter.log("Create job parameters for execution #1:<p>");
            Reporter.log("javax.transaction.global.timeout=300<p>");
            Reporter.log("commit.interval="+itemCount.toString()+"<p>");
            Reporter.log("init.inventory.quantity="+initInventory.toString()+"<p>");
            Reporter.log("forced.fail.count="+forcedFailCount.toString()+"<p>");
            Reporter.log("dummy.delay.seconds="+dummyDelay.toString()+"<p>");
            Reporter.log("expected.inventory="+expectedInventory.toString()+"<p>");
            jobParams.put("javax.transaction.global.timeout", "300");
            jobParams.put("commit.interval", itemCount.toString());
            jobParams.put("init.inventory.quantity", initInventory.toString());
            jobParams.put("forced.fail.count", forcedFailCount.toString());
            jobParams.put("dummy.delay.seconds", dummyDelay.toString());
            jobParams.put("expected.inventory", expectedInventory.toString());
	    	
	        Reporter.log("Invoke startJobAndWaitForResult<p>");
	        JobExecution jobExec = jobOp.startJobAndWaitForResult("job_chunk_globaltran",jobParams);
	
	        Reporter.log("execution #1 JobExecution getBatchStatus()="+jobExec.getBatchStatus()+"<p>");
	        Reporter.log("execution #1 JobExecution getExitStatus()="+jobExec.getExitStatus()+"<p>");
	        assertObjEquals("Inventory=" +expectedInventory + " InitialCheckpoint=" + null +" OrderCount="+ expectedCompletedOrders , jobExec.getExitStatus());
	        assertObjEquals(BatchStatus.FAILED, jobExec.getBatchStatus());
        } catch (Exception e) {
    		handleException(METHOD, e);
    	}
        
    }
    
    /*
   	 * @testName: testGlobalTranForcedExceptionCheckpointRestart
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    @org.junit.Test  
    public void testGlobalTranForcedExceptionCheckpointRestart() throws Exception {
    	String METHOD = "testGlobalTranForcedExceptionCheckpointRestart";
        begin(METHOD);
    	
        try {
	        Integer initInventory = 99;
	        Integer forcedFailCount = 20;
	        Integer itemCount = 9;
	        Integer dummyDelay = 0;
	
	        Integer expectedInventory = TransactionTests.calculateGlobalTranExpectedInventory(initInventory, forcedFailCount, itemCount);
	        Integer expectedCompletedOrders = TransactionTests.calculateExpectedCompleteOrders(initInventory, forcedFailCount, itemCount);
	        
	    	Properties jobParams = new Properties();
	    	
            Reporter.log("Create job parameters for execution #1:<p>");
            Reporter.log("javax.transaction.global.timeout=300<p>");
            Reporter.log("commit.interval="+itemCount.toString()+"<p>");
            Reporter.log("init.inventory.quantity="+initInventory.toString()+"<p>");
            Reporter.log("forced.fail.count="+forcedFailCount.toString()+"<p>");
            Reporter.log("dummy.delay.seconds="+dummyDelay.toString()+"<p>");
            Reporter.log("expected.inventory="+expectedInventory.toString()+"<p>");
            jobParams.put("javax.transaction.global.timeout", "300");
            jobParams.put("commit.interval", itemCount.toString());
            jobParams.put("init.inventory.quantity", initInventory.toString());
            jobParams.put("forced.fail.count", forcedFailCount.toString());
            jobParams.put("dummy.delay.seconds", dummyDelay.toString());
            jobParams.put("expected.inventory", expectedInventory.toString());
	    	
	
	        Reporter.log("Invoke startJobAndWaitForResult<p>");
	        JobExecution jobExec = jobOp.startJobAndWaitForResult("job_chunk_globaltran",jobParams);
	        long jobInstanceId = jobExec.getInstanceId();
	        
	        Reporter.log("execution #1 JobExecution getBatchStatus()="+jobExec.getBatchStatus()+"<p>");
	        Reporter.log("execution #1 JobExecution getExitStatus()="+jobExec.getExitStatus()+"<p>");
	        assertObjEquals(BatchStatus.FAILED, jobExec.getBatchStatus());
	        assertObjEquals("Inventory=" +expectedInventory + " InitialCheckpoint=" + null +" OrderCount="+ expectedCompletedOrders , jobExec.getExitStatus());
	        
	        forcedFailCount = 0;
	        jobParams.put("forced.fail.count", forcedFailCount.toString());
	        
	        expectedInventory = TransactionTests.calculateGlobalTranExpectedInventory(expectedInventory, forcedFailCount, itemCount);
	        Integer expectedCompletedOrders2 = TransactionTests.calculateExpectedCompleteOrders(initInventory, forcedFailCount, itemCount);
	        
	        Reporter.log("Invoke restartJobAndWaitForResult with id: " + jobInstanceId + "<p>");
	        JobExecution restartedJobExec = jobOp.restartJobAndWaitForResult(jobExec.getExecutionId(), jobParams);
	        
	        Reporter.log("restarted job JobExecution getBatchStatus()="+restartedJobExec.getBatchStatus()+"<p>");
	        Reporter.log("restarted job JobExecution getExitStatus()="+restartedJobExec.getExitStatus()+"<p>");
	        assertObjEquals("Inventory=" +expectedInventory + " InitialCheckpoint=" + expectedCompletedOrders+" OrderCount="+ expectedCompletedOrders2, restartedJobExec.getExitStatus());
	        assertObjEquals(BatchStatus.COMPLETED, restartedJobExec.getBatchStatus());
        } catch (Exception e) {
    		handleException(METHOD, e);
    	}
        
    }
    
    

    /*
   	 * @testName: testGlobalTranForcedTimeoutCheckpointRestart
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */    
    @Test
    @org.junit.Test  
    public void testGlobalTranForcedTimeoutCheckpointRestart() throws Exception {
    	String METHOD = "testGlobalTranForcedTimeoutCheckpointRestart";
        begin(METHOD);
        
        try {
    	
	        Integer initInventory = 99;
	        Integer forcedFailCount = 15;
	        Integer itemCount = 9;
	        Integer dummyDelay = 2000; //delay in milliseconds
	        Integer globalTimeout = 1; //seconds

	        Integer expectedInventory = TransactionTests.calculateGlobalTranExpectedInventory(initInventory, forcedFailCount, itemCount);
	        Integer expectedCompletedOrders = TransactionTests.calculateExpectedCompleteOrders(initInventory, forcedFailCount, itemCount);
	        
	    	Properties jobParams = new Properties();
	    	
            Reporter.log("Create job parameters for execution #1:<p>");
            Reporter.log("javax.transaction.global.timeout="+globalTimeout.toString()+"<p>");
            Reporter.log("commit.interval="+itemCount.toString()+"<p>");
            Reporter.log("init.inventory.quantity="+initInventory.toString()+"<p>");
            Reporter.log("forced.fail.count="+forcedFailCount.toString()+"<p>");
            Reporter.log("dummy.delay.seconds="+dummyDelay.toString()+"<p>");
            Reporter.log("expected.inventory="+expectedInventory.toString()+"<p>");
            jobParams.put("javax.transaction.global.timeout", globalTimeout.toString()); //seconds
            jobParams.put("commit.interval", itemCount.toString());
            jobParams.put("init.inventory.quantity", initInventory.toString());
            jobParams.put("forced.fail.count", forcedFailCount.toString());
            jobParams.put("dummy.delay.seconds", dummyDelay.toString());
            jobParams.put("expected.inventory", expectedInventory.toString());
	    	
	        Reporter.log("Invoke startJobAndWaitForResult<p>");
	        JobExecution jobExec = jobOp.startJobAndWaitForResult("job_chunk_globaltran",jobParams);
	        long jobInstanceId = jobExec.getInstanceId();
	        
	        Reporter.log("execution #1 JobExecution getBatchStatus()="+jobExec.getBatchStatus()+"<p>");
	        Reporter.log("execution #1 JobExecution getExitStatus()="+jobExec.getExitStatus()+"<p>");
	        assertObjEquals("Inventory=" +expectedInventory + " InitialCheckpoint=" + null +" OrderCount="+ expectedCompletedOrders , jobExec.getExitStatus());
	        assertObjEquals(BatchStatus.FAILED, jobExec.getBatchStatus());
	        
	        
	        forcedFailCount = 0;
	        dummyDelay = 0;
	        jobParams.put("forced.fail.count", forcedFailCount.toString());
	        jobParams.put("dummy.delay.seconds", dummyDelay.toString());
	        
	        expectedInventory = TransactionTests.calculateGlobalTranExpectedInventory(expectedInventory, forcedFailCount, itemCount);
	        Integer expectedCompletedOrders2 = TransactionTests.calculateExpectedCompleteOrders(initInventory, forcedFailCount, itemCount);
	        
	        Reporter.log("Invoke restartJobAndWaitForResult with id: " + jobInstanceId + "<p>");
	        JobExecution restartedJobExec = jobOp.restartJobAndWaitForResult(jobInstanceId, jobParams);
	        
	        Reporter.log("restarted job JobExecution getBatchStatus()="+restartedJobExec.getBatchStatus()+"<p>");
	        Reporter.log("restarted job JobExecution getExitStatus()="+restartedJobExec.getExitStatus()+"<p>");
	        assertObjEquals("Inventory=" +expectedInventory + " InitialCheckpoint=" + expectedCompletedOrders+" OrderCount="+ expectedCompletedOrders2, restartedJobExec.getExitStatus());
	        assertObjEquals(BatchStatus.COMPLETED, restartedJobExec.getBatchStatus());
        } catch (Exception e) {
    		handleException(METHOD, e);
    	}
        
    } 

   
    /**
     * Calculates how many items should be left in the inventory table after all commits and rollbacks.
     */
	private static int calculateGlobalTranExpectedInventory(int initInventory, int forcedFailCount, int commitInterval) {
		int expectedResult;
		
		if (forcedFailCount <= 0 ){
			expectedResult =  0;
		} else {
			expectedResult = (initInventory - forcedFailCount ) + (forcedFailCount % commitInterval);
		}
		
		return expectedResult;
	}
    
	
	/**
	 * Calculates how many orders should  in the orders table after all commits and rollbacks.
	 */
	private static int calculateExpectedCompleteOrders(int initInventory, int forcedFailCount, int commitInterval) {
		int expectedResult;
		
		if (forcedFailCount <= 0 ){
			expectedResult =  initInventory;
		} else {
			expectedResult = (forcedFailCount / commitInterval ) * commitInterval; //integer arithmetic, so we drop the remainder
		}
		
		return expectedResult;
	}
	
	private static void handleException(String methodName, Exception e) throws Exception {
		Reporter.log("Caught exception: " + e.getMessage()+"<p>");
		Reporter.log(methodName + " failed<p>");
		throw e;
	}
    
}
