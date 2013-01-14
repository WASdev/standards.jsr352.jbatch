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
package com.ibm.batch.container.services.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
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

import javax.batch.runtime.StepExecution;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.ibm.batch.container.config.IBatchConfig;
import com.ibm.batch.container.exception.BatchContainerServiceException;
import com.ibm.batch.container.exception.PersistenceException;
import com.ibm.batch.container.jobinstance.StepExecutionImpl;
import com.ibm.batch.container.services.IPersistenceManagerService;
import com.ibm.batch.container.status.JobStatus;
import com.ibm.batch.container.status.JobStatusKey;
import com.ibm.batch.container.status.StepStatus;
import com.ibm.batch.container.status.StepStatusKey;
import com.ibm.ws.batch.container.checkpoint.CheckpointData;
import com.ibm.ws.batch.container.checkpoint.CheckpointDataKey;

public class JDBCPersistenceManagerImpl extends AbstractPersistenceManagerImpl implements IPersistenceManagerService {

	private static final String CLASSNAME = JDBCPersistenceManagerImpl.class.getName();

	private final static Logger logger = Logger.getLogger(CLASSNAME);
	
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
	
	private static final String DELETE_CHECKPOINTDATA = "delete from checkpointdata where id = ?";
	
	// JOB OPERATOR QUERIES
	private static final String INSERT_JOBINSTANCEDATA = "insert into jobinstancedata values(?, ?)";
	
	private static final String INSERT_EXECUTIONDATA = "insert into executionInstanceData values(?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static final String SELECT_JOBINSTANCEDATA_COUNT = "select count(name) from jobinstancedata where name = ?";
	
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
    
	protected String driver = ""; //"org.apache.derby.jdbc.EmbeddedDriver";
	protected String url = ""; //"jdbc:derby:";
	protected String userId = "";
	protected String pwd = "";
	
	/* (non-Javadoc)
	 * @see com.ibm.batch.container.services.impl.AbstractPersistenceManagerImpl#init(com.ibm.batch.container.IBatchConfig)
	 */
	@Override
	public void init(IBatchConfig batchConfig) throws BatchContainerServiceException {
		super.init(batchConfig);
		
		logger.entering(CLASSNAME, "init", batchConfig);
		
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
		
		logger.exiting(CLASSNAME, "init");
	}

	
    /* (non-Javadoc)
     * @see com.ibm.batch.container.services.impl.AbstractPersistenceManagerImpl#_createJobStatus(com.ibm.batch.container.status.JobStatusKey, com.ibm.batch.container.status.JobStatus)
     */
    @Override
	protected void _createJobStatus(JobStatusKey key, JobStatus value) {
    	logger.entering(CLASSNAME, "_createJobStatus", new Object[] {key, value});
    	executeInsert(key.getJobInstanceId(), value, INSERT_JOBSTATUS);
    	logger.exiting(CLASSNAME, "_createJobStatus");
	}

	/* (non-Javadoc)
	 * @see com.ibm.batch.container.services.impl.AbstractPersistenceManagerImpl#_getJobStatus(com.ibm.batch.container.status.JobStatusKey)
	 */
	@Override
	protected List<JobStatus> _getJobStatus(JobStatusKey key) {
		logger.entering(CLASSNAME, "_getJobStatus", key);
		List<JobStatus> jobStatuses = executeQuery(key.getJobInstanceId(), SELECT_JOBSTATUS);
		logger.exiting(CLASSNAME, "_getJobStatus", jobStatuses);
		return jobStatuses;
	}
    
    /* (non-Javadoc)
     * @see com.ibm.batch.container.services.impl.AbstractPersistenceManagerImpl#_updateJobStatus(com.ibm.batch.container.status.JobStatusKey, com.ibm.batch.container.status.JobStatus)
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
     * @see com.ibm.batch.container.services.impl.AbstractPersistenceManagerImpl#_deleteJobStatus(com.ibm.batch.container.status.JobStatusKey)
     */
    @Override
    protected void _deleteJobStatus(JobStatusKey key) {
    	logger.entering(CLASSNAME, "_deleteJobStatus", key);
    	executeDelete(key.getJobInstanceId(), DELETE_JOBSTATUS);
    	logger.exiting(CLASSNAME, "_deleteJobStatus");
    }

    /* (non-Javadoc)
     * @see com.ibm.batch.container.services.impl.AbstractPersistenceManagerImpl#_createStepStatus(com.ibm.batch.container.status.StepStatusKey, com.ibm.batch.container.status.StepStatus)
     */
    @Override
	protected void _createStepStatus(StepStatusKey key, StepStatus value) {
    	logger.entering(CLASSNAME, "_createStepStatus", new Object[] {key, value});
        executeInsert(key.getKeyPrimitive(), value, INSERT_STEPSTATUS);
        logger.exiting(CLASSNAME, "_createStepStatus");
	}

