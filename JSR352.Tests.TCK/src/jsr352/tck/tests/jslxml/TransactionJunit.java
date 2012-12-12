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
import java.util.Properties;
import java.util.logging.Logger;

import javax.batch.runtime.JobExecution;

import jsr352.tck.utils.IOHelper;
import jsr352.tck.utils.JobOperatorBridge;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;


@Ignore("Only run in JEE environment.")
public class TransactionJunit {
    
    private final static Logger logger = Logger.getLogger(TransactionJunit.class.getName());

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
    public void testGlobalTranNoExceptions() throws Exception {
    	String METHOD = "testLocalTransactionModeAutoCommitDefault";
        begin(METHOD);
    	
        Integer initInventory = 99;
        Integer forcedFailCount = 0;
        Integer commitInterval = 2;
        Integer dummyDelay = 0;
        Boolean autoCommit = false;

        Integer expectedInventory = this.calculateGlobalTranExpectedResult(initInventory, forcedFailCount, commitInterval);
        
    	Properties jobParams = new Properties();
    	
        jobParams.put("javax.transaction.global.mode", "true");
        jobParams.put("javax.transaction.global.timeout", "20");
        jobParams.put("commit.interval", commitInterval.toString());
        jobParams.put("init.inventory.quantity", initInventory.toString());
        jobParams.put("forced.fail.count", forcedFailCount.toString());
        jobParams.put("dummy.delay.seconds", dummyDelay.toString());
        jobParams.put("expected.inventory", expectedInventory.toString());
        jobParams.put("auto.commit", autoCommit.toString());
        
        
        URL jobXMLURL = TransactionJunit.class.getResource("/job_chunk_globaltran.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());

        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML,jobParams);

        assertEquals("Inventory=" +expectedInventory + " InitialCheckpoint=" + null, jobExec.getExitStatus());
        assertEquals("COMPLETED", jobExec.getStatus());
        
    }

    @Ignore
    @Test  
    public void testGlobalTranForcedExceptionWithRollback() throws Exception {
    	String METHOD = "testLocalTransactionModeAutoCommitDefault";
        begin(METHOD);
    	
        Integer initInventory = 99;
        Integer forcedFailCount = 20;
        Integer commitInterval = 9;
        Integer dummyDelay = 0;
        Boolean autoCommit = false;

        Integer expectedInventory = TransactionJunit.calculateGlobalTranExpectedResult(initInventory, forcedFailCount, commitInterval);
        
    	Properties jobParams = new Properties();
    	
        jobParams.put("javax.transaction.global.mode", "true");
        jobParams.put("javax.transaction.global.timeout", "10");
        jobParams.put("commit.interval", commitInterval.toString());
        jobParams.put("init.inventory.quantity", initInventory.toString());
        jobParams.put("forced.fail.count", forcedFailCount.toString());
        jobParams.put("dummy.delay.seconds", dummyDelay.toString());
        jobParams.put("expected.inventory", expectedInventory.toString());
        jobParams.put("auto.commit", autoCommit.toString());
    	
        URL jobXMLURL = TransactionJunit.class.getResource("/job_chunk_globaltran.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());

        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML,jobParams);

        assertEquals("Inventory=" +expectedInventory + " InitialCheckpoint=null", jobExec.getExitStatus());
        assertEquals("FAILED", jobExec.getStatus());
        
    }
    
    
    @Test  
    public void testGlobalTranForcedExceptionCheckpointRestart() throws Exception {
    	String METHOD = "testGlobalTranForcedExceptionCheckpointRestart";
        begin(METHOD);
    	
        Integer initInventory = 99;
        Integer forcedFailCount = 20;
        Integer commitInterval = 9;
        Integer dummyDelay = 0;
        Boolean autoCommit = false;

        Integer expectedInventory = TransactionJunit.calculateGlobalTranExpectedResult(initInventory, forcedFailCount, commitInterval);
        Integer expectedInitChkp = TransactionJunit.calculateExpectedCheckpoint(initInventory, forcedFailCount, commitInterval);
        
    	Properties jobParams = new Properties();
    	
        jobParams.put("javax.transaction.global.mode", "true");
        jobParams.put("javax.transaction.global.timeout", "10");
        jobParams.put("commit.interval", commitInterval.toString());
        jobParams.put("init.inventory.quantity", initInventory.toString());
        jobParams.put("forced.fail.count", forcedFailCount.toString());
        jobParams.put("dummy.delay.seconds", dummyDelay.toString());
        jobParams.put("auto.commit", autoCommit.toString());
    	
        URL jobXMLURL = TransactionJunit.class.getResource("/job_chunk_globaltran.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());

        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML,jobParams);
        long jobInstanceId = jobExec.getInstanceId();
        
        assertEquals("FAILED", jobExec.getStatus());
        assertEquals("Inventory=" +expectedInventory + " InitialCheckpoint=" + null, jobExec.getExitStatus());
        
        forcedFailCount = 0;
        jobParams.put("forced.fail.count", forcedFailCount.toString());
        
        expectedInventory = TransactionJunit.calculateGlobalTranExpectedResult(expectedInventory, forcedFailCount, commitInterval);
        
        
        JobExecution restartedJobExec = jobOp.restartJobAndWaitForResult(jobInstanceId, jobParams);
        
        assertEquals("Inventory=" +expectedInventory + " InitialCheckpoint=" + expectedInitChkp, restartedJobExec.getExitStatus());
        assertEquals("COMPLETED", restartedJobExec.getStatus());
        
    }
    
