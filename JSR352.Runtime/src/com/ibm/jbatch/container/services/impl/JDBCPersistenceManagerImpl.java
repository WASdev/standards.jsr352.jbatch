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
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator.BatchStatus;
import javax.batch.runtime.JobExecution;
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
import com.ibm.jbatch.container.jobinstance.JobOperatorJobExecutionImpl;
import com.ibm.jbatch.container.jobinstance.StepExecutionImpl;
import com.ibm.jbatch.container.persistence.CheckpointData;
import com.ibm.jbatch.container.persistence.CheckpointDataKey;
import com.ibm.jbatch.container.status.JobStatus;
import com.ibm.jbatch.container.status.JobStatusKey;
import com.ibm.jbatch.container.status.StepStatus;
import com.ibm.jbatch.container.status.StepStatusKey;
import com.ibm.jbatch.container.util.TCCLObjectInputStream;
import com.ibm.jbatch.spi.IBatchConfig;

public class JDBCPersistenceManagerImpl extends AbstractPersistenceManagerImpl {

	private static final String CLASSNAME = JDBCPersistenceManagerImpl.class.getName();

	private final static Logger logger = Logger.getLogger(CLASSNAME);
	
	private static final String JOBSTATUS_TABLE = "JOBSTATUS";
	private static final String STEPSTATUS_TABLE = "STEPSTATUS";
	private static final String CHECKPOINTDATA_TABLE = "CHECKPOINTDATA";
	private static final String JOBINSTANCEDATA_TABLE = "JOBINSTANCEDATA";
	private static final String EXECUTIONINSTANCEDATA_TABLE = "EXECUTIONINSTANCEDATA";
	private static final String STEPEXECUTIONINSTANCEDATA_TABLE = "STEPEXECUTIONINSTANCEDATA";
	
	private static final String CREATE_TAB_JOBSTATUS = "CREATE TABLE JOBSTATUS("
			+ "id BIGINT,obj BLOB)";
	private static final String CREATE_TAB_STEPSTATUS = "CREATE TABLE STEPSTATUS("
			+ "id VARCHAR(512),obj BLOB)";
	private static final String CREATE_TAB_CHECKPOINTDATA = "CREATE TABLE CHECKPOINTDATA("
			+ "id VARCHAR(512),obj BLOB)";
	private static final String CREATE_TAB_JOBINSTANCEDATA = "CREATE TABLE JOBINSTANCEDATA("
			+ "id VARCHAR(512), name VARCHAR(512))";
	private static final String CREATE_TAB_EXECUTIONINSTANCEDATA = "CREATE TABLE EXECUTIONINSTANCEDATA("
			+ "id VARCHAR(512),"
			+ "createtime TIMESTAMP,"
			+ "starttime TIMESTAMP,"
			+ "endtime TIMESTAMP,"
			+ "updatetime TIMESTAMP,"
			+ "parameters BLOB,"
			+ "jobinstanceid VARCHAR(512),"
			+ "batchstatus VARCHAR(512),"
			+ "exitstatus VARCHAR(512))";
	private static final String CREATE_TAB_STEPEXECUTIONINSTANCEDATA = "CREATE TABLE STEPEXECUTIONINSTANCEDATA("
			+ "id VARCHAR(512),"
			+ "jobexecid VARCHAR(512),"
			+ "stepexecid VARCHAR(512),"
			+ "batchstatus VARCHAR(512),"
			+ "exitstatus VARCHAR(512),"
			+ "stepname VARCHAR(512),"
			+ "readcount VARCHAR(512),"
			+ "writecount VARCHAR(512),"
			+ "commitcount VARCHAR(512),"
			+ "rollbackcount VARCHAR(512),"
			+ "readskipcount VARCHAR(512),"
			+ "processskipcount VARCHAR(512),"
			+ "filtercount VARCHAR(512),"
			+ "writeskipcount VARCHAR(512),"
			+ "startTime TIMESTAMP,endTime TIMESTAMP,"
			+ "persistentData BLOB)";
	
	private static final String INSERT_JOBSTATUS = "insert into jobstatus values(?, ?)";
	
	private static final String UPDATE_JOBSTATUS = "update jobstatus set obj = ? where id = ?";

	private static final String SELECT_JOBSTATUS = "select id, obj from jobstatus where id = ?";
	
	private static final String DELETE_JOBSTATUS = "delete from jobstatus where id = ?";

	private static final String INSERT_STEPSTATUS = "insert into stepstatus values(?, ?)";
	
	private static final String UPDATE_STEPSTATUS = "update stepstatus set obj = ? where id = ?";

	private static final String SELECT_STEPSTATUS = "select id, obj from stepstatus where id = ?";
	
	private static final String DELETE_STEPSTATUS = "delete from stepstatus where id = ?";

	private static final String INSERT_CHECKPOINTDATA = "insert into checkpointdata values(?, ?)";

	private static final String UPDATE_CHECKPOINTDATA = "update checkpointdata set obj = ? where id = ?";

	private static final String SELECT_CHECKPOINTDATA = "select id, obj from checkpointdata where id = ?";
	
	private static final String CREATE_CHECKPOINTDATA_INDEX = "create index chk_index on checkpointdata(id)";
	
	private static final String DELETE_CHECKPOINTDATA = "delete from checkpointdata where id = ?";
	
