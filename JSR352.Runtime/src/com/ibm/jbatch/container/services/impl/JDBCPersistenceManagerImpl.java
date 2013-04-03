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
package com.ibm.jbatch.container.services.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.ibm.jbatch.container.context.impl.MetricImpl;
import com.ibm.jbatch.container.context.impl.StepContextImpl;
import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.exception.PersistenceException;
import com.ibm.jbatch.container.impl.PartitionedStepBuilder;
import com.ibm.jbatch.container.jobinstance.JobInstanceImpl;
import com.ibm.jbatch.container.jobinstance.JobOperatorJobExecution;
import com.ibm.jbatch.container.jobinstance.RuntimeFlowInSplitExecution;
import com.ibm.jbatch.container.jobinstance.RuntimeJobExecution;
import com.ibm.jbatch.container.jobinstance.StepExecutionImpl;
import com.ibm.jbatch.container.persistence.CheckpointData;
import com.ibm.jbatch.container.persistence.CheckpointDataKey;
import com.ibm.jbatch.container.services.IJobExecution;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.status.JobStatus;
import com.ibm.jbatch.container.status.StepStatus;
import com.ibm.jbatch.container.util.TCCLObjectInputStream;
import com.ibm.jbatch.spi.services.IBatchConfig;

public class JDBCPersistenceManagerImpl implements IPersistenceManagerService, JDBCPersistenceManagerSQLConstants {

	private static final String CLASSNAME = JDBCPersistenceManagerImpl.class.getName();

	private final static Logger logger = Logger.getLogger(CLASSNAME);

	private IBatchConfig batchConfig = null;

	protected DataSource dataSource = null;
	protected String jndiName = null;

	protected String driver = ""; 
	protected String schema = "";
	protected String url = ""; 
	protected String userId = "";
	protected String pwd = "";

	/* (non-Javadoc)
	 * @see com.ibm.jbatch.container.services.impl.AbstractPersistenceManagerImpl#init(com.ibm.jbatch.container.IBatchConfig)
	 */
	@Override
	public void init(IBatchConfig batchConfig) throws BatchContainerServiceException {
		logger.config("Entering CLASSNAME.init(), batchConfig =" + batchConfig);

		this.batchConfig = batchConfig;

		schema = batchConfig.getDatabaseConfigurationBean().getSchema();

		if (!batchConfig.isJ2seMode()) {
			jndiName = batchConfig.getDatabaseConfigurationBean().getJndiName();

			logger.config("JNDI name = " + jndiName);

			if (jndiName == null || jndiName.equals("")) {
				throw new BatchContainerServiceException("JNDI name is not defined.");
			}

			try {
				Context ctx = new InitialContext();
				dataSource = (DataSource) ctx.lookup(jndiName);

			} catch (NamingException e) {
				logger.severe("Lookup failed for JNDI name: " + jndiName + 
						".  One cause of this could be that the batch runtime is incorrectly configured to EE mode when it should be in SE mode.");
				throw new BatchContainerServiceException(e);
			}

		} else {
			driver = batchConfig.getDatabaseConfigurationBean().getJdbcDriver();
			url = batchConfig.getDatabaseConfigurationBean().getJdbcUrl();
			userId = batchConfig.getDatabaseConfigurationBean().getDbUser();
			pwd = batchConfig.getDatabaseConfigurationBean().getDbPassword();

			logger.config("driver: " + driver + ", url: " + url);
		}

		try {
			// only auto-create on Derby
			if(isDerby()) {	
				if(!isSchemaValid()) {
					createSchema();
				}
				checkAllTables();
			}
		} catch (SQLException e) {
			logger.severe(e.getLocalizedMessage());
			throw new BatchContainerServiceException(e);
		}

		logger.config("Exiting CLASSNAME.init()");
	}

	/**
	 * Checks if the default schema JBATCH or the schema defined in batch-config exists.
	 * 
	 * @return true if the schema exists, false otherwise.
	 * @throws SQLException
	 */
	private boolean isSchemaValid() throws SQLException {
		logger.entering(CLASSNAME, "isSchemaValid");
		Connection conn = getConnectionToDefaultSchema();
		DatabaseMetaData dbmd = conn.getMetaData();
		ResultSet rs = dbmd.getSchemas();
		while(rs.next()) {
			if (schema.equalsIgnoreCase(rs.getString("TABLE_SCHEM")) ) {
				cleanupConnection(conn, rs, null);
				logger.exiting(CLASSNAME, "isSchemaValid", true);
				return true;
			}
		}
		cleanupConnection(conn, rs, null);
		logger.exiting(CLASSNAME, "isSchemaValid", false);
		return false;
	}

	private boolean isDerby() throws SQLException {
		logger.entering(CLASSNAME, "isDerby");
		Connection conn = getConnectionToDefaultSchema();
		DatabaseMetaData dbmd = conn.getMetaData();
		boolean derby = dbmd.getDatabaseProductName().toLowerCase().indexOf("derby") > 0;
		logger.exiting(CLASSNAME, "isDerby", derby);
		return derby;
	}

	/**
	 * Creates the default schema JBATCH or the schema defined in batch-config.
	 * 
	 * @throws SQLException
	 */
	private void createSchema() throws SQLException {
		logger.entering(CLASSNAME, "createSchema");
		Connection conn = getConnectionToDefaultSchema();

		logger.log(Level.INFO, schema + " schema does not exists. Trying to create it.");
		PreparedStatement ps = null;
		ps = conn.prepareStatement("CREATE SCHEMA " + schema);
		ps.execute();

		cleanupConnection(conn, null, ps);
		logger.exiting(CLASSNAME, "createSchema");
	}

	/**
	 * Checks if all the runtime batch table exists. If not, it creates them.
	 *  
	 * @throws SQLException
	 */
	private void checkAllTables() throws SQLException {
		logger.entering(CLASSNAME, "checkAllTables");

		createIfNotExists(CHECKPOINTDATA_TABLE, CREATE_TAB_CHECKPOINTDATA);
		executeStatement(CREATE_CHECKPOINTDATA_INDEX);
		createIfNotExists(JOBINSTANCEDATA_TABLE, CREATE_TAB_JOBINSTANCEDATA);

		createIfNotExists(EXECUTIONINSTANCEDATA_TABLE,
				CREATE_TAB_EXECUTIONINSTANCEDATA);
		createIfNotExists(STEPEXECUTIONINSTANCEDATA_TABLE,
				CREATE_TAB_STEPEXECUTIONINSTANCEDATA);

		createIfNotExists(JOBSTATUS_TABLE, CREATE_TAB_JOBSTATUS);
		createIfNotExists(STEPSTATUS_TABLE, CREATE_TAB_STEPSTATUS);	

		logger.exiting(CLASSNAME, "checkAllTables");
	}

	/**
	 * Creates tableName using the createTableStatement DDL.
	 * 
	 * @param tableName
	 * @param createTableStatement
	 * @throws SQLException
	 */
	private void createIfNotExists(String tableName, String createTableStatement) throws SQLException {
		logger.entering(CLASSNAME, "createIfNotExists", new Object[] {tableName, createTableStatement});

		Connection conn = getConnection();
		DatabaseMetaData dbmd = conn.getMetaData();
		ResultSet rs = dbmd.getTables(null, schema, tableName, null);
		PreparedStatement ps = null;
		if(!rs.next()) {
			logger.log(Level.INFO, tableName + " table does not exists. Trying to create it.");
			ps = conn.prepareStatement(createTableStatement);
			ps.executeUpdate();
		}

		cleanupConnection(conn, rs, ps);
		logger.exiting(CLASSNAME, "createIfNotExists");
	}

	/**
	 * Executes the provided SQL statement
	 * 
	 * @param statement
	 * @throws SQLException
	 */
	private void executeStatement(String statement) throws SQLException {
		logger.entering(CLASSNAME, "executeStatement", statement);

		Connection conn = getConnection();
		PreparedStatement ps = null;

		ps = conn.prepareStatement(statement);
		ps.executeUpdate();

		cleanupConnection(conn, ps);
		logger.exiting(CLASSNAME, "executeStatement");
	}


	/* (non-Javadoc)
	 * @see com.ibm.jbatch.container.services.impl.AbstractPersistenceManagerImpl#createCheckpointData(com.ibm.ws.batch.container.checkpoint.CheckpointDataKey, com.ibm.ws.batch.container.checkpoint.CheckpointData)
	 */
	@Override
	public void createCheckpointData(CheckpointDataKey key, CheckpointData value) {
		logger.entering(CLASSNAME, "createCheckpointData", new Object[] {key, value});
		insertCheckpointData(key.getCommaSeparatedKey(), value);
		logger.exiting(CLASSNAME, "createCheckpointData");
	}

