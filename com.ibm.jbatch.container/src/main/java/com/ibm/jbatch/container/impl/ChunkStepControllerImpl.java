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
package com.ibm.jbatch.container.impl;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.api.chunk.CheckpointAlgorithm;
import javax.batch.runtime.BatchStatus;

import com.ibm.jbatch.container.artifact.proxy.CheckpointAlgorithmProxy;
import com.ibm.jbatch.container.artifact.proxy.ChunkListenerProxy;
import com.ibm.jbatch.container.artifact.proxy.InjectionReferences;
import com.ibm.jbatch.container.artifact.proxy.ItemProcessListenerProxy;
import com.ibm.jbatch.container.artifact.proxy.ItemProcessorProxy;
import com.ibm.jbatch.container.artifact.proxy.ItemReadListenerProxy;
import com.ibm.jbatch.container.artifact.proxy.ItemReaderProxy;
import com.ibm.jbatch.container.artifact.proxy.ItemWriteListenerProxy;
import com.ibm.jbatch.container.artifact.proxy.ItemWriterProxy;
import com.ibm.jbatch.container.artifact.proxy.ProxyFactory;
import com.ibm.jbatch.container.artifact.proxy.RetryProcessListenerProxy;
import com.ibm.jbatch.container.artifact.proxy.RetryReadListenerProxy;
import com.ibm.jbatch.container.artifact.proxy.RetryWriteListenerProxy;
import com.ibm.jbatch.container.artifact.proxy.SkipProcessListenerProxy;
import com.ibm.jbatch.container.artifact.proxy.SkipReadListenerProxy;
import com.ibm.jbatch.container.artifact.proxy.SkipWriteListenerProxy;
import com.ibm.jbatch.container.context.impl.MetricImpl;
import com.ibm.jbatch.container.context.impl.StepContextImpl;
import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.exception.TransactionManagementException;
import com.ibm.jbatch.container.jobinstance.RuntimeJobExecution;
import com.ibm.jbatch.container.persistence.CheckpointData;
import com.ibm.jbatch.container.persistence.CheckpointDataKey;
import com.ibm.jbatch.container.persistence.CheckpointManager;
import com.ibm.jbatch.container.persistence.ItemCheckpointAlgorithm;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.servicesmanager.ServicesManager;
import com.ibm.jbatch.container.servicesmanager.ServicesManagerImpl;
import com.ibm.jbatch.container.util.PartitionDataWrapper;
import com.ibm.jbatch.container.util.TCCLObjectInputStream;
import com.ibm.jbatch.container.validation.ArtifactValidationException;
import com.ibm.jbatch.jsl.model.Chunk;
import com.ibm.jbatch.jsl.model.ItemProcessor;
import com.ibm.jbatch.jsl.model.ItemReader;
import com.ibm.jbatch.jsl.model.ItemWriter;
import com.ibm.jbatch.jsl.model.Property;
import com.ibm.jbatch.jsl.model.Step;

public class ChunkStepControllerImpl extends SingleThreadedStepControllerImpl {

	private final static String sourceClass = ChunkStepControllerImpl.class.getName();
	private final static Logger logger = Logger.getLogger(sourceClass);

    protected static final int DEFAULT_TRAN_TIMEOUT_SECONDS = 180;  // From the spec Sec. 9.7

	private Chunk chunk = null;
	private ItemReaderProxy readerProxy = null;
	private ItemProcessorProxy processorProxy = null;
	private ItemWriterProxy writerProxy = null;
	private CheckpointManager checkpointManager;
	private ServicesManager servicesManager = ServicesManagerImpl.getInstance();
	private IPersistenceManagerService _persistenceManagerService = null;
	private SkipHandler skipHandler = null;
	CheckpointDataKey readerChkptDK, writerChkptDK = null;
	CheckpointData readerChkptData = null;
	CheckpointData writerChkptData = null;
	List<ChunkListenerProxy> chunkListeners = null;
	List<SkipProcessListenerProxy> skipProcessListeners = null;
	List<SkipReadListenerProxy> skipReadListeners = null;
	List<SkipWriteListenerProxy> skipWriteListeners = null;
	List<RetryProcessListenerProxy> retryProcessListeners = null;
	List<RetryReadListenerProxy> retryReadListeners = null;
	List<RetryWriteListenerProxy> retryWriteListeners = null;
	List<ItemReadListenerProxy> itemReadListeners = null;
	List<ItemProcessListenerProxy> itemProcessListeners = null;
	List<ItemWriteListenerProxy> itemWriteListeners = null;
	private RetryHandler retryHandler;

	// metrics
	long readCount = 0;
	long writeCount = 0;
	long readSkipCount = 0;
	long processSkipCount = 0;
	long writeSkipCount = 0;

	protected ChunkStatus currentChunkStatus;
	protected SingleItemStatus currentItemStatus;

	// Default is item-based policy
	protected boolean customCheckpointPolicy = false;
	protected Integer checkpointAtThisItemCount = null;  // Default to spec value elsewhere.

	protected int stepPropertyTranTimeoutSeconds = DEFAULT_TRAN_TIMEOUT_SECONDS;	

