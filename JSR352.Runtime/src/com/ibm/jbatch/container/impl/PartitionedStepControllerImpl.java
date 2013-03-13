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
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Stack;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.api.partition.PartitionPlan;
import javax.batch.api.partition.PartitionReducer.PartitionStatus;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.JobOperator.BatchStatus;
import javax.batch.operations.JobStartException;

import com.ibm.jbatch.container.artifact.proxy.InjectionReferences;
import com.ibm.jbatch.container.artifact.proxy.PartitionAnalyzerProxy;
import com.ibm.jbatch.container.artifact.proxy.PartitionMapperProxy;
import com.ibm.jbatch.container.artifact.proxy.PartitionReducerProxy;
import com.ibm.jbatch.container.artifact.proxy.ProxyFactory;
import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.jobinstance.RuntimeJobExecutionHelper;
import com.ibm.jbatch.container.jsl.CloneUtility;
import com.ibm.jbatch.container.util.BatchPartitionPlan;
import com.ibm.jbatch.container.util.BatchWorkUnit;
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

	private volatile List<BatchWorkUnit> parallelBatchWorkUnits;

	private PartitionReducerProxy partitionReducerProxy = null;
	
	private PartitionAnalyzerProxy analyzerProxy = null;
	
	final List<JSLJob> subJobs = new ArrayList<JSLJob>();

	protected PartitionedStepControllerImpl(final RuntimeJobExecutionHelper jobExecutionImpl, final Step step) {
		super(jobExecutionImpl, step);

	}


	@Override
	public void stop() {

		stepContext.setBatchStatus(BatchStatus.STOPPING);

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
        PartitionPlan previousPlan = null;
        final PartitionMapper partitionMapper = step.getPartition().getMapper();

        //from persisted plan from previous run
        if (stepStatus.getPlan() != null) {
            previousPlan = stepStatus.getPlan();
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
            if (mapperPlan.getPartitionsOverride() || previousPlan == null){
                plan.setPartitions(mapperPlan.getPartitions());
            } else {
                plan.setPartitions(previousPlan.getPartitions());
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
            plan.setPartitions(numPartitions);
            plan.setThreads(numThreads);
            plan.setPartitionProperties(partitionProps);
            plan.setPartitionsOverride(false); //FIXME what is the default for a static plan??
            
        }
        
        return plan;
	}
	
	
	@Override
	protected void invokeCoreStep() throws JobRestartException, JobStartException {


	    PartitionPlan plan = this.generatePartitionPlan();
	    
        this.partitions = plan.getPartitions();
        this.threads = plan.getThreads();
        this.partitionProperties = plan.getPartitionProperties();
	    
		//persist the partition plan so on restart we have the same plan to reuse
		stepStatus.setPlan(plan);
		
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
		
		
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Number of partitions in step: " + partitions + " in step " + step.getId());
			logger.fine("Subjob properties defined by partition mapper: " + partitionProperties);
		}
		

		//Set up a blocking queue to pick up collector data from a partitioned thread
		LinkedBlockingQueue<PartitionDataWrapper> analyzerQueue = null;
        if (this.analyzerProxy != null) {
            analyzerQueue =  new LinkedBlockingQueue<PartitionDataWrapper>();
        }

        BlockingQueue<BatchWorkUnit> completedWorkQueue = new LinkedBlockingQueue<BatchWorkUnit>();
        
		this.subJobExitStatusQueue = new Stack<String>();
		
		// Build all sub jobs from partitioned step
		synchronized (subJobs) {		
			//check if we've already issued a stop
	        if (jobExecutionImpl.getJobContext().getBatchStatus().equals(BatchStatus.STOPPING)){
	            this.stepContext.setBatchStatus(BatchStatus.STOPPED);
	            
	            return;
	        }
		
			for (int instance = 0; instance < partitions; instance++) {
				subJobs.add(PartitionedStepBuilder.buildSubJob(jobExecutionImpl.getInstanceId(),this.jobExecutionImpl.getJobContext(), step, instance));
			}
	
			// Then build all the subjobs but do not start them yet
			if (stepStatus.getStartCount() > 1 && !!!plan.getPartitionsOverride()) {
				parallelBatchWorkUnits = batchKernel.buildRestartableParallelJobs(subJobs, partitionProperties, analyzerQueue, subJobExitStatusQueue, completedWorkQueue, this.containment, null);
				
			} else {
				parallelBatchWorkUnits = batchKernel.buildNewParallelJobs(subJobs, partitionProperties, analyzerQueue, subJobExitStatusQueue, completedWorkQueue, this.containment, null);
				
			}

		}
		
		//Start up to to the max num we are allowed from the num threads attribute
		Iterator<BatchWorkUnit> iterator = parallelBatchWorkUnits.iterator();
		for (int i=0; i < this.threads && iterator.hasNext(); i++ ) {
		    
		    //start or restart a subjob
            if (stepStatus.getStartCount() > 1 && !!!plan.getPartitionsOverride()) {
                batchKernel.restartGeneratedJob(iterator.next());
            } else {
                batchKernel.startGeneratedJob(iterator.next());
            }
		    
		}

		
		List<BatchWorkUnit> completedWork = new ArrayList<BatchWorkUnit>(this.partitions);
		
        // Now wait for the queues to fill up
        int completedPartitions = 0;
        while (true) {

            try {
            if (analyzerProxy != null) {

                PartitionDataWrapper dataWrapper = analyzerQueue.take();

                switch (dataWrapper.getEventType()) {
                case ANALYZE_COLLECTOR_DATA:
                    analyzerProxy.analyzeCollectorData(dataWrapper.getCollectorData());
                    break;
                case ANALYZE_STATUS:
                    analyzerProxy.analyzeStatus(dataWrapper.getBatchstatus(), dataWrapper.getExitStatus());
                    completedPartitions++;
                    if (iterator.hasNext()) {
                        // block until at least one thread has finished to
                        // submit more batch work.
                        // hold on to the finished work to look at later
                        completedWork.add(completedWorkQueue.take());

                        // start or restart a subjob
                        if (stepStatus.getStartCount() > 1) {
                            batchKernel.startGeneratedJob(iterator.next());
                        } else {
                            batchKernel.restartGeneratedJob(iterator.next());
                        }

                    }
                    break;
                case STEP_ALREADY_COMPLETED:
                    completedPartitions++;
                    if (iterator.hasNext()) {
                        // block until at least one thread has finished to
                        // submit more batch work.
                        // hold on to the finished work to look at later
                        completedWork.add(completedWorkQueue.take());

                        // start or restart a subjob
                        if (stepStatus.getStartCount() > 1) {
                            batchKernel.startGeneratedJob(iterator.next());
                        } else {
                            batchKernel.restartGeneratedJob(iterator.next());
                        }

                    }

                    break;
                default:
                    throw new IllegalStateException("Invalid partition state");
                }

                if (completedPartitions == partitions) {
                    break;
                }
            } else {

                if (iterator.hasNext()) {
                    // block until at least one thread has finished to
                    // submit more batch work. hold on to the finished work to
                    // look at later
                    completedWork.add(completedWorkQueue.take());

                    // start or restart a subjob
                    if (stepStatus.getStartCount() > 1) {
                        batchKernel.startGeneratedJob(iterator.next());
                    } else {
                        batchKernel.restartGeneratedJob(iterator.next());
                    }
                } else {
                    break;
                }
            }
            }catch (InterruptedException e) {
                throw new BatchContainerRuntimeException(e);
            }
        }        

		/**
		 * check the batch status of each subJob after it's done to see if we need to issue a rollback
		 * start rollback if any have stopped or failed
		 */
		boolean rollback = false;
		boolean partitionFailed = false;
		
		//make sure all the work has finished
		for (int i = 0; completedWork.size() < this.partitions; i++) {
		    try {
                completedWork.add(completedWorkQueue.take());
            } catch (InterruptedException e) {
                throw new BatchContainerRuntimeException(e);
            }
		}
		
		for (final BatchWorkUnit subJob : completedWork) {


			BatchStatus batchStatus = subJob.getJobExecutionImpl().getJobContext().getBatchStatus();
			if (batchStatus.equals(BatchStatus.FAILED)) {
				if (logger.isLoggable(Level.FINE)) {
					logger.fine("Subjob " + subJob.getJobExecutionImpl().getExecutionId() + " ended with status '" + batchStatus + "'" );
					logger.fine("Starting logical transaction rollback.");
				}
				
				rollback = true;
				partitionFailed = true;

				//Keep track of the failing status and throw an exception to propagate after the rest of the partitions are complete
				
				stepContext.setBatchStatus(BatchStatus.FAILED);
				
			} else if (batchStatus.equals(BatchStatus.STOPPED)) {
				if (logger.isLoggable(Level.FINE)) {
					logger.fine("Subjob " + subJob.getJobExecutionImpl().getExecutionId() + "ended with status '" + batchStatus + "'" );
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

        
        // get last item in queue - this should be the last subjob to run
        if (this.stepContext.getExitStatus() == null){
        	// still need to deal with the potential of a null exit status coming out of a batchlet subjob
        	this.stepContext.setExitStatus(this.subJobExitStatusQueue.pop());
            this.subJobExitStatusQueue.clear();

        }
        
        if (partitionFailed) {
            throw new BatchContainerRuntimeException("One or more partitions failed");
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