	/* (non-Javadoc)
	 * @see com.ibm.jbatch.container.services.impl.AbstractPersistenceManagerImpl#getCheckpointData(com.ibm.ws.batch.container.checkpoint.CheckpointDataKey)
	 */
	@Override
	public CheckpointData getCheckpointData(CheckpointDataKey key) {
		logger.entering(CLASSNAME, "getCheckpointData", key==null ? "<null>" : key);
		CheckpointData checkpointData = queryCheckpointData(key.getCommaSeparatedKey());
		logger.exiting(CLASSNAME, "getCheckpointData", checkpointData==null ? "<null>" : checkpointData);
		return checkpointData;
	}

	/* (non-Javadoc)
	 * @see com.ibm.jbatch.container.services.impl.AbstractPersistenceManagerImpl#updateCheckpointData(com.ibm.ws.batch.container.checkpoint.CheckpointDataKey, com.ibm.ws.batch.container.checkpoint.CheckpointData)
	 */
	@Override
	public void updateCheckpointData(CheckpointDataKey key, CheckpointData value) {
		logger.entering(CLASSNAME, "updateCheckpointData", new Object[] {key, value});
		CheckpointData data = queryCheckpointData(key.getCommaSeparatedKey());
		if(data != null) {
			updateCheckpointData(key.getCommaSeparatedKey(), value);
		} else {
			createCheckpointData(key, value);
		}
		logger.exiting(CLASSNAME, "updateCheckpointData");
	}


	/**
	 * @return the database connection and sets it to the default schema JBATCH or the schema defined in batch-config.
	 * 
	 * @throws SQLException
	 */
	protected Connection getConnection() throws SQLException {
		logger.finest("Entering: " + CLASSNAME + ".getConnection");
		Connection connection = null;

		if(!batchConfig.isJ2seMode()) {
			logger.finest("J2EE mode, getting connection from data source");
			connection = dataSource.getConnection();
			logger.finest("autocommit="+connection.getAutoCommit());
		} else {
			try {
				Class.forName(driver);
			} catch (ClassNotFoundException e) {
				throw new PersistenceException(e);
			}
			logger.finest("JSE mode, getting connection from " + url);
			connection = DriverManager.getConnection(url, userId, pwd);
			logger.finest("autocommit="+connection.getAutoCommit());
		}
		setSchemaOnConnection(connection);

		logger.finest("Exiting: " + CLASSNAME + ".getConnection() with conn =" + connection);
		return connection;
	}

	/**
	 * @return the database connection. The schema is set to whatever default its used by the underlying database.
	 * @throws SQLException
	 */
	protected Connection getConnectionToDefaultSchema() throws SQLException {
		logger.finest("Entering getConnectionToDefaultSchema");
		Connection connection = null;

		if(!batchConfig.isJ2seMode()) {
			logger.finest("J2EE mode, getting connection from data source");
			try {
				connection = dataSource.getConnection();
			} catch(SQLException e) {
				logException("FAILED GETTING DATABASE CONNECTION", e);
				throw new PersistenceException(e);
			}
			logger.finest("autocommit="+connection.getAutoCommit());
		} else {
			try {
				Class.forName(driver);
			} catch (ClassNotFoundException e) {
				logException("ClassNotFoundException: Cannot load driver class: " + driver, e);
				throw new PersistenceException(e);
			}
			logger.finest("JSE mode, getting connection from " + url);
			try {
				connection = DriverManager.getConnection(url, userId, pwd);
			} catch (SQLException e) {
				logException("FAILED GETTING DATABASE CONNECTION.  FOR EMBEDDED DERBY CHECK FOR OTHER USER LOCKING THE CURRENT DATABASE (Try using a different database instance).", e); 
				throw new PersistenceException(e);
			}
			logger.finest("autocommit="+connection.getAutoCommit());
		}
		logger.finest("Exiting from getConnectionToDefaultSchema, conn= " +connection);
		return connection;
	}

	private void logException(String msg, Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);