	public ChunkStepControllerImpl(RuntimeJobExecution jobExecutionImpl, Step step, StepContextImpl stepContext, long rootJobExecutionId, BlockingQueue<PartitionDataWrapper> analyzerStatusQueue) {
		super(jobExecutionImpl, step, stepContext, rootJobExecutionId, analyzerStatusQueue);
	}

	/**
	 * Utility Class to hold status for a single item as the read-process portion of
	 * the chunk loop interact.
	 */
	private class SingleItemStatus {

		public boolean isSkipped() {
			return skipped;
		}

		public void setSkipped(boolean skipped) {
			this.skipped = skipped;
		}

		public boolean isFiltered() {
			return filtered;
		}

		public void setFiltered(boolean filtered) {
			this.filtered = filtered;
		}

		private boolean skipped = false;
		private boolean filtered = false;
	}

	private static enum ChunkStatusType { NORMAL, RETRY_AFTER_ROLLBACK};

	/**
	 * Utility Class to hold status for the chunk as a whole.  
	 * 
	 * One key usage is to maintain the state reflecting the sequence in which
	 * we catch a retryable exception, rollback the previous chunk, process 1-item-at-a-time
	 * until we reach "where we left off", then revert to normal chunk processing.
	 * 
	 * Another usage is simply to communicate that the reader readItem() returned 'null', so
	 * we're done the chunk.
	 */
	private class ChunkStatus {
		
		ChunkStatusType type;

		ChunkStatus() {
			type = ChunkStatusType.NORMAL;
		}

		ChunkStatus(ChunkStatusType type) {
			this.type = type;
		}

		public boolean isFinished() {
			return finished;
		}

		public void setFinished(boolean finished) {
			this.finished = finished;
		}

		public boolean isRetryingAfterRollback() {
			return type == ChunkStatusType.RETRY_AFTER_ROLLBACK;
		}

		public boolean wasMarkedForRollbackWithRetry() {
			return markedForRollbackWithRetry;
		}

		public Exception getRetryableException() {
			return retryableException;
		}

		public void markForRollbackWithRetry(Exception retryableException) {
			this.markedForRollbackWithRetry = true;
			this.retryableException = retryableException;
		}

		public int getItemsTouchedInCurrentChunk() {
			return itemsTouchedInCurrentChunk;
		}

		public void incrementItemsTouchedInCurrentChunk() {
			this.itemsTouchedInCurrentChunk++;
		}

		public int getItemsToProcessOneByOneAfterRollback() {
			return itemsToProcessOneByOneAfterRollback;
		}

		public void setItemsToProcessOneByOneAfterRollback(
				int itemsToProcessOneByOneAfterRollback) {
			this.itemsToProcessOneByOneAfterRollback = itemsToProcessOneByOneAfterRollback;
		}

		private boolean finished = false;
		private Exception retryableException = null;

		private boolean markedForRollbackWithRetry = false;
		private int itemsTouchedInCurrentChunk = 0;
		private int itemsToProcessOneByOneAfterRollback = 0; // For retry with rollback
	}

	/**
	 * We read and process one item at a time but write in chunks (group of
	 * items). So, this method loops until we either reached the end of the
	 * reader (not more items to read), or the writer buffer is full or a
	 * checkpoint is triggered.
	 * 
	 * @return an array list of objects to write
	 */
	private List<Object> readAndProcess() {
		logger.entering(sourceClass, "readAndProcess");

		List<Object> chunkToWrite = new ArrayList<Object>();
		Object itemRead = null;
		Object itemProcessed = null;

		while (true) {
			currentItemStatus = new SingleItemStatus();
			currentChunkStatus.incrementItemsTouchedInCurrentChunk();
			itemRead = readItem();

			if (currentChunkStatus.wasMarkedForRollbackWithRetry()) {
				break;
			}

			if (!currentItemStatus.isSkipped() && !currentChunkStatus.isFinished()) {
				itemProcessed = processItem(itemRead);

				if (currentChunkStatus.wasMarkedForRollbackWithRetry()) {
					break;
				}

				if (!currentItemStatus.isSkipped() && !currentItemStatus.isFiltered()) {
					chunkToWrite.add(itemProcessed);
				}
			}

			// Break out of the loop to deliver one-at-a-time processing after rollback.
			// No point calling isReadyToCheckpoint(), we know we're done.  Let's not
			// complicate the checkpoint algorithm to hold this logic, just break right here.
			if (currentChunkStatus.isRetryingAfterRollback()) {
				break;
			}

			// This will force the current item to finish processing on a stop request
			if (stepContext.getBatchStatus().equals(BatchStatus.STOPPING)) {
				currentChunkStatus.setFinished(true);
			}

			// The spec, in Sec. 11.10, Chunk with Custom Checkpoint Processing, clearly
			// outlines that this gets called even when we've already read a null (which
			// arguably is pointless).   But we'll follow the spec.
			if (checkpointManager.isReadyToCheckpoint()) {
				break;
			}

			// last record in readerProxy reached
			if (currentChunkStatus.isFinished()) {
				break;
			}

		}
		logger.exiting(sourceClass, "readAndProcess", chunkToWrite);
		return chunkToWrite;
	}

