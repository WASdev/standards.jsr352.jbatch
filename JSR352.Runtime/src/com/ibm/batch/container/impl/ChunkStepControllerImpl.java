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
package com.ibm.batch.container.impl;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jsr352.batch.jsl.Chunk;
import jsr352.batch.jsl.JSLProperties;
import jsr352.batch.jsl.ObjectFactory;
import jsr352.batch.jsl.Property;
import jsr352.batch.jsl.Step;

import com.ibm.batch.container.artifact.proxy.CheckpointAlgorithmProxy;
import com.ibm.batch.container.artifact.proxy.CheckpointListenerProxy;
import com.ibm.batch.container.artifact.proxy.ItemProcessorProxy;
import com.ibm.batch.container.artifact.proxy.ItemReaderProxy;
import com.ibm.batch.container.artifact.proxy.ItemWriterProxy;
import com.ibm.batch.container.artifact.proxy.ProxyFactory;
import com.ibm.batch.container.artifact.proxy.RetryListenerProxy;
import com.ibm.batch.container.artifact.proxy.SkipListenerProxy;
import com.ibm.batch.container.context.impl.MetricImpl;
import com.ibm.batch.container.exception.BatchContainerRuntimeException;
import com.ibm.batch.container.exception.BatchContainerServiceException;
import com.ibm.batch.container.jobinstance.RuntimeJobExecutionImpl;
import com.ibm.batch.container.services.IPersistenceManagerService;
import com.ibm.batch.container.services.ServicesManager;
import com.ibm.batch.container.services.ServicesManager.ServiceType;
import com.ibm.batch.container.util.TCCLObjectInputStream;
import com.ibm.batch.container.util.ExecutionStatus;
import com.ibm.batch.container.util.ExecutionStatus.BatchStatus;
import com.ibm.batch.container.validation.ArtifactValidationException;
import com.ibm.ws.batch.container.checkpoint.CheckpointAlgorithm;
import com.ibm.ws.batch.container.checkpoint.CheckpointAlgorithmFactory;
import com.ibm.ws.batch.container.checkpoint.CheckpointData;
import com.ibm.ws.batch.container.checkpoint.CheckpointDataKey;
import com.ibm.ws.batch.container.checkpoint.CheckpointManager;
import com.ibm.ws.batch.container.checkpoint.ItemCheckpointAlgorithm;
import com.ibm.ws.batch.container.checkpoint.ItemTimeCheckpointAlgorithm;
import com.ibm.ws.batch.container.checkpoint.TimeCheckpointAlgorithm;

public class ChunkStepControllerImpl extends SingleThreadedStepControllerImpl {

	private final static String sourceClass = ChunkStepControllerImpl.class
			.getName();
	private final static Logger logger = Logger.getLogger(sourceClass);

	private Chunk chunk = null;
	private ItemReaderProxy readerProxy = null;
	private ItemProcessorProxy processorProxy = null;
	private ItemWriterProxy writerProxy = null;
	private CheckpointAlgorithmProxy checkpointProxy = null;
	private CheckpointAlgorithm chkptAlg = null;
	private CheckpointManager checkpointManager;
	private ServicesManager servicesManager = ServicesManager.getInstance();
	private IPersistenceManagerService _persistenceManagerService = null;
	private SkipHandler skipHandler = null;
	CheckpointDataKey readerChkptDK, writerChkptDK = null;
	CheckpointData readerChkptData = null;
	CheckpointData writerChkptData = null;
	List<CheckpointListenerProxy> checkpointListeners = null;
	List<SkipListenerProxy> skipListeners = null;
	List<RetryListenerProxy> retryListeners = null;
	private RetryHandler retryHandler;
	
	// metrics
	long readCount = 0;
	long writeCount = 0;
	long readSkipCount = 0;
	long processSkipCount = 0;
	long writeSkipCount = 0;

	private enum CHUNK_ARTIFACT {
		READER("reader"),
		PROCESSOR("processor"),
		WRITER("writer");
		
		private String chunkArtifactType;
		
		CHUNK_ARTIFACT(String chunkArtifactType){
			this.chunkArtifactType = chunkArtifactType;
		}
		
