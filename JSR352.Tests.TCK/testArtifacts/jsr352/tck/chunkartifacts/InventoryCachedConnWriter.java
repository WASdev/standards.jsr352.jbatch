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
import java.sql.SQLException;
import java.util.List;

import javax.batch.api.AbstractItemWriter;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import jsr352.tck.chunktypes.InventoryRecord;

@javax.inject.Named("inventoryCachedConnWriter")
public class InventoryCachedConnWriter extends AbstractItemWriter<InventoryRecord> {
	

	protected DataSource dataSource = null;
	private Connection connection;


	public void open(Externalizable cpd) throws NamingException, SQLException {
	    
		InitialContext ctx = new InitialContext();
		dataSource = (DataSource) ctx.lookup(ConnectionHelper.jndiName);

		connection = ConnectionHelper.getConnection(dataSource);
	}
	
	

	public void write(List<InventoryRecord> records) throws SQLException {
		int itemID = -1;
		int quantity = -1;
		
		for (InventoryRecord record : records) {
			itemID = record.getItemID();
			quantity = record.getQuantity();
		}
		
		
		
		PreparedStatement statement = null;
		
		try {
			
			statement = connection.prepareStatement(ConnectionHelper.UPDATE_INVENTORY);
			statement.setInt(2, itemID);
			statement.setInt(1, quantity);
			int rs = statement.executeUpdate();

			
		} catch (SQLException e) {
			ConnectionHelper.cleanupConnection(connection, null, statement);
			throw e;
		} finally {
			ConnectionHelper.cleanupConnection(null, null, statement);
		}
		
	}

	public void close() {
		ConnectionHelper.cleanupConnection(connection, null, null);
	}



	@Override
	public void writeItems(List<InventoryRecord> items) throws Exception {
		// TODO Auto-generated method stub
		
	}
}