	/**
	 * Reads an item from the reader
	 * 
	 * @return the item read
	 */
	private Object readItem() {
		logger.entering(sourceClass, "readItem");
		Object itemRead = null;

		try {
			// call read listeners before and after the actual read
			for (ItemReadListenerProxy readListenerProxy : itemReadListeners) {
				readListenerProxy.beforeRead();
			}

			itemRead = readerProxy.readItem();

			for (ItemReadListenerProxy readListenerProxy : itemReadListeners) {
				readListenerProxy.afterRead(itemRead);
			}

			// itemRead == null means we reached the end of
			// the readerProxy "resultset"
			currentChunkStatus.setFinished(itemRead == null);
		} catch (Exception e) {
			stepContext.setException(e);
			for (ItemReadListenerProxy readListenerProxy : itemReadListeners) {
				readListenerProxy.onReadError(e);
			}
			if(!currentChunkStatus.isRetryingAfterRollback()) {
				if (retryReadException(e)) {
					if (!retryHandler.isRollbackException(e)) {
						// retry without rollback
						itemRead = readItem();
					} else {
						// retry with rollback
						currentChunkStatus.markForRollbackWithRetry(e);
					}
				}
				else if(skipReadException(e)) {
					currentItemStatus.setSkipped(true);
					stepContext.getMetric(MetricImpl.MetricType.READ_SKIP_COUNT).incValue();

				}
				else {
					throw new BatchContainerRuntimeException(e);
				}
			}
			else {
				// coming from a rollback retry
				if(skipReadException(e)) {
					currentItemStatus.setSkipped(true);
					stepContext.getMetric(MetricImpl.MetricType.READ_SKIP_COUNT).incValue();

				}
				else if (retryReadException(e)) {
					if (!retryHandler.isRollbackException(e)) {
						// retry without rollback
						itemRead = readItem();
					}
					else {
						// retry with rollback
						currentChunkStatus.markForRollbackWithRetry(e);
					}
				}
				else {
					throw new BatchContainerRuntimeException(e);
				}
			}

		} catch (Throwable e) {
			throw new BatchContainerRuntimeException(e);
		}

		logger.exiting(sourceClass, "readItem", itemRead==null ? "<null>" : itemRead);
		return itemRead;
	}

	/**
	 * Process an item previously read by the reader
	 * 
	 * @param itemRead
	 *            the item read
	 * @return the processed item
	 */
	private Object processItem(Object itemRead) {
		logger.entering(sourceClass, "processItem", itemRead);
		Object processedItem = null;

		// if no processor defined for this chunk
		if (processorProxy == null){
			return itemRead;
		}

		try {

			// call process listeners before and after the actual process call
			for (ItemProcessListenerProxy processListenerProxy : itemProcessListeners) {
				processListenerProxy.beforeProcess(itemRead);
			}

			processedItem = processorProxy.processItem(itemRead);

			if (processedItem == null) {
				currentItemStatus.setFiltered(true);
			}

			for (ItemProcessListenerProxy processListenerProxy : itemProcessListeners) {
				processListenerProxy.afterProcess(itemRead, processedItem);
			}
		} catch (Exception e) {
			for (ItemProcessListenerProxy processListenerProxy : itemProcessListeners) {
				processListenerProxy.onProcessError(itemRead, e);
			}
			if(!currentChunkStatus.isRetryingAfterRollback()) {
				if (retryProcessException(e, itemRead)) {
					if (!retryHandler.isRollbackException(e)) {
						processedItem = processItem(itemRead);
					} else {
						currentChunkStatus.markForRollbackWithRetry(e);
					}
				}
				else if (skipProcessException(e, itemRead)) {
					currentItemStatus.setSkipped(true);
					stepContext.getMetric(MetricImpl.MetricType.PROCESS_SKIP_COUNT).incValue();
				}
				else {
					throw new BatchContainerRuntimeException(e);
				}
			}
			else {
				if (skipProcessException(e, itemRead)) {
					currentItemStatus.setSkipped(true);
					stepContext.getMetric(MetricImpl.MetricType.PROCESS_SKIP_COUNT).incValue();
				} else if (retryProcessException(e, itemRead)) {

					if (!retryHandler.isRollbackException(e)) {
						// retry without rollback
						processedItem = processItem(itemRead);
					} else {
						// retry with rollback
						currentChunkStatus.markForRollbackWithRetry(e);
					}
				} else {
					throw new BatchContainerRuntimeException(e);
				}
			}

		} catch (Throwable e) {
			throw new BatchContainerRuntimeException(e);
		}

		logger.exiting(sourceClass, "processItem", processedItem==null ? "<null>" : processedItem);
		return processedItem;
	}

