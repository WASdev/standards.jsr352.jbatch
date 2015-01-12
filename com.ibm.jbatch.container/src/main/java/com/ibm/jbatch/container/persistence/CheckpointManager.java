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
package com.ibm.jbatch.container.persistence;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.api.chunk.CheckpointAlgorithm;

import com.ibm.jbatch.container.artifact.proxy.ItemReaderProxy;
import com.ibm.jbatch.container.artifact.proxy.ItemWriterProxy;
import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.servicesmanager.ServicesManager;
import com.ibm.jbatch.container.servicesmanager.ServicesManagerImpl;

public class CheckpointManager {
	private final static String sourceClass = CheckpointManager.class.getName();
	private final static Logger logger = Logger.getLogger(sourceClass);

	private ServicesManager servicesManager = ServicesManagerImpl.getInstance();
	private IPersistenceManagerService _persistenceManagerService = null;
	
	private ItemReaderProxy readerProxy = null;
	private ItemWriterProxy writerProxy = null;
	int commitInterval = 0;
	private CheckpointAlgorithm checkpointAlgorithm;
	private long executionId = 0;
	private String stepId = null;
	private long jobInstanceID = 0;
	

	public CheckpointManager(ItemReaderProxy reader, ItemWriterProxy writer,CheckpointAlgorithm chkptAlg,
			long executionId, long jobInstanceID, String  stepId) {
		this.readerProxy = reader;
		this.writerProxy = writer;
		this.checkpointAlgorithm = chkptAlg;
		this.executionId = executionId;
		this.stepId = stepId;
		this.jobInstanceID = jobInstanceID;
		
		_persistenceManagerService = servicesManager.getPersistenceManagerService();
	}


	public void beginCheckpoint() {
		try {
			checkpointAlgorithm.beginCheckpoint();
		} catch (Exception e) {
		    throw new BatchContainerRuntimeException("Checkpoint algorithm failed", e);
		}
	}

	public void endCheckpoint() {
		try {
			checkpointAlgorithm.endCheckpoint();
		} catch (Exception e) {
		    throw new BatchContainerRuntimeException("Checkpoint algorithm failed", e);
		}
	}

	public boolean isReadyToCheckpoint() {

		String method = "isReadyToCheckpoint";
		if(logger.isLoggable(Level.FINER)) { logger.entering(sourceClass, method); }

		boolean checkpoint = false;
		
		try {
			checkpoint = checkpointAlgorithm.isReadyToCheckpoint();
		} catch (Exception e) {
		    throw new BatchContainerRuntimeException("Checkpoint algorithm failed", e);
		}

		if (logger.isLoggable(Level.FINE) && checkpoint)
			logger.fine("isReadyToCheckpoint - " + checkpoint);

		if(logger.isLoggable(Level.FINER)) { logger.exiting(sourceClass, method);}
		
		return checkpoint;
	}
	
	public void checkpoint() {
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
			
			_persistenceManagerService.updateCheckpointData(readerChkptDK, readerChkptData);
			
			writerOOS = new ObjectOutputStream(writerChkptBA);
			writerOOS.writeObject(writerProxy.checkpointInfo());
			writerOOS.close();
			CheckpointData writerChkptData = new CheckpointData(jobInstanceID, stepId, "WRITER");
			writerChkptData.setRestartToken(writerChkptBA.toByteArray());
			writerChkptDK = new CheckpointDataKey(jobInstanceID, stepId, "WRITER");

			_persistenceManagerService.updateCheckpointData(writerChkptDK, writerChkptData);
			
		} catch (Exception ex){
			// is this what I should be throwing here?
			throw new BatchContainerServiceException("Cannot persist the checkpoint data for [" + stepId + "]", ex);
		}

		if(logger.isLoggable(Level.FINER)) { logger.exiting(sourceClass, method, " [executionId " + executionId + "] ");}

	}
	
	public int checkpointTimeout() {
		
		int returnTimeout = 0; 

        try {
            returnTimeout = this.checkpointAlgorithm.checkpointTimeout();
        } catch (Exception e) {
            throw new BatchContainerRuntimeException("Checkpoint algorithm checkpointTimeout() failed", e);
        }

        return returnTimeout;
	}

}
