/**
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import javax.batch.annotation.BatchContext;
import javax.batch.annotation.BatchProperty;
import javax.batch.annotation.CheckpointInfo;
import javax.batch.annotation.ItemReader;
import javax.batch.annotation.Open;
import javax.batch.annotation.ReadItem;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import jsr352.tck.chunktypes.InventoryCheckpointData;
import jsr352.tck.chunktypes.InventoryRecord;

@ItemReader("InventoryReader")
@javax.inject.Named("InventoryReader")
public class InventoryReader {

	private static final String CLASSNAME = InventoryReader.class.getName();
	private final static Logger logger = Logger.getLogger(CLASSNAME);
	

	protected DataSource dataSource = null;
	
    @BatchContext
    JobContext<?> jobCtx;
	
    @BatchContext
    StepContext<?,?> stepCtx;
    
	@BatchProperty(name="forced.fail.count")
	String forcedFailCountProp;
	
	@BatchProperty(name="dummy.delay.seconds")
	String dummyDelayProp;
	
	@BatchProperty(name="auto.commit")
	String autoCommitProp;

	boolean autoCommit = true;
	
	int forcedFailCount, dummyDelay, expectedReaderChkp = -1;

	int readerIndex = 0; //the number of items that have already been read
	InventoryCheckpointData inventoryCheckpoint = new InventoryCheckpointData();
	
	
	@Open
	public void openMe(InventoryCheckpointData cpd) throws NamingException {

		forcedFailCount = Integer.parseInt(forcedFailCountProp);
		dummyDelay = Integer.parseInt(dummyDelayProp);
		autoCommit = Boolean.parseBoolean(autoCommitProp);
		
		InitialContext ctx = new InitialContext();
		dataSource = (DataSource) ctx.lookup(ConnectionHelper.jndiName);
		
		if (cpd != null) {
			this.readerIndex = cpd.getInventoryCount();
			
			stepCtx.getProperties().setProperty("init.checkpoint", this.readerIndex + "");
		} 	

	}

	@ReadItem
	public InventoryRecord readItem() throws Exception {
		if (forcedFailCount != 0 && readerIndex >= forcedFailCount) {
			//after reading up to the forced fail number force a dummy delay
			if (dummyDelay > 0) {
				Thread.sleep(dummyDelay); //sleep for dummyDelay seconds
			} else {
				throw new Exception("Fail on purpose in InventoryRecord.readItem()");	
			}
			
		} 

		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		
		try {
			connection = ConnectionHelper.getConnection(dataSource);

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
			throw e;
		} finally {
			ConnectionHelper.cleanupConnection(connection, rs, statement);
		}

	}

	@CheckpointInfo
	public InventoryCheckpointData getInventoryCheckpoint() {
		logger.finer("InventoryReader.getInventoryCheckpoint() index = " +this.inventoryCheckpoint.getInventoryCount());
		
		return this.inventoryCheckpoint;
		 
	}



}