	/**
	 * Writes items
	 * 
	 * @param theChunk
	 *            the array list with all items processed ready to be written
	 */
	private void writeChunk(List<Object> theChunk) {
		logger.entering(sourceClass, "writeChunk", theChunk);
		if (!theChunk.isEmpty()) {
			try {

				// call read listeners before and after the actual read
				for (ItemWriteListenerProxy writeListenerProxy : itemWriteListeners) {
					writeListenerProxy.beforeWrite(theChunk);
				}

				writerProxy.writeItems(theChunk);

				for (ItemWriteListenerProxy writeListenerProxy : itemWriteListeners) {
					writeListenerProxy.afterWrite(theChunk);
				}
			} catch (Exception e) {
				this.stepContext.setException(e);
				for (ItemWriteListenerProxy writeListenerProxy : itemWriteListeners) {
					writeListenerProxy.onWriteError(theChunk, e);
				}
				if(!currentChunkStatus.isRetryingAfterRollback()) {

					if (retryWriteException(e, theChunk)) {
						if (!retryHandler.isRollbackException(e)) {
							// retry without rollback
							writeChunk(theChunk);
						} else {
							// retry with rollback
							currentChunkStatus.markForRollbackWithRetry(e);
						}
					} else if (skipWriteException(e, theChunk)) {
						stepContext.getMetric(MetricImpl.MetricType.WRITE_SKIP_COUNT).incValueBy(1);
					} else {
						throw new BatchContainerRuntimeException(e);
					}

				}
				else {
					if (skipWriteException(e, theChunk)) {
						stepContext.getMetric(MetricImpl.MetricType.WRITE_SKIP_COUNT).incValueBy(1);
					} else if (retryWriteException(e, theChunk)) {
						if (!retryHandler.isRollbackException(e)) {
							// retry without rollback
							writeChunk(theChunk);
						} else {
							// retry with rollback
							currentChunkStatus.markForRollbackWithRetry(e);
						}
					} else {
						throw new BatchContainerRuntimeException(e);
					}
				}

			} catch (Throwable e) {
				throw new BatchContainerRuntimeException(e);
			}
		}
		logger.exiting(sourceClass, "writeChunk");
	}

	/**
	 * Prime the next chunk's ChunkStatus based on the previous one
	 * (if there was one), particularly taking into account retry-with-rollback
	 * and the one-at-a-time processing it entails.
	 * @return the upcoming chunk's ChunkStatus
	 */
	private ChunkStatus getNextChunkStatusBasedOnPrevious() {
		
		// If this is the first chunk
		if (currentChunkStatus == null) {
			return new ChunkStatus();
		}

		ChunkStatus nextChunkStatus = null;

		// At this point the 'current' status is the previous chunk's status.
		if (currentChunkStatus.wasMarkedForRollbackWithRetry()) {

			// Re-position reader & writer
			transactionManager.begin();
			positionReaderAtCheckpoint();
			positionWriterAtCheckpoint();
			transactionManager.commit();

			nextChunkStatus = new ChunkStatus(ChunkStatusType.RETRY_AFTER_ROLLBACK);
			
			// What happens if we get a retry-with-rollback on a single item that we were processing
			// after a prior retry with rollback?   We don't want to revert to normal processing
			// after completing only the single item of the "single item chunk".  We want to complete
			// the full portion of the original chunk.  So be careful to propagate this number if
			// it already exists.
			int numToProcessOneByOne = currentChunkStatus.getItemsToProcessOneByOneAfterRollback();
			if (numToProcessOneByOne > 0) {
				// Retry after rollback AFTER a previous retry after rollback
				nextChunkStatus.setItemsToProcessOneByOneAfterRollback(numToProcessOneByOne);
			} else {
				// "Normal" (i.e. the first) retry after rollback.
				nextChunkStatus.setItemsToProcessOneByOneAfterRollback(currentChunkStatus.getItemsTouchedInCurrentChunk());
			}
		} else if (currentChunkStatus.isRetryingAfterRollback()) {
			// In this case the 'current' (actually the last) chunk was a single-item retry after rollback chunk,
			// so we have to see if it's time to revert to normal processing.
			int numToProcessOneByOne = currentChunkStatus.getItemsToProcessOneByOneAfterRollback();
			if (numToProcessOneByOne == 1) {
				// we're done, revert to normal
				nextChunkStatus = new ChunkStatus();
			} else {
				nextChunkStatus = new ChunkStatus(ChunkStatusType.RETRY_AFTER_ROLLBACK);
				nextChunkStatus.setItemsToProcessOneByOneAfterRollback(numToProcessOneByOne - 1);
			}
		} else {
			nextChunkStatus = new ChunkStatus();
		}
		
		return nextChunkStatus;
	}

