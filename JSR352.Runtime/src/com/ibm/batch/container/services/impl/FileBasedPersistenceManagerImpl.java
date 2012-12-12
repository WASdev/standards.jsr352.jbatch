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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.runtime.StepExecution;

import com.ibm.batch.container.IBatchConfig;
import com.ibm.batch.container.exception.BatchContainerServiceException;
import com.ibm.batch.container.exception.PersistenceException;
import com.ibm.batch.container.services.IPersistenceDataKey;
import com.ibm.batch.container.services.IPersistenceManagerService;
import com.ibm.batch.container.status.JobStatus;

public class FileBasedPersistenceManagerImpl  extends AbstractMapBasedPersistenceManagerImpl implements
		IPersistenceManagerService {

	private static final String CLASSNAME = FileBasedPersistenceManagerImpl.class.getName();
	private static Logger logger = Logger.getLogger(FileBasedPersistenceManagerImpl.class.getPackage().getName());;
	private static boolean _isInited = false;
	private static final String JOBSTATUSSTORE = "jobstatus.dat";
	private static final String STEPSTATUSSTORE = "stepstatus.dat";
	private static final String CHECKPOINTSTORE = "checkpoint.dat";
	
	
	public FileBasedPersistenceManagerImpl() {
		
	}
	

	public void init(IBatchConfig batchConfig) throws BatchContainerServiceException {
		super.init(batchConfig);
		_loadDataStores();
		
	}

	
	protected void _loadDataStores() {
		
		Hashtable store = null;
		store = _loadStore(JOB_STATUS_STORE_ID);
		if(store != null) {
			_jobStatusStore = store;
		} else {
			_jobStatusStore = new Hashtable<Long, JobStatus>();
			_saveStore(JOB_STATUS_STORE_ID);
		}
		
		
//		store = _loadStore(STEP_STATUS_STORE_ID);
//		if(store != null) {
//			_stepStatusStore = store;
//		} else {
//			_stepStatusStore = new Hashtable<String,StepStatus>();
//			_saveStore(STEP_STATUS_STORE_ID);
//		}
//	
//		store = _loadStore(CHECKPOINT_STORE_ID);
//		if(store != null) {
//			_checkpointStore = store;
//		} else {
//			_checkpointStore = new Hashtable<String,CheckpointData>();
//			_saveStore(CHECKPOINT_STORE_ID);
//		}
		
		
		
		
		
	}


	protected void _saveStore(int storeId) {
		
		 String path = _getStorePath(storeId);
		 
		 try {
		      //use buffering
		      OutputStream file = new FileOutputStream( new File(path) );
		      OutputStream buffer = new BufferedOutputStream( file );
		      ObjectOutput output = new ObjectOutputStream( buffer );
		      try{
		    	  
		    	  switch (storeId) {
					case JOB_STATUS_STORE_ID:
						 output.writeObject(_jobStatusStore);
						break;
					
//					case STEP_STATUS_STORE_ID:
//						output.writeObject(_stepStatusStore);
//						break;
//					
//					case CHECKPOINT_STORE_ID:
//						output.writeObject(_checkpointStore);
//						break;
					default:
						break;
				
				}
		    	  
		      }
		      finally{
		        output.close();
		      }
		    }  
		    catch(IOException ex){
		      logger.log(Level.SEVERE, "Error saving Store:" + path  , ex);
		    }

		
	}

	protected String _getStorePath(int storeId) {
		String storeName = null;
		
		switch (storeId) {
			case JOB_STATUS_STORE_ID:
				storeName = JOBSTATUSSTORE;
				break;
			
			case STEP_STATUS_STORE_ID:
				storeName = STEPSTATUSSTORE;
				break;
			
			case CHECKPOINT_STORE_ID:
				storeName = CHECKPOINTSTORE;
				break;
			default:
				logger.severe("Invalid storeId! " + storeId);
				break;
		
		}
		String storePath = batchConfig.getBatchContainerHome() + File.separator + storeName;
		return storePath;
	}
	
	protected Hashtable _loadStore(int storeId) {
		
		
		String path = _getStorePath(storeId);
		File jobStore = new File(path);
		Hashtable store = null;
		if(jobStore.exists()) {
			try{
				
			      //use buffering
			      InputStream file = new FileInputStream( jobStore );
			      InputStream buffer = new BufferedInputStream( file );
			      ObjectInput input = new ObjectInputStream ( buffer );
			      try{
			        //deserialize the object
			    	  store = (Hashtable)input.readObject();
			       
			      }
			      finally{
			        input.close();
			      }
			    }
			    catch(ClassNotFoundException ex){
			      logger.log(Level.SEVERE, "Error loading store " + path + ". Class not found.", ex);
			    }
			    catch(IOException ex){
			    	logger.log(Level.SEVERE, "Error loading store " + path, ex);
			    }
		}	    
		return store;
		
	}


	public void shutdown() throws BatchContainerServiceException {
		// TODO Auto-generated method stub
		
	}





	@Override
	public void createData(int storeDestination, IPersistenceDataKey key,
			Serializable value) throws PersistenceException {
		// TODO Auto-generated method stub
		
	}





	@Override
	public void deleteData(int storeDestination, IPersistenceDataKey key)
			throws PersistenceException {
		// TODO Auto-generated method stub
		
	}





	@Override
	public List getData(int storeDestination, IPersistenceDataKey key)
			throws PersistenceException {
		// TODO Auto-generated method stub
		return null;
	}





	@Override
	public void updateData(int storeDestination, IPersistenceDataKey key,
			Serializable value) throws PersistenceException {
		// TODO Auto-generated method stub
		
	}





	@Override
	public int jobOperatorGetJobInstanceCount(String jobName) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public long jobOperatorQueryJobExecutionJobInstanceId(long executionID) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public String jobOperatorQueryJobExecutionStatus(long executionID,
			String requestedStatus) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Timestamp jobOperatorQueryJobExecutionTimestamp(long executionID,
			String timetype) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public List<Long> jobOperatorgetJobInstanceIds(String jobName, int start,
			int count) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Set<String> jobOperatorgetJobNames() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void jobExecutionStatusStringUpdate(long key, String statusToUpdate,
			String statusString, Timestamp updatets) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void jobExecutionTimestampUpdate(long key, String timestampToUpdate,
			Timestamp ts) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void jobOperatorCreateExecutionData(long key, Timestamp createTime,
			Timestamp starttime, Timestamp endtime, Timestamp updateTime,
			Properties parms, long instanceID, String batchstatus,
			String exitstatus) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void jobOperatorCreateJobInstanceData(long key, String jobNameValue) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void stepExecutionCreateStepExecutionData(String stepExecutionKey,
			long jobExecutionID, long stepExecutionID) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public long stepExecutionQueryID(String key, String idtype) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public List<StepExecution> stepExecutionQueryIDList(long key, String idtype) {
		// TODO Auto-generated method stub
		return null;
	}


}