		public String getChunkArtifactType() {
			return this.chunkArtifactType;
		}
	}
	
	
	public ChunkStepControllerImpl(RuntimeJobExecutionImpl jobExecutionImpl, Step step) {
		super(jobExecutionImpl, step);
		// TODO Auto-generated constructor stub
	}
	
	// TODO: complete refactoring, remove starts/end comment and remove old read-process-write loop
	// mostly works but a few failures like " WRITE: the chunk write did not at the correct boundry (idx) ->11"
	// need to debug some more
/*
 * Refactoring starts here
 * 
 */
	
	/**
	 * Utility Class to hold statuses at each level of Read-Process-Write loop
	 *
	 */
	private class ItemStatus {

		public boolean isSkipped() {
			return skipped;
		}
		public void setSkipped(boolean skipped) {
			this.skipped = skipped;
		}
		public boolean isCheckPointed() {
			return checkPointed;
		}
		
		public void setCheckPointed(boolean checkPointed) {
			this.checkPointed = checkPointed;
		}

		public boolean isFinished() {
			return finished;
		}
		public void setFinished(boolean finished) {
			this.finished = finished;
		}

		private boolean skipped = false;
		private boolean finished = false;
		private boolean checkPointed = false;
		
	}
	
	/**
	 * We read and process one item at a time but write in chunks (group of items).
	 * So, this method loops until we either reached the end of the reader (not more items to read),
	 * or the writer buffer is full or a checkpoint is triggered.
	 * 
	 * @param chunkSize write buffer size
	 * @param theStatus flags when the read-process reached the last record or a checkpoint is required 
	 * @return an array list of objects to write
	 */
	private List<Object> readAndProcess(int chunkSize, ItemStatus theStatus) {
		logger.entering(sourceClass, "readAndProcess", new Object[] {chunkSize, theStatus});
		
		List<Object> chunkToWrite = new ArrayList<Object>();
		Object itemRead = null;
		Object itemProcessed = null;
		int readProcessedCount = 0;
		
		while(true) {
			ItemStatus status = new ItemStatus();
			itemRead = readItem(status);
			
			if (!status.isSkipped() && !status.isFinished()) {
				itemProcessed = processItem(itemRead, status);
				
				if (!status.isSkipped()) {
					chunkToWrite.add(itemProcessed);
					readProcessedCount++;
				}
			}
			
			theStatus.setFinished(status.isFinished());
			theStatus.setCheckPointed(checkpointManager.ApplyCheckPointPolicy());
			
			// write buffer size reached
			if (readProcessedCount == chunkSize) {
				break;
			}
			
			// checkpoint reached
			if (theStatus.isCheckPointed()) {
				break;
			}
			
			// last record in readerProxy reached
			if (status.isFinished()) {
				break;
			}
			
			// TODO: we need to break here on rollback read
			// if (status.isRollbackRead) {
			//	break;
			//}
			
		}
		logger.exiting(sourceClass, "readAndProcess", chunkToWrite);
		return chunkToWrite;
	}
	
	/**
	 * Reads an item from the reader
	 * 
	 * @param status flags the current read status
	 * @return the item read
	 */
	private Object readItem(ItemStatus status) {
		logger.entering(sourceClass, "readItem", status);
		Object itemRead = null;
		
		try {
			itemRead = readerProxy.readItem();
			
			// itemRead == null means we reached the end of
			// the readerProxy "resultset"
			status.setFinished(itemRead == null);
			if (!status.isFinished()) {
				stepContext.getMetric(MetricImpl.Counter.valueOf("READ_COUNT")).incValue();	
			}
		} catch (Exception e) {
			
			if (skipReadException(e)) {
				status.setSkipped(true);
				stepContext.getMetric(MetricImpl.Counter.valueOf("READ_SKIP_COUNT")).incValue();
			} else if (retryReadException(e)) {
				itemRead = readItem(status);
			// TODO: handler rollback read
			// } else if (rollbackReadException(e)) {
			//	status.setRollbackRead(true);
			} else {
				throw new BatchContainerRuntimeException(e);
			}
			
		} catch (Throwable e) {
			throw new BatchContainerRuntimeException(e);
		}
	
		logger.exiting(sourceClass, "readItem", itemRead);
		return itemRead;
	}
	