	/**
	 * Main Read-Process-Write loop
	 * 
	 * @throws Exception
	 */
	private void invokeChunk() {
		logger.entering(sourceClass, "invokeChunk");

		List<Object> chunkToWrite = new ArrayList<Object>();

		try {
			transactionManager.begin();
			this.openReaderAndWriter();
			transactionManager.commit();

			while (true) {

				// Done with the previous chunk status so advance reference to next one.
				currentChunkStatus = getNextChunkStatusBasedOnPrevious();

				// Sequence surrounding beginCheckpoint() updated per MR
				// https://java.net/bugzilla/show_bug.cgi?id=5873
				setNextChunkTransactionTimeout();

				// Remember we "wrap" the built-in item-count + time-limit "algorithm"
				// in a CheckpointAlgorithm for ease in keeping the sequence consistent
				checkpointManager.beginCheckpoint();

				transactionManager.begin();

				for (ChunkListenerProxy chunkProxy : chunkListeners) {
					chunkProxy.beforeChunk();
				}

				chunkToWrite = readAndProcess();

				if (currentChunkStatus.wasMarkedForRollbackWithRetry()) {
					rollbackAfterRetryableException();
					continue;
				}

				// MR 1.0 Rev A clarified we'd only write a chunk with at least one item.
				// See, e.g. Sec 11.6 of Spec
				if (chunkToWrite.size() > 0) {
					writeChunk(chunkToWrite);
				}

				if (currentChunkStatus.wasMarkedForRollbackWithRetry()) {
					rollbackAfterRetryableException();
					continue;
				}

				for (ChunkListenerProxy chunkProxy : chunkListeners) {
					chunkProxy.afterChunk();
				}

				checkpointManager.checkpoint();

				this.persistUserData();

				transactionManager.commit();

				checkpointManager.endCheckpoint();

				invokeCollectorIfPresent();

				updateNormalMetrics(chunkToWrite.size());

				// exit loop when last record is written
				if (currentChunkStatus.isFinished()) {
					transactionManager.begin();

					writerProxy.close();
					readerProxy.close();

					transactionManager.commit();
					break;
				}
			}
		} catch (Throwable t) {		
			// Note we've already carefully handled skippable and retryable exceptions.  Anything surfacing to this
			// level does not need to be considered as either.
			try {
				logger.log(Level.SEVERE, "Failure in Read-Process-Write Loop", t);
				transactionManager.setRollbackOnly();

				callReaderAndWriterCloseOnThrowable(t);

				// Signature is onError(Exception) so only try to call if we have an Exception, but not an Error.
				if (t instanceof Exception) {
					callChunkListenerOnError((Exception)t);
				}
				// Let's not count only retry rollbacks but also non-retry rollbacks.
				stepContext.getMetric(MetricImpl.MetricType.ROLLBACK_COUNT).incValue();
			} finally {
				transactionManager.rollback();
			}
			logger.exiting(sourceClass, "invokeChunk");
			throw new BatchContainerRuntimeException("Failure in Read-Process-Write Loop", t);
		} 

		logger.finest("Exiting normally");
		logger.exiting(sourceClass, "invokeChunk");
	}
	
	private void updateNormalMetrics(int writeCount) {

		int readCount = currentChunkStatus.getItemsTouchedInCurrentChunk();
		if (currentChunkStatus.isFinished()) { 
			readCount--;
		}
		int filterCount = readCount - writeCount;

		if (readCount < 0 || filterCount < 0 || writeCount < 0) {
			throw new IllegalStateException("Somehow one of the metrics was zero.  Read count: " + readCount + 
					", Filter count: " + filterCount + ", Write count: " + writeCount);
		}
		stepContext.getMetric(MetricImpl.MetricType.COMMIT_COUNT).incValue();
		stepContext.getMetric(MetricImpl.MetricType.READ_COUNT).incValueBy(readCount);
		stepContext.getMetric(MetricImpl.MetricType.FILTER_COUNT).incValueBy(filterCount);
		stepContext.getMetric(MetricImpl.MetricType.WRITE_COUNT).incValueBy(writeCount);
	}

	private void callChunkListenerOnError(Exception e) {
		logger.fine("Caught exception in chunk processing. Attempting to call onError() for chunk listeners.");
		for (ChunkListenerProxy chunkProxy : chunkListeners) {
			try {
				chunkProxy.onError(e);
		    // 2. Catch throwable, not exception
			} catch (Throwable t) {
				// Fail-fast and abort.
				throw new BatchContainerRuntimeException("Caught secondary throwable when calling chunk listener onError().", t);
			}
		}
	}

	private void rollbackAfterRetryableException() {

		writerProxy.close();
		readerProxy.close();
		callChunkListenerOnError(currentChunkStatus.getRetryableException());
		transactionManager.rollback();

		stepContext.getMetric(MetricImpl.MetricType.ROLLBACK_COUNT).incValue();
	}
	
	private void callReaderAndWriterCloseOnThrowable(Throwable t) {
		logger.fine("Caught throwable in chunk processing. Attempting to close all readers and writers.");

		try {
			writerProxy.close();
		} catch (Throwable t1) {
			logWarning("Secondary throwable closing writer on rollback path.  Swallow throwable and continue with rollback.", t1);
		}		
			
		try {
			readerProxy.close();
		} catch (Throwable t1) {
			logWarning("Secondary throwable closing reader on rollback path.  Swallow throwable and continue to close writer.", t1);
		} 
	}
	
