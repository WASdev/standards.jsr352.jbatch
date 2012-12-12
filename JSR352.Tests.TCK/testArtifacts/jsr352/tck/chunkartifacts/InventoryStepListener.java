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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import javax.batch.annotation.AfterStep;
import javax.batch.annotation.BatchContext;
import javax.batch.annotation.StepListener;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import jsr352.tck.common.StatusConstants;

@StepListener("InventoryStepListener")
@javax.inject.Named("InventoryStepListener")
public class InventoryStepListener implements StatusConstants {
	
    private final static String sourceClass = InventoryStepListener.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);
	
    @BatchContext
    StepContext<?,?> stepCtx;

    @BatchContext
    JobContext<?> jobCtx;
	
	protected String jndiName = "jdbc/orderDB";

	protected DataSource dataSource = null;
	
	private void init() throws NamingException {
		InitialContext ctx = new InitialContext();
		dataSource = (DataSource) ctx.lookup(jndiName);
	}


	private int getInventoryCount() throws Exception {

		this.init();
		
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		
		try {
			connection = ConnectionHelper.getConnection(dataSource);

			statement = connection
					.prepareStatement(ConnectionHelper.SELECT_INVENTORY);
			statement.setInt(1, 1);
			rs = statement.executeQuery();

			int quantity = -1;
			while (rs.next()) {
				quantity = rs.getInt("quantity");
			}
			
			return quantity;

		} catch (SQLException e) {
			throw e;
		} finally {
			ConnectionHelper.cleanupConnection(connection, rs, statement);
		}

	}



    public void beforeStep() {
    }
    
    
    
    
    @AfterStep
    public void afterStep() throws Exception {
    	logger.fine("afterStep");
    	
    	int finalInventoryCount = this.getInventoryCount();
    	String initCheckpoint = stepCtx.getProperties().getProperty("init.checkpoint");
    	
		String exitStatus = "Inventory=" + finalInventoryCount + " InitialCheckpoint=" + initCheckpoint;
		jobCtx.setExitStatus(exitStatus);
    }

	
}
