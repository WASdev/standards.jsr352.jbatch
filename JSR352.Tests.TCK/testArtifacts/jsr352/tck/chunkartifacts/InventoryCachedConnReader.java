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
package jsr352.tck.chunkartifacts;

import java.io.Externalizable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import javax.batch.annotation.BatchProperty;
import javax.batch.api.AbstractItemReader;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import jsr352.tck.chunktypes.InventoryCheckpointData;
import jsr352.tck.chunktypes.InventoryRecord;

@javax.inject.Named("inventoryCachedConnReader")
public class InventoryCachedConnReader extends AbstractItemReader<InventoryRecord> {

	private static final String CLASSNAME = InventoryCachedConnReader.class.getName();
	private final static Logger logger = Logger.getLogger(CLASSNAME);
	

	protected DataSource dataSource = null;
	
	private Connection connection = null;
	
    @Inject
    JobContext<?> jobCtx;
	
    @Inject
    StepContext<?,?> stepCtx;
    
    @Inject    
    @BatchProperty(name="forced.fail.count")
	String forcedFailCountProp;
	
    @Inject    
    @BatchProperty(name="dummy.delay.seconds")
	String dummyDelayProp;
	
    @Inject    
    @BatchProperty(name="auto.commit")
	String autoCommitProp;

	boolean autoCommit = true;
	
	int forcedFailCount, dummyDelay, expectedReaderChkp = -1;

	int readerIndex = 0; //the number of items that have already been read
	InventoryCheckpointData inventoryCheckpoint = new InventoryCheckpointData();
	

	public InventoryRecord readItem() throws Exception {
		if (forcedFailCount != 0 && readerIndex >= forcedFailCount) {
			//after reading up to the forced fail number force a dummy delay
			if (dummyDelay > 0) {
				Thread.sleep(dummyDelay); //sleep for dummyDelay seconds
			} else {
				//Clean up when we hit an exception
				ConnectionHelper.cleanupConnection(connection, null, null);
				throw new Exception("Fail on purpose in InventoryRecord.readItem()");	
			}
			
		} 

		
		PreparedStatement statement = null;
		ResultSet rs = null;
		
		try {
			

			statement = connection.prepareStatement(ConnectionHelper.SELECT_INVENTORY);
			statement.setInt(1, 1);
			rs = statement.executeQuery();

			int quantity = -1;
			while (rs.next()) {
				quantity = rs.getInt("quantity");
			}
			
			//If we run out of items we are done so stop processing orders
			if (quantity < 1) {
				return null;
			}
			
			readerIndex++;			
			this.inventoryCheckpoint.setInventoryCount(readerIndex);

			return new InventoryRecord(1, quantity);
		} catch (SQLException e) {
			ConnectionHelper.cleanupConnection(connection, rs, statement);
			throw e;
		} finally {
			ConnectionHelper.cleanupConnection(null, rs, statement);
		}

	}


	public InventoryCheckpointData checkpointInfo() {
		logger.finer("InventoryReader.getInventoryCheckpoint() index = " +this.inventoryCheckpoint.getInventoryCount());
		
		return this.inventoryCheckpoint;
		 
	}


	public void close() {
		ConnectionHelper.cleanupConnection(connection, null, null);
	}

	//InventoryCheckpointData
    @Override
    public void open(Externalizable cpd) throws Exception {
        InventoryCheckpointData checkpointData = (InventoryCheckpointData)cpd;
        
        forcedFailCount = Integer.parseInt(forcedFailCountProp);
        dummyDelay = Integer.parseInt(dummyDelayProp);
        autoCommit = Boolean.parseBoolean(autoCommitProp);
        
        InitialContext ctx = new InitialContext();
        dataSource = (DataSource) ctx.lookup(ConnectionHelper.jndiName);
        
        connection = ConnectionHelper.getConnection(dataSource);
        
        if (checkpointData != null) {
            this.readerIndex = checkpointData.getInventoryCount();
            
            stepCtx.getProperties().setProperty("init.checkpoint", this.readerIndex + "");
        }   
    }




}
