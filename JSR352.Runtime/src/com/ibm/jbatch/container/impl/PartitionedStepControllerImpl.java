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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.api.partition.PartitionPlan;
import javax.batch.api.partition.PartitionReducer.PartitionStatus;
import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.batch.operations.JobExecutionNotMostRecentException;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.JobStartException;
import javax.batch.runtime.BatchStatus;

import com.ibm.jbatch.container.artifact.proxy.InjectionReferences;
import com.ibm.jbatch.container.artifact.proxy.PartitionAnalyzerProxy;
import com.ibm.jbatch.container.artifact.proxy.PartitionMapperProxy;
import com.ibm.jbatch.container.artifact.proxy.PartitionReducerProxy;
import com.ibm.jbatch.container.artifact.proxy.ProxyFactory;
import com.ibm.jbatch.container.artifact.proxy.StepListenerProxy;
import com.ibm.jbatch.container.context.impl.StepContextImpl;
import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.jobinstance.RuntimeJobExecution;
import com.ibm.jbatch.container.jsl.CloneUtility;
import com.ibm.jbatch.container.util.BatchPartitionPlan;
import com.ibm.jbatch.container.util.BatchPartitionWorkUnit;
import com.ibm.jbatch.container.util.BatchWorkUnit;
import com.ibm.jbatch.container.util.PartitionDataWrapper;
import com.ibm.jbatch.container.util.PartitionDataWrapper.PartitionEventType;
import com.ibm.jbatch.container.util.PartitionsBuilderConfig;
import com.ibm.jbatch.container.validation.ArtifactValidationException;
import com.ibm.jbatch.jsl.model.Analyzer;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.jbatch.jsl.model.JSLProperties;
import com.ibm.jbatch.jsl.model.PartitionMapper;
import com.ibm.jbatch.jsl.model.PartitionReducer;
import com.ibm.jbatch.jsl.model.Property;
import com.ibm.jbatch.jsl.model.Step;

public class PartitionedStepControllerImpl extends BaseStepControllerImpl {

	private final static String sourceClass = PartitionedStepControllerImpl.class.getName();
	private final static Logger logger = Logger.getLogger(sourceClass);

	private static final int DEFAULT_PARTITION_INSTANCES = 1;
	private static final int DEFAULT_THREADS = 0; //0 means default to number of instances

	private PartitionPlan plan = null;

	private int partitions = DEFAULT_PARTITION_INSTANCES;
	private int threads = DEFAULT_THREADS;

	private Properties[] partitionProperties = null;

	private volatile List<BatchPartitionWorkUnit> parallelBatchWorkUnits;

	private PartitionReducerProxy partitionReducerProxy = null;

	// On invocation this will be re-primed to reflect already-completed partitions from a previous execution.
	int numPreviouslyCompleted = 0;

	private PartitionAnalyzerProxy analyzerProxy = null;

	final List<JSLJob> subJobs = new ArrayList<JSLJob>();
	protected List<StepListenerProxy> stepListeners = null;

	List<BatchPartitionWorkUnit> completedWork = new ArrayList<BatchPartitionWorkUnit>();
	
	BlockingQueue<BatchPartitionWorkUnit> completedWorkQueue = null;

	protected PartitionedStepControllerImpl(final RuntimeJobExecution jobExecutionImpl, final Step step, StepContextImpl stepContext, long rootJobExecutionId) {
		super(jobExecutionImpl, step, stepContext, rootJobExecutionId);
	}

	@Override
	public void stop() {

		updateBatchStatus(BatchStatus.STOPPING);

		// It's possible we may try to stop a partitioned step before any
		// sub steps have been started.
		synchronized (subJobs) {

			if (parallelBatchWorkUnits != null) {
				for (BatchWorkUnit subJob : parallelBatchWorkUnits) {
					try {
						batchKernel.stopJob(subJob.getJobExecutionImpl().getExecutionId());
					} catch (Exception e) {
						// TODO - Is this what we want to know.  
						// Blow up if it happens to force the issue.
						throw new IllegalStateException(e);
					}
				}
			}
		}
	}