    @Test
    public void testNoTranForcedExceptionCheckpointRestart() throws Exception {
    	String METHOD = "testGlobalTranForcedExceptionCheckpointRestart";
        begin(METHOD);
    	
        Integer initInventory = 99;
        Integer forcedFailCount = 21;
        Integer commitInterval = 9;
        Integer dummyDelay = 0;
        Boolean autoCommit = false;

        Integer expectedInventory = initInventory - forcedFailCount;
        Integer expectedInitChkp = TransactionJunit.calculateExpectedCheckpoint(initInventory, forcedFailCount, commitInterval);
        
    	Properties jobParams = new Properties();
    	
        jobParams.put("javax.transaction.global.mode", "false");
        jobParams.put("javax.transaction.global.timeout", "10");
        jobParams.put("commit.interval", commitInterval.toString());
        jobParams.put("init.inventory.quantity", initInventory.toString());
        jobParams.put("forced.fail.count", forcedFailCount.toString());
        jobParams.put("dummy.delay.seconds", dummyDelay.toString());
        jobParams.put("auto.commit", autoCommit.toString());
    	
        URL jobXMLURL = TransactionJunit.class.getResource("/job_chunk_globaltran.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());

        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML,jobParams);
        long jobInstanceId = jobExec.getInstanceId();
        
        assertEquals("FAILED", jobExec.getStatus());
        assertEquals("Inventory=" +expectedInventory + " InitialCheckpoint=" + null, jobExec.getExitStatus());
        
        forcedFailCount = 0;
        jobParams.put("forced.fail.count", forcedFailCount.toString());
        
        expectedInventory = TransactionJunit.calculateGlobalTranExpectedResult(expectedInventory, forcedFailCount, commitInterval);
        
        
        JobExecution restartedJobExec = jobOp.restartJobAndWaitForResult(jobInstanceId, jobParams);
        
        assertEquals("Inventory=" +expectedInventory + " InitialCheckpoint=" + expectedInitChkp, restartedJobExec.getExitStatus());
        assertEquals("COMPLETED", restartedJobExec.getStatus());
        
    }
    

        
    @Test  
    public void testGlobalTranForcedTimeoutCheckpointRestart() throws Exception {
    	String METHOD = "testGlobalTranForcedTimeoutCheckpointRestart";
        begin(METHOD);
    	
        Integer initInventory = 99;
        Integer forcedFailCount = 15;
        Integer commitInterval = 9;
        Integer dummyDelay = 3000; //delay in milliseconds
        Boolean autoCommit = false;

        Integer expectedInventory = TransactionJunit.calculateGlobalTranExpectedResult(initInventory, forcedFailCount, commitInterval);
        Integer expectedInitChkp = TransactionJunit.calculateExpectedCheckpoint(initInventory, forcedFailCount, commitInterval);
        
    	Properties jobParams = new Properties();
    	
        jobParams.put("javax.transaction.global.mode", "true");
        jobParams.put("javax.transaction.global.timeout", "2");
        jobParams.put("commit.interval", commitInterval.toString());
        jobParams.put("init.inventory.quantity", initInventory.toString());
        jobParams.put("forced.fail.count", forcedFailCount.toString());
        jobParams.put("dummy.delay.seconds", dummyDelay.toString());
        jobParams.put("auto.commit", autoCommit.toString());
    	
        URL jobXMLURL = TransactionJunit.class.getResource("/job_chunk_globaltran.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());

        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML,jobParams);
        long jobInstanceId = jobExec.getInstanceId();
        
        assertEquals("Inventory=" +expectedInventory + " InitialCheckpoint=" + null, jobExec.getExitStatus());
        assertEquals("FAILED", jobExec.getStatus());
        
        
        forcedFailCount = 0;
        dummyDelay = 0;
        jobParams.put("forced.fail.count", forcedFailCount.toString());
        jobParams.put("dummy.delay.seconds", dummyDelay.toString());
        
        expectedInventory = TransactionJunit.calculateGlobalTranExpectedResult(expectedInventory, forcedFailCount, commitInterval);
        
        
        JobExecution restartedJobExec = jobOp.restartJobAndWaitForResult(jobInstanceId, jobParams);
        
        assertEquals("Inventory=" +expectedInventory + " InitialCheckpoint=" + expectedInitChkp, restartedJobExec.getExitStatus());
        assertEquals("COMPLETED", restartedJobExec.getStatus());
        
    } 