	// JOB OPERATOR QUERIES
	private static final String INSERT_JOBINSTANCEDATA = "insert into jobinstancedata values(?, ?)";
	
	private static final String INSERT_EXECUTIONDATA = "insert into executionInstanceData values(?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static final String SELECT_JOBINSTANCEDATA_COUNT = "select count(id) as jobinstancecount from jobinstancedata where name = ?";
	
	private static final String SELECT_JOBINSTANCEDATA_IDS = "select id from jobinstancedata where name = ?";
	
	private static final String SELECT_JOBINSTANCEDATA_NAMES = "select name from jobinstancedata";
	
	public static final String START_TIME = "starttime";
	public static final String CREATE_TIME = "createtime";
	public static final String END_TIME = "endtime";
	public static final String UPDATE_TIME = "updatetime";
	public static final String BATCH_STATUS = "batchstatus";
	public static final String EXIT_STATUS = "exitstatus";
	public static final String INSTANCE_ID = "instanceId";
	public static final String JOBEXEC_ID = "jobexecid";
	public static final String STEPEXEC_ID = "stepexecid";
	public static final String STEPCONTEXT = "stepcontext";


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
		super.init(batchConfig);
		
		logger.entering(CLASSNAME, "init", batchConfig);
		
		schema = batchConfig.getDatabaseConfigurationBean().getSchema();
		