		logger.log(Level.SEVERE, msg +  "; Exception stack trace: " + sw);
	}

	/**
	 * Set the default schema JBATCH or the schema defined in batch-config on the connection object.
	 * 
	 * @param connection
	 * @throws SQLException
	 */
	private void setSchemaOnConnection(Connection connection) throws SQLException {
		logger.finest("Entering " + CLASSNAME +".setSchemaOnConnection()");

		PreparedStatement ps = null;
		ps = connection.prepareStatement("SET SCHEMA ?");
		ps.setString(1, schema);
		ps.executeUpdate(); 

		ps.close();

		logger.finest("Exiting " + CLASSNAME +".setSchemaOnConnection()");
	}

	/**
	 * select data from DB table
	 * 
	 * @param key - the IPersistenceDataKey object
	 * @return List of serializable objects store in the DB table
	 * 
	 * Ex. select id, obj from tablename where id = ?
	 */
	private CheckpointData queryCheckpointData(Object key) {
		logger.entering(CLASSNAME, "queryCheckpointData", new Object[] {key, SELECT_CHECKPOINTDATA});
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		ObjectInputStream objectIn = null;
		CheckpointData data = null;
		try {
			conn = getConnection();
			statement = conn.prepareStatement(SELECT_CHECKPOINTDATA);
			statement.setObject(1, key);
			rs = statement.executeQuery();
			if (rs.next()) {
				byte[] buf = rs.getBytes("obj");
				data = (CheckpointData)deserializeObject(buf);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			throw new PersistenceException(e);
		} catch (ClassNotFoundException e) {
			throw new PersistenceException(e);
		} finally {
			if (objectIn != null) {
				try {
					objectIn.close();
				} catch (IOException e) {
					throw new PersistenceException(e);
				}
			}
			cleanupConnection(conn, rs, statement);
		}
		logger.exiting(CLASSNAME, "queryCheckpointData");
		return data;	
	}


	/**
	 * insert data to DB table
	 * 
	 * @param key - the IPersistenceDataKey object
	 * @param value - serializable object to store  
	 * 
	 * Ex. insert into tablename values(?, ?)
	 */
	private <T> void insertCheckpointData(Object key, T value) {
		logger.entering(CLASSNAME, "insertCheckpointData", new Object[] {key, value});
		Connection conn = null;
		PreparedStatement statement = null;
		ByteArrayOutputStream baos = null;
		ObjectOutputStream oout = null;
		byte[] b;
		try {
			conn = getConnection();
			statement = conn.prepareStatement(INSERT_CHECKPOINTDATA);
			baos = new ByteArrayOutputStream();
			oout = new ObjectOutputStream(baos);
			oout.writeObject(value);

			b = baos.toByteArray();

			statement.setObject(1, key);
			statement.setBytes(2, b);
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			throw new PersistenceException(e);
		} finally {
			if (baos != null) {
				try {
					baos.close();
				} catch (IOException e) {
					throw new PersistenceException(e);
				}
			}
			if (oout != null) {
				try {
					oout.close();
				} catch (IOException e) {
					throw new PersistenceException(e);
				}
			}
			cleanupConnection(conn, null, statement);
		}
		logger.exiting(CLASSNAME, "insertCheckpointData");
	}

	/**
	 * update data in DB table
	 * 
	 * @param value - serializable object to store 
	 * @param key - the IPersistenceDataKey object
	 * @param query - SQL statement to execute. 
	 * 
	 * Ex. update tablename set obj = ? where id = ?
	 */
	private void updateCheckpointData(Object key, CheckpointData value) {
		logger.entering(CLASSNAME, "updateCheckpointData", new Object[] {key, value});
		Connection conn = null;
		PreparedStatement statement = null;
		ByteArrayOutputStream baos = null;
		ObjectOutputStream oout = null;
		byte[] b;
		try {
			conn = getConnection();
			statement = conn.prepareStatement(UPDATE_CHECKPOINTDATA);
			baos = new ByteArrayOutputStream();
			oout = new ObjectOutputStream(baos);
			oout.writeObject(value);

			b = baos.toByteArray();

			statement.setBytes(1, b);
			statement.setObject(2, key);
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			throw new PersistenceException(e);
		} finally {
			if (baos != null) {
				try {
					baos.close();
				} catch (IOException e) {
					throw new PersistenceException(e);
				}
			}
			if (oout != null) {
				try {
					oout.close();
				} catch (IOException e) {
					throw new PersistenceException(e);
				}
			}
			cleanupConnection(conn, null, statement);
		}
		logger.exiting(CLASSNAME, "updateCheckpointData");
	}



	/**
	 * closes connection, result set and statement
	 * 
	 * @param conn - connection object to close
	 * @param rs - result set object to close
	 * @param statement - statement object to close
	 */
	private void cleanupConnection(Connection conn, ResultSet rs, PreparedStatement statement) {

		logger.logp(Level.FINEST, CLASSNAME, "cleanupConnection", "Entering",  new Object[] {conn, rs==null ? "<null>" : rs, statement==null ? "<null>" : statement});

		if (statement != null) {
			try {
				statement.close();
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
		}

		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
		}

		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				throw new PersistenceException(e);
			} finally {
				try {
					conn.close();
				} catch (SQLException e) {
					throw new PersistenceException(e);
				}
			}
		}
		logger.logp(Level.FINEST, CLASSNAME, "cleanupConnection", "Exiting");
	}

	/**
	 * closes connection and statement
	 * 
	 * @param conn - connection object to close
	 * @param statement - statement object to close
	 */
	private void cleanupConnection(Connection conn, PreparedStatement statement) {

		logger.logp(Level.FINEST, CLASSNAME, "cleanupConnection", "Entering",  new Object[] {conn, statement});

		if (statement != null) {
			try {
				statement.close();
			} catch (SQLException e) {
				throw new PersistenceException(e);
			}
		}

		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				throw new PersistenceException(e);
			} finally {
				try {
					conn.close();
				} catch (SQLException e) {
					throw new PersistenceException(e);
				}
			}
		}
		logger.logp(Level.FINEST, CLASSNAME, "cleanupConnection", "Exiting");
	}


	@Override
	public int jobOperatorGetJobInstanceCount(String jobName, String appTag) {
		
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		int count;

		try {
			conn = getConnection();
			statement = conn.prepareStatement("select count(jobinstanceid) as jobinstancecount from jobinstancedata where name = ? and apptag = ?");
			statement.setString(1, jobName);
			statement.setString(2, appTag);
			rs = statement.executeQuery();
			rs.next();
			count = rs.getInt("jobinstancecount");

		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
		finally {
			cleanupConnection(conn, rs, statement);
		}
		return count;
	}
	
	@Override
	public int jobOperatorGetJobInstanceCount(String jobName) {

		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		int count;

		try {
			conn = getConnection();
			statement = conn.prepareStatement(SELECT_JOBINSTANCEDATA_COUNT);
			statement.setString(1, jobName);
			rs = statement.executeQuery();
			rs.next();
			count = rs.getInt("jobinstancecount");

		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
		finally {
			cleanupConnection(conn, rs, statement);
		}
		return count;
	}


	@Override
	public List<Long> jobOperatorGetJobInstanceIds(String jobName, String appTag, int start, int count) {
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		List<Long> data = new ArrayList<Long>();

		try {
			conn = getConnection();
			statement = conn.prepareStatement("select jobinstanceid from jobinstancedata where name = ? and apptag = ? order by jobinstanceid desc");
			statement.setObject(1, jobName);
			statement.setObject(2, appTag);
			rs = statement.executeQuery();
			while (rs.next()) {
				long id = rs.getLong("jobinstanceid");
				data.add(id);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
		finally {
			cleanupConnection(conn, rs, statement);
		}

		if (data.size() > 0){
			try {
				return data.subList(start, start+count);
			}
			catch (IndexOutOfBoundsException oobEx){
				return data.subList(start, data.size());
			}
		}
		else return data;
	}
	
	@Override
	public List<Long> jobOperatorGetJobInstanceIds(String jobName, int start, int count) {

		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		List<Long> data = new ArrayList<Long>();

		try {
			conn = getConnection();
			statement = conn.prepareStatement(SELECT_JOBINSTANCEDATA_IDS);
			statement.setObject(1, jobName);
			rs = statement.executeQuery();
			while (rs.next()) {
				long id = rs.getLong("jobinstanceid");
				data.add(id);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
		finally {
			cleanupConnection(conn, rs, statement);
		}

		if (data.size() > 0){
			try {
				return data.subList(start, start+count);
			}
			catch (IndexOutOfBoundsException oobEx){
				return data.subList(start, data.size());
			}
		}
		else return data;
	}

	@Override
	public Map<Long, String> jobOperatorGetExternalJobInstanceData() {

		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		HashMap<Long, String> data = new HashMap<Long,String>();

		try {
			conn = getConnection();

			// Filter out 'subjob' parallel execution entries which start with the special character
			final String filter = "not like '" + PartitionedStepBuilder.JOB_ID_SEPARATOR + "%'";

			statement = conn.prepareStatement("select distinct jobinstanceid, name from jobinstancedata where name " + filter );
			rs = statement.executeQuery();
			while (rs.next()) {
				long id = rs.getLong("jobinstanceid");
				String name = rs.getString("name");
				data.put(id, name);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
		finally {
			cleanupConnection(conn, rs, statement);
		}

		return data;
	}

	@Override
	public Timestamp jobOperatorQueryJobExecutionTimestamp(long key, TimestampType timestampType) {



		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		Timestamp createTimestamp = null;
		Timestamp endTimestamp = null;
		Timestamp updateTimestamp = null;
		Timestamp startTimestamp = null;

		try {
			conn = getConnection();
			statement = conn.prepareStatement("select createtime, endtime, updatetime, starttime from executioninstancedata where jobexecid = ?");
			statement.setObject(1, key);
			rs = statement.executeQuery();
			while (rs.next()) {
				createTimestamp = rs.getTimestamp(1);
				endTimestamp = rs.getTimestamp(2);
				updateTimestamp = rs.getTimestamp(3);
				startTimestamp = rs.getTimestamp(4);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
		finally {
			cleanupConnection(conn, rs, statement);
		}

		if (timestampType.equals(TimestampType.CREATE)) {
			return createTimestamp;
		} else if (timestampType.equals(TimestampType.END)) {
			return endTimestamp;
		} else if (timestampType.equals(TimestampType.LAST_UPDATED)) {
			return updateTimestamp;
		} else if (timestampType.equals(TimestampType.STARTED)) {
			return startTimestamp;
		} else {
			throw new IllegalArgumentException("Unexpected enum value.");
		}
	}

	@Override
	public String jobOperatorQueryJobExecutionBatchStatus(long key) {

		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		String status = null;

		try {
			conn = getConnection();
			statement = conn.prepareStatement("select batchstatus from executioninstancedata where jobexecid = ?");
			statement.setLong(1, key);
			rs = statement.executeQuery();
			while (rs.next()) {
				status = rs.getString(1);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}

		return status;
	}


	@Override
	public String jobOperatorQueryJobExecutionExitStatus(long key) {

		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		String status = null;

		try {
			conn = getConnection();
			statement = conn.prepareStatement("select exitstatus from executioninstancedata where jobexecid = ?");
			statement.setLong(1, key);
			rs = statement.executeQuery();
			while (rs.next()) {
				status = rs.getString(1);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}

		return status;
	}

	@Override
	public long jobOperatorQueryJobExecutionJobInstanceId(long executionID) throws NoSuchJobExecutionException {

		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		long jobinstanceID = 0;

		try {
			conn = getConnection();
			statement = conn.prepareStatement("select jobinstanceid from executioninstancedata where jobexecid = ?");
			statement.setLong(1, executionID);
			rs = statement.executeQuery();
			if (rs.next()) {
				jobinstanceID = rs.getLong("jobinstanceid");
			} else {
				String msg = "Did not find job instance associated with executionID =" + executionID;
				logger.fine(msg);
				throw new NoSuchJobExecutionException(msg);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
		finally {
			cleanupConnection(conn, rs, statement);
		}

		return jobinstanceID;
	}

	@Override
	public Properties getParameters(long executionId){
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		Properties props = null;
		ObjectInputStream objectIn = null;

		try {
			conn = getConnection();
			statement = conn.prepareStatement("select parameters from executioninstancedata where jobexecid = ?"); 
			statement.setLong(1, executionId);
			rs = statement.executeQuery();
			while (rs.next()) {

				// get the object based data
				byte[] buf = rs.getBytes("parameters");
				if (buf != null) {
					objectIn = new ObjectInputStream(new ByteArrayInputStream(buf));
					props = (Properties) objectIn.readObject();
				}
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			throw new PersistenceException(e);
		} catch (ClassNotFoundException e) {
			throw new PersistenceException(e);
		} finally {
			if (objectIn != null) {
				try {
					objectIn.close();
				} catch (IOException e) {
					throw new PersistenceException(e);
				}
			}
			cleanupConnection(conn, rs, statement);
		}

		return props;

	}


	public Map<String, StepExecution> getMostRecentStepExecutionsForJobInstance(long instanceId) {

		Map<String, StepExecution> data = new HashMap<String, StepExecution>();

		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;

		long jobexecid = 0;
		long stepexecid = 0;
		String stepname = null;
		String batchstatus = null;
		String exitstatus = null;
		Exception ex = null;
		long readCount =0;
		long writeCount = 0;
		long commitCount = 0;
		long rollbackCount = 0;
		long readSkipCount = 0;
		long processSkipCount = 0;
		long filterCount = 0;
		long writeSkipCount = 0;
		Timestamp startTS = null;
		Timestamp endTS = null;
		StepExecutionImpl stepEx = null;
		ObjectInputStream objectIn = null;

		try {
			conn = getConnection();
			statement = conn.prepareStatement("select A.* from stepexecutioninstancedata as A inner join executioninstancedata as B on A.jobexecid = B.jobexecid where B.jobinstanceid = ? order by A.stepexecid desc"); 
			statement.setLong(1, instanceId);
			rs = statement.executeQuery();
			while (rs.next()) {
				stepname = rs.getString("stepname");
				if (data.containsKey(stepname)) {
					continue;
				} else {

					jobexecid = rs.getLong("jobexecid");
					batchstatus = rs.getString("batchstatus");
					exitstatus = rs.getString("exitstatus");
					readCount = rs.getLong("readcount");
					writeCount = rs.getLong("writecount");
					commitCount = rs.getLong("commitcount");
					rollbackCount = rs.getLong("rollbackcount");
					readSkipCount = rs.getLong("readskipcount");
					processSkipCount = rs.getLong("processskipcount");
					filterCount = rs.getLong("filtercount");
					writeSkipCount = rs.getLong("writeSkipCount");
					startTS = rs.getTimestamp("startTime");
					endTS = rs.getTimestamp("endTime");
					// get the object based data
					Serializable persistentData = null;
					byte[] pDataBytes = rs.getBytes("persistentData");
					if (pDataBytes != null) {
						objectIn = new TCCLObjectInputStream(new ByteArrayInputStream(pDataBytes));
						persistentData = (Serializable)objectIn.readObject();
					}

					stepEx = new StepExecutionImpl(jobexecid, stepexecid);

					stepEx.setBatchStatus(BatchStatus.valueOf(batchstatus));
					stepEx.setExitStatus(exitstatus);
					stepEx.setStepName(stepname);
					stepEx.setReadCount(readCount);
					stepEx.setWriteCount(writeCount);
					stepEx.setCommitCount(commitCount);
					stepEx.setRollbackCount(rollbackCount);
					stepEx.setReadSkipCount(readSkipCount);
					stepEx.setProcessSkipCount(processSkipCount);
					stepEx.setFilterCount(filterCount);
					stepEx.setWriteSkipCount(writeSkipCount);
					stepEx.setStartTime(startTS);
					stepEx.setEndTime(endTS);	
					stepEx.setPersistentUserData(persistentData);

					data.put(stepname, stepEx);
				}
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			throw new PersistenceException(e);
		} catch (ClassNotFoundException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}

		return data;
	}


	@Override
	public List<StepExecution> getStepExecutionsForJobExecution(long execid) {
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;

		long jobexecid = 0;
		long stepexecid = 0;
		String stepname = null;
		String batchstatus = null;
		String exitstatus = null;
		Exception ex = null;
		long readCount =0;
		long writeCount = 0;
		long commitCount = 0;
		long rollbackCount = 0;
		long readSkipCount = 0;
		long processSkipCount = 0;
		long filterCount = 0;
		long writeSkipCount = 0;
		Timestamp startTS = null;
		Timestamp endTS = null;
		StepExecutionImpl stepEx = null;
		ObjectInputStream objectIn = null;

		List<StepExecution> data = new ArrayList<StepExecution>();

		try {
			conn = getConnection();
			statement = conn.prepareStatement("select * from stepexecutioninstancedata where jobexecid = ?"); 
			statement.setLong(1, execid);
			rs = statement.executeQuery();
			while (rs.next()) {
				jobexecid = rs.getLong("jobexecid");
				stepexecid = rs.getLong("stepexecid");
				stepname = rs.getString("stepname");
				batchstatus = rs.getString("batchstatus");
				exitstatus = rs.getString("exitstatus");
				readCount = rs.getLong("readcount");
				writeCount = rs.getLong("writecount");
				commitCount = rs.getLong("commitcount");
				rollbackCount = rs.getLong("rollbackcount");
				readSkipCount = rs.getLong("readskipcount");
				processSkipCount = rs.getLong("processskipcount");
				filterCount = rs.getLong("filtercount");
				writeSkipCount = rs.getLong("writeSkipCount");
				startTS = rs.getTimestamp("startTime");
				endTS = rs.getTimestamp("endTime");
				// get the object based data
				Serializable persistentData = null;
				byte[] pDataBytes = rs.getBytes("persistentData");
				if (pDataBytes != null) {
					objectIn = new TCCLObjectInputStream(new ByteArrayInputStream(pDataBytes));
					persistentData = (Serializable)objectIn.readObject();
				}

				stepEx = new StepExecutionImpl(jobexecid, stepexecid);

				stepEx.setBatchStatus(BatchStatus.valueOf(batchstatus));
				stepEx.setExitStatus(exitstatus);
				stepEx.setStepName(stepname);
				stepEx.setReadCount(readCount);
				stepEx.setWriteCount(writeCount);
				stepEx.setCommitCount(commitCount);
				stepEx.setRollbackCount(rollbackCount);
				stepEx.setReadSkipCount(readSkipCount);
				stepEx.setProcessSkipCount(processSkipCount);
				stepEx.setFilterCount(filterCount);
				stepEx.setWriteSkipCount(writeSkipCount);
				stepEx.setStartTime(startTS);
				stepEx.setEndTime(endTS);	
				stepEx.setPersistentUserData(persistentData);

				data.add(stepEx);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			throw new PersistenceException(e);
		} catch (ClassNotFoundException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}

		return data;
	}

	
    @Override
    public StepExecution getStepExecutionByStepExecutionId(long stepExecId) {
        Connection conn = null;
        PreparedStatement statement = null;
        ResultSet rs = null;

        long jobexecid = 0;
        long stepexecid = 0;
        String stepname = null;
        String batchstatus = null;
        String exitstatus = null;
        Exception ex = null;
        long readCount = 0;
        long writeCount = 0;
        long commitCount = 0;
        long rollbackCount = 0;
        long readSkipCount = 0;
        long processSkipCount = 0;
        long filterCount = 0;
        long writeSkipCount = 0;
        Timestamp startTS = null;
        Timestamp endTS = null;
        StepExecutionImpl stepEx = null;
        ObjectInputStream objectIn = null;

        try {
            conn = getConnection();
            statement = conn.prepareStatement("select * from stepexecutioninstancedata where stepexecid = ?");
            statement.setLong(1, stepExecId);
            rs = statement.executeQuery();
            while (rs.next()) {
                jobexecid = rs.getLong("jobexecid");
                stepexecid = rs.getLong("stepexecid");
                stepname = rs.getString("stepname");
                batchstatus = rs.getString("batchstatus");
                exitstatus = rs.getString("exitstatus");
                readCount = rs.getLong("readcount");
                writeCount = rs.getLong("writecount");
                commitCount = rs.getLong("commitcount");
                rollbackCount = rs.getLong("rollbackcount");
                readSkipCount = rs.getLong("readskipcount");
                processSkipCount = rs.getLong("processskipcount");
                filterCount = rs.getLong("filtercount");
                writeSkipCount = rs.getLong("writeSkipCount");
                startTS = rs.getTimestamp("startTime");
                endTS = rs.getTimestamp("endTime");
                // get the object based data
                Serializable persistentData = null;
                byte[] pDataBytes = rs.getBytes("persistentData");
                if (pDataBytes != null) {
                    objectIn = new TCCLObjectInputStream(new ByteArrayInputStream(pDataBytes));
                    persistentData = (Serializable) objectIn.readObject();
                }

                stepEx = new StepExecutionImpl(jobexecid, stepexecid);

                stepEx.setBatchStatus(BatchStatus.valueOf(batchstatus));
                stepEx.setExitStatus(exitstatus);
                stepEx.setStepName(stepname);
                stepEx.setReadCount(readCount);
                stepEx.setWriteCount(writeCount);
                stepEx.setCommitCount(commitCount);
                stepEx.setRollbackCount(rollbackCount);
                stepEx.setReadSkipCount(readSkipCount);
                stepEx.setProcessSkipCount(processSkipCount);
                stepEx.setFilterCount(filterCount);
                stepEx.setWriteSkipCount(writeSkipCount);
                stepEx.setStartTime(startTS);
                stepEx.setEndTime(endTS);
                stepEx.setPersistentUserData(persistentData);


            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } catch (IOException e) {
            throw new PersistenceException(e);
        } catch (ClassNotFoundException e) {
            throw new PersistenceException(e);
        } finally {
            cleanupConnection(conn, rs, statement);
        }

        return stepEx;
    }

	@Override
	public void updateBatchStatusOnly(long key, BatchStatus batchStatus, Timestamp updatets) {
		Connection conn = null;
		PreparedStatement statement = null;
		ByteArrayOutputStream baos = null;
		ObjectOutputStream oout = null;
		byte[] b;

		try {
			conn = getConnection();
			statement = conn.prepareStatement("update executioninstancedata set batchstatus = ?, updatetime = ? where jobexecid = ?");
			statement.setString(1, batchStatus.name());
			statement.setTimestamp(2, updatets);
			statement.setLong(3, key);
			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new PersistenceException(e);
		} finally {
			if (baos != null) {
				try {
					baos.close();
				} catch (IOException e) {
					throw new PersistenceException(e);
				}
			}
			if (oout != null) {
				try {
					oout.close();
				} catch (IOException e) {
					throw new PersistenceException(e);
				}
			}
			cleanupConnection(conn, null, statement);
		}
	}

	@Override
	public void updateWithFinalExecutionStatusesAndTimestamps(long key,
			BatchStatus batchStatus, String exitStatus, Timestamp updatets) {
		// TODO Auto-generated methddod stub
		Connection conn = null;
		PreparedStatement statement = null;
		ByteArrayOutputStream baos = null;
		ObjectOutputStream oout = null;
		byte[] b;

		try {
			conn = getConnection();
			statement = conn.prepareStatement("update executioninstancedata set batchstatus = ?, exitstatus = ?, endtime = ?, updatetime = ? where jobexecid = ?");

			statement.setString(1, batchStatus.name());
			statement.setString(2, exitStatus);
			statement.setTimestamp(3, updatets);
			statement.setTimestamp(4, updatets);
			statement.setLong(5, key);

			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new PersistenceException(e);
		} finally {
			if (baos != null) {
				try {
					baos.close();
				} catch (IOException e) {
					throw new PersistenceException(e);
				}
			}
			if (oout != null) {
				try {
					oout.close();
				} catch (IOException e) {
					throw new PersistenceException(e);
				}
			}
			cleanupConnection(conn, null, statement);
		}

	}

	public void markJobStarted(long key, Timestamp startTS) {
		Connection conn = null;
		PreparedStatement statement = null;
		ByteArrayOutputStream baos = null;
		ObjectOutputStream oout = null;

		try {
			conn = getConnection();
			statement = conn.prepareStatement("update executioninstancedata set batchstatus = ?, starttime = ?, updatetime = ? where jobexecid = ?");

			statement.setString(1, BatchStatus.STARTED.name());
			statement.setTimestamp(2, startTS);
			statement.setTimestamp(3, startTS);
			statement.setLong(4, key);

			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new PersistenceException(e);
		} finally {
			if (baos != null) {
				try {
					baos.close();
				} catch (IOException e) {
					throw new PersistenceException(e);
				}
			}
			if (oout != null) {
				try {
					oout.close();
				} catch (IOException e) {
					throw new PersistenceException(e);
				}
			}
			cleanupConnection(conn, null, statement);
		}
	}


	@Override
	public IJobExecution jobOperatorGetJobExecution(long jobExecutionId) {
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		Timestamp createtime = null;
		Timestamp starttime = null;
		Timestamp endtime = null;
		Timestamp updatetime = null;
		long instanceId = 0;
		String batchStatus = null;
		String exitStatus = null;
		JobOperatorJobExecution jobEx = null;
		ObjectInputStream objectIn = null;
		String jobName = null;

		try {
			conn = getConnection();
			statement = conn.prepareStatement("select A.createtime, A.starttime, A.endtime, A.updatetime, A.parameters, A.jobinstanceid, A.batchstatus, A.exitstatus, B.name from executioninstancedata as A inner join jobinstancedata as B on A.jobinstanceid = B.jobinstanceid where jobexecid = ?"); 
			statement.setLong(1, jobExecutionId);
			rs = statement.executeQuery();
			while (rs.next()) {
				createtime = rs.getTimestamp("createtime");
				starttime = rs.getTimestamp("starttime");
				endtime = rs.getTimestamp("endtime");
				updatetime = rs.getTimestamp("updatetime");
				instanceId = rs.getLong("jobinstanceid");

				// get the object based data
				batchStatus = rs.getString("batchstatus");
				exitStatus = rs.getString("exitstatus");

				// get the object based data
				Properties params = null;
				byte[] buf = rs.getBytes("parameters");
				params = (Properties)deserializeObject(buf);

				jobName = rs.getString("name");

				jobEx = new JobOperatorJobExecution(jobExecutionId, instanceId);
				jobEx.setCreateTime(createtime);
				jobEx.setStartTime(starttime);
				jobEx.setEndTime(endtime);
				jobEx.setJobParameters(params);
				jobEx.setLastUpdateTime(updatetime);
				jobEx.setBatchStatus(batchStatus);
				jobEx.setExitStatus(exitStatus);
				jobEx.setJobName(jobName);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			throw new PersistenceException(e);
		} catch (ClassNotFoundException e) {
			throw new PersistenceException(e);
		} finally {
			if (objectIn != null) {
				try {
					objectIn.close();
				} catch (IOException e) {
					throw new PersistenceException(e);
				}
			}
			cleanupConnection(conn, rs, statement);
		}
		return jobEx;
	}

	@Override
	public List<IJobExecution> jobOperatorGetJobExecutions(long jobInstanceId) {
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		Timestamp createtime = null;
		Timestamp starttime = null;
		Timestamp endtime = null;
		Timestamp updatetime = null;
		long jobExecutionId = 0;
		long instanceId = 0;
		String batchStatus = null;
		String exitStatus = null;
		String jobName = null;
		List<IJobExecution> data = new ArrayList<IJobExecution>();
		JobOperatorJobExecution jobEx = null;
		ObjectInputStream objectIn = null;

		try {
			conn = getConnection();
			statement = conn.prepareStatement("select A.jobexecid, A.createtime, A.starttime, A.endtime, A.updatetime, A.parameters, A.batchstatus, A.exitstatus, B.name from executioninstancedata as A inner join jobinstancedata as B ON A.jobinstanceid = B.jobinstanceid where A.jobinstanceid = ?"); 
			statement.setLong(1, jobInstanceId);
			rs = statement.executeQuery();
			while (rs.next()) {
				jobExecutionId = rs.getLong("jobexecid");
				createtime = rs.getTimestamp("createtime");
				starttime = rs.getTimestamp("starttime");
				endtime = rs.getTimestamp("endtime");
				updatetime = rs.getTimestamp("updatetime");
				batchStatus = rs.getString("batchstatus");
				exitStatus = rs.getString("exitstatus");
				jobName = rs.getString("name");

				// get the object based data
				byte[] buf = rs.getBytes("parameters");
				Properties params = (Properties)deserializeObject(buf);

				jobEx = new JobOperatorJobExecution(jobExecutionId, instanceId);
				jobEx.setCreateTime(createtime);
				jobEx.setStartTime(starttime);
				jobEx.setEndTime(endtime);
				jobEx.setLastUpdateTime(updatetime);
				jobEx.setBatchStatus(batchStatus);
				jobEx.setExitStatus(exitStatus);
				jobEx.setJobName(jobName);

				data.add(jobEx);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			throw new PersistenceException(e);
		} catch (ClassNotFoundException e) {
			throw new PersistenceException(e);
		} finally {
			if (objectIn != null) {
				try {
					objectIn.close();
				} catch (IOException e) {
					throw new PersistenceException(e);
				}
			}
			cleanupConnection(conn, rs, statement);
		}
		return data;
	}

	@Override
	public Set<Long> jobOperatorGetRunningExecutions(String jobName){
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		Set<Long> executionIds = new HashSet<Long>();

		try {
			conn = getConnection();
			statement = conn.prepareStatement("SELECT A.jobexecid FROM executioninstancedata AS A INNER JOIN jobinstancedata AS B ON A.jobinstanceid = B.jobinstanceid WHERE A.batchstatus IN (?,?,?) AND B.name = ?"); 
			statement.setString(1, BatchStatus.STARTED.name());
			statement.setString(2, BatchStatus.STARTING.name());
			statement.setString(3, BatchStatus.STOPPING.name());
			statement.setString(4, jobName);
			rs = statement.executeQuery();
			while (rs.next()) {
				executionIds.add(rs.getLong("jobexecid"));
			}

		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}
		return executionIds;		
	}

	@Override
	public String getJobCurrentTag(long jobInstanceId) {
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		String apptag = null;

		try {
			conn = getConnection();
			statement = conn.prepareStatement(SELECT_JOBINSTANCEDATA_APPTAG); 
			statement.setLong(1, jobInstanceId);
			rs = statement.executeQuery();
			while (rs.next()) {
				apptag = rs.getString(APPTAG);
			}

		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}

		return apptag;
	}

	@Override
	public void purge(String apptag) {

		logger.entering(CLASSNAME, "purge", apptag);
		String deleteJobs = "DELETE FROM jobinstancedata WHERE apptag = ?";
		String deleteJobExecutions = "DELETE FROM executioninstancedata " 
				+ "WHERE jobexecid IN (" 
				+ "SELECT B.jobexecid FROM jobinstancedata AS A INNER JOIN executioninstancedata AS B " 
				+ "ON A.jobinstanceid = B.jobinstanceid " 
				+ "WHERE A.apptag = ?)";
		String deleteStepExecutions = "DELETE FROM stepexecutioninstancedata " 
				+ "WHERE stepexecid IN ("
				+ "SELECT C.stepexecid FROM jobinstancedata AS A INNER JOIN executioninstancedata AS B "
				+ "ON A.jobinstanceid = B.jobinstanceid INNER JOIN stepexecutioninstancedata AS C "
				+ "ON B.jobexecid = C.jobexecid "
				+ "WHERE A.apptag = ?)";

		Connection conn = null;
		PreparedStatement statement = null;
		try {
			conn = getConnection();
			statement = conn.prepareStatement(deleteStepExecutions);
			statement.setString(1, apptag);
			statement.executeUpdate();

			statement = conn.prepareStatement(deleteJobExecutions);
			statement.setString(1, apptag);
			statement.executeUpdate();

			statement = conn.prepareStatement(deleteJobs);
			statement.setString(1, apptag);
			statement.executeUpdate();

		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {

			cleanupConnection(conn, null, statement);
		}
		logger.exiting(CLASSNAME, "purge");

	}

	@Override
	public JobStatus getJobStatusFromExecution(long executionId) {

		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		JobStatus retVal = null;

		try {
			conn = getConnection();
			statement = conn.prepareStatement("select A.obj from jobstatus as A inner join " + 
					"executioninstancedata as B on A.id = B.jobinstanceid where B.jobexecid = ?");
			statement.setLong(1, executionId);
			rs = statement.executeQuery();
			byte[] buf = null;
			if (rs.next()) {
				buf = rs.getBytes("obj");
			}
			retVal = (JobStatus)deserializeObject(buf);
		} catch (Exception e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}
		logger.exiting(CLASSNAME, "executeQuery");
		return retVal;	
	}

	public long getJobInstanceIdByExecutionId(long executionId) throws NoSuchJobExecutionException {
		long instanceId= 0;

		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;

		try {
			conn = getConnection();
			statement = conn.prepareStatement("select jobinstanceid from executioninstancedata where jobexecid = ?");
			statement.setObject(1, executionId);
			rs = statement.executeQuery();
			if (rs.next()) {
				instanceId = rs.getLong("jobinstanceid");
			} else {
				String msg = "Did not find job instance associated with executionID =" + executionId;
				logger.fine(msg);
				throw new NoSuchJobExecutionException(msg);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}

		return instanceId;
	}

	/**
	 * This method is used to serialized an object saved into a table BLOB field.
	 *  
	 * @param theObject the object to be serialized
	 * @return a object byte array
	 * @throws IOException
	 */
	private byte[] serializeObject(Serializable theObject) throws IOException {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oout = new ObjectOutputStream(baos);
		oout.writeObject(theObject);
		byte[] data = baos.toByteArray();
		baos.close();
		oout.close();

		return data;
	}

	/**
	 * This method is used to de-serialized a table BLOB field to its original object form.
	 * 
	 * @param buffer the byte array save a BLOB
	 * @return the object saved as byte array
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private Serializable deserializeObject(byte[] buffer) throws IOException, ClassNotFoundException {

		Serializable theObject = null;
		ObjectInputStream objectIn = null;

		if (buffer != null) {
			objectIn = new ObjectInputStream(new ByteArrayInputStream(buffer));
			theObject = (Serializable)objectIn.readObject();
			objectIn.close();
		}
		return theObject;
	}

	/* (non-Javadoc)
	 * @see com.ibm.jbatch.container.services.IPersistenceManagerService#createJobInstance(java.lang.String, java.lang.String, java.lang.String, java.util.Properties)
	 */
	@Override
	public JobInstance createSubJobInstance(String name, String apptag) {
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		JobInstanceImpl jobInstance = null;

		try {
			conn = getConnection();
			statement = conn.prepareStatement("INSERT INTO jobinstancedata (name, apptag) VALUES(?, ?)", Statement.RETURN_GENERATED_KEYS);
			statement.setString(1, name);
			statement.setString(2, apptag); 
			statement.executeUpdate();
			rs = statement.getGeneratedKeys();
			if(rs.next()) {
				long jobInstanceID = rs.getLong(1);
				jobInstance = new JobInstanceImpl(jobInstanceID);
				jobInstance.setJobName(name);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}
		return jobInstance;
	}

	/* (non-Javadoc)
	 * @see com.ibm.jbatch.container.services.IPersistenceManagerService#createJobInstance(java.lang.String, java.lang.String, java.lang.String, java.util.Properties)
	 */
	@Override
	public JobInstance createJobInstance(String name, String apptag, String jobXml) {
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		JobInstanceImpl jobInstance = null;

		try {
			conn = getConnection();
			statement = conn.prepareStatement("INSERT INTO jobinstancedata (name, apptag) VALUES(?, ?)", Statement.RETURN_GENERATED_KEYS);
			statement.setString(1, name);
			statement.setString(2, apptag);
			statement.executeUpdate();
			rs = statement.getGeneratedKeys();
			if(rs.next()) {
				long jobInstanceID = rs.getLong(1);
				jobInstance = new JobInstanceImpl(jobInstanceID, jobXml);
				jobInstance.setJobName(name);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}
		return jobInstance;
	}

	/* (non-Javadoc)
	 * @see com.ibm.jbatch.container.services.IPersistenceManagerService#createJobExecution(com.ibm.jbatch.container.jsl.JobNavigator, javax.batch.runtime.JobInstance, java.util.Properties, com.ibm.jbatch.container.context.impl.JobContextImpl)
	 */
	@Override
	public RuntimeJobExecution createJobExecution(JobInstance jobInstance, Properties jobParameters, BatchStatus batchStatus) {
		Timestamp now = new Timestamp(System.currentTimeMillis());
		long newExecutionId = createRuntimeJobExecutionEntry(jobInstance, jobParameters, batchStatus, now);
		RuntimeJobExecution jobExecution = new RuntimeJobExecution(jobInstance, newExecutionId);
		jobExecution.setBatchStatus(batchStatus.name());
		jobExecution.setCreateTime(now);
		jobExecution.setLastUpdateTime(now);
		return jobExecution;
	}

	private long createRuntimeJobExecutionEntry(JobInstance jobInstance, Properties jobParameters, BatchStatus batchStatus, Timestamp timestamp) {
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		long newJobExecutionId = 0L;
		try {
			conn = getConnection();
			statement = conn.prepareStatement("INSERT INTO executioninstancedata (jobinstanceid, createtime, updatetime, batchstatus, parameters) VALUES(?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			statement.setLong(1, jobInstance.getInstanceId());
			statement.setTimestamp(2, timestamp);
			statement.setTimestamp(3, timestamp);
			statement.setString(4, batchStatus.name());
			statement.setObject(5, serializeObject(jobParameters));
			statement.executeUpdate();
			rs = statement.getGeneratedKeys();
			if(rs.next()) {
				newJobExecutionId = rs.getLong(1);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}
		return newJobExecutionId;
	}

	@Override
	public RuntimeFlowInSplitExecution createFlowInSplitExecution(JobInstance jobInstance, BatchStatus batchStatus) {
		Timestamp now = new Timestamp(System.currentTimeMillis());
		long newExecutionId = createRuntimeJobExecutionEntry(jobInstance, null, batchStatus, now);
		RuntimeFlowInSplitExecution flowExecution = new RuntimeFlowInSplitExecution(jobInstance, newExecutionId);
		flowExecution.setBatchStatus(batchStatus.name());
		flowExecution.setCreateTime(now);
		flowExecution.setLastUpdateTime(now);
		return flowExecution;
	}

	/* (non-Javadoc)
	 * @see com.ibm.jbatch.container.services.IPersistenceManagerService#createStepExecution(long, com.ibm.jbatch.container.context.impl.StepContextImpl)
	 */
	@Override
	public StepExecutionImpl createStepExecution(long rootJobExecId, StepContextImpl stepContext) {
		StepExecutionImpl stepExecution = null;
		String batchStatus = stepContext.getBatchStatus() == null ? BatchStatus.STARTING.name() : stepContext.getBatchStatus().name();
		String exitStatus = stepContext.getExitStatus();
		String stepName = stepContext.getStepName();

		long readCount = 0;
		long writeCount = 0;
		long commitCount = 0;
		long rollbackCount = 0;
		long readSkipCount = 0;
		long processSkipCount = 0;
		long filterCount = 0;
		long writeSkipCount = 0;
		Timestamp startTime = stepContext.getStartTimeTS();
		Timestamp endTime = stepContext.getEndTimeTS();

		Metric[] metrics = stepContext.getMetrics();
		for (int i = 0; i < metrics.length; i++) {
			if (metrics[i].getType().equals(MetricImpl.MetricType.READ_COUNT)) {
				readCount = metrics[i].getValue();
			} else if (metrics[i].getType().equals(MetricImpl.MetricType.WRITE_COUNT)) {
				writeCount = metrics[i].getValue();	
			} else if (metrics[i].getType().equals(MetricImpl.MetricType.PROCESS_SKIP_COUNT)) {
				processSkipCount = metrics[i].getValue();	
			} else if (metrics[i].getType().equals(MetricImpl.MetricType.COMMIT_COUNT)) {
				commitCount = metrics[i].getValue();
			} else if (metrics[i].getType().equals(MetricImpl.MetricType.ROLLBACK_COUNT)) {
				rollbackCount = metrics[i].getValue();	
			} else if (metrics[i].getType().equals(MetricImpl.MetricType.READ_SKIP_COUNT)) {
				readSkipCount = metrics[i].getValue();
			} else if (metrics[i].getType().equals(MetricImpl.MetricType.FILTER_COUNT)) {
				filterCount = metrics[i].getValue();	
			} else if (metrics[i].getType().equals(MetricImpl.MetricType.WRITE_SKIPCOUNT)) {
				writeSkipCount = metrics[i].getValue();	
			}		
		}
		Serializable persistentData = stepContext.getPersistentUserData();

		stepExecution = createStepExecution(rootJobExecId, batchStatus, exitStatus, stepName, readCount, 
				writeCount, commitCount, rollbackCount, readSkipCount, processSkipCount, filterCount, writeSkipCount, startTime,
				endTime, persistentData);

		return stepExecution;
	}


	private StepExecutionImpl createStepExecution(long rootJobExecId,  String batchStatus, String exitStatus, String stepName, long readCount, 
			long writeCount, long commitCount, long rollbackCount, long readSkipCount, long processSkipCount, long filterCount,
			long writeSkipCount, Timestamp startTime, Timestamp endTime, Serializable persistentData) {

		logger.entering(CLASSNAME, "createStepExecution", new Object[] {rootJobExecId, batchStatus, exitStatus==null ? "<null>" : exitStatus, stepName, readCount, 
				writeCount, commitCount, rollbackCount, readSkipCount, processSkipCount, filterCount, writeSkipCount, startTime == null ? "<null>" : startTime,
						endTime==null ? "<null>" :endTime , persistentData==null ? "<null>" : persistentData});

		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		StepExecutionImpl stepExecution = null;
		String query = "INSERT INTO stepexecutioninstancedata (jobexecid, batchstatus, exitstatus, stepname, readcount," 
				+ "writecount, commitcount, rollbackcount, readskipcount, processskipcount, filtercount, writeskipcount, starttime,"
				+ "endtime, persistentdata) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		try {
			conn = getConnection();
			statement = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
			statement.setLong(1, rootJobExecId);
			statement.setString(2, batchStatus);
			statement.setString(3, exitStatus);
			statement.setString(4, stepName);
			statement.setLong(5, readCount);
			statement.setLong(6, writeCount);
			statement.setLong(7, commitCount);
			statement.setLong(8, rollbackCount);
			statement.setLong(9, readSkipCount);
			statement.setLong(10, processSkipCount);
			statement.setLong(11, filterCount);
			statement.setLong(12, writeSkipCount);
			statement.setTimestamp(13, startTime);
			statement.setTimestamp(14, endTime);
			statement.setObject(15, serializeObject(persistentData));

			statement.executeUpdate();
			rs = statement.getGeneratedKeys();
			if(rs.next()) {
				long stepExecutionId = rs.getLong(1);
				stepExecution = new StepExecutionImpl(rootJobExecId, stepExecutionId);
				stepExecution.setStepName(stepName);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, null, statement);
		}
		logger.exiting(CLASSNAME, "createStepExecution");

		return stepExecution;
	}

	/* (non-Javadoc)
	 * @see com.ibm.jbatch.container.services.IPersistenceManagerService#updateStepExecution(long, com.ibm.jbatch.container.context.impl.StepContextImpl)
	 */
	@Override
	public void updateStepExecution(long rootJobExecId, StepContextImpl stepContext) {
		long stepExecutionId = stepContext.getStepExecutionId();
		String batchStatus = stepContext.getBatchStatus() == null ? BatchStatus.STARTING.name() : stepContext.getBatchStatus().name();
		String exitStatus = stepContext.getExitStatus();
		String stepName = stepContext.getStepName();

		long readCount = 0;
		long writeCount = 0;
		long commitCount = 0;
		long rollbackCount = 0;
		long readSkipCount = 0;
		long processSkipCount = 0;
		long filterCount = 0;
		long writeSkipCount = 0;
		Timestamp startTime = stepContext.getStartTimeTS();
		Timestamp endTime = stepContext.getEndTimeTS();

		Metric[] metrics = stepContext.getMetrics();
		for (int i = 0; i < metrics.length; i++) {
			if (metrics[i].getType().equals(MetricImpl.MetricType.READ_COUNT)) {
				readCount = metrics[i].getValue();
			} else if (metrics[i].getType().equals(MetricImpl.MetricType.WRITE_COUNT)) {
				writeCount = metrics[i].getValue();	
			} else if (metrics[i].getType().equals(MetricImpl.MetricType.PROCESS_SKIP_COUNT)) {
				processSkipCount = metrics[i].getValue();	
			} else if (metrics[i].getType().equals(MetricImpl.MetricType.COMMIT_COUNT)) {
				commitCount = metrics[i].getValue();
			} else if (metrics[i].getType().equals(MetricImpl.MetricType.ROLLBACK_COUNT)) {
				rollbackCount = metrics[i].getValue();	
			} else if (metrics[i].getType().equals(MetricImpl.MetricType.READ_SKIP_COUNT)) {
				readSkipCount = metrics[i].getValue();
			} else if (metrics[i].getType().equals(MetricImpl.MetricType.FILTER_COUNT)) {
				filterCount = metrics[i].getValue();	
			} else if (metrics[i].getType().equals(MetricImpl.MetricType.WRITE_SKIPCOUNT)) {
				writeSkipCount = metrics[i].getValue();	
			}
		}
		Serializable persistentData = stepContext.getPersistentUserData();

		updateStepExecution(stepExecutionId, rootJobExecId, batchStatus, exitStatus, stepName, readCount, 
				writeCount, commitCount, rollbackCount, readSkipCount, processSkipCount, filterCount, 
				writeSkipCount, startTime, endTime, persistentData);

	}


	private void updateStepExecution(long stepExecutionId, long jobExecId,  String batchStatus, String exitStatus, String stepName, long readCount, 
			long writeCount, long commitCount, long rollbackCount, long readSkipCount, long processSkipCount, long filterCount,
			long writeSkipCount, Timestamp startTime, Timestamp endTime, Serializable persistentData) {

		logger.entering(CLASSNAME, "updateStepExecution", new Object[] {stepExecutionId, jobExecId, batchStatus, exitStatus==null ? "<null>" : exitStatus, stepName, readCount, 
				writeCount, commitCount, rollbackCount, readSkipCount, processSkipCount, filterCount, writeSkipCount, startTime==null ? "<null>" : startTime,
						endTime==null ? "<null>" : endTime, persistentData==null ? "<null>" : persistentData});

		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		StepExecutionImpl stepExecution = null;
		String query = "UPDATE stepexecutioninstancedata SET jobexecid = ?, batchstatus = ?, exitstatus = ?, stepname = ?,  readcount = ?," 
				+ "writecount = ?, commitcount = ?, rollbackcount = ?, readskipcount = ?, processskipcount = ?, filtercount = ?, writeskipcount = ?,"
				+ " starttime = ?, endtime = ?, persistentdata = ? WHERE stepexecid = ?";

		try {
			conn = getConnection();
			statement = conn.prepareStatement(query);
			statement.setLong(1, jobExecId);
			statement.setString(2, batchStatus);
			statement.setString(3, exitStatus);
			statement.setString(4, stepName);
			statement.setLong(5, readCount);
			statement.setLong(6, writeCount);
			statement.setLong(7, commitCount);
			statement.setLong(8, rollbackCount);
			statement.setLong(9, readSkipCount);
			statement.setLong(10, processSkipCount);
			statement.setLong(11, filterCount);
			statement.setLong(12, writeSkipCount);
			statement.setTimestamp(13, startTime);
			statement.setTimestamp(14, endTime);
			statement.setObject(15, serializeObject(persistentData));
			statement.setLong(16, stepExecutionId); 
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, null, statement);
		}

		logger.exiting(CLASSNAME, "updateStepExecution");
	}

	/* (non-Javadoc)
	 * @see com.ibm.jbatch.container.services.IPersistenceManagerService#createJobStatus(long)
	 */
	@Override
	public JobStatus createJobStatus(long jobInstanceId) {
		logger.entering(CLASSNAME, "createJobStatus", jobInstanceId);
		Connection conn = null;
		PreparedStatement statement = null;
		JobStatus jobStatus = new JobStatus(jobInstanceId);
		try {
			conn = getConnection();
			statement = conn.prepareStatement("INSERT INTO jobstatus (id, obj) VALUES(?, ?)");
			statement.setLong(1, jobInstanceId);
			statement.setBytes(2, serializeObject(jobStatus));
			statement.executeUpdate();

		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, null, statement);
		}
		logger.exiting(CLASSNAME, "createJobStatus");
		return jobStatus;
	}

	/* (non-Javadoc)
	 * @see com.ibm.jbatch.container.services.IPersistenceManagerService#getJobStatus(long)
	 */
	@Override
	public JobStatus getJobStatus(long instanceId) {
		logger.entering(CLASSNAME, "getJobStatus", instanceId);
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		RuntimeJobExecution jobExecution = null;
		String query = "SELECT obj FROM jobstatus WHERE id = ?";
		JobStatus jobStatus = null;

		try {
			conn = getConnection();
			statement = conn.prepareStatement(query);
			statement.setLong(1, instanceId);
			rs = statement.executeQuery();
			if(rs.next()) {
				jobStatus = (JobStatus)deserializeObject(rs.getBytes(1));
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			throw new PersistenceException(e);
		} catch (ClassNotFoundException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}
		logger.exiting(CLASSNAME, "getJobStatus", jobStatus);
		return jobStatus;
	}

	/* (non-Javadoc)
	 * @see com.ibm.jbatch.container.services.IPersistenceManagerService#updateJobStatus(long, com.ibm.jbatch.container.status.JobStatus)
	 */
	@Override
	public void updateJobStatus(long instanceId, JobStatus jobStatus) {
		logger.entering(CLASSNAME, "updateJobStatus", new Object[] {instanceId, jobStatus});
		Connection conn = null;
		PreparedStatement statement = null;
		try {
			conn = getConnection();
			statement = conn.prepareStatement("UPDATE jobstatus SET obj = ? WHERE id = ?");
			statement.setBytes(1, serializeObject(jobStatus));
			statement.setLong(2, instanceId);
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, null, statement);
		}
		logger.exiting(CLASSNAME, "updateJobStatus");
	}	

	/* (non-Javadoc)
	 * @see com.ibm.jbatch.container.services.IPersistenceManagerService#createStepStatus(long)
	 */
	@Override
	public StepStatus createStepStatus(long stepExecId) {
		logger.entering(CLASSNAME, "createStepStatus", stepExecId);
		Connection conn = null;
		PreparedStatement statement = null;
		StepStatus stepStatus = new StepStatus(stepExecId);
		try {
			conn = getConnection();
			statement = conn.prepareStatement("INSERT INTO stepstatus (id, obj) VALUES(?, ?)");
			statement.setLong(1, stepExecId);
			statement.setBytes(2, serializeObject(stepStatus));
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, null, statement);
		}
		logger.exiting(CLASSNAME, "createStepStatus");
		return stepStatus;
	}

	/* (non-Javadoc)
	 * @see com.ibm.jbatch.container.services.IPersistenceManagerService#getStepStatus(long, java.lang.String)
	 */
	@Override
	public StepStatus getStepStatus(long instanceId, String stepName) {
		logger.entering(CLASSNAME, "getStepStatus", new Object[] {instanceId, stepName});
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		RuntimeJobExecution jobExecution = null;
		String query = "SELECT obj FROM stepstatus WHERE id IN ("
				+ "SELECT B.stepexecid FROM executioninstancedata A INNER JOIN stepexecutioninstancedata B ON A.jobexecid = B.jobexecid " 
				+ "WHERE A.jobinstanceid = ? and B.stepname = ?)";
		StepStatus stepStatus = null;

		try {
			conn = getConnection();
			statement = conn.prepareStatement(query);
			statement.setLong(1, instanceId);
			statement.setString(2, stepName);
			rs = statement.executeQuery();
			if(rs.next()) {
				stepStatus = (StepStatus)deserializeObject(rs.getBytes(1));
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			throw new PersistenceException(e);
		} catch (ClassNotFoundException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}
		logger.exiting(CLASSNAME, "getStepStatus", stepStatus==null ? "<null>" : stepStatus);
		return stepStatus;
	}

	/* (non-Javadoc)
	 * @see com.ibm.jbatch.container.services.IPersistenceManagerService#updateStepStatus(long, com.ibm.jbatch.container.status.StepStatus)
	 */
	@Override
	public void updateStepStatus(long stepExecutionId, StepStatus stepStatus) {
		logger.entering(CLASSNAME, "updateStepStatus", new Object[] {stepExecutionId, stepStatus});
		Connection conn = null;
		PreparedStatement statement = null;
		try {
			conn = getConnection();
			statement = conn.prepareStatement("UPDATE stepstatus SET obj = ? WHERE id = ?");
			statement.setBytes(1, serializeObject(stepStatus));
			statement.setLong(2, stepExecutionId);
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, null, statement);
		}
		logger.exiting(CLASSNAME, "updateStepStatus");
	}

	/* (non-Javadoc)
	 * @see com.ibm.jbatch.container.services.IPersistenceManagerService#getTagName(long)
	 */
	@Override
	public String getTagName(long jobExecutionId) {
		logger.entering(CLASSNAME, "getTagName", jobExecutionId);
		String apptag = null;
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		String query = "SELECT A.apptag FROM jobinstancedata A INNER JOIN executioninstancedata B ON A.jobinstanceid = B.jobinstanceid"
				+ " WHERE B.jobexecid = ?";
		try {
			conn = getConnection();
			statement = conn.prepareStatement(query);
			statement.setLong(1, jobExecutionId);
			rs = statement.executeQuery();
			if(rs.next()) {
				apptag = rs.getString(1);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}
		logger.exiting(CLASSNAME, "getTagName");
		return apptag;
	}

	@Override
	public long getMostRecentExecutionId(long jobInstanceId) {
		logger.entering(CLASSNAME, "getMostRecentExecutionId", jobInstanceId);
		long mostRecentId = -1;
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		String query = "SELECT jobexecid FROM executioninstancedata WHERE jobinstanceid = ? ORDER BY createtime DESC";

		try {
			conn = getConnection();
			statement = conn.prepareStatement(query);
			statement.setLong(1, jobInstanceId);
			rs = statement.executeQuery();
			if(rs.next()) {
				mostRecentId = rs.getLong(1);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}
		logger.exiting(CLASSNAME, "getMostRecentExecutionId");
		return mostRecentId;
	}

	@Override
	public void shutdown() throws BatchContainerServiceException {
		// TODO Auto-generated method stub

	}


}
