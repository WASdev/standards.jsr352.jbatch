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
import java.util.Stack;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.api.PartitionReducer.PartitionStatus;
import javax.batch.api.PartitionPlan;
import javax.batch.operations.JobOperator.BatchStatus;
import javax.batch.operations.exception.JobRestartException;


import com.ibm.jbatch.container.artifact.proxy.InjectionReferences;
import com.ibm.jbatch.container.artifact.proxy.PartitionAnalyzerProxy;
import com.ibm.jbatch.container.artifact.proxy.PartitionMapperProxy;
import com.ibm.jbatch.container.artifact.proxy.PartitionReducerProxy;
import com.ibm.jbatch.container.artifact.proxy.ProxyFactory;
import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.jobinstance.ParallelJobExecution;
import com.ibm.jbatch.container.jobinstance.RuntimeJobExecutionImpl;
import com.ibm.jbatch.container.jsl.CloneUtility;
import com.ibm.jbatch.container.util.BatchPartitionPlan;
import com.ibm.jbatch.container.util.PartitionDataWrapper;
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

	private int partitions = DEFAULT_PARTITION_INSTANCES;
	private int threads = DEFAULT_THREADS;
	
	private Properties[] partitionProperties = null;

	private volatile List<ParallelJobExecution> parallelJobExecs;

	private PartitionReducerProxy partitionReducerProxy = null;
	
	private PartitionAnalyzerProxy analyzerProxy = null;
	
	final List<JSLJob> subJobs = new ArrayList<JSLJob>();

	protected PartitionedStepControllerImpl(final RuntimeJobExecutionImpl jobExecutionImpl, final Step step) {
		super(jobExecutionImpl, step);

	}


	@Override
	public void stop() {

		stepContext.setBatchStatus(BatchStatus.STOPPING);

		// It's possible we may try to stop a partitioned step before any
		// sub steps have been started.
		synchronized (subJobs) {

			if (parallelJobExecs != null) {
				for (ParallelJobExecution subJob : parallelJobExecs) {
					try {
						batchKernel.stopJob(subJob.getJobExecution().getExecutionId());
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
        final PartitionMapper partitionMapper = step.getPartition().getMapper();

        //from persisted plan from previous run
        if (stepStatus.getPlan() != null) {
            plan = stepStatus.getPlan();
            
        } else if (partitionMapper != null) { //from partition mapper

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
            
            plan = new BatchPartitionPlan();
            
            plan.setPartitionCount(mapperPlan.getPartitionCount());
            plan.setThreadCount(mapperPlan.getThreadCount());
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
                    numThreads = Integer.parseInt(partitionsAttr);
                    
                    if (numThreads == 0) {
                        numThreads = numPartitions;
                    }
                    
                    
                } catch (final NumberFormatException e) {
                    throw new IllegalArgumentException("Could not parse partition threads value in stepId: " + step.getId()
                        + ", with instances=" + partitionsAttr, e);

                }   
                if (numThreads < 0) {
                    throw new IllegalArgumentException("Threads value must be 0 or greater in stepId: " + step.getId()
                        + ", with instances=" + partitionsAttr);

                }
            } else { //default to number of partitions if threads isn't set
                numThreads = numPartitions;
            }
            
            
            if (step.getPartition().getPlan().getProperties() != null) {
                
                List<JSLProperties> jslProperties = step.getPartition().getPlan().getProperties();
                for (JSLProperties props : jslProperties) {
                    int targetPartition = Integer.parseInt(props.getPartition());
                    
                    // Arrays have have start index of 0, partitions are 1 based
                    // so we subtract 1
                    try {
                        partitionProps[targetPartition - 1] = CloneUtility.jslPropertiesToJavaProperties(props);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw new BatchContainerRuntimeException("There are only " + numPartitions + " partition instances, but there are "
                                + jslProperties.size() + " partition properties lists defined.", e);
                    }
                }
                
            }
            
            plan = new BatchPartitionPlan();
            plan.setPartitionCount(numPartitions);
            plan.setThreadCount(threads);
            plan.setPartitionProperties(partitionProps);
            
        }
        
        return plan;
	}
	
	
	@Override
	protected void invokeCoreStep() throws JobRestartException {


	    PartitionPlan plan = this.generatePartitionPlan();
	    
        this.partitions = plan.getPartitionCount();
        this.threads = plan.getThreadCount();
        this.partitionProperties = plan.getPartitionProperties();
	    
		//persist the partition plan so on restart we have the same plan to reuse
		stepStatus.setPlan(plan);
		
		
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Number of partitions in step: " + partitions + " in step " + step.getId());
			logger.fine("Subjob properties defined by partition mapper: " + partitionProperties);
		}
		

		//Set up a blocking queue to pick up collector data from a partitioned thread
		LinkedBlockingQueue<PartitionDataWrapper> analyzerQueue = null;
        if (this.analyzerProxy != null) {
            analyzerQueue =  new LinkedBlockingQueue<PartitionDataWrapper>();
        }
		
		this.subJobExitStatusQueue = new Stack<String>();
		
		// Build all sub jobs from partitioned step
		synchronized (subJobs) {		
			//check if we've already issued a stop
	        if (jobExecutionImpl.getJobContext().getBatchStatus().equals(BatchStatus.STOPPING)){
	            this.stepContext.setBatchStatus(BatchStatus.STOPPED);
	            
	            return;
	        }
		
			for (int instance = 0; instance < partitions; instance++) {
				subJobs.add(PartitionedStepBuilder.buildSubJob(jobExecutionImpl.getInstanceId(), step, instance));
			}
	
			// Then execute all subjobs in parallel
			if (stepStatus.getStartCount() > 1) {
				parallelJobExecs = batchKernel.restartParallelJobs(subJobs, partitionProperties, analyzerQueue, subJobExitStatusQueue);
			} else {
				parallelJobExecs = batchKernel.startParallelJobs(subJobs, partitionProperties, analyzerQueue, subJobExitStatusQueue);	
			}
			

		}
		
        // Now wait for the queues to fill up
        int completedPartitions = 0;
        if (analyzerProxy != null) {
            while (true) {

                try {
                    PartitionDataWrapper dataWrapper = analyzerQueue.take();

                    switch (dataWrapper.getEventType()) {
                    case ANALYZE_COLLECTOR_DATA:
                        analyzerProxy.analyzeCollectorData(dataWrapper.getCollectorData());
                        break;
                    case ANALYZE_STATUS:
                        analyzerProxy.analyzeStatus(dataWrapper.getBatchstatus().name(), dataWrapper.getExitStatus());
                        completedPartitions++;
                        break;
                    case STEP_FINISHED:
                        completedPartitions++;
                        break;
                    default:
                        throw new IllegalStateException("Invalid partition state");
                    }

                } catch (InterruptedException e) {
                    throw new BatchContainerRuntimeException(e);
                }

                if (completedPartitions == partitions) {
                    break;
                }
            }

        }

		/**
		 * Wait for the all the parallel jobs to end/stop/fail/complete etc..
		 * check the batch status of each subJob after it's done to see if we need to issue a rollback
		 * start rollback if any have stopped or failed
		 */
		boolean rollback = false;
		boolean partitionedFailed = false;
		for (final ParallelJobExecution subJob : parallelJobExecs) {

	        subJob.waitForResult(); // This is like a call to Thread.join()

			BatchStatus batchStatus = subJob.getJobExecution().getJobContext().getBatchStatus();
			if (batchStatus.equals(BatchStatus.FAILED)) {
				if (logger.isLoggable(Level.FINE)) {
					logger.fine("Subjob " + subJob.getJobExecution().getExecutionId() + "ended with status '" + batchStatus + "'" );
					logger.fine("Starting logical transaction rollback.");
				}
				
				rollback = true;
				partitionedFailed = true;

				//Keep track of the failing status and throw an exception to propagate after the rest of the partitions are complete
				
				stepContext.setBatchStatus(BatchStatus.FAILED);
				
			} else if (batchStatus.equals(BatchStatus.STOPPED)) {
				if (logger.isLoggable(Level.FINE)) {
					logger.fine("Subjob " + subJob.getJobExecution().getExecutionId() + "ended with status '" + batchStatus + "'" );
					logger.fine("Starting logical transaction rollback.");
				}
				
				rollback = true;
				
				//If another partition has already failed leave the status alone
				if (!!!BatchStatus.FAILED.equals(stepContext.getBatchStatus())) {
				    stepContext.setBatchStatus(BatchStatus.STOPPED);    
				}
				
			} 
			
		}

		//If rollback is false we never issued a rollback so we can issue a logicalTXSynchronizationBeforeCompletion
		//NOTE: this will get issued even in a subjob fails or stops if no logicalTXSynchronizationRollback method is provied
		//We are assuming that not providing a rollback was intentional
        if (rollback == true) {
            if (this.partitionReducerProxy != null) {
                this.partitionReducerProxy.rollbackPartitionedStep();
            }
            
        } else {
            if (this.partitionReducerProxy != null) {
                this.partitionReducerProxy.beforePartitionedStepCompletion();
            }
        }

        if (partitionedFailed) {
            throw new BatchContainerRuntimeException("One or more partitions failed");
        }
        
        // get last item in queue - this should be the last subjob to run
        if (this.stepContext.getExitStatus() == null){
        	// still need to deal with the potential of a null exit status coming out of a batchlet subjob
        	this.stepContext.setExitStatus(this.subJobExitStatusQueue.pop());
            this.subJobExitStatusQueue.clear();

        }
		return;

	}

	@Override
	protected void setupStepArtifacts() {
		
		Analyzer analyzer = step.getPartition().getAnalyzer();
		
		if (analyzer != null) {
			final List<Property> propList = analyzer.getProperties() == null ? null : analyzer.getProperties()
					.getPropertyList();
			
	        InjectionReferences injectionRef = new InjectionReferences(jobExecutionImpl.getJobContext(), stepContext, 
	                propList);
	        
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
			
	         InjectionReferences injectionRef = new InjectionReferences(jobExecutionImpl.getJobContext(), stepContext, 
	                    propList);
			
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
	}
}