    /* (non-Javadoc)
     * @see com.ibm.batch.container.services.impl.AbstractPersistenceManagerImpl#_getStepStatus(com.ibm.batch.container.status.StepStatusKey)
     */
    @Override
	protected List<StepStatus> _getStepStatus(StepStatusKey key) {
    	logger.entering(CLASSNAME, "_getStepStatus", key);
    	List<StepStatus> stepStatuses = executeQuery(key.getKeyPrimitive(), SELECT_STEPSTATUS); 
    	logger.exiting(CLASSNAME, "_getStepStatus", stepStatuses);
    	return stepStatuses;
	}

    /* (non-Javadoc)
     * @see com.ibm.batch.container.services.impl.AbstractPersistenceManagerImpl#_updateStepStatus(com.ibm.batch.container.status.StepStatusKey, com.ibm.batch.container.status.StepStatus)
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
     * @see com.ibm.batch.container.services.impl.AbstractPersistenceManagerImpl#_deleteStepStatus(com.ibm.batch.container.status.StepStatusKey)
     */
    @Override
    protected void _deleteStepStatus(StepStatusKey key) {
    	logger.entering(CLASSNAME, "_deleteStepStatus", key);
    	executeDelete(key.getKeyPrimitive(), DELETE_STEPSTATUS);
    	logger.exiting(CLASSNAME, "_deleteStepStatus");
    }

	/* (non-Javadoc)
	 * @see com.ibm.batch.container.services.impl.AbstractPersistenceManagerImpl#_createCheckpointData(com.ibm.ws.batch.container.checkpoint.CheckpointDataKey, com.ibm.ws.batch.container.checkpoint.CheckpointData)
	 */
	@Override
	protected void _createCheckpointData(CheckpointDataKey key, CheckpointData value) {
		executeInsert(key.getJobInstanceId(), value, INSERT_CHECKPOINTDATA);
		logger.entering(CLASSNAME, "_createCheckpointData", new Object[] {key, value});
		executeInsert(key.getCommaSeparatedKey(), value, INSERT_CHECKPOINTDATA);
		logger.exiting(CLASSNAME, "_createCheckpointData");
	}

	/* (non-Javadoc)
	 * @see com.ibm.batch.container.services.impl.AbstractPersistenceManagerImpl#_getCheckpointData(com.ibm.ws.batch.container.checkpoint.CheckpointDataKey)
	 */
	@Override
	protected List<CheckpointData> _getCheckpointData(CheckpointDataKey key) {
		logger.entering(CLASSNAME, "_getCheckpointData", key);
		List<CheckpointData> checkpointData = executeQuery(key.getCommaSeparatedKey(), SELECT_CHECKPOINTDATA);
		logger.exiting(CLASSNAME, "_getCheckpointData", checkpointData);
		return checkpointData;
	}

	/* (non-Javadoc)
	 * @see com.ibm.batch.container.services.impl.AbstractPersistenceManagerImpl#_updateCheckpointData(com.ibm.ws.batch.container.checkpoint.CheckpointDataKey, com.ibm.ws.batch.container.checkpoint.CheckpointData)
	 */
	@Override
	protected void _updateCheckpointData(CheckpointDataKey key, CheckpointData value) {
		logger.entering(CLASSNAME, "_updateCheckpointData", new Object[] {key, value});
		List<CheckpointData> data = executeQuery(key.getCommaSeparatedKey(), SELECT_CHECKPOINTDATA);
		if(data != null && !data.isEmpty()) {
			executeUpdate(value, key.getCommaSeparatedKey(), UPDATE_CHECKPOINTDATA);
		}
		logger.exiting(CLASSNAME, "_updateCheckpointData");
	}
	
	/* (non-Javadoc)
	 * @see com.ibm.batch.container.services.impl.AbstractPersistenceManagerImpl#_deleteCheckpointData(com.ibm.ws.batch.container.checkpoint.CheckpointDataKey)
	 */
	@Override
	protected void _deleteCheckpointData(CheckpointDataKey key) {
		executeDelete(key.getJobInstanceId(), DELETE_CHECKPOINTDATA);
		logger.entering(CLASSNAME, "_deleteCheckpointData", key);
		executeDelete(key.getCommaSeparatedKey(), DELETE_CHECKPOINTDATA);
		logger.exiting(CLASSNAME, "_deleteCheckpointData");
	}

	/**
	 * @return the database connection.
	 * 
	 * @throws SQLException
	 */
	protected Connection getConnection() throws SQLException {
		logger.entering(CLASSNAME, "getConnection");
		Connection connection = null;
		
		if(!batchConfig.isJ2seMode()) {
			logger.fine("J2EE mode, getting connection from data source");
			connection = dataSource.getConnection();
			connection.setAutoCommit(false);
			logger.fine("autocommit="+connection.getAutoCommit());
		} else {
			try {
				Class.forName(driver);
			} catch (ClassNotFoundException e) {
				throw new PersistenceException(e);
			}
			logger.log(Level.FINE, "JSE mode, getting connection from {0}", url);
			connection = DriverManager.getConnection(url, userId, pwd);
			connection.setAutoCommit(true);
			logger.fine("autocommit="+connection.getAutoCommit());
		}
		logger.exiting(CLASSNAME, "getConnection", connection);
		return connection;
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
			oout.reset();
			oout.writeObject(batchstatus);
			b = baos.toByteArray();
			
			statement.setBytes(8, b);
			
			oout.reset();
			oout.writeObject(exitstatus);
			b = baos.toByteArray();
			
			statement.setBytes(9, b);
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
			
			baos = new ByteArrayOutputStream();
			oout = new ObjectOutputStream(baos);
			oout.writeObject(statusString);

			b = baos.toByteArray();
			statement.setBytes(1, b);
			statement.setTimestamp(2, updatets);
			statement.setLong(3, key);
			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
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

			oout.reset();
			oout.writeObject(status);
			b = baos.toByteArray();

			statement.setBytes(7, b);
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
			cleanupConnection(conn, rs, statement);
		}
		
