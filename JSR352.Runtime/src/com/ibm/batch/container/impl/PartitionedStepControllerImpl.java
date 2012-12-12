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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.api.parameters.PartitionPlan;

import jsr352.batch.jsl.Analyzer;
import jsr352.batch.jsl.JSLJob;
import jsr352.batch.jsl.PartitionReducer;
import jsr352.batch.jsl.PartitionMapper;
import jsr352.batch.jsl.Property;
import jsr352.batch.jsl.Step;

import com.ibm.batch.container.artifact.proxy.PartitionMapperProxy;
import com.ibm.batch.container.artifact.proxy.PartitionReducerProxy;
import com.ibm.batch.container.artifact.proxy.ProxyFactory;
import com.ibm.batch.container.exception.BatchContainerServiceException;
import com.ibm.batch.container.jobinstance.RuntimeJobExecutionImpl;
import com.ibm.batch.container.jobinstance.ParallelJobExecution;
import com.ibm.batch.container.util.ExecutionStatus;
import com.ibm.batch.container.util.ExecutionStatus.BatchStatus;
import com.ibm.batch.container.validation.ArtifactValidationException;

public class PartitionedStepControllerImpl extends BaseStepControllerImpl {

	private final static String sourceClass = PartitionedStepControllerImpl.class.getName();
	private final static Logger logger = Logger.getLogger(sourceClass);

	private static final int DEFAULT_PARTITION_INSTANCES = 1;

	private int numPartitions = DEFAULT_PARTITION_INSTANCES;
	private Properties[] partitionProperties = null;

	private volatile List<ParallelJobExecution> parallelJobExecs;

	private PartitionReducerProxy partitionReducer = null;
	
	final List<JSLJob> subJobs = new ArrayList<JSLJob>();

	protected PartitionedStepControllerImpl(final RuntimeJobExecutionImpl jobExecutionImpl, final Step step) {
		super(jobExecutionImpl, step);

	}

	@Override
	public void stop() {

		stepContext.setBatchStatus(ExecutionStatus.getStringValue(BatchStatus.STOPPING));

		// It's possible we may try to stop a partitioned step before any
		// sub steps have been started.
		synchronized (subJobs) {

			if (parallelJobExecs != null) {
				for (ParallelJobExecution subJob : parallelJobExecs) {
					try {
						batchKernel.stopJob(subJob.getJobExecution().getInstanceId());
					} catch (Exception e) {
						// TODO - Is this what we want to know.  
						// Blow up if it happens to force the issue.
						throw new IllegalStateException(e);
					}
				}
			}
		}
	}

