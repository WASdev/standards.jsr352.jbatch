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
package com.ibm.ws.batch.container.checkpoint;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.batch.container.artifact.proxy.ItemReaderProxy;
import com.ibm.batch.container.artifact.proxy.ItemWriterProxy;
import com.ibm.batch.container.exception.BatchContainerServiceException;
import com.ibm.batch.container.services.IPersistenceManagerService;
import com.ibm.batch.container.services.ServicesManager;
import com.ibm.batch.container.services.ServicesManager.ServiceType;

public class CheckpointManager {
	private final static String sourceClass = CheckpointManager.class.getName();
	private final static Logger logger = Logger.getLogger(sourceClass);

	private ServicesManager servicesManager = ServicesManager.getInstance();
	private IPersistenceManagerService _persistenceManagerService = null;
	
	private ItemReaderProxy readerProxy = null;
	private ItemWriterProxy writerProxy = null;
	int commitInterval = 0;
	private CheckpointAlgorithm checkpointAlgorithm;
	private boolean ckptStarted;
	private long executionId = 0;
	private String stepId = null;
	private long jobInstanceID = 0;


	//TODO - need revise
	public CheckpointManager(ItemReaderProxy reader, ItemWriterProxy writer,CheckpointAlgorithm chkptAlg,
			int commitInterval, long executionId, long jobInstanceID, String  stepId) {
		this.readerProxy = reader;
		this.writerProxy = writer;
		this.checkpointAlgorithm = chkptAlg;
		this.commitInterval = commitInterval;
		this.executionId = executionId;
		this.stepId = stepId;
		this.jobInstanceID = jobInstanceID;
		
		_persistenceManagerService = (IPersistenceManagerService) servicesManager.getService(ServiceType.PERSISTENCE_MANAGEMENT_SERVICE);
	}
	//	  private ITransaction _userTran = null;
	//	  private ITransactionManagementService _transactionManagementService = null;

	public void beginCheckpoint(int timeoutVal)
	{
		String method = "startCheckpoint";
		if(logger.isLoggable(Level.FINER)) { logger.entering(sourceClass, method);}

		//this._userTran.setTransactionTimeout(timeoutVal);
        //checkpointProxy.beginCheckpoint();
        //this._userTran.begin(); // Tran # 3
        //if(logger.isLoggable(Level.FINE)) { logger.fine("jobInstanceId=" + ijobInstanceId + "  userTran.begin()    issued by CheckpointMgr.startCheckpoint()" );}
        ckptStarted = true;

		if(logger.isLoggable(Level.FINER)) {
			//logger.exiting(sourceClass, method, " [executionId " + executionId + "] [name " + checkpointProxy.getCheckpointAlgorithmClassName() + "] [timeout "
			//		+ timeoutVal+ "]");
		}
	}

	public void beginCheckpoint()
	{
		String method = "beginCheckpoint";
		if(logger.isLoggable(Level.FINER)) { logger.entering(sourceClass, method);}
		//	      itimeout = algorithm.getRecommendedTimeOutValue();
        //	      this._userTran.setTransactionTimeout(itimeout);
        //checkpointProxy.beginCheckpoint();
        //	      this._userTran.begin(); // Tran # 3
        if(logger.isLoggable(Level.FINE)) { logger.fine("executionId=" + executionId );}
        ckptStarted = true;

		if(logger.isLoggable(Level.FINER)) {
			//logger.exiting(sourceClass, method, " [executionId " + executionId + "] [name " + checkpointAlgorithm.getCheckpointAlgorithmClassName() + "]");
		}
	}

	public void endCheckpoint() {
		String method = "endCheckpoint";
		if(logger.isLoggable(Level.FINER)) { logger.entering(sourceClass, method);}

		//checkpointProxy.endCheckpoint();

		if(logger.isLoggable(Level.FINER)) {
			//logger.exiting(sourceClass, method, " [executionId " + executionId + "] [name " + checkpointProxy.getCheckpointAlgorithmClassName() + "]");
		}
	}

	public boolean ApplyCheckPointPolicy(/*boolean forceCheckpoint*/)
	{
		String method = "ApplyCheckPointPolicy";
		if(logger.isLoggable(Level.FINER)) { logger.entering(sourceClass, method); }

		boolean checkpoint = false;
		
		try {
			checkpoint = checkpointAlgorithm.isReadyToCheckpoint();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (checkpoint) {
			if (logger.isLoggable(Level.FINE) && checkpoint)
				logger.fine("ApplyCheckPointPolicy - going to checkpoint");
			
			//checkpoint();	      
			
			if (logger.isLoggable(Level.FINE) && checkpoint)
				logger.fine("ApplyCheckPointPolicy - back from checkpoint");

		}    

		if (logger.isLoggable(Level.FINE) && checkpoint)
			logger.fine("ApplyCheckPointPolicy - " + checkpoint);

		if(logger.isLoggable(Level.FINER)) { logger.exiting(sourceClass, method);}
		return checkpoint;
	}

	public boolean isStarted()
	{
		return ckptStarted;
	}


	public void checkpoint()
	{
		String method = "checkpoint";
		if(logger.isLoggable(Level.FINER)) { logger.entering(sourceClass, method, " [executionId " + executionId + "] "); }

		ByteArrayOutputStream readerChkptBA = new ByteArrayOutputStream();
		ByteArrayOutputStream writerChkptBA = new ByteArrayOutputStream();
		
		ObjectOutputStream readerOOS = null, writerOOS = null;
		CheckpointDataKey readerChkptDK = null, writerChkptDK = null;
		
		try{
			readerOOS = new ObjectOutputStream(readerChkptBA);
			readerOOS.writeObject(readerProxy.checkpointInfo());
			readerOOS.close();
			CheckpointData readerChkptData  = new CheckpointData(jobInstanceID, stepId, "READER");
			readerChkptData.setRestartToken(readerChkptBA.toByteArray());
			readerChkptDK = new CheckpointDataKey(jobInstanceID, stepId, "READER");
			_persistenceManagerService.deleteData(IPersistenceManagerService.CHECKPOINT_STORE_ID, readerChkptDK);
			_persistenceManagerService.createData(IPersistenceManagerService.CHECKPOINT_STORE_ID, readerChkptDK, readerChkptData);
			
			writerOOS = new ObjectOutputStream(writerChkptBA);
			writerOOS.writeObject(writerProxy.checkpointInfo());
			writerOOS.close();
			CheckpointData writerChkptData = new CheckpointData(jobInstanceID, stepId, "WRITER");
			writerChkptData.setRestartToken(writerChkptBA.toByteArray());
			writerChkptDK = new CheckpointDataKey(jobInstanceID, stepId, "WRITER");
			_persistenceManagerService.deleteData(IPersistenceManagerService.CHECKPOINT_STORE_ID, writerChkptDK);
			_persistenceManagerService.createData(IPersistenceManagerService.CHECKPOINT_STORE_ID, writerChkptDK, writerChkptData);	
		}
		catch (Exception ex){
			// is this what I should be throwing here?
			throw new BatchContainerServiceException("Cannot persist the checkpoint data for [" + stepId + "]", ex);
		}

		if(logger.isLoggable(Level.FINER)) { logger.exiting(sourceClass, method, " [executionId " + executionId + "] ");}

	}

}