	private PartitionPlan generatePartitionPlan() {
		// Determine the number of partitions


		PartitionPlan plan = null;
		Integer previousNumPartitions = null;
		final PartitionMapper partitionMapper = step.getPartition().getMapper();

		//from persisted plan from previous run
		if (stepStatus.getNumPartitions() != null) {
			previousNumPartitions = stepStatus.getNumPartitions();
		}

		if (partitionMapper != null) { //from partition mapper

			PartitionMapperProxy partitionMapperProxy;

			final List<Property> propertyList = partitionMapper.getProperties() == null ? null
					: partitionMapper.getProperties().getPropertyList();

			// Set all the contexts associated with this controller.
			// Some of them may be null
			InjectionReferences injectionRef = new InjectionReferences(jobExecutionImpl.getJobContext(), stepContext, 
					propertyList);

			try {
				partitionMapperProxy = ProxyFactory.createPartitionMapperProxy(
						partitionMapper.getRef(), injectionRef, stepContext);
			} catch (final ArtifactValidationException e) {
				throw new BatchContainerServiceException(
						"Cannot create the PartitionMapper ["
								+ partitionMapper.getRef() + "]", e);
			}


			PartitionPlan mapperPlan = partitionMapperProxy.mapPartitions();

			//Set up the new partition plan
			plan = new BatchPartitionPlan();
			plan.setPartitionsOverride(mapperPlan.getPartitionsOverride());

			//When true is specified, the partition count from the current run
			//is used and all results from past partitions are discarded.
			if (mapperPlan.getPartitionsOverride() || previousNumPartitions == null){
				plan.setPartitions(mapperPlan.getPartitions());
			} else {
				plan.setPartitions(previousNumPartitions);
			}

			if (mapperPlan.getThreads() == 0) {
				plan.setThreads(plan.getPartitions());
			} else {
				plan.setThreads(mapperPlan.getThreads());    
			}

			plan.setPartitionProperties(mapperPlan.getPartitionProperties());

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Partition plan defined by partition mapper: " + plan);
			}

		} else if (step.getPartition().getPlan() != null) { //from static partition element in jsl


			String partitionsAttr = step.getPartition().getPlan().getPartitions();
			String threadsAttr = null;

			int numPartitions = Integer.MIN_VALUE;
			int numThreads;
			Properties[] partitionProps = null;

			if (partitionsAttr != null) {
				try {
					numPartitions = Integer.parseInt(partitionsAttr);
				} catch (final NumberFormatException e) {
					throw new IllegalArgumentException("Could not parse partition instances value in stepId: " + step.getId()
							+ ", with instances=" + partitionsAttr, e);
				}   
				partitionProps = new Properties[numPartitions];
				if (numPartitions < 1) {
					throw new IllegalArgumentException("Partition instances value must be 1 or greater in stepId: " + step.getId()
							+ ", with instances=" + partitionsAttr);
				}
			}

			threadsAttr = step.getPartition().getPlan().getThreads();
			if (threadsAttr != null) {
				try {
					numThreads = Integer.parseInt(threadsAttr);
					if (numThreads == 0) {
						numThreads = numPartitions;
					}
				} catch (final NumberFormatException e) {
					throw new IllegalArgumentException("Could not parse partition threads value in stepId: " + step.getId()
							+ ", with threads=" + threadsAttr, e);
				}   
				if (numThreads < 0) {
					throw new IllegalArgumentException("Threads value must be 0 or greater in stepId: " + step.getId()
							+ ", with threads=" + threadsAttr);

				}
			} else { //default to number of partitions if threads isn't set
				numThreads = numPartitions;
			}


			if (step.getPartition().getPlan().getProperties() != null) {

				List<JSLProperties> jslProperties = step.getPartition().getPlan().getProperties();
				for (JSLProperties props : jslProperties) {
					int targetPartition = Integer.parseInt(props.getPartition());

                    try {
                        partitionProps[targetPartition] = CloneUtility.jslPropertiesToJavaProperties(props);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw new BatchContainerRuntimeException("There are only " + numPartitions + " partition instances, but there are "
                                + jslProperties.size()
                                + " partition properties lists defined. Remember that partition indexing is 0 based like Java arrays.", e);
                    }
                }
			}
			plan = new BatchPartitionPlan();
			plan.setPartitions(numPartitions);
			plan.setThreads(numThreads);
			plan.setPartitionProperties(partitionProps);
			plan.setPartitionsOverride(false); //FIXME what is the default for a static plan??
		}


