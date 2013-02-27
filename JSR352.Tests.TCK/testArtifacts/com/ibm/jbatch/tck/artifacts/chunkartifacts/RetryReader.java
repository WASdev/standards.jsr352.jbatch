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
package com.ibm.jbatch.tck.artifacts.chunkartifacts;

import java.io.Externalizable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import javax.batch.annotation.BatchProperty;
import javax.batch.api.AbstractItemReader;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.testng.Reporter;

import com.ibm.jbatch.tck.artifacts.chunktypes.NumbersCheckpointData;
import com.ibm.jbatch.tck.artifacts.chunktypes.NumbersRecord;
import com.ibm.jbatch.tck.artifacts.reusable.MyParentException;


@javax.inject.Named("retryReader")
public class RetryReader extends AbstractItemReader<NumbersRecord> {

	private static final String CLASSNAME = NumbersReader.class.getName();
	private final static Logger logger = Logger.getLogger(CLASSNAME);
	

	protected DataSource dataSource = null;
	
	private static final int STATE_NORMAL = 0;
	private static final int STATE_RETRY = 1;
	private static final int STATE_SKIP = 2;
	private static final int STATE_EXCEPTION = 3;
	
	private int testState = STATE_NORMAL;
	
    @Inject
    StepContext<?,?> stepCtx;
    
    @Inject    
    @BatchProperty(name="forced.fail.count.read")
	String forcedFailCountProp;
    
    @Inject
    @BatchProperty(name="rollback")
	String rollbackProp;
    

	int forcedFailCount, expectedReaderChkp = -1;
	boolean rollback;
	boolean didRetry;

	int readerIndex = 1;
	
	int failindex = 0;
	
	NumbersCheckpointData numbersCheckpoint = new NumbersCheckpointData();
	
	public void open(Externalizable cpd) throws NamingException {
	    NumbersCheckpointData numbersCheckpointData = (NumbersCheckpointData)cpd;
	    
		forcedFailCount = Integer.parseInt(forcedFailCountProp);
		rollback = Boolean.parseBoolean(rollbackProp);
		
		InitialContext ctx = new InitialContext();
		dataSource = (DataSource) ctx.lookup(RetryConnectionHelper.jndiName);
		
		
		if (cpd != null) {
			this.readerIndex = numbersCheckpointData.getCount();
			stepCtx.getProperties().setProperty("init.checkpoint", this.readerIndex + "");
		} 	
	}

	@Override
	public NumbersRecord readItem() throws Exception {
		int i = readerIndex;
		
		Reporter.log("Reading item: " + readerIndex + "...<br>");

		// Throw an exception when forcedFailCount is reached
		if (forcedFailCount != 0 && (readerIndex >= forcedFailCount) && (testState == STATE_NORMAL)) {
			    //forcedFailCount = 0;
			    failindex = readerIndex;
			    testState = STATE_RETRY;
			    Reporter.log("Fail on purpose NumbersRecord.readItem<p>");
				throw new MyParentException("Fail on purpose in NumbersRecord.readItem()");	
				
		} else if (forcedFailCount != 0 && (readerIndex >= forcedFailCount) && (testState == STATE_EXCEPTION)) {
			failindex = readerIndex;
			testState = STATE_SKIP;
			forcedFailCount = 0;
			Reporter.log("Test skip -- Fail on purpose NumbersRecord.readItem<p>");
			throw new MyParentException("Test skip -- Fail on purpose in NumbersRecord.readItem()");	
		}
		
		
		if (testState == STATE_RETRY)
		{
			// should be retrying at same index with no rollback
			/*if(!rollback) {
					if(failindex != readerIndex)
						throw new Exception("Error reading data.  Expected to be at index " + failindex + " but got index " + readerIndex);
			}
						
			// should be retrying at last checkpoint with rollback
			else {
						
					int checkpointIndex = Integer.parseInt(stepCtx.getProperties().getProperty("checkpoint.index"));
							
					if(checkpointIndex != readerIndex)
						throw new Exception("Error reading data.  Expected to be at index " + checkpointIndex + " but got index " + readerIndex);
			}*/
			
			if(stepCtx.getProperties().getProperty("retry.read.exception.invoked") != "true") {
				Reporter.log("onRetryReadException not invoked<p>");
				throw new Exception("onRetryReadException not invoked");
			} else {
				Reporter.log("onRetryReadException was invoked<p>");
			}
			
			if(stepCtx.getProperties().getProperty("retry.read.exception.match") != "true") {
				Reporter.log("retryable exception does not match<p>");
				throw new Exception("retryable exception does not match");
			} else {
				Reporter.log("Retryable exception matches<p>");
			}
			testState = STATE_EXCEPTION;
			//Reporter.log("Test skip after retry -- Fail on purpose in NumbersRecord.readItem<p>");
			//throw new MyParentException("Test skip after retry -- Fail on purpose in NumbersRecord.readItem()");	
		}
		else if(testState == STATE_SKIP) {
			if(stepCtx.getProperties().getProperty("skip.read.item.invoked") != "true") {
				Reporter.log("onSkipReadItem not invoked<p>");
				throw new Exception("onSkipReadItem not invoked");
			} else {
				Reporter.log("onSkipReadItem was invoked<p>");
			}
			
			if(stepCtx.getProperties().getProperty("skip.read.item.match") != "true") {
				Reporter.log("skippable exception does not match<p>");
				throw new Exception("skippable exception does not match");
			} else {
				Reporter.log("skippable exception matches<p>");
			}
			testState = STATE_NORMAL;
		}
		

		if (readerIndex > 20) {
			return null;
		}

		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		
		try {
			connection = RetryConnectionHelper.getConnection(dataSource);

			statement = connection.prepareStatement(RetryConnectionHelper.SELECT_NUMBERS);
			statement.setInt(1, readerIndex);
			rs = statement.executeQuery();

			int quantity = -1;
			while (rs.next()) {
				quantity = rs.getInt("quantity");
			}
					
			readerIndex++;
			Reporter.log("Read [item: " + i + " quantity: " + quantity + "]<p>");
			return new NumbersRecord(i, quantity);
		} catch (SQLException e) {
			throw e;
		} finally {
			RetryConnectionHelper.cleanupConnection(connection, rs, statement);
		}

	}

	@Override
	 public Externalizable checkpointInfo() throws Exception {
		 NumbersCheckpointData _chkptData = new  NumbersCheckpointData();
		_chkptData.setCount(readerIndex);
		stepCtx.getProperties().setProperty("checkpoint.index", Integer.toString(readerIndex));
		return _chkptData; 
	}



}

