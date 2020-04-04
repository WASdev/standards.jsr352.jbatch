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

import jakarta.batch.api.partition.PartitionPlan;
import jakarta.batch.api.partition.PartitionReducer.PartitionStatus;
import jakarta.batch.operations.JobExecutionAlreadyCompleteException;
import jakarta.batch.operations.JobExecutionNotMostRecentException;
import jakarta.batch.operations.JobExecutionNotRunningException;
import jakarta.batch.operations.JobRestartException;
import jakarta.batch.operations.JobStartException;
import jakarta.batch.runtime.BatchStatus;

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
	
	private enum ExecutionType {START, RESTART_NORMAL, RESTART_OVERRIDE, RESTART_AFTER_COMPLETION};
	private ExecutionType executionType = null; 

	// On invocation this will be re-primed to reflect already-completed partitions from a previous execution.
	int numPreviouslyCompleted = 0;

	private PartitionAnalyzerProxy analyzerProxy = null;

	final List<JSLJob> subJobs = new ArrayList<JSLJob>();
	protected List<StepListenerProxy> stepListeners = null;

	List<BatchPartitionWorkUnit> finishedWork = new ArrayList<BatchPartitionWorkUnit>();
	
	BlockingQueue<BatchPartitionWorkUnit> finishedWorkQueue = null;

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
					long jobExecutionId = -1;
					try {
						jobExecutionId = subJob.getJobExecutionImpl().getExecutionId();
						batchKernel.stopJob(jobExecutionId);
					} catch (JobExecutionNotRunningException e) {
						logger.fine("Caught exception trying to stop subjob: " + jobExecutionId + ", which was not running.");
						// We want to stop all running sub steps. 
						// We do not want to throw an exception if a sub step has already been completed.
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

	private void calculateExecutionType() {
		// We want to ignore override on the initial execution
		if (isRestartExecution()) {
			if (restartAfterCompletion) {
				executionType = ExecutionType.RESTART_AFTER_COMPLETION;
			} else 	if (plan.getPartitionsOverride()) {
				executionType = ExecutionType.RESTART_OVERRIDE;
			} else {
				executionType = ExecutionType.RESTART_NORMAL;
			}
		} else {
			executionType = ExecutionType.START;
		}
	}

	private void validateNumberOfPartitions() {
				
		int currentPlanSize = plan.getPartitions();
		
		if (executionType == ExecutionType.RESTART_NORMAL) {
			int previousPlanSize = stepStatus.getNumPartitions();
			if (previousPlanSize > 0 && previousPlanSize != currentPlanSize) {
				String msg = "On a normal restart, the plan on restart specified: " + currentPlanSize + " # of partitions, but the previous " +
						"executions' plan specified a different number: " + previousPlanSize + " # of partitions.  Failing job.";
				logger.severe(msg);
				throw new IllegalStateException(msg);				
			}
		}
		
		//persist the partition plan so on restart we have the same plan to reuse	
		stepStatus.setNumPartitions(currentPlanSize);
	}

	
	@Override
	protected void invokeCoreStep() throws JobRestartException, JobStartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException {

		this.plan = this.generatePartitionPlan();

		calculateExecutionType();
		
		validateNumberOfPartitions();		

		/* When true is specified, the partition count from the current run
		 * is used and all results from past partitions are discarded. Any
		 * resource cleanup or back out of work done in the previous run is the
		 * responsibility of the application. The PartitionReducer artifact's
		 * rollbackPartitionedStep method is invoked during restart before any
		 * partitions begin processing to provide a cleanup hook.
		 */
		if (executionType == ExecutionType.RESTART_OVERRIDE) {
			if (this.partitionReducerProxy != null) {
				this.partitionReducerProxy.rollbackPartitionedStep();
			}			
		} 

		logger.fine("Number of partitions in step: " + partitions + " in step " + step.getId() + "; Subjob properties defined by partition mapper: " + partitionProperties);

		//Set up a blocking queue to pick up collector data from a partitioned thread
		if (this.analyzerProxy != null) {
			this.analyzerStatusQueue =  new LinkedBlockingQueue<PartitionDataWrapper>();
		}
		this.finishedWorkQueue = new LinkedBlockingQueue<BatchPartitionWorkUnit>();

		// Build all sub jobs from partitioned step
		buildSubJobBatchWorkUnits();

		// kick off the threads
		executeAndWaitForCompletion();

		// Deal with the results.
		checkFinishedPartitions();

		// Call before completion
		if (this.partitionReducerProxy != null) {
			this.partitionReducerProxy.beforePartitionedStepCompletion();
		}
	}

	private void buildSubJobBatchWorkUnits() throws JobRestartException, JobStartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException  {
		synchronized (subJobs) {		
			//check if we've already issued a stop
			if (jobExecutionImpl.getJobContext().getBatchStatus().equals(BatchStatus.STOPPING)){
				logger.fine("Step already in STOPPING state, exiting from buildSubJobBatchWorkUnits() before beginning execution");
				return;
			}

			for (int instance = 0; instance < partitions; instance++) {
				subJobs.add(PartitionedStepBuilder.buildPartitionSubJob(jobExecutionImpl.getJobContext(), stepContext, step, instance));
			}

			PartitionsBuilderConfig config = new PartitionsBuilderConfig(subJobs, partitionProperties, analyzerStatusQueue, finishedWorkQueue, jobExecutionImpl.getExecutionId());
			// Then build all the subjobs but do not start them yet
			if (executionType == ExecutionType.RESTART_NORMAL) {				
				parallelBatchWorkUnits = batchKernel.buildOnRestartParallelPartitions(config);
			} else { 	
				// This case includes RESTART_OVERRIDE and RESTART_AFTER_COMPLETION.
				//
				// So we're just going to create new "subjob" job instances in the DB in these cases, 
				// and we'll have to make sure we're dealing with the correct ones, say in a subsequent "normal" restart
				// (of the current execution which is itself a restart)
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

		if (numTotalForThisExecution == 0) {
			logger.finer("All partitions have already completed on a previous execution.  Returning");
			return;
		}

		//Start up to to the max num we are allowed from the num threads attribute
		for (int i=0; i < this.threads && i < numTotalForThisExecution; i++, numCurrentSubmitted++) {
			if (stepStatus.getStartCount() > 1 && !plan.getPartitionsOverride()) {
				batchKernel.restartGeneratedJob(parallelBatchWorkUnits.get(i));
			} else {
				batchKernel.startGeneratedJob(parallelBatchWorkUnits.get(i));
			}
		}

		boolean readyToSubmitAnother = false;
		boolean exceptionThrownAnalyzingCollectorData = false;
		boolean exceptionThrownAnalyzingStatus = false;


		while (true) {
			logger.finer("Begin main loop in waitForQueueCompletion(), readyToSubmitAnother = " + readyToSubmitAnother);
			try {
				if (analyzerProxy != null) {
					logger.fine("Found analyzer, proceeding on analyzerQueue path");
					PartitionDataWrapper dataWrapper = analyzerStatusQueue.take();
					if (PartitionEventType.ANALYZE_COLLECTOR_DATA.equals(dataWrapper.getEventType())) {
						logger.finer("Analyze collector data: " + dataWrapper.getCollectorData());
						try {
							analyzerProxy.analyzeCollectorData(dataWrapper.getCollectorData());
						} catch (Throwable t) {
							exceptionThrownAnalyzingCollectorData = true;
							logger.warning("Caught exception calling analyzeCollectorData(), catching and continuing.");
						}
						continue; // without being ready to submit another
					} else if (PartitionEventType.ANALYZE_STATUS.equals(dataWrapper.getEventType())) {
						logger.fine("Calling analyzeStatus()");
						try {
							analyzerProxy.analyzeStatus(dataWrapper.getBatchstatus(), dataWrapper.getExitStatus());
						} catch (Throwable t) {
							exceptionThrownAnalyzingStatus = true;
							// TODO - If we've caught an exception here we might want to stop submitting new partitions!
							// To start, it is a smaller change in the working code to continue to submit them, so let's do that.
							logger.warning("Caught exception calling analyzeStatus(), catching and continuing.  Will even continue starting new partitions if there are more to run.");
						}
						logger.fine("Analyze status called for completed partition: batchStatus= " + dataWrapper.getBatchstatus() + ", exitStatus = " + dataWrapper.getExitStatus());
						finishedWork.add(finishedWorkQueue.take());  // Shouldn't be a a long wait.
						readyToSubmitAnother = true;
					} else {
						logger.warning("Invalid partition state");
						throw new IllegalStateException("Invalid partition state");
					}
				} else {
					logger.fine("No analyzer, proceeding on finishedWorkQueue path");
					// block until at least one thread has finished to
					// submit more batch work. hold on to the finished work to look at later
					finishedWork.add(finishedWorkQueue.take());
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
							batchKernel.restartGeneratedJob(parallelBatchWorkUnits.get(numCurrentSubmitted++));
						} else {
							batchKernel.startGeneratedJob(parallelBatchWorkUnits.get(numCurrentSubmitted++));
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
		
		if (exceptionThrownAnalyzingCollectorData) {
			rollbackPartitionedStepAndThrowExc("Throwable previously caught analyzing collector data, rolling back step.");
		}
		
		if (exceptionThrownAnalyzingStatus) {
			rollbackPartitionedStepAndThrowExc("Throwable previously caught analyzing status, rolling back step.");
		}
	}        

	private void checkFinishedPartitions() {

		/**
		 * check the batch status of each subJob after it's done to see if we need to issue a rollback
		 * start rollback if any have stopped or failed
		 */
		boolean failingPartitionSeen = false;
		boolean stoppedPartitionSeen = false;
		
		for (final BatchWorkUnit subJob : finishedWork) {
			BatchStatus batchStatus = subJob.getJobExecutionImpl().getJobContext().getBatchStatus();
			logger.fine("Subjob " + subJob.getJobExecutionImpl().getExecutionId() + " ended with status '" + batchStatus);
			// Keep looping, just to see the log messages perhaps.
			if (batchStatus.equals(BatchStatus.FAILED)) {
				failingPartitionSeen = true;
				stepContext.setBatchStatus(BatchStatus.FAILED);
			} 
			// This code seems to suggest it might be valid for a partition to end up in STOPPED state without 
			// the "top-level" step having been aware of this.   It's unclear from the spec if this is even possible
			// or a desirable spec interpretation.  Nevertheless, we'll code it as such noting the ambiguity.
			// 
			// However, in the RI at least, we won't bother updating the step level BatchStatus, since to date we 
			// would only transition the status in such a way independently.
			if (batchStatus.equals(BatchStatus.STOPPED)) {
				stoppedPartitionSeen = true;
			}
		}

		if (failingPartitionSeen) {
			rollbackPartitionedStepAndThrowExc("One or more partitions failed");
		} else if (stoppedPartitionSeen) {
			rollbackPartitionedStep();
		}
	}

	private void rollbackPartitionedStep() {
		//If rollback is false we never issued a rollback so we can issue a logicalTXSynchronizationBeforeCompletion
		//NOTE: this will get issued even in a subjob fails or stops if no logicalTXSynchronizationRollback method is provied
		//We are assuming that not providing a rollback was intentional
		if (this.partitionReducerProxy != null) {
			this.partitionReducerProxy.rollbackPartitionedStep();
		}
	}

	private void rollbackPartitionedStepAndThrowExc(String msg) {
		rollbackPartitionedStep();
		throw new BatchContainerRuntimeException(msg);
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
	
	@Override
	protected void persistStepExecution() {
		// Call special aggregating method
		_persistenceManagementService.updateWithFinalPartitionAggregateStepExecution(rootJobExecutionId, stepContext);
	}
}