	@Override
	protected void invokeCoreStep() {

		
		// Determine the number of partitions
    			
    			
		String instancesAttr = null;
		
		if (step.getPartition().getPartitionPlan() != null) {
	    	instancesAttr = step.getPartition().getPartitionPlan().getInstances();
	    	if (instancesAttr != null) {
	    		try {
	    			numPartitions = Integer.parseInt(instancesAttr);
	    		} catch (final NumberFormatException e) {
	    			throw new IllegalArgumentException("Could not parse partition instances value in stepId: " + step.getId()
						+ ", with instances=" + instancesAttr, e);

	    		}	
				if (numPartitions < 1) {
					throw new IllegalArgumentException("Partition instances value must be 1 or greater in stepId: " + step.getId()
						+ ", with instances=" + instancesAttr);

				}
			}
		}
		
		
		final PartitionMapper partitionMapper = step.getPartition().getPartitionMapper();

		// If a partition mapper is provided it always overrides the partition plan
		// instances attribute value
		if (partitionMapper != null) {

			PartitionMapperProxy partitionMapperProxy;

			final List<Property> propertyList = partitionMapper.getProperties() == null ? null : partitionMapper.getProperties()
					.getPropertyList();

			try {
				partitionMapperProxy = ProxyFactory.createPartitionMapperProxy(partitionMapper.getRef(), propertyList);
			} catch (final ArtifactValidationException e) {
				throw new BatchContainerServiceException("Cannot create the PartitionMapper [" + partitionMapper.getRef() + "]", e);
			}

			// Set all the contexts associated with this controller.
			// Some of them may be null
			partitionMapperProxy.setJobContext(jobExecutionImpl.getJobContext());
			partitionMapperProxy.setStepContext(stepContext);
			partitionMapperProxy.setFlowContext(flowContext);
			partitionMapperProxy.setSplitContext(splitContext);

			final PartitionPlan plan = partitionMapperProxy.calculatePartitions();
			numPartitions = plan.getPartitionCount();

			if (logger.isLoggable(Level.FINE)) {
				if (instancesAttr != null) {
					logger.fine("Overriding instances attribute of " + instancesAttr + " with subJobCount of " + numPartitions
							+ " in step " + step.getId());
				}
			}

			partitionProperties = plan.getPartitionProperties();

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Subjob properties defined by partition mapper: " + partitionProperties);
			}

		}

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Number of partitions in step: " + numPartitions + " in step " + step.getId());
			logger.fine("Subjob properties defined by partition mapper: " + partitionProperties);
		}
		
		// Build all sub jobs from partitioned step

		synchronized (subJobs) {		
			//check if we've already issued a stop
	        if (jobExecutionImpl.getJobContext().getBatchStatus().equals(ExecutionStatus.getStringValue(BatchStatus.STOPPING))){
	            this.stepContext.setBatchStatus(ExecutionStatus.getStringValue(BatchStatus.STOPPED));
	            
	            return;
	        }
		
			for (int instance = 0; instance < numPartitions; instance++) {
				subJobs.add(ParallelJobBuilder.buildSubJob(jobExecutionImpl.getExecutionId(), step, partitionProperties, instance));
			}
	
			// TODO Call PartitionReducer methods before sub jobs start
	
			// Then execute all subjobs in parallel
			parallelJobExecs = batchKernel.startParallelJobs(subJobs, partitionProperties, analyzerProxy);

		}
		/**
		 * Wait for the all the parallel jobs to end/stop/fail/complete etc..
		 * check the batch status of each subJob after it's done to see if we need to issue a rollback
		 * start rollback if any have stopped or failed
		 */
		boolean rollback = false;
		for (final ParallelJobExecution subJob : parallelJobExecs) {

			subJob.waitForResult(); // This is like a call to Thread.join()
			
			String batchStatus = subJob.getJobExecution().getJobContext().getBatchStatus();
			if (batchStatus.equals(ExecutionStatus.getStringValue(BatchStatus.FAILED))) {
				if (logger.isLoggable(Level.FINE)) {
					logger.fine("Subjob " + subJob.getJobExecution().getExecutionId() + "ended with status '" + batchStatus + "'" );
					logger.fine("Starting logical transaction rollback.");
				}
				
				if (this.partitionReducer != null) {
					this.stop();
					this.partitionReducer.partitionReducerRollback();
					rollback = true;
				}
				
				stepContext.setBatchStatus(ExecutionStatus.getStringValue(BatchStatus.FAILED));
				break;
				
			} else if (batchStatus.equals(ExecutionStatus.getStringValue(BatchStatus.STOPPED))) {
				if (logger.isLoggable(Level.FINE)) {
					logger.fine("Subjob " + subJob.getJobExecution().getExecutionId() + "ended with status '" + batchStatus + "'" );
					logger.fine("Starting logical transaction rollback.");
				}
				
				if (this.partitionReducer != null) {
					this.stop();
					this.partitionReducer.partitionReducerRollback();
					rollback = true;
				}
				
				stepContext.setBatchStatus(ExecutionStatus.getStringValue(BatchStatus.STOPPED));
				break;
			} 
			
		}

		//If rollback is false we never issued a rollback so we can issue a logicalTXSynchronizationBeforeCompletion
		//NOTE: this will get issued even in a subjob fails or stops if no logicalTXSynchronizationRollback method is provied
		//We are assuming that not providing a rollback was intentional
		if (rollback != true) {
			if (this.partitionReducer != null) {
				this.partitionReducer.partitionReducerBeforeCompletion();
			}
		}

		return;

	}

	@Override
	protected void setupStepArtifacts() {
		
		Analyzer analyzer = step.getPartition().getAnalyzer();
		
		if (analyzer != null) {
			final List<Property> propList = analyzer.getProperties() == null ? null : analyzer.getProperties()
					.getPropertyList();
			try {
				analyzerProxy = ProxyFactory.createPartitionAnalyzerProxy(analyzer.getRef(), propList);
			} catch (final ArtifactValidationException e) {
				throw new BatchContainerServiceException("Cannot create the analyzer [" + analyzer.getRef() + "]", e);
			}
			
			analyzerProxy.setJobContext(jobExecutionImpl.getJobContext());
			analyzerProxy.setSplitContext(splitContext);
			analyzerProxy.setFlowContext(flowContext);
			analyzerProxy.setStepContext(stepContext);
		} 
			
		PartitionReducer partitionReducer = step.getPartition().getPartitionReducer();
			
		if (partitionReducer != null) {

			final List<Property> propList = partitionReducer.getProperties() == null ? null : partitionReducer.getProperties()
					.getPropertyList();
			try {
				this.partitionReducer = ProxyFactory.createPartitionReducerProxy(partitionReducer.getRef(), propList);
			} catch (final ArtifactValidationException e) {
				throw new BatchContainerServiceException("Cannot create the analyzer [" + partitionReducer.getRef() + "]",
						e);
			}
		}

	}

	@Override
	protected void invokePreStepArtifacts() {

		// Invoke the logicalTX before all parallel steps start (must occur
		// before mapper as well)
		if (this.partitionReducer != null) {
			this.partitionReducer.partitionReducerBegin();
		}

	}

	@Override
	protected void invokePostStepArtifacts() {
		// Invoke the logicalTX after all parallel steps are done
		if (this.partitionReducer != null) {
			this.partitionReducer.partitionReducerAfterCompletion(stepStatus.getExitStatus());
		}
	}
}