    @Test  
    public void testLocalTransactionModeAutoCommitDefault() throws Exception {
    	String METHOD = "testLocalTransactionModeAutoCommitDefault";
        begin(METHOD);
    	
        Integer initInventory = 99;
        Integer forcedFailCount = 30;
        Integer commitInterval = 9;
        Integer dummyDelay = 0;
        Boolean autoCommit = true;        

        Integer expectedInventory = 69;
        Integer expectedInitChkp = TransactionJunit.calculateExpectedCheckpoint(initInventory, forcedFailCount, commitInterval);
        
    	Properties jobParams = new Properties();
    	
        jobParams.put("javax.transaction.global.mode", "false");
        jobParams.put("javax.transaction.global.timeout", "5");
        jobParams.put("commit.interval", commitInterval.toString());
        jobParams.put("init.inventory.quantity", initInventory.toString());
        jobParams.put("forced.fail.count", forcedFailCount.toString());
        jobParams.put("dummy.delay.seconds", dummyDelay.toString());
        jobParams.put("auto.commit", autoCommit.toString());
    	
        URL jobXMLURL = TransactionJunit.class.getResource("/job_chunk_globaltran.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());

        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML,jobParams);
        long jobInstanceId = jobExec.getInstanceId();
        
        assertEquals(new String ("COMPLETED"), jobExec.getStatus());
        assertEquals("Inventory=" +expectedInventory + " InitialCheckpoint=" + null, jobExec.getExitStatus());
        assertEquals("FAILED", jobExec.getStatus());
        
        
        forcedFailCount = 0;
        jobParams.put("forced.fail.count", forcedFailCount.toString());
        
        expectedInventory = 0;
                
        JobExecution restartedJobExec = jobOp.restartJobAndWaitForResult(jobInstanceId, jobParams);
        
        assertEquals("Inventory=" +expectedInventory + " InitialCheckpoint=" + expectedInitChkp, restartedJobExec.getExitStatus());
        assertEquals("COMPLETED", restartedJobExec.getStatus());
    }

    /** still working on tests below **/
    
    @Ignore
    @Test  
    public void testLocalTransactionModeNoAutoCommitDefault() throws Exception {
        String METHOD = "testLocalTransactionModeNoAutoCommitDefault";
        begin(METHOD);
        URL jobXMLURL = TransactionJunit.class.getResource("/job_batchlet_1step.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());

        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);

        assertEquals(new String ("COMPLETED"), jobExec.getStatus());
    }
    
    @Ignore
    @Test  
    public void testGlobalTransactionModeWithRestart() throws Exception {
        String METHOD = "testLocalTransactionModeAutoCommitDefault";
        begin(METHOD);
        URL jobXMLURL = TransactionJunit.class.getResource("/job_batchlet_1step.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());

        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);

        assertEquals(new String ("COMPLETED"), jobExec.getStatus());
    }
    
	private static int calculateGlobalTranExpectedResult(int initInventory, int forcedFailCount, int commitInterval) {
		int expectedResult;
		
		if (forcedFailCount <= 0 ){
			expectedResult =  0;
		} else {
			expectedResult = (initInventory - forcedFailCount ) + (forcedFailCount % commitInterval);
		}
		
		return expectedResult;
	}
    
	private static int calculateExpectedCheckpoint(int initInventory, int forcedFailCount, int commitInterval) {
		int expectedResult;
		
		if (forcedFailCount <= 0 ){
			expectedResult =  initInventory;
		} else {
			expectedResult = (forcedFailCount / commitInterval ) * commitInterval;
		}
		
		return expectedResult;
	}
    
}