	/**
	 * Process an item previously read by the reader
	 * 
	 * @param itemRead the item read
	 * @param status flags the current process status
	 * @return the processed item
	 */
	private Object processItem(Object itemRead, ItemStatus status) {
		logger.entering(sourceClass, "processItem", new Object[] {itemRead, status});
		Object processedItem = null;
		
		try {
			
			processedItem = processorProxy.processItem(itemRead);
			
		} catch (Exception e) {
			
			if (skipProcessException(e, itemRead)) {
				status.setSkipped(true);
				stepContext.getMetric(MetricImpl.Counter.valueOf("PROCESS_SKIP_COUNT")).incValue();
			} else if (retryProcessException(e, itemRead)) {
				processedItem = processItem(itemRead, status);
			} else {
				throw new BatchContainerRuntimeException(e);
			}
			
		} catch (Throwable e) {
			throw new BatchContainerRuntimeException(e);
		}
		
		logger.exiting(sourceClass, "processItem", processedItem);
		return processedItem;
	}

	/**
	 * Writes items
	 * 
	 * @param theChunk the array list with all items processed ready to be written
	 */
	private void writeChunk(List<Object> theChunk) {
		logger.entering(sourceClass, "writeChunk", theChunk);
		if (!theChunk.isEmpty()) {
			
			try {
				
				writerProxy.writeItems(theChunk);
				stepContext.getMetric(MetricImpl.Counter.valueOf("WRITE_COUNT")).incValueBy(theChunk.size());
			} catch (Exception e) {
				
				if (skipWriteException(e, theChunk)) {
					stepContext.getMetric(MetricImpl.Counter.valueOf("WRITE_SKIP_COUNT")).incValueBy(theChunk.size());
				} else if (retryWriteException(e, theChunk)) {
					writeChunk(theChunk);
				} else {
					throw new BatchContainerRuntimeException(e);
				}
				
			} catch (Throwable e) {
				throw new BatchContainerRuntimeException(e);
			}	
		}
		logger.exiting(sourceClass, "writeChunk");
	}

	/**
	 * Main Read-Process-Write loop
	 * 
	 * @throws Exception 
	 */
	private void invokeChunk() throws Exception {
		logger.entering(sourceClass, "invokeChunk2");
		
		int chunkSize = ChunkHelper.getBufferSize(chunk);
		List<Object> chunkToWrite = new ArrayList<Object>();
		boolean checkPointed = true;
		
		while(true) {
			if (jobExecutionImpl.getJobContext().getBatchStatus().equals(
					ExecutionStatus.getStringValue(BatchStatus.STOPPING))) {
				this.stepContext.setBatchStatus(ExecutionStatus.getStringValue(BatchStatus.STOPPED));
				break;
			}
			
			try {
				// begin new transaction at first iteration or after a checkpoint commit
				if (checkPointed) {
					transactionManager.begin();
				}
				
				ItemStatus status = new ItemStatus();
				chunkToWrite = readAndProcess(chunkSize, status);
				writeChunk(chunkToWrite);
				checkPointed = status.isCheckPointed();
				
				// we could finish the chunck in 3 conditions: buffer is full, checkpoint, not more input
				if (status.isCheckPointed() || status.isFinished()) {
					
					// TODO: missing before checkpoint listeners
					// 1.- check if spec list proper steps for before checkpoint
					// 2.- ask Andy about retry
					// 3.- when do we stop?
					checkpointManager.checkpoint();
					
					for (CheckpointListenerProxy cpListenerProxy : checkpointListeners) {
						cpListenerProxy.afterCheckpoint();
					}
					
					transactionManager.commit();
					
					// exit loop when last record is written
					if (status.isFinished()) {
						break;
					}
				}
				
			} catch (Exception e) {
				
				transactionManager.rollback();
				logger.log(Level.SEVERE, "OMG! something bad happened in the Read-Process-Write Loop, your turn.", e);
				throw new BatchContainerRuntimeException(e);
				
			}
			
		}
		
		logger.exiting(sourceClass, "invokeChunk2");
	}


	protected void invokeCoreStep() throws BatchContainerServiceException {

		this.chunk = step.getChunk();

		initializeChunkArtifacts();

		try {
			invokeChunk();
		} catch (Exception re) {
			throw new BatchContainerServiceException(re);
		}

		// TODO invoke analyzeExitStatus in analyzer if it exists
	}