		if (!batchConfig.isJ2seMode()) {
			jndiName = batchConfig.getDatabaseConfigurationBean().getJndiName();
			
			logger.log(Level.FINE, "JNDI name is {0}", jndiName);
			
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
			
			logger.log(Level.FINE, "driver: {0}, url: {1}", new Object[]{driver, url});
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
		
		logger.exiting(CLASSNAME, "init");
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

		logger.log(Level.WARNING, schema + " schema does not exists. Trying to create it.");
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
		
		createIfNotExists(JOBSTATUS_TABLE, CREATE_TAB_JOBSTATUS);
		createIfNotExists(STEPSTATUS_TABLE, CREATE_TAB_STEPSTATUS);

		createIfNotExists(CHECKPOINTDATA_TABLE, CREATE_TAB_CHECKPOINTDATA);
		executeStatement(CREATE_CHECKPOINTDATA_INDEX);
		createIfNotExists(JOBINSTANCEDATA_TABLE, CREATE_TAB_JOBINSTANCEDATA);

		createIfNotExists(EXECUTIONINSTANCEDATA_TABLE,
				CREATE_TAB_EXECUTIONINSTANCEDATA);
		createIfNotExists(STEPEXECUTIONINSTANCEDATA_TABLE,
				CREATE_TAB_STEPEXECUTIONINSTANCEDATA);
		
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
			logger.log(Level.WARNING, tableName + " table does not exists. Trying to create it.");
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
     * @see com.ibm.jbatch.container.services.impl.AbstractPersistenceManagerImpl#_createJobStatus(com.ibm.jbatch.container.status.JobStatusKey, com.ibm.jbatch.container.status.JobStatus)
     */
    @Override
	protected void _createJobStatus(JobStatusKey key, JobStatus value) {
    	logger.entering(CLASSNAME, "_createJobStatus", new Object[] {key, value});
    	executeInsert(key.getJobInstanceId(), value, INSERT_JOBSTATUS);
    	logger.exiting(CLASSNAME, "_createJobStatus");
	}

	/* (non-Javadoc)
	 * @see com.ibm.jbatch.container.services.impl.AbstractPersistenceManagerImpl#_getJobStatus(com.ibm.jbatch.container.status.JobStatusKey)
	 */
	@Override
	protected List<JobStatus> _getJobStatus(JobStatusKey key) {
		logger.entering(CLASSNAME, "_getJobStatus", key);
		List<JobStatus> jobStatuses = executeQuery(key.getJobInstanceId(), SELECT_JOBSTATUS);
		logger.exiting(CLASSNAME, "_getJobStatus", jobStatuses);
		return jobStatuses;
	}
    
    /* (non-Javadoc)
     * @see com.ibm.jbatch.container.services.impl.AbstractPersistenceManagerImpl#_updateJobStatus(com.ibm.jbatch.container.status.JobStatusKey, com.ibm.jbatch.container.status.JobStatus)
     */
    @Override
    protected void _updateJobStatus(JobStatusKey key, JobStatus value) {
    	logger.entering(CLASSNAME, "_updateJobStatus", new Object[] {key, value});
   		List<JobStatus> data = executeQuery(key.getJobInstanceId(), SELECT_JOBSTATUS);
   		if(data != null && !data.isEmpty()) {
    		executeUpdate(value, key.getJobInstanceId(), UPDATE_JOBSTATUS);
    	}
   		logger.exiting(CLASSNAME, "_updateJobStatus");
    }

    /* (non-Javadoc)
     * @see com.ibm.jbatch.container.services.impl.AbstractPersistenceManagerImpl#_deleteJobStatus(com.ibm.jbatch.container.status.JobStatusKey)
     */
    @Override
    protected void _deleteJobStatus(JobStatusKey key) {
    	logger.entering(CLASSNAME, "_deleteJobStatus", key);
    	executeDelete(key.getJobInstanceId(), DELETE_JOBSTATUS);
    	logger.exiting(CLASSNAME, "_deleteJobStatus");
    }

    /* (non-Javadoc)
     * @see com.ibm.jbatch.container.services.impl.AbstractPersistenceManagerImpl#_createStepStatus(com.ibm.jbatch.container.status.StepStatusKey, com.ibm.jbatch.container.status.StepStatus)
     */
    @Override
	protected void _createStepStatus(StepStatusKey key, StepStatus value) {
    	logger.entering(CLASSNAME, "_createStepStatus", new Object[] {key, value});
        executeInsert(key.getKeyPrimitive(), value, INSERT_STEPSTATUS);
        logger.exiting(CLASSNAME, "_createStepStatus");
	}

    /* (non-Javadoc)
     * @see com.ibm.jbatch.container.services.impl.AbstractPersistenceManagerImpl#_getStepStatus(com.ibm.jbatch.container.status.StepStatusKey)
     */
    @Override
	protected List<StepStatus> _getStepStatus(StepStatusKey key) {
    	logger.entering(CLASSNAME, "_getStepStatus", key);
    	List<StepStatus> stepStatuses = executeQuery(key.getKeyPrimitive(), SELECT_STEPSTATUS); 
    	logger.exiting(CLASSNAME, "_getStepStatus", stepStatuses);
    	return stepStatuses;
	}

    /* (non-Javadoc)
     * @see com.ibm.jbatch.container.services.impl.AbstractPersistenceManagerImpl#_updateStepStatus(com.ibm.jbatch.container.status.StepStatusKey, com.ibm.jbatch.container.status.StepStatus)
     */
    @Override
    protected void _updateStepStatus(StepStatusKey key, StepStatus value) {
    	logger.entering(CLASSNAME, "_updateStepStatus", new Object[] {key, value});
		List<StepStatus> data = executeQuery(key.getKeyPrimitive(), SELECT_STEPSTATUS);
		if(data != null && !data.isEmpty()) {
			executeUpdate(value, key.getKeyPrimitive(), UPDATE_STEPSTATUS);
		}
		logger.exiting(CLASSNAME, "_updateStepStatus");
    }
    
    /* (non-Javadoc)
     * @see com.ibm.jbatch.container.services.impl.AbstractPersistenceManagerImpl#_deleteStepStatus(com.ibm.jbatch.container.status.StepStatusKey)
     */
    @Override
    protected void _deleteStepStatus(StepStatusKey key) {
    	logger.entering(CLASSNAME, "_deleteStepStatus", key);
    	executeDelete(key.getKeyPrimitive(), DELETE_STEPSTATUS);
    	logger.exiting(CLASSNAME, "_deleteStepStatus");
    }

	/* (non-Javadoc)
	 * @see com.ibm.jbatch.container.services.impl.AbstractPersistenceManagerImpl#_createCheckpointData(com.ibm.ws.batch.container.checkpoint.CheckpointDataKey, com.ibm.ws.batch.container.checkpoint.CheckpointData)
	 */
	@Override
	protected void _createCheckpointData(CheckpointDataKey key, CheckpointData value) {
		logger.entering(CLASSNAME, "_createCheckpointData", new Object[] {key, value});
		executeInsert(key.getCommaSeparatedKey(), value, INSERT_CHECKPOINTDATA);
		logger.exiting(CLASSNAME, "_createCheckpointData");
	}

	/* (non-Javadoc)
	 * @see com.ibm.jbatch.container.services.impl.AbstractPersistenceManagerImpl#_getCheckpointData(com.ibm.ws.batch.container.checkpoint.CheckpointDataKey)
	 */
	@Override
	protected List<CheckpointData> _getCheckpointData(CheckpointDataKey key) {
		logger.entering(CLASSNAME, "_getCheckpointData", key);
		List<CheckpointData> checkpointData = executeQuery(key.getCommaSeparatedKey(), SELECT_CHECKPOINTDATA);
		logger.exiting(CLASSNAME, "_getCheckpointData", checkpointData);
		return checkpointData;
	}

	/* (non-Javadoc)
	 * @see com.ibm.jbatch.container.services.impl.AbstractPersistenceManagerImpl#_updateCheckpointData(com.ibm.ws.batch.container.checkpoint.CheckpointDataKey, com.ibm.ws.batch.container.checkpoint.CheckpointData)
	 */
	@Override
	protected void _updateCheckpointData(CheckpointDataKey key, CheckpointData value) {
		logger.entering(CLASSNAME, "_updateCheckpointData", new Object[] {key, value});
		List<CheckpointData> data = executeQuery(key.getCommaSeparatedKey(), SELECT_CHECKPOINTDATA);
		if(data != null && !data.isEmpty()) {
			executeUpdate(value, key.getCommaSeparatedKey(), UPDATE_CHECKPOINTDATA);
		} else {
		    _createCheckpointData(key, value);
		}
		logger.exiting(CLASSNAME, "_updateCheckpointData");
	}
	
	/* (non-Javadoc)
	 * @see com.ibm.jbatch.container.services.impl.AbstractPersistenceManagerImpl#_deleteCheckpointData(com.ibm.ws.batch.container.checkpoint.CheckpointDataKey)
	 */
	@Override
	protected void _deleteCheckpointData(CheckpointDataKey key) {
		executeDelete(key.getJobInstanceId(), DELETE_CHECKPOINTDATA);
		logger.entering(CLASSNAME, "_deleteCheckpointData", key);
		executeDelete(key.getCommaSeparatedKey(), DELETE_CHECKPOINTDATA);
		logger.exiting(CLASSNAME, "_deleteCheckpointData");
	}

	/**
	 * @return the database connection and sets it to the default schema JBATCH or the schema defined in batch-config.
	 * 
	 * @throws SQLException
	 */
	protected Connection getConnection() throws SQLException {
		logger.entering(CLASSNAME, "getConnection");
		Connection connection = null;
		
		if(!batchConfig.isJ2seMode()) {
			logger.fine("J2EE mode, getting connection from data source");
			connection = dataSource.getConnection();
			logger.fine("autocommit="+connection.getAutoCommit());
		} else {
			try {
				Class.forName(driver);
			} catch (ClassNotFoundException e) {
				throw new PersistenceException(e);
			}
			logger.log(Level.FINE, "JSE mode, getting connection from {0}", url);
			connection = DriverManager.getConnection(url, userId, pwd);
			logger.fine("autocommit="+connection.getAutoCommit());
		}
		setSchemaOnConnection(connection);
		
        logger.exiting(CLASSNAME, "getConnection", connection);
        return connection;
	}
	
	/**
	 * @return the database connection. The schema is set to whatever default its used by the underlying database.
	 * @throws SQLException
	 */
	protected Connection getConnectionToDefaultSchema() throws SQLException {
		logger.entering(CLASSNAME, "getConnection");
		Connection connection = null;
		
		if(!batchConfig.isJ2seMode()) {
			logger.fine("J2EE mode, getting connection from data source");
			connection = dataSource.getConnection();
			logger.fine("autocommit="+connection.getAutoCommit());
		} else {
			try {
				Class.forName(driver);
			} catch (ClassNotFoundException e) {
								
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				
				logger.log(Level.SEVERE, "ClassNotFoundException: Cannot load driver class: " + driver + "; Exception stack trace: " + sw);
				
				throw new PersistenceException(e);
			}
			logger.log(Level.FINE, "JSE mode, getting connection from {0}", url);
			connection = DriverManager.getConnection(url, userId, pwd);
			logger.fine("autocommit="+connection.getAutoCommit());
		}
		logger.exiting(CLASSNAME, "getConnection", connection);
		return connection;
	}
	
	/**
	 * Set the default schema JBATCH or the schema defined in batch-config on the connection object.
	 * 
	 * @param connection
	 * @throws SQLException
	 */
	private void setSchemaOnConnection(Connection connection) throws SQLException {
		logger.entering(CLASSNAME, "setSchemaOnConnection");
		
		PreparedStatement ps = null;
		ps = connection.prepareStatement("SET SCHEMA ?");
		ps.setString(1, schema);
		ps.executeUpdate(); 
		
		ps.close();
	    
	    logger.exiting(CLASSNAME, "setSchemaOnConnection");
	}
	
	/**
	 * insert data to DB table
	 * 
	 * @param key - the IPersistenceDataKey object
	 * @param value - serializable object to store  
	 * @param query - SQL statement to execute
	 * 
	 * Ex. insert into tablename values(?, ?)
	 */
	private <T> void executeInsert(Object key, T value, String query) {
		logger.entering(CLASSNAME, "executeInsert", new Object[] {key, value, query});
		Connection conn = null;
		PreparedStatement statement = null;
		ByteArrayOutputStream baos = null;
		ObjectOutputStream oout = null;
		byte[] b;
		try {
			conn = getConnection();
			statement = conn.prepareStatement(query);
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
		logger.exiting(CLASSNAME, "executeInsert");
	}
	
	private void executeJobInstanceDataInsert(long key, String jobName, String query){
		Connection conn = null;
		PreparedStatement statement = null;
		ByteArrayOutputStream baos = null;
		ObjectOutputStream oout = null;
		byte[] b;
		try {
			conn = getConnection();
			statement = conn.prepareStatement(query);
			baos = new ByteArrayOutputStream();
			oout = new ObjectOutputStream(baos);
			oout.writeObject(jobName);

			b = baos.toByteArray();

			statement.setLong(1, key);
			statement.setString(2, jobName);
			
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
	}
	
	private void executeExecutionDataInsert(long key, Timestamp createtime, Timestamp starttime, Timestamp endtime, 
											Timestamp updatetime, Properties parms, long instanceID, String batchstatus, String exitstatus, String query) {
		Connection conn = null;
		PreparedStatement statement = null;
		ByteArrayOutputStream baos = null;
		ObjectOutputStream oout = null;
		byte[] b;
		try {
			conn = getConnection();
			statement = conn.prepareStatement(query);
			baos = new ByteArrayOutputStream();
			oout = new ObjectOutputStream(baos);
			oout.writeObject(parms);

			b = baos.toByteArray();

			statement.setLong(1, key);
			statement.setTimestamp(2, createtime);
			statement.setTimestamp(3,starttime);
			statement.setTimestamp(4, endtime);
			statement.setTimestamp(5, updatetime);
			statement.setBytes(6, b);
			statement.setLong(7, instanceID);
			statement.setString(8, batchstatus);
			statement.setString(9, exitstatus);
			
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
	private <T> void executeUpdate(T value, Object key, String query) {
		logger.entering(CLASSNAME, "executeUpdate", new Object[] {key, value, query});
		Connection conn = null;
		PreparedStatement statement = null;
		ByteArrayOutputStream baos = null;
		ObjectOutputStream oout = null;
		byte[] b;
		try {
			conn = getConnection();
			statement = conn.prepareStatement(query);
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
		logger.exiting(CLASSNAME, "executeUpdate");
	}
	
	public void jobExecutionTimestampUpdate(long key, String timestampToUpdate, Timestamp ts) {
		Connection conn = null;
		PreparedStatement statement = null;

		try {
			conn = getConnection();
			statement = conn.prepareStatement("update executioninstancedata set " + timestampToUpdate + " = ? where id = ?");
			
			statement.setTimestamp(1, ts);	
			statement.setObject(2, key);
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, null, statement);
		}
	}
	
	public void jobExecutionStatusStringUpdate(long key, String statusToUpdate, String statusString, Timestamp updatets) {
		Connection conn = null;
		PreparedStatement statement = null;
		ByteArrayOutputStream baos = null;
		ObjectOutputStream oout = null;
		byte[] b;

		try {
			conn = getConnection();
			statement = conn.prepareStatement("update executioninstancedata set " + statusToUpdate + " = ?, updatetime = ? where id = ?");
			

			statement.setString(1, statusString);
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
	
	private void executeExecutionDataUpdate(long key, Timestamp createtime,
			Timestamp starttime, Timestamp endtime, Timestamp updatetime,
			Properties parms, long instanceID, String status, String query) {
		Connection conn = null;
		PreparedStatement statement = null;
		ByteArrayOutputStream baos = null;
		ObjectOutputStream oout = null;
		byte[] b;
		try {
			conn = getConnection();
			statement = conn.prepareStatement(query);
			baos = new ByteArrayOutputStream();
			oout = new ObjectOutputStream(baos);
			oout.writeObject(parms);

			b = baos.toByteArray();

			statement.setObject(1, key);
			statement.setTimestamp(2, createtime);
			statement.setTimestamp(3, starttime);
			statement.setTimestamp(4, endtime);
			statement.setTimestamp(5, updatetime);
			statement.setBytes(6, b);

			statement.setString(7, status);

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
	}
	
	/**
	 * delete data from DB table
	 * 
	 * @param key - the IPersistenceDataKey object
	 * @param query - SQL statement to execute.
	 * 
	 * Ex. delete from tablename where id = ?
	 */
	private void executeDelete(Object key, String query) {
		logger.entering(CLASSNAME, "executeDelete", new Object[] {key, query});
		Connection conn = null;
		PreparedStatement statement = null;
		try {
			conn = getConnection();
			statement = conn.prepareStatement(query);
			statement.setObject(1, key);
			statement.executeUpdate();

		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {

			cleanupConnection(conn, null, statement);
		}
		logger.exiting(CLASSNAME, "executeDelete");
	}
	
	/**
	 * select data from DB table
	 * 
	 * @param key - the IPersistenceDataKey object
	 * @param query - SQL statement to execute.
	 * @return List of serializable objects store in the DB table
	 * 
	 * Ex. select id, obj from tablename where id = ?
	 */
	private <T> List<T> executeQuery(Object key, String query) {
		logger.entering(CLASSNAME, "executeQuery", new Object[] {key, query});
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		List<T> data = new ArrayList<T>();
		ObjectInputStream objectIn = null;
		try {
			conn = getConnection();
			statement = conn.prepareStatement(query);
			statement.setObject(1, key);
			rs = statement.executeQuery();
			while (rs.next()) {
				byte[] buf = rs.getBytes("obj");
				if (buf != null) {
					objectIn = new ObjectInputStream(new ByteArrayInputStream(buf));
				}
				T deSerializedObject = (T) objectIn.readObject();
				data.add(deSerializedObject);
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
		logger.exiting(CLASSNAME, "executeQuery");
		return data;	
	}
	
	private List<Long> executeIDQuery(Object key, String query) {
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		List<Long> data = new ArrayList<Long>();
	
		try {
			conn = getConnection();
			statement = conn.prepareStatement(query);
			statement.setObject(1, key);
			rs = statement.executeQuery();
			while (rs.next()) {
				long id = rs.getLong("id");
				data.add(id);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
		finally {
			cleanupConnection(conn, rs, statement);
		}
		
		return data;	
	}
	
	private Set<String> executeNameQuery(String query) {
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		Set<String> data = new HashSet<String>();
		ObjectInputStream objectIn = null;
	
		try {
			conn = getConnection();
			statement = conn.prepareStatement(query);
			rs = statement.executeQuery();
			while (rs.next()) {
				String name = rs.getString("name");
				data.add(name);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
		finally {
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



	/**
	 * closes connection, result set and statement
	 * 
	 * @param conn - connection object to close
	 * @param rs - result set object to close
	 * @param statement - statement object to close
	 */
	private void cleanupConnection(Connection conn, ResultSet rs, PreparedStatement statement) {

		logger.entering(CLASSNAME, "cleanupConnection", new Object[] {conn, rs, statement});
		
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
		logger.exiting(CLASSNAME, "cleanupConnection");
	}
	
	/**
	 * closes connection and statement
	 * 
	 * @param conn - connection object to close
	 * @param statement - statement object to close
	 */
	private void cleanupConnection(Connection conn, PreparedStatement statement) {

		logger.entering(CLASSNAME, "cleanupConnection", new Object[] {conn,statement});
		
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
		logger.exiting(CLASSNAME, "cleanupConnection");
	}


	/**
	 * JOB OPERATOR METHODS
	 */
	
	@Override
    public void jobOperatorCreateJobInstanceData(long key, String Value){
		this.executeJobInstanceDataInsert(key, Value, this.INSERT_JOBINSTANCEDATA);
	}

	
	@Override
	public int jobOperatorGetJobInstanceCount(String jobName) {
		/*
		List<Integer> data = executeQuery(jobName, SELECT_JOBINSTANCEDATA_COUNT);
		if(data != null && !data.isEmpty()) {
			return data.get(0);
		}
		else return 0;
		*/
		
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		String status = null;
		ObjectInputStream objectIn = null;
		int count;
		
		try {
			conn = getConnection();
			statement = conn.prepareStatement(this.SELECT_JOBINSTANCEDATA_COUNT);
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
	public List<Long> jobOperatorgetJobInstanceIds(String jobName, int start,
			int count) {
		List<Long> data = executeIDQuery(jobName, SELECT_JOBINSTANCEDATA_IDS);
		
		if (data.size() > 0){
			try {
				return data.subList(start, start+count);
			}
			catch (IndexOutOfBoundsException oobEx){
				return data;
			}
		}
		else return data;
	}


	@Override
	public Set<String> jobOperatorgetJobNames() {
		
		Set<String> data = executeNameQuery(SELECT_JOBINSTANCEDATA_NAMES);
		
		return data;
	}


	@Override
	public void jobOperatorCreateExecutionData(long key,
			Timestamp createTime, Timestamp starttime, Timestamp endtime, Timestamp updateTime, Properties parms,
			long instanceID, String batchstatus, String exitstatus) {
		
		executeExecutionDataInsert(key, createTime, starttime, endtime, updateTime, parms, instanceID, batchstatus, exitstatus, INSERT_EXECUTIONDATA);
		
	}
	
	public Timestamp jobOperatorQueryJobExecutionTimestamp(long key, String requestedTimestamp){
		
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		Timestamp timestamp = null;
		
		try {
			conn = getConnection();
			statement = conn.prepareStatement("select " + requestedTimestamp + " from executioninstancedata where id = ?");
			statement.setObject(1, key);
			rs = statement.executeQuery();
			while (rs.next()) {
				timestamp = rs.getTimestamp(requestedTimestamp);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
		finally {
			cleanupConnection(conn, rs, statement);
		}
		
		return timestamp;
	}
	
	// Get Batch or Exit Status
	public String jobOperatorQueryJobExecutionStatus(long key, String requestedStatus){
		
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		String status = null;
		ObjectInputStream objectIn = null;
		
		try {
			conn = getConnection();
			statement = conn.prepareStatement("select " + requestedStatus + " from executioninstancedata where id = ?");
			statement.setObject(1, key);
			rs = statement.executeQuery();
			while (rs.next()) {
				status = rs.getString(requestedStatus);
				}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} finally {
			cleanupConnection(conn, rs, statement);
		}
		
		return status;
	}
	
	public long jobOperatorQueryJobExecutionJobInstanceId(long executionID){
		
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		long jobinstanceID = 0;
		
		try {
			conn = getConnection();
			statement = conn.prepareStatement("select jobinstanceid from executioninstancedata where id = ?");
			statement.setLong(1, executionID);
			rs = statement.executeQuery();
			while (rs.next()) {
				jobinstanceID = rs.getLong("jobinstanceid");
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
	public List<JobExecution> jobOperatorGetJobExecutionsByJobInstanceID(long jobInstanceID){
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		List<JobExecution> data = new ArrayList<JobExecution>();
		JobOperatorJobExecutionImpl jobExecutionImpl;
	
		Properties props;
		ObjectInputStream objectIn = null;
		
		try {
			conn = getConnection();
			statement = conn.prepareStatement("select * from executioninstancedata where jobinstanceid = ?");
			statement.setLong(1, jobInstanceID);
			rs = statement.executeQuery();
			
			while (rs.next()) {
				jobExecutionImpl = new JobOperatorJobExecutionImpl(rs.getLong("id"), rs.getLong("jobinstanceid"), null);
				jobExecutionImpl.setCreateTime(rs.getTimestamp("createtime"));
				jobExecutionImpl.setStartTime(rs.getTimestamp("starttime"));
				jobExecutionImpl.setLastUpdateTime(rs.getTimestamp("updatetime"));
				jobExecutionImpl.setEndTime(rs.getTimestamp("endtime"));
				jobExecutionImpl.setBatchStatus(rs.getString("batchstatus"));
				jobExecutionImpl.setExitStatus(rs.getString("exitstatus"));
				
				// get the object based data
				byte[] buf = rs.getBytes("parameters");
				if (buf != null) {
					objectIn = new ObjectInputStream(new ByteArrayInputStream(buf));
				}
				props = (Properties) objectIn.readObject();
				
				jobExecutionImpl.setJobProperties(props);
				
				data.add(jobExecutionImpl);
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
	public Properties getParameters(long instanceId){
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		Properties props = null;
		ObjectInputStream objectIn = null;
		
		try {
			conn = getConnection();
			statement = conn.prepareStatement("select parameters from executioninstancedata where jobinstanceid = ?"); 
			statement.setLong(1, instanceId);
			rs = statement.executeQuery();
			while (rs.next()) {
				
				// get the object based data
				byte[] buf = rs.getBytes("parameters");
				if (buf != null) {
					objectIn = new ObjectInputStream(new ByteArrayInputStream(buf));
				}
				props = (Properties) objectIn.readObject();
				
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

	@Override
	public void stepExecutionCreateStepExecutionData(String stepExecutionKey,
			long jobExecutionID, StepContextImpl stepContext) {

		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		long jobinstanceID = 0;
		long stepExecutionID = stepContext.getStepExecutionId();
		String batchStatus = stepContext.getBatchStatus().name();
		String exitStatus = stepContext.getExitStatus();
		String stepName = stepContext.getId();
		Object persistentData = stepContext.getPersistentUserData();
		ByteArrayOutputStream baos = null;
		ObjectOutputStream oout = null;
		byte[] pdataBytes;
		
    	long readCnt = 0;
    	long writeCnt = 0;
    	long processSkipCnt = 0;
    	long commitCnt = 0;
    	long rollbackCnt = 0;
    	long readSkipCnt = 0;
    	long filterCnt = 0;
    	long writeSkipCnt = 0;
    	
    	Timestamp startTimeTS = stepContext.getStartTimeTS();
    	Timestamp endTimeTS = stepContext.getEndTimeTS();
    	
		Metric[] metrics = stepContext.getMetrics();
		for (int i = 0; i < metrics.length; i++) {
			
			if (metrics[i].getName().equals(MetricImpl.MetricName.READCOUNT)) {
				readCnt = metrics[i].getValue();
			} else if (metrics[i].getName().equals(MetricImpl.MetricName.WRITECOUNT)) {
				writeCnt = metrics[i].getValue();	
			} else if (metrics[i].getName().equals(MetricImpl.MetricName.PROCESSSKIPCOUNT)) {
				processSkipCnt = metrics[i].getValue();	
			} else if (metrics[i].getName().equals(MetricImpl.MetricName.COMMITCOUNT)) {
				commitCnt = metrics[i].getValue();
			} else if (metrics[i].getName().equals(MetricImpl.MetricName.ROLLBACKCOUNT)) {
				rollbackCnt = metrics[i].getValue();	
			} else if (metrics[i].getName().equals(MetricImpl.MetricName.READSKIPCOUNT)) {
				readSkipCnt = metrics[i].getValue();
			} else if (metrics[i].getName().equals(MetricImpl.MetricName.FILTERCOUNT)) {
				filterCnt = metrics[i].getValue();	
			} else if (metrics[i].getName().equals(MetricImpl.MetricName.WRITESKIPCOUNT)) {
				writeSkipCnt = metrics[i].getValue();	
			}
		
		}
		
				
		try {
			conn = getConnection();
			statement = conn.prepareStatement("insert into stepexecutionInstanceData values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			statement.setString(1, stepExecutionKey);
			statement.setLong(2, jobExecutionID);
			statement.setLong(3, stepExecutionID);
			statement.setString(4, batchStatus);
			statement.setString(5, exitStatus);
			statement.setString(6, stepName);
			statement.setLong(7, readCnt);
			statement.setLong(8, writeCnt);
			statement.setLong(9, commitCnt);
			statement.setLong(10, rollbackCnt);
			statement.setLong(11, readSkipCnt);
			statement.setLong(12, processSkipCnt);
			statement.setLong(13, filterCnt);
			statement.setLong(14, writeSkipCnt);
            statement.setTimestamp(15, startTimeTS);
            statement.setTimestamp(16, endTimeTS);
            baos = new ByteArrayOutputStream();
			oout = new ObjectOutputStream(baos);
			oout.writeObject(persistentData);

			pdataBytes = baos.toByteArray();
			
			statement.setObject(17,pdataBytes);
            
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
			cleanupConnection(conn, rs, statement);
		}		
	}
	
	@Override
	public List<StepExecution> getStepExecutionIDListQueryByJobID(long execid){
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
		Externalizable persistentData = null;
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
				byte[] pDataBytes = rs.getBytes("persistentData");
				if (pDataBytes != null) {
					objectIn = new TCCLObjectInputStream(new ByteArrayInputStream(pDataBytes));
				}
				persistentData = (Externalizable)objectIn.readObject();
				
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
				stepEx.setpersistentUserData(persistentData);
				
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
	public StepExecution getStepExecutionObjQueryByStepID(long stepExecutionId){
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
		Externalizable persistentDataObj = null;
		StepExecutionImpl stepEx = null;
		ObjectInputStream objectIn = null;
	
		try {
			conn = getConnection();
			statement = conn.prepareStatement("select * from stepexecutioninstancedata where stepexecid = ?"); 
			statement.setLong(1, stepExecutionId);
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
				byte[] pDataBytes = rs.getBytes("persistentData");
				if (pDataBytes != null) {
					objectIn = new TCCLObjectInputStream(new ByteArrayInputStream(pDataBytes));
				}
				
				persistentDataObj = (Externalizable)objectIn.readObject();
				
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
				stepEx.setpersistentUserData(persistentDataObj);
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
		
		return stepEx;
	}
	
	/*
	@Override
	public long stepExecutionQueryID(long stepExecutionId){
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		long id = 0;
		
		try {
			conn = getConnection();
			statement = conn.prepareStatement("select stepexecutionid from stepexecutioninstancedata where id = " + key); 
			statement.setLong(1, stepExecutionId);
			rs = statement.executeQuery();
			while (rs.next()) {
				id = rs.getLong(idtype);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
		finally {
			cleanupConnection(conn, rs, statement);
		}
		
		return id;
	}
	*/
	
	public void jobOperatorUpdateBatchStatusWithUPDATETSonly(long key, String statusToUpdate, String statusString, Timestamp updatets) {
		Connection conn = null;
		PreparedStatement statement = null;
		ByteArrayOutputStream baos = null;
		ObjectOutputStream oout = null;
		byte[] b;

		try {
			conn = getConnection();
			statement = conn.prepareStatement("update executioninstancedata set " + statusToUpdate + " = ?, updatetime = ? where id = ?");
			
			statement.setString(1, statusString);
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
	
	public void jobOperatorUpdateBatchStatusWithSTATUSandUPDATETSonly(long key, String statusToUpdate, String statusString, Timestamp updatets){
		Connection conn = null;
		PreparedStatement statement = null;
		ByteArrayOutputStream baos = null;
		ObjectOutputStream oout = null;
		byte[] b;

		try {
			conn = getConnection();
			statement = conn.prepareStatement("update executioninstancedata set " + statusToUpdate + " = ?, updatetime = ? where id = ?");
			
			statement.setString(1, statusString);
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
	public JobExecution jobOperatorGetJobExecution(long jobExecutionId) {
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		Timestamp createtime = null;
		Timestamp starttime = null;
		Timestamp endtime = null;
		Timestamp updatetime = null;
		Properties params = null;
		long instanceId = 0;
		String batchStatus = null;
		String exitStatus = null;
		JobOperatorJobExecutionImpl jobEx = null;
		ObjectInputStream objectIn = null;
	
		try {
			conn = getConnection();
			statement = conn.prepareStatement("select createtime, starttime, endtime, updatetime, parameters, jobinstanceid, batchstatus, exitstatus  from executioninstancedata where id = ?"); 
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
				byte[] buf = rs.getBytes("parameters");
				if (buf != null) {
					objectIn = new ObjectInputStream(new ByteArrayInputStream(buf));
				}
				params = (Properties) objectIn.readObject();
				
				jobEx = new JobOperatorJobExecutionImpl(jobExecutionId, instanceId, null);
				jobEx.setCreateTime(createtime);
				jobEx.setStartTime(starttime);
				jobEx.setEndTime(endtime);
				jobEx.setJobProperties(params);
				jobEx.setLastUpdateTime(updatetime);
				jobEx.setBatchStatus(batchStatus);
				jobEx.setExitStatus(exitStatus);
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
	public List<JobExecution> jobOperatorGetJobExecutions(long jobInstanceId) {
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		Timestamp createtime = null;
		Timestamp starttime = null;
		Timestamp endtime = null;
		Timestamp updatetime = null;
		Properties params = null;
		long jobExecutionId = 0;
		long instanceId = 0;
		String batchStatus = null;
		String exitStatus = null;
		List<JobExecution> data = new ArrayList<JobExecution>();
		JobOperatorJobExecutionImpl jobEx = null;
		ObjectInputStream objectIn = null;
	
		try {
			conn = getConnection();
			statement = conn.prepareStatement("select id, createtime, starttime, endtime, updatetime, parameters, batchstatus, exitstatus  from executioninstancedata where jobinstanceid = ?"); 
			statement.setLong(1, jobInstanceId);
			rs = statement.executeQuery();
			while (rs.next()) {
				jobExecutionId = rs.getLong("id");
				createtime = rs.getTimestamp("createtime");
				starttime = rs.getTimestamp("starttime");
				endtime = rs.getTimestamp("endtime");
				updatetime = rs.getTimestamp("updatetime");
				
				// get the object based data
				batchStatus = rs.getString("batchstatus");
				exitStatus = rs.getString("exitstatus");
				
				// get the object based data
				byte[] buf = rs.getBytes("parameters");
				if (buf != null) {
					objectIn = new ObjectInputStream(new ByteArrayInputStream(buf));
				}
				params = (Properties) objectIn.readObject();
				
				jobEx = new JobOperatorJobExecutionImpl(jobExecutionId, instanceId, null);
				jobEx.setCreateTime(createtime);
				jobEx.setStartTime(starttime);
				jobEx.setEndTime(endtime);
				jobEx.setLastUpdateTime(updatetime);
				jobEx.setBatchStatus(batchStatus);
				jobEx.setExitStatus(exitStatus);
				
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
		long executionId = 0;
		long instanceId = 0;
		Set<Long> instanceIds1 = new HashSet<Long>();
		Set<Long> executionIds = new HashSet<Long>();
		JobOperatorJobExecutionImpl jobEx = null;
		ObjectInputStream objectIn = null;
	
		try {
			conn = getConnection();
			statement = conn.prepareStatement("select id from jobinstancedata where name = ?"); 
			statement.setString(1, jobName);
			rs = statement.executeQuery();
			while (rs.next()) {
				instanceId = rs.getLong("id");
				instanceIds1.add(instanceId);
			}
			statement = conn.prepareStatement("select id from executioninstancedata where jobinstanceid = ? and batchstatus = ?");
			for (long instanceid : instanceIds1){
				statement.setLong(1, instanceid);
				statement.setString(2, BatchStatus.STARTED.name());
				rs = statement.executeQuery();
				while (rs.next()){
					executionId = rs.getLong("id");
				}
				executionIds.add(executionId);
			}
		} catch (SQLException e) {
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
		return executionIds;		
	}
}