		// Set the other instance variables for convenience.
		this.partitions = plan.getPartitions();
		this.threads = plan.getThreads();
		this.partitionProperties = plan.getPartitionProperties();

		return plan;
	}


	@Override
	protected void invokeCoreStep() throws JobRestartException, JobStartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException {

		this.plan = this.generatePartitionPlan();

		//persist the partition plan so on restart we have the same plan to reuse
		stepStatus.setNumPartitions(plan.getPartitions());

		/* When true is specified, the partition count from the current run
		 * is used and all results from past partitions are discarded. Any
		 * resource cleanup or back out of work done in the previous run is the
		 * responsibility of the application. The PartitionReducer artifact's
		 * rollbackPartitionedStep method is invoked during restart before any
		 * partitions begin processing to provide a cleanup hook.
		 */
		if (plan.getPartitionsOverride()) {
			if (this.partitionReducerProxy != null) {
				this.partitionReducerProxy.rollbackPartitionedStep();
			}
		}

		logger.fine("Number of partitions in step: " + partitions + " in step " + step.getId() + "; Subjob properties defined by partition mapper: " + partitionProperties);

		//Set up a blocking queue to pick up collector data from a partitioned thread
		if (this.analyzerProxy != null) {
			this.analyzerStatusQueue =  new LinkedBlockingQueue<PartitionDataWrapper>();
		}
		this.completedWorkQueue = new LinkedBlockingQueue<BatchPartitionWorkUnit>();

		// Build all sub jobs from partitioned step
		buildSubJobBatchWorkUnits();

		// kick off the threads
		executeAndWaitForCompletion();

		// Deal with the results.
		checkCompletedWork();
	}
	private void buildSubJobBatchWorkUnits() throws JobRestartException, JobStartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException  {
		synchronized (subJobs) {		
			//check if we've already issued a stop
			if (jobExecutionImpl.getJobContext().getBatchStatus().equals(BatchStatus.STOPPING)){
				logger.fine("Step already in STOPPING state, exiting from buildSubJobBatchWorkUnits() before beginning execution");
				return;
			}

			for (int instance = 0; instance < partitions; instance++) {
				subJobs.add(PartitionedStepBuilder.buildPartitionSubJob(jobExecutionImpl.getInstanceId(),jobExecutionImpl.getJobContext(), step, instance));
			}

			PartitionsBuilderConfig config = new PartitionsBuilderConfig(subJobs, partitionProperties, analyzerStatusQueue, completedWorkQueue, jobExecutionImpl.getExecutionId());
			// Then build all the subjobs but do not start them yet
			if (stepStatus.getStartCount() > 1 && !plan.getPartitionsOverride()) {
				parallelBatchWorkUnits = batchKernel.buildOnRestartParallelPartitions(config);
			} else {
				parallelBatchWorkUnits = batchKernel.buildNewParallelPartitions(config);
			}

			// NOTE:  At this point I might not have as many work units as I had partitions, since some may have already completed.
		}
	}

	private void executeAndWaitForCompletion() throws JobRestartException {
		
		if (jobExecutionImpl.getJobContext().getBatchStatus().equals(BatchStatus.STOPPING)){
			logger.fine("Step already in STOPPING state, exiting from executeAndWaitForCompletion() before beginning execution");
			return;
		}
		
		int numTotalForThisExecution = parallelBatchWorkUnits.size();
		this.numPreviouslyCompleted = partitions - numTotalForThisExecution; 
		int numCurrentCompleted = 0;
		int numCurrentSubmitted = 0;

		logger.fine("Calculated that " + numPreviouslyCompleted + " partitions are already complete out of total # = " 
				+ partitions + ", with # remaining =" + numTotalForThisExecution);

		//Start up to to the max num we are allowed from the num threads attribute
		for (int i=0; i < this.threads && i < numTotalForThisExecution; i++, numCurrentSubmitted++) {
			if (stepStatus.getStartCount() > 1 && !!!plan.getPartitionsOverride()) {
				batchKernel.restartGeneratedJob(parallelBatchWorkUnits.get(i));
			} else {
				batchKernel.startGeneratedJob(parallelBatchWorkUnits.get(i));
			}
		}

		boolean readyToSubmitAnother = false;
		while (true) {
			logger.finer("Begin main loop in waitForQueueCompletion(), readyToSubmitAnother = " + readyToSubmitAnother);
			try {
				if (analyzerProxy != null) {
					logger.fine("Found analyzer, proceeding on analyzerQueue path");
					PartitionDataWrapper dataWrapper = analyzerStatusQueue.take();
					if (PartitionEventType.ANALYZE_COLLECTOR_DATA.equals(dataWrapper.getEventType())) {
						logger.finer("Analyze collector data: " + dataWrapper.getCollectorData());
						analyzerProxy.analyzeCollectorData(dataWrapper.getCollectorData());
						continue; // without being ready to submit another
					} else if (PartitionEventType.ANALYZE_STATUS.equals(dataWrapper.getEventType())) {
						analyzerProxy.analyzeStatus(dataWrapper.getBatchstatus(), dataWrapper.getExitStatus());
						logger.fine("Analyze status called for completed partition: batchStatus= " + dataWrapper.getBatchstatus() + ", exitStatus = " + dataWrapper.getExitStatus());
						completedWork.add(completedWorkQueue.take());  // Shouldn't be a a long wait.
						readyToSubmitAnother = true;
					} else {
						logger.warning("Invalid partition state");
						throw new IllegalStateException("Invalid partition state");
					}
				} else {
					logger.fine("No analyzer, proceeding on completedWorkQueue path");
					// block until at least one thread has finished to
					// submit more batch work. hold on to the finished work to look at later
					completedWork.add(completedWorkQueue.take());
					readyToSubmitAnother = true;
				}
			} catch (InterruptedException e) {
				logger.severe("Caught exc"+ e);
				throw new BatchContainerRuntimeException(e);
			}

			if (readyToSubmitAnother) {
				numCurrentCompleted++;
				logger.fine("Ready to submit another (if there is another left to submit); numCurrentCompleted = " + numCurrentCompleted);
				if (numCurrentCompleted < numTotalForThisExecution) {
					if (numCurrentSubmitted < numTotalForThisExecution) {
						logger.fine("Submitting # " + numCurrentSubmitted + " out of " + numTotalForThisExecution + " total for this execution");
						if (stepStatus.getStartCount() > 1) {
							batchKernel.startGeneratedJob(parallelBatchWorkUnits.get(numCurrentSubmitted++));
						} else {
							batchKernel.restartGeneratedJob(parallelBatchWorkUnits.get(numCurrentSubmitted++));
						}
						readyToSubmitAnother = false;
					}
				} else {
					logger.fine("Finished... breaking out of loop");
					break;
				}
			} else {
				logger.fine("Not ready to submit another."); // Must have just done a collector
			}
		}
	}        

	private void checkCompletedWork() {

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Check completed work list.");
		}

		/**
		 * check the batch status of each subJob after it's done to see if we need to issue a rollback
		 * start rollback if any have stopped or failed
		 */
		boolean rollback = false;
		boolean partitionFailed = false;
		
		for (final BatchWorkUnit subJob : completedWork) {
			BatchStatus batchStatus = subJob.getJobExecutionImpl().getJobContext().getBatchStatus();
			if (batchStatus.equals(BatchStatus.FAILED)) {
				logger.fine("Subjob " + subJob.getJobExecutionImpl().getExecutionId() + " ended with status '" + batchStatus + "'; Starting logical transaction rollback.");

				rollback = true;
				partitionFailed = true;

				//Keep track of the failing status and throw an exception to propagate after the rest of the partitions are complete
				stepContext.setBatchStatus(BatchStatus.FAILED);
			} 
		}

		//If rollback is false we never issued a rollback so we can issue a logicalTXSynchronizationBeforeCompletion
		//NOTE: this will get issued even in a subjob fails or stops if no logicalTXSynchronizationRollback method is provied
		//We are assuming that not providing a rollback was intentional
		if (rollback == true) {
			if (this.partitionReducerProxy != null) {
				this.partitionReducerProxy.rollbackPartitionedStep();
			}
			if (partitionFailed) {
				throw new BatchContainerRuntimeException("One or more partitions failed");
			}
		} else {
			if (this.partitionReducerProxy != null) {
				this.partitionReducerProxy.beforePartitionedStepCompletion();
			}
		}
	}

	@Override
	protected void setupStepArtifacts() {

		InjectionReferences injectionRef = null;
		injectionRef = new InjectionReferences(jobExecutionImpl.getJobContext(), stepContext, null);
		this.stepListeners = jobExecutionImpl.getListenerFactory().getStepListeners(step, injectionRef, stepContext);

		Analyzer analyzer = step.getPartition().getAnalyzer();

		if (analyzer != null) {
			final List<Property> propList = analyzer.getProperties() == null ? null : analyzer.getProperties()
					.getPropertyList();

			injectionRef = new InjectionReferences(jobExecutionImpl.getJobContext(), stepContext, propList);

			try {
				analyzerProxy = ProxyFactory.createPartitionAnalyzerProxy(analyzer.getRef(), injectionRef, stepContext);
			} catch (final ArtifactValidationException e) {
				throw new BatchContainerServiceException("Cannot create the analyzer [" + analyzer.getRef() + "]", e);
			}
		} 

		PartitionReducer partitionReducer = step.getPartition().getReducer();

		if (partitionReducer != null) {

			final List<Property> propList = partitionReducer.getProperties() == null ? null : partitionReducer.getProperties()
					.getPropertyList();

			injectionRef = new InjectionReferences(jobExecutionImpl.getJobContext(), stepContext, propList);

			try {
				this.partitionReducerProxy = ProxyFactory.createPartitionReducerProxy(partitionReducer.getRef(), injectionRef, stepContext);
			} catch (final ArtifactValidationException e) {
				throw new BatchContainerServiceException("Cannot create the analyzer [" + partitionReducer.getRef() + "]",
						e);
			}
		}

	}

	@Override
	protected void invokePreStepArtifacts() {

		if (stepListeners != null) {
			for (StepListenerProxy listenerProxy : stepListeners) {
				// Call beforeStep on all the step listeners
				listenerProxy.beforeStep();
			}
		}

		// Invoke the reducer before all parallel steps start (must occur
		// before mapper as well)
		if (this.partitionReducerProxy != null) {
			this.partitionReducerProxy.beginPartitionedStep();
		}

	}

	@Override
	protected void invokePostStepArtifacts() {
		// Invoke the reducer after all parallel steps are done
		if (this.partitionReducerProxy != null) {

			if ((BatchStatus.COMPLETED).equals(stepContext.getBatchStatus())) {
				this.partitionReducerProxy.afterPartitionedStepCompletion(PartitionStatus.COMMIT);
			}else {
				this.partitionReducerProxy.afterPartitionedStepCompletion(PartitionStatus.ROLLBACK); 
			}

		}

		// Called in spec'd order, e.g. Sec. 11.7
		if (stepListeners != null) {
			for (StepListenerProxy listenerProxy : stepListeners) {
				// Call afterStep on all the step listeners
				listenerProxy.afterStep();
			}
		}
	}

	@Override
	protected void sendStatusFromPartitionToAnalyzerIfPresent() {
		// Since we're already on the main thread, there will never
		// be anything to do on this thread.  It's only on the partitioned
		// threads that there is something to send back.
	}
}
