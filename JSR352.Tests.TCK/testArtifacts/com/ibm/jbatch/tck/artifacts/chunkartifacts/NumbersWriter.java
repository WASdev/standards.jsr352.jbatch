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

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import javax.batch.annotation.BatchProperty;
import javax.batch.api.chunk.AbstractItemWriter;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.testng.Reporter;

import com.ibm.jbatch.tck.artifacts.chunktypes.NumbersRecord;
import com.ibm.jbatch.tck.artifacts.reusable.MyParentException;


@javax.inject.Named("numbersWriter")
public class NumbersWriter extends AbstractItemWriter<NumbersRecord> {
	

	protected DataSource dataSource = null;
	
    @Inject
    StepContext<?,?> stepCtx;


    @Inject    
    @BatchProperty(name="forced.fail.count.write")
	String forcedFailCountProp;
	
	private static final int STATE_NORMAL = 0;
	private static final int STATE_RETRY = 1;
	private int testState = STATE_NORMAL;
	
	int forcedFailCount = 0;
	boolean isInited = false;
	int count = 1;
	
	@Override
	public void open(Serializable cpd) throws NamingException {
		InitialContext ctx = new InitialContext();
		dataSource = (DataSource) ctx.lookup(RetryConnectionHelper.jndiName);
	}
	
	@Override
	public void writeItems(List<NumbersRecord> records) throws Exception {
		int item = -1;
		int quantity = -1;
		int check = -1;
		
		if(!isInited) {
			forcedFailCount = Integer.parseInt(forcedFailCountProp);
			isInited = true;
		}
		
		for (NumbersRecord record : records) {
			item = record.getItem();
			quantity = record.getQuantity();
			
			// Throw an exception when forcedFailCount is reached
			if (forcedFailCount != 0 && count >= forcedFailCount) {
					   forcedFailCount = 0;
						testState = STATE_RETRY;
						Reporter.log("Fail on purpose in NumbersRecord.writeItems<p>");
						throw new MyParentException("Fail on purpose in NumbersRecord.writeItems()");	
			}
			
			if (testState == STATE_RETRY)
			{
				
				if(stepCtx.getProperties().getProperty("retry.write.exception.invoked") != "true") {
					Reporter.log("onRetryWriteException not invoked<p>");
					throw new Exception("onRetryWriteException not invoked");
				}
				
				if(stepCtx.getProperties().getProperty("retry.write.exception.match") != "true") {
					Reporter.log("retryable exception does not match");
					throw new Exception("retryable exception does not match");
				}
				
				testState = STATE_NORMAL;
			}
		
			Connection connection = null;	
			PreparedStatement statement = null;
			
			try {
				connection = RetryConnectionHelper.getConnection(dataSource);
	
				statement = connection.prepareStatement(RetryConnectionHelper.UPDATE_NUMBERS);
				statement.setInt(2, item);
				statement.setInt(1, quantity);
				int rs = statement.executeUpdate();
				count++;
				
			} catch (SQLException e) {
				e.printStackTrace();
				throw e;
			} finally {
				RetryConnectionHelper.cleanupConnection(connection, null, statement);
			}
		}
	}

}