	private void logWarning(String msg, Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);			
		logger.warning(msg + "Exception stack trace: \n" + sw.toString());
	}
	
	@Override
	protected void invokeCoreStep() throws BatchContainerServiceException {

		this.chunk = step.getChunk();

		initializeChunkArtifacts();
		
		initializeCheckpointManager();
		
		invokeChunk();
	}

	private void initializeCheckpointManager() {
		
		CheckpointAlgorithm checkpointAlgorithm = null;

		checkpointAtThisItemCount = ChunkHelper.getItemCount(chunk);
		int timeLimitSeconds = ChunkHelper.getTimeLimit(chunk);
		customCheckpointPolicy = ChunkHelper.isCustomCheckpointPolicy(chunk);  // Supplies default if needed

		if (!customCheckpointPolicy) {

			ItemCheckpointAlgorithm ica = new ItemCheckpointAlgorithm();
			ica.setItemCount(checkpointAtThisItemCount);
			ica.setTimeLimitSeconds(timeLimitSeconds);
			logger.fine("Initialize checkpoint manager with item-count=" + checkpointAtThisItemCount + 
					", and time limit = " + timeLimitSeconds + " seconds.");
			checkpointAlgorithm = ica;

		} else { 

			if (chunk.getCheckpointAlgorithm() == null) {
				throw new IllegalArgumentException("Configured checkpoint-policy of 'custom' but without a corresponding <checkpoint-algorithm> element.");
			}
			
			try {
				List<Property> propList = (chunk.getCheckpointAlgorithm().getProperties() == null) ? null : chunk.getCheckpointAlgorithm().getProperties().getPropertyList();

				InjectionReferences injectionRef = new InjectionReferences(jobExecutionImpl.getJobContext(), stepContext, propList);

				checkpointAlgorithm = ProxyFactory.createCheckpointAlgorithmProxy(chunk.getCheckpointAlgorithm().getRef(), injectionRef, stepContext);

				if (logger.isLoggable(Level.FINE)) {
					logger.fine("Created CheckpointAlgorithmProxy for custom checkpoint algorithm [" + checkpointAlgorithm + "]");
				}
					
			} catch (ArtifactValidationException e) {
				throw new BatchContainerServiceException("Cannot create the CheckpointAlgorithm for policy [" + chunk.getCheckpointPolicy()
						+ "]", e);
			}

		}	
		
		// Finally, for both policies now
		checkpointManager = new CheckpointManager(readerProxy, writerProxy, checkpointAlgorithm, jobExecutionImpl.getExecutionId(), jobExecutionImpl
					.getJobInstance().getInstanceId(), step.getId());
		
		// A related piece of data we'll calculate here is the tran timeout.   Though we won't include
		// it in the checkpoint manager since we'll set it directly on the tran mgr before each chunk.
		stepPropertyTranTimeoutSeconds = initStepTransactionTimeout();
	}


	/*
	 * Initialize itemreader, itemwriter, and item processor checkpoint
	 */
	private void initializeChunkArtifacts() {
		String sourceMethod = "initializeChunkArtifacts";
		if (logger.isLoggable(Level.FINE))
			logger.entering(sourceClass, sourceMethod);

		ItemReader itemReader = chunk.getReader();
		List<Property> itemReaderProps = itemReader.getProperties() == null ? null : itemReader.getProperties().getPropertyList();
		try {
			InjectionReferences injectionRef = new InjectionReferences(jobExecutionImpl.getJobContext(), stepContext, 
					itemReaderProps);

			readerProxy = ProxyFactory.createItemReaderProxy(itemReader.getRef(), injectionRef, stepContext);

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Created ItemReaderProxy for " + itemReader.getRef());
			}
		} catch (ArtifactValidationException e) {
			throw new BatchContainerServiceException("Cannot create the ItemReader [" + itemReader.getRef() + "]", e);
		}

		ItemProcessor itemProcessor = chunk.getProcessor();
		if (itemProcessor != null){
			List<Property> itemProcessorProps = itemProcessor.getProperties() == null ? null : itemProcessor.getProperties().getPropertyList();
			try {

				InjectionReferences injectionRef = new InjectionReferences(jobExecutionImpl.getJobContext(), stepContext, 
						itemProcessorProps);

				processorProxy = ProxyFactory.createItemProcessorProxy(itemProcessor.getRef(), injectionRef, stepContext);
				if (logger.isLoggable(Level.FINE)) {
					logger.fine("Created ItemProcessorProxy for " + itemProcessor.getRef());
				}
			} catch (ArtifactValidationException e) {
				throw new BatchContainerServiceException("Cannot create the ItemProcessor [" + itemProcessor.getRef() + "]", e);
			}
		}

		ItemWriter itemWriter = chunk.getWriter();
		List<Property> itemWriterProps = itemWriter.getProperties() == null ? null : itemWriter.getProperties().getPropertyList();
		try {
			InjectionReferences injectionRef = new InjectionReferences(jobExecutionImpl.getJobContext(), stepContext, 
					itemWriterProps);

			writerProxy = ProxyFactory.createItemWriterProxy(itemWriter.getRef(), injectionRef, stepContext);
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Created ItemWriterProxy for " + itemWriter.getRef());
			}
		} catch (ArtifactValidationException e) {
			throw new BatchContainerServiceException("Cannot create the ItemWriter [" + itemWriter.getRef() + "]", e);
		}



		InjectionReferences injectionRef = new InjectionReferences(jobExecutionImpl.getJobContext(), stepContext, 
				null);

		this.chunkListeners = jobExecutionImpl.getListenerFactory().getChunkListeners(step, injectionRef, stepContext);
		this.skipProcessListeners = jobExecutionImpl.getListenerFactory().getSkipProcessListeners(step, injectionRef, stepContext);
		this.skipReadListeners = jobExecutionImpl.getListenerFactory().getSkipReadListeners(step, injectionRef, stepContext);
		this.skipWriteListeners = jobExecutionImpl.getListenerFactory().getSkipWriteListeners(step, injectionRef, stepContext);
		this.retryProcessListeners = jobExecutionImpl.getListenerFactory().getRetryProcessListeners(step, injectionRef, stepContext);
		this.retryReadListeners = jobExecutionImpl.getListenerFactory().getRetryReadListeners(step, injectionRef, stepContext);
		this.retryWriteListeners = jobExecutionImpl.getListenerFactory().getRetryWriteListeners(step, injectionRef, stepContext);
		this.itemReadListeners = jobExecutionImpl.getListenerFactory().getItemReadListeners(step, injectionRef, stepContext);
		this.itemProcessListeners = jobExecutionImpl.getListenerFactory().getItemProcessListeners(step, injectionRef, stepContext);
		this.itemWriteListeners = jobExecutionImpl.getListenerFactory().getItemWriteListeners(step, injectionRef, stepContext);

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Setting contexts for chunk artifacts");
		}

		skipHandler = new SkipHandler(chunk, jobExecutionImpl.getJobInstance().getInstanceId(), step.getId());
		skipHandler.addSkipProcessListener(skipProcessListeners);
		skipHandler.addSkipReadListener(skipReadListeners);
		skipHandler.addSkipWriteListener(skipWriteListeners);

		retryHandler = new RetryHandler(chunk, jobExecutionImpl.getJobInstance().getInstanceId(), step.getId());

		retryHandler.addRetryProcessListener(retryProcessListeners);
		retryHandler.addRetryReadListener(retryReadListeners);
		retryHandler.addRetryWriteListener(retryWriteListeners);

		if (logger.isLoggable(Level.FINE))
			logger.exiting(sourceClass, sourceMethod);
	}

	private void openReaderAndWriter() {
		String sourceMethod = "openReaderAndWriter";

		if (logger.isLoggable(Level.FINE))
			logger.entering(sourceClass, sourceMethod);

		_persistenceManagerService = servicesManager.getPersistenceManagerService();
		readerChkptDK = new CheckpointDataKey(jobExecutionImpl.getJobInstance().getInstanceId(), step.getId(), "READER");
		CheckpointData readerChkptData = _persistenceManagerService.getCheckpointData(readerChkptDK);
		try {

			// check for data in backing store
			if (readerChkptData != null) {

				byte[] readertoken = readerChkptData.getRestartToken();
				ByteArrayInputStream readerChkptBA = new ByteArrayInputStream(readertoken);
				TCCLObjectInputStream readerOIS = null;
				try {
					readerOIS = new TCCLObjectInputStream(readerChkptBA);
					readerProxy.open((Serializable) readerOIS.readObject());
					readerOIS.close();
				} catch (Exception ex) {
					// is this what I should be throwing here?
							throw new BatchContainerServiceException("Cannot persist the checkpoint data for [" + step.getId() + "]", ex);
				}
			} else {
				// no chkpt data exists in the backing store
				readerChkptData = null;
				readerProxy.open(null);
			}
		} catch (ClassCastException e) {
			logger.warning("Expected CheckpointData but found" + readerChkptData );
			throw new IllegalStateException("Expected CheckpointData but found" + readerChkptData );
		}

		writerChkptDK = new CheckpointDataKey(jobExecutionImpl.getJobInstance().getInstanceId(), step.getId(), "WRITER");
		CheckpointData writerChkptData = _persistenceManagerService.getCheckpointData(writerChkptDK);

		try {
			// check for data in backing store
			if (writerChkptData != null) {
				byte[] writertoken = writerChkptData.getRestartToken();
				ByteArrayInputStream writerChkptBA = new ByteArrayInputStream(writertoken);
				TCCLObjectInputStream writerOIS = null;
				try {
					writerOIS = new TCCLObjectInputStream(writerChkptBA);
					writerProxy.open((Serializable) writerOIS.readObject());
					writerOIS.close();
				} catch (Exception ex) {
					// is this what I should be throwing here?
							throw new BatchContainerServiceException("Cannot persist the checkpoint data for [" + step.getId() + "]", ex);
				}
			} else {
				// no chkpt data exists in the backing store
				writerChkptData = null;
				writerProxy.open(null);
			}
		} catch (ClassCastException e) {
			logger.warning("Expected Checkpoint but found" + writerChkptData);
			throw new IllegalStateException("Expected Checkpoint but found" + writerChkptData);
		}

		// set up metrics
		// stepContext.addMetric(MetricImpl.Counter.valueOf("READ_COUNT"), 0);
		// stepContext.addMetric(MetricImpl.Counter.valueOf("WRITE_COUNT"), 0);
		// stepContext.addMetric(MetricImpl.Counter.valueOf("READ_SKIP_COUNT"),
		// 0);
		// stepContext.addMetric(MetricImpl.Counter.valueOf("PROCESS_SKIP_COUNT"),
		// 0);
		// stepContext.addMetric(MetricImpl.Counter.valueOf("WRITE_SKIP_COUNT"),
		// 0);

		if (logger.isLoggable(Level.FINE))
			logger.exiting(sourceClass, sourceMethod);
	}

	@Override
	public void stop() {
		stepContext.setBatchStatus(BatchStatus.STOPPING);

		// we don't need to call stop on the chunk implementation here since a
		// chunk always returns control to
		// the batch container after every item.

	}

	boolean skipReadException(Exception e) {

		try {
			skipHandler.handleExceptionRead(e);
		} catch (BatchContainerRuntimeException bcre) {
			return false;
		}

		return true;

	}

	boolean retryReadException(Exception e) {

		try {
			retryHandler.handleExceptionRead(e);
		} catch (BatchContainerRuntimeException bcre) {
			return false;
		}

		return true;

	}

	boolean skipProcessException(Exception e, Object record) {

		try {
			skipHandler.handleExceptionWithRecordProcess(e, record);
		} catch (BatchContainerRuntimeException bcre) {
			return false;
		}

		return true;

	}

	boolean retryProcessException(Exception e, Object record) {

		try {
			retryHandler.handleExceptionProcess(e, record);
		} catch (BatchContainerRuntimeException bcre) {
			return false;
		}

		return true;

	}

	boolean skipWriteException(Exception e, List<Object> chunkToWrite) {

		try {
			skipHandler.handleExceptionWithRecordListWrite(e, chunkToWrite);
		} catch (BatchContainerRuntimeException bcre) {
			return false;
		}

		return true;

	}

	boolean retryWriteException(Exception e, List<Object> chunkToWrite) {

		try {
			retryHandler.handleExceptionWrite(e, chunkToWrite);
		} catch (BatchContainerRuntimeException bcre) {
			return false;
		}

		return true;

	}

	private void setNextChunkTransactionTimeout() {
		int nextTimeout = 0;

		if (customCheckpointPolicy) {
			// Even on a retry-with-rollback, we'll continue to let
			// the custom CheckpointAlgorithm set a tran timeout.  
			//
			// We're guessing the application could need a smaller timeout than 
			// 180 seconds, (the default established by the batch chunk).
			nextTimeout = this.checkpointManager.checkpointTimeout();
		} else  {
			nextTimeout = stepPropertyTranTimeoutSeconds;
		}
		transactionManager.setTransactionTimeout(nextTimeout);
	}
	
    /**
     * Note we can rely on the StepContext properties already having been set at this point.
     * 
     * @return global transaction timeout defined in step properties. default
     */
    private int initStepTransactionTimeout() {
        logger.entering(sourceClass, "initStepTransactionTimeout");
        Properties p = stepContext.getProperties();
        int timeout = DEFAULT_TRAN_TIMEOUT_SECONDS; // default as per spec.
        if (p != null && !p.isEmpty()) {

            String propertyTimeOut = p.getProperty("javax.transaction.global.timeout");
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "javax.transaction.global.timeout = {0}", propertyTimeOut==null ? "<null>" : propertyTimeOut);
            }
            if (propertyTimeOut != null && !propertyTimeOut.isEmpty()) {
                timeout = Integer.parseInt(propertyTimeOut, 10);
            }
        }
        logger.exiting(sourceClass, "initStepTransactionTimeout", timeout);
        return timeout;
    }

	private void positionReaderAtCheckpoint() {
		_persistenceManagerService = servicesManager.getPersistenceManagerService();
		readerChkptDK = new CheckpointDataKey(jobExecutionImpl.getJobInstance().getInstanceId(), step.getId(), "READER");

		CheckpointData readerData = _persistenceManagerService.getCheckpointData(readerChkptDK);
		try {
			// check for data in backing store
			if (readerData != null) {
				byte[] readertoken = readerData.getRestartToken();
				ByteArrayInputStream readerChkptBA = new ByteArrayInputStream(readertoken);
				TCCLObjectInputStream readerOIS = null;
				try {
					readerOIS = new TCCLObjectInputStream(readerChkptBA);
					readerProxy.open((Serializable) readerOIS.readObject());
					readerOIS.close();
				} catch (Exception ex) {
					// is this what I should be throwing here?
							throw new BatchContainerServiceException("Cannot persist the checkpoint data for [" + step.getId() + "]", ex);
				}
			} else {
				// no chkpt data exists in the backing store
				readerData = null;
				readerProxy.open(null);
			}
		} catch (ClassCastException e) {
			throw new IllegalStateException("Expected CheckpointData but found" + readerData);
		}
	}

	private void positionWriterAtCheckpoint() {
		_persistenceManagerService = servicesManager.getPersistenceManagerService();
		writerChkptDK = new CheckpointDataKey(jobExecutionImpl.getJobInstance().getInstanceId(), step.getId(), "WRITER");

		CheckpointData writerData =  _persistenceManagerService.getCheckpointData(writerChkptDK);

		try {
			// check for data in backing store
			if (writerData != null) {
				byte[] writertoken = writerData.getRestartToken();
				ByteArrayInputStream writerChkptBA = new ByteArrayInputStream(writertoken);
				TCCLObjectInputStream writerOIS = null;
				try {
					writerOIS = new TCCLObjectInputStream(writerChkptBA);
					writerProxy.open((Serializable) writerOIS.readObject());
					writerOIS.close();
				} catch (Exception ex) {
					// is this what I should be throwing here?
							throw new BatchContainerServiceException("Cannot persist the checkpoint data for [" + step.getId() + "]", ex);
				}
			} else {
				// no chkpt data exists in the backing store
				writerData = null;
				writerProxy.open(null);
			}
		} catch (ClassCastException e) {
			throw new IllegalStateException("Expected CheckpointData but found" + writerData);
		}
	}
}