		return data;	
	}



	/**
	 * closes connection to DB
	 * 
	 * @param conn - connection object to close
	 * @param rs - result set object to close
	 * @param statement - statement object to close
	 * @param commit - commit database changes when the connection is closed
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
	 * JOB OPERATOR METHODS
	 */
	
	@Override
    public void jobOperatorCreateJobInstanceData(long key, String Value){
	}

	
	@Override
	public int jobOperatorGetJobInstanceCount(String jobName) {
		List<Integer> data = executeQuery(jobName, SELECT_JOBINSTANCEDATA_COUNT);
		if(data != null && !data.isEmpty()) {
			return data.get(0);
		}
		else return 0;
		
	}


	@Override
	public List<Long> jobOperatorgetJobInstanceIds(String jobName, int start,
			int count) {
		List<Long> data = executeIDQuery(jobName, SELECT_JOBINSTANCEDATA_IDS);
		return data.subList(start, start+count);
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
		String status;
		ObjectInputStream objectIn = null;
		
		try {
			conn = getConnection();
			statement = conn.prepareStatement("select " + requestedStatus + " from executioninstancedata where id = ?");
			statement.setObject(1, key);
			rs = statement.executeQuery();
			while (rs.next()) {
				byte[] buf = rs.getBytes(requestedStatus);
				if (buf != null) {
					objectIn = new ObjectInputStream(new ByteArrayInputStream(buf));
				}
				status = (String) objectIn.readObject();
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			cleanupConnection(conn, rs, statement);
		}
		
		//return status;
		return "FIGURING OUT HOW TO GET A STRING FROM A BLOB";
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
	public void stepExecutionCreateStepExecutionData(String stepExecutionKey,
			long jobExecutionID, long stepExecutionID) {

		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		long jobinstanceID = 0;
		ByteArrayOutputStream baos = null;
		ObjectOutputStream oout = null;
		byte[] b;
		
		
		try {
			conn = getConnection();
			statement = conn.prepareStatement("insert into stepexecutionInstanceData values(?, ?, ?)");
			statement.setString(1, stepExecutionKey);
			statement.setLong(2, jobExecutionID);
			statement.setLong(3, stepExecutionID);
			//baos = new ByteArrayOutputStream();
			//oout = new ObjectOutputStream(baos);
			//oout.writeObject(stepContext);

			//b = baos.toByteArray();
			//statement.setBytes(4, b);
			statement.executeUpdate();
		} catch (SQLException e) {
			throw new PersistenceException(e);
//		} catch (IOException e) {
//			throw new PersistenceException(e);
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
	public List<StepExecution> stepExecutionQueryIDList(long key, String idtype){
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		long jobexecid = 0;
		long stepexecid = 0;
		List<StepExecution> data = new ArrayList<StepExecution>();
		StepExecutionImpl stepEx = null;
		ObjectInputStream objectIn = null;
	
		try {
			conn = getConnection();
			statement = conn.prepareStatement("select jobexecid, stepexecid from stepexecutioninstancedata where " + idtype + " = ?"); 
			statement.setLong(1, key);
			rs = statement.executeQuery();
			while (rs.next()) {
				jobexecid = rs.getLong("jobexecid");
				stepexecid = rs.getLong("stepexecid");
				//byte[] buf = rs.getBytes("obj");
				//if (buf != null) {
				//	objectIn = new ObjectInputStream(new ByteArrayInputStream(buf));
				//}
				//StepContextImpl deSerializedStepContext = (StepContextImpl) objectIn.readObject();
				
				stepEx = new StepExecutionImpl(jobexecid, stepexecid);
				//stepEx.setStepContext(deSerializedStepContext);
				data.add(stepEx);
			}
		} catch (SQLException e) {
			throw new PersistenceException(e);
		//} catch (IOException e) {
		//	throw new PersistenceException(e);
		//} catch (ClassNotFoundException e) {
		//	throw new PersistenceException(e);
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
	public long stepExecutionQueryID(String key, String idtype){
		Connection conn = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		long id = 0;
		
		try {
			conn = getConnection();
			statement = conn.prepareStatement("select " + idtype + " from stepexecutioninstancedata where id = " + key); 
			//statement.setString(1, key);
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

}
