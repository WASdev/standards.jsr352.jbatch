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

import java.io.Externalizable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.batch.api.AbstractItemReader;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import jsr352.tck.chunktypes.InventoryCheckpointData;
import jsr352.tck.chunktypes.InventoryRecord;

@javax.inject.Named("inventoryInitReader")
public class InventoryInitReader extends AbstractItemReader<InventoryRecord> {

    protected DataSource dataSource = null;

    private int count = 0;

    public void open(Externalizable cpd) throws NamingException {

        InitialContext ctx = new InitialContext();
        dataSource = (DataSource) ctx.lookup(ConnectionHelper.jndiName);

    }

    @Override
    public InventoryRecord readItem() throws SQLException {
        if (count > 0) {
            return null;
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
                count++;
            }

            return new InventoryRecord(1, quantity);

        } catch (SQLException e) {
            throw e;
        } finally {
            ConnectionHelper.cleanupConnection(connection, rs, statement);
        }

    }


    @Override
    public Externalizable checkpointInfo() throws Exception {
        InventoryCheckpointData chkpData = new InventoryCheckpointData();
        chkpData.setInventoryCount(count);
        return chkpData;
    }
}