	/*
	 * Initialize itemreader, itemwriter, and item processor checkpoint
	 */
	private void initializeChunkArtifacts() {
		String sourceMethod = "initializeChunkArtifacts";
		if (logger.isLoggable(Level.FINE))
			logger.entering(sourceClass, sourceMethod);

		List<Property> propList = (chunk.getProperties() == null) ? null
				: chunk.getProperties().getPropertyList();

		String readerId = chunk.getReader();
		try {
			//Filter the properties targetted to a specific chunk artifact
			List<Property> filteredProps = this.filterChunkProperties(propList, CHUNK_ARTIFACT.READER);
			readerProxy = ProxyFactory.createItemReaderProxy(readerId, filteredProps);
			
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Created ItemReaderProxy for " + readerId);
			}
		} catch (ArtifactValidationException e) {
			throw new BatchContainerServiceException(
					"Cannot create the ItemReader [" + readerId + "]", e);
		}

		String processorId = chunk.getProcessor();
		try {
			//Filter the properties targetted to a specific chunk artifact
			List<Property> filteredProps = this.filterChunkProperties(propList, CHUNK_ARTIFACT.PROCESSOR);
			processorProxy = ProxyFactory.createItemProcessorProxy(processorId,	filteredProps);
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Created ItemProcessorProxy for " + processorId);
			}
		} catch (ArtifactValidationException e) {
			throw new BatchContainerServiceException(
					"Cannot create the ItemProcessor [" + processorId + "]", e);
		}

		String writerId = chunk.getWriter();
		try {
			List<Property> filteredProps = this.filterChunkProperties(propList, CHUNK_ARTIFACT.WRITER);
			writerProxy = ProxyFactory.createItemWriterProxy(writerId, filteredProps);
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Created ItemWriterProxy for " + writerId);
			}
		} catch (ArtifactValidationException e) {
			throw new BatchContainerServiceException(
					"Cannot create the ItemWriter [" + writerId + "]", e);
		}

		try {
			checkpointProxy = CheckpointAlgorithmFactory
					.getCheckpointAlgorithmProxy(step);
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Created CheckpointAlgorithmProxy for policy ["
						+ chunk.getCheckpointPolicy() + "]");
			}
		} catch (ArtifactValidationException e) {
			throw new BatchContainerServiceException(
					"Cannot create the CheckpointAlgorithm for policy ["
							+ chunk.getCheckpointPolicy() + "]", e);
		}

		int commitInterval = ChunkHelper.getCommitInterval(chunk);
		this.checkpointListeners = jobExecutionImpl.getListenerFactory()
				.getCheckpointListeners(step);
		this.skipListeners = jobExecutionImpl.getListenerFactory()
				.getSkipListeners(step);
		for (SkipListenerProxy listenerProxy : this.skipListeners) {
			listenerProxy.setJobContext(jobExecutionImpl.getJobContext());
			//listenerProxy.setSplitContext(splitContext);
			//listenerProxy.setFlowContext(flowContext);
			listenerProxy.setStepContext(stepContext);
		}
		
		this.retryListeners = jobExecutionImpl.getListenerFactory()
				.getRetryListeners(step);
		for (RetryListenerProxy listenerProxy : this.retryListeners) {
			listenerProxy.setJobContext(jobExecutionImpl.getJobContext());
			//listenerProxy.setSplitContext(splitContext);
			//listenerProxy.setFlowContext(flowContext);
			listenerProxy.setStepContext(stepContext);
		}
		
		if (checkpointProxy.getCheckpointType() == "item") {
			chkptAlg = new ItemCheckpointAlgorithm();
			chkptAlg.setThreshold(commitInterval);
		} else if (checkpointProxy.getCheckpointType() == "time") {
			chkptAlg = new TimeCheckpointAlgorithm();
			chkptAlg.setThreshold(commitInterval);
		} else if (checkpointProxy.getCheckpointType() == "item-time") {
			chkptAlg = new ItemTimeCheckpointAlgorithm();

			JSLProperties jslProps = step.getChunk().getCheckpointAlgorithm()
					.getProperties();
			String itemString;
			String timeString;
			int item = 0;
			int time = 0;

			if (jslProps != null) {
				for (Property property : jslProps.getPropertyList()) {
					String propName = property.getName();
					if (propName.equals("item")) {
						itemString = property.getValue();
						item = Integer.parseInt(itemString);
					} else if (propName.equals("time")) {
						timeString = property.getValue();
						time = Integer.parseInt(timeString);
					}
				}
			}

			if (item > 0 && time > 0) {
				chkptAlg.setThresholds(item, time);
			}
		} else {
			chkptAlg = checkpointProxy;
		}

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Setting contexts for chunk artifacts");
		}

		readerProxy.setJobContext(jobExecutionImpl.getJobContext());
		processorProxy.setJobContext(jobExecutionImpl.getJobContext());
		writerProxy.setJobContext(jobExecutionImpl.getJobContext());

		readerProxy.setStepContext(stepContext);
		processorProxy.setStepContext(stepContext);
		writerProxy.setStepContext(stepContext);
		
		if (logger.isLoggable(Level.FINE))
			logger.fine("Initialize checkpoint manager with commit-interval="
					+ commitInterval);

		checkpointManager = new CheckpointManager(readerProxy, writerProxy,
				chkptAlg, commitInterval, jobExecutionImpl.getExecutionId(),
				jobExecutionImpl.getJobInstance().getInstanceId(), step.getId());

		skipHandler = new SkipHandler(chunk, jobExecutionImpl.getJobInstance()
				.getInstanceId(), step.getId());
		for (SkipListenerProxy skipListenerProxy : skipListeners) {
			skipHandler.addSkipListener(skipListenerProxy);
		}

		retryHandler = new RetryHandler(chunk, jobExecutionImpl
				.getJobInstance().getInstanceId(), step.getId());
		for (RetryListenerProxy retryListenerProxy : retryListeners) {
			retryHandler.addRetryListener(retryListenerProxy);
		}

		_persistenceManagerService = (IPersistenceManagerService) servicesManager
				.getService(ServiceType.PERSISTENCE_MANAGEMENT_SERVICE);
		readerChkptDK = new CheckpointDataKey(jobExecutionImpl.getJobInstance()
				.getInstanceId(), step.getId(), "READER");
		List<?> data = _persistenceManagerService.getData(
				IPersistenceManagerService.CHECKPOINT_STORE_ID, readerChkptDK);
		try {
			
			//check for data in backing store
			if (data.size() >= 1) {
				
				readerChkptData = (CheckpointData) data.get(0);
				byte[] readertoken = readerChkptData.getRestartToken();
				ByteArrayInputStream readerChkptBA = new ByteArrayInputStream(
						readertoken);
				TCCLObjectInputStream readerOIS = null;
				try {
					readerOIS = new TCCLObjectInputStream(readerChkptBA);
					readerProxy.openReader(readerOIS.readObject());
					readerOIS.close();
				} catch (Exception ex) {
					// is this what I should be throwing here?
					throw new BatchContainerServiceException(
							"Cannot persist the checkpoint data for ["
									+ step.getId() + "]", ex);
				}
			} else {
				// no chkpt data exists in the backing store
				readerChkptData = null;
				readerProxy.openReader(null);
			}
		} catch (ClassCastException e) {
			throw new IllegalStateException("Expected CheckpointData but found"
					+ data.get(0));
		} 

		writerChkptDK = new CheckpointDataKey(jobExecutionImpl.getJobInstance()
				.getInstanceId(), step.getId(), "WRITER");
		data = _persistenceManagerService.getData(
				IPersistenceManagerService.CHECKPOINT_STORE_ID, writerChkptDK);

		try {
			writerChkptData = (CheckpointData) data.get(0);
			byte[] writertoken = writerChkptData.getRestartToken();
			ByteArrayInputStream writerChkptBA = new ByteArrayInputStream(
					writertoken);
			TCCLObjectInputStream writerOIS = null;
			try {
				writerOIS = new TCCLObjectInputStream(writerChkptBA);
				writerProxy.openWriter(writerOIS.readObject());
				writerOIS.close();
			} catch (Exception ex) {
				// is this what I should be throwing here?
				throw new BatchContainerServiceException(
						"Cannot persist the checkpoint data for ["
								+ step.getId() + "]", ex);
			}
		} catch (ClassCastException e) {
			throw new IllegalStateException("Expected Checkpoint but found"
					+ data.get(0));
		} catch (IndexOutOfBoundsException ioobEx) {
			// no chkpt data exists in the backing store
			writerChkptData = null;
			writerProxy.openWriter(null);
		}
		
		// set up metrics
		//stepContext.addMetric(MetricImpl.Counter.valueOf("READ_COUNT"), 0);
		//stepContext.addMetric(MetricImpl.Counter.valueOf("WRITE_COUNT"), 0);
		//stepContext.addMetric(MetricImpl.Counter.valueOf("READ_SKIP_COUNT"), 0);
		//stepContext.addMetric(MetricImpl.Counter.valueOf("PROCESS_SKIP_COUNT"), 0);
		//stepContext.addMetric(MetricImpl.Counter.valueOf("WRITE_SKIP_COUNT"), 0);

		if (logger.isLoggable(Level.FINE))
			logger.exiting(sourceClass, sourceMethod);
	}

	@Override
	public void stop() {
		stepContext.setBatchStatus(ExecutionStatus
				.getStringValue(BatchStatus.STOPPING));

		// we don't need to call stop on the chunk implementation here since a
		// chunk always returns control to
		// the batch container after every item.

	}

	boolean skipReadException(Exception e) {

		try {
			skipHandler.handleException(e);
		} catch (BatchContainerRuntimeException bcre){
			return false;
		}
		
		return true;

	}

	boolean retryReadException(Exception e) {
		
		try {
			retryHandler.handleNoRollbackExceptionRead(e);
		}
		catch (BatchContainerRuntimeException bcre){
			return false;
		}
		
		return true;

	}

	boolean skipProcessException(Exception e, Object record) {

		try {
			skipHandler.handleExceptionWithRecordProcess(e, record);
		} catch (BatchContainerRuntimeException bcre){
			return false;
		}
		
		return true;

	}
	
	boolean retryProcessException(Exception e, Object record) {

		try {
			retryHandler.handleNoRollbackExceptionWithRecordProcess(e, record);
		} catch (BatchContainerRuntimeException bcre){
			return false;
		}
		
		return true;

	}
	
	boolean skipWriteException(Exception e, List<Object> chunkToWrite) {

		Object writeObjs[] = chunkToWrite.toArray();
		for (int i = 0; i < writeObjs.length; i++) {
			try {
				skipHandler.handleExceptionWithRecordWrite(e, writeObjs[i]);
			}
			catch (BatchContainerRuntimeException bcre){
				return false;
			}
		}
		
		return true;

	}

	boolean retryWriteException(Exception e, List<Object> chunkToWrite) {

		Object writeObjs[] = chunkToWrite.toArray();
		for (int i = 0; i < writeObjs.length; i++) {
			try {
				retryHandler.handleNoRollbackExceptionRead(e);
			}
			catch (BatchContainerRuntimeException bcre){
				return false;
			}
		}
		
		return true;

	}
	
	/**
	 * Use this to filter the chunk properties for readers, writers and processors. This filter 
	 * includes all properties where target string equals filter. For example, if filter is set to CHUNK_ARTIFACT.READER
	 * this includes all properties where target contains "reader" or target is not defined.  
	 * @param chunkProps the full list of jsl properties under a chunk
	 * @param filter CHUNK_ARTIFACT type to use to filter the chunk properties that are targetted to a specific 
	 *        chunk artifact.
	 * @return A new copy of a list of properties after 'filter' has been applied to 'chunkProps' 
	 */
	private List<Property> filterChunkProperties(List<Property> chunkProps, CHUNK_ARTIFACT filter) {
		if (chunkProps == null){
			return null;
		}
		
		List<Property> filteredPropertyList = new ArrayList<Property>();
		ObjectFactory jslObjectFactory = new ObjectFactory();
		
		for (Property prop: chunkProps) {
			String target = prop.getTarget();
			
			if (target == null || target.contains(filter.getChunkArtifactType())){
				Property filteredProp = jslObjectFactory.createProperty();
				filteredProp.setName(prop.getName());
				filteredProp.setValue(prop.getValue());
				filteredProp.setTarget(prop.getTarget());
				
				filteredPropertyList.add(filteredProp);
				
			}
		}
		
		return filteredPropertyList;
	}
}
