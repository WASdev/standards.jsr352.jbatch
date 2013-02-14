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

import jsr352.tck.chunktypes.NumbersCheckpointData;
import jsr352.tck.chunktypes.NumbersRecord;

@javax.inject.Named("numbersInitWriter")
public class NumbersInitWriter extends AbstractItemWriter<NumbersRecord> {
	
	
	protected DataSource dataSource = null;
	
	public void open(Externalizable cpd) throws NamingException {
		InitialContext ctx = new InitialContext();
		dataSource = (DataSource) ctx.lookup(RetryConnectionHelper.jndiName);
	}
	
	@Override
	public void writeItems(List<NumbersRecord> records) throws SQLException {
		
		
		int item = -1;
		int quantity = -1;

		Connection connection = null;	
		PreparedStatement statement = null;
		
		try {
			for (NumbersRecord record : records) {
				item = record.getItem();
				quantity = record.getQuantity();
				
				connection = RetryConnectionHelper.getConnection(dataSource);
	
				statement = connection.prepareStatement(RetryConnectionHelper.UPDATE_NUMBERS);
				statement.setInt(2, item);
				statement.setInt(1, quantity);
				int rs = statement.executeUpdate();
			}
			
		} catch (SQLException e) {
			throw e;
		} finally {
			RetryConnectionHelper.cleanupConnection(connection, null, statement);
		}
		}
	}
