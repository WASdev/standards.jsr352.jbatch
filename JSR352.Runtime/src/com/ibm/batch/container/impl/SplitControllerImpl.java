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

import java.io.Externalizable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jsr352.batch.jsl.Flow;
import jsr352.batch.jsl.JSLJob;
import jsr352.batch.jsl.Split;

import com.ibm.batch.container.AbortedBeforeStartException;
import com.ibm.batch.container.IExecutionElementController;
import com.ibm.batch.container.artifact.proxy.PartitionAnalyzerProxy;
import com.ibm.batch.container.artifact.proxy.SplitListenerProxy;
import com.ibm.batch.container.context.impl.FlowContextImpl;
import com.ibm.batch.container.context.impl.SplitContextImpl;
import com.ibm.batch.container.context.impl.StepContextImpl;
import com.ibm.batch.container.exception.BatchContainerRuntimeException;
import com.ibm.batch.container.jobinstance.RuntimeJobExecutionImpl;
import com.ibm.batch.container.jobinstance.ParallelJobExecution;
import com.ibm.batch.container.services.ServicesManager;
import com.ibm.batch.container.services.ServicesManager.ServiceType;
import com.ibm.batch.container.util.ExecutionStatus;
import com.ibm.batch.container.util.ExecutionStatus.BatchStatus;

public class SplitControllerImpl implements IExecutionElementController {

    private final static String sourceClass = SplitControllerImpl.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);
	
	private final RuntimeJobExecutionImpl jobExecutionImpl;
    protected SplitContextImpl currentSplitContext;
    
	private volatile List<ParallelJobExecution> parallelJobExecs;

	private final ServicesManager servicesManager;
	private final BatchKernelImpl batchKernel;
    
	final List<JSLJob> subJobs = new ArrayList<JSLJob>();
	
	private PartitionAnalyzerProxy analyzerProxy;
	private List<SplitListenerProxy> splitListeners = null;
	
    protected Split split;

    public SplitControllerImpl(RuntimeJobExecutionImpl jobExecutionImpl, Split split) {
        this.jobExecutionImpl = jobExecutionImpl;
        this.currentSplitContext = new SplitContextImpl(split.getId());
        this.split = split;
        
		servicesManager = ServicesManager.getInstance();
		batchKernel = (BatchKernelImpl) servicesManager.getService(ServiceType.BATCH_KERNEL_SERVICE);
        
    }

    @Override
    public void stop() { 
		currentSplitContext.setBatchStatus(ExecutionStatus.getStringValue(BatchStatus.STOPPING));

		// It's possible we may try to stop a split before any
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
    public String execute() throws AbortedBeforeStartException {
        String sourceMethod = "execute";
        if (logger.isLoggable(Level.FINER)) {
            logger.entering(sourceClass, sourceMethod);
        }
        
        currentSplitContext.setBatchStatus(ExecutionStatus.getStringValue(BatchStatus.STARTING));

        List<Flow> flows = this.split.getFlow();
        
		// Build all sub jobs from flows in split
		synchronized (subJobs) {
			
			//check if we've already issued a stop
	        if (currentSplitContext.getBatchStatus().equals(ExecutionStatus.getStringValue(BatchStatus.STOPPING))){
	            this.currentSplitContext.setBatchStatus(ExecutionStatus.getStringValue(BatchStatus.STOPPED));
	            
	            return currentSplitContext.getExitStatus();
	        }
		
			for (int instance = 0; instance < flows.size(); instance++) {
				subJobs.add(ParallelJobBuilder.buildSubJob(jobExecutionImpl.getExecutionId(), this.split, flows.get(instance), null, instance));
			}

			
            //Set up flow listeners and call beforeSplit()
    		this.splitListeners = jobExecutionImpl.getListenerFactory().getSplitListeners(split);

    		for (SplitListenerProxy listenerProxy : splitListeners) {
    			listenerProxy.setJobContext(jobExecutionImpl.getJobContext());
    			listenerProxy.setSplitContext(this.currentSplitContext);
    		}

    		currentSplitContext.setBatchStatus(ExecutionStatus.getStringValue(BatchStatus.STARTED));
    		
    		// Call @BeforeSplit on all the split listeners
    		for (SplitListenerProxy listenerProxy : splitListeners) {
    			listenerProxy.beforeSplit();
    		}
			
			// Then execute all subjobs in parallel
			//FIXME Right now we don't pass any job parameters along for a split!!! I don't think this is right
			//FIXME Each flow in a split should probably get a copy of the job params
			parallelJobExecs = batchKernel.startParallelJobs(subJobs, null, this.analyzerProxy);

		}
        
		// Then wait for the all the parallel jobs to end/stop/fail/complete
		// etc..
		// This is like a call to Thread.join()
		for (final ParallelJobExecution subJob : parallelJobExecs) {
			subJob.waitForResult();
		}
		

		//FIXME
		//check the batch status of each subJob after it's done to see if it stopped or failed
		for (final ParallelJobExecution subJob : parallelJobExecs) {
			String batchStatus = subJob.getJobExecution().getJobContext().getBatchStatus();
			if (batchStatus.equals(ExecutionStatus.getStringValue(BatchStatus.FAILED))) {
				if (logger.isLoggable(Level.FINE)) {
					logger.fine("Subjob " + subJob.getJobExecution().getExecutionId() + "ended with status '" + batchStatus + "'" );
					logger.fine("Starting logical transaction rollback.");
				}
				
				this.currentSplitContext.setBatchStatus(ExecutionStatus.getStringValue(BatchStatus.FAILED));
				
				break;
			} else if (batchStatus.equals(ExecutionStatus.getStringValue(BatchStatus.STOPPED))){
				if (logger.isLoggable(Level.FINE)) {
					logger.fine("Subjob " + subJob.getJobExecution().getExecutionId() + "ended with status '" + batchStatus + "'" );
					logger.fine("Starting logical transaction rollback.");
				}
				this.currentSplitContext.setBatchStatus(ExecutionStatus.getStringValue(BatchStatus.STOPPED));
				break;
			}
			
		}
		
        
        String curStatusString = currentSplitContext.getBatchStatus();
        if (curStatusString == null) {
            throw new IllegalStateException("Split BatchStatus should have been set by now");
        }
    	
        if (logger.isLoggable(Level.FINER)) {
            logger.exiting(sourceMethod, sourceMethod);
        }

        // Transition to "COMPLETED"
        if (currentSplitContext.getBatchStatus().equals(BatchStatus.STARTED)) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Transitioning split status to COMPLETED for split: " + split.getId());
            }
            this.currentSplitContext.setBatchStatus(ExecutionStatus.getStringValue(BatchStatus.COMPLETED));
        } 
        
		// Call @AfterSplit on all the split listeners
		for (SplitListenerProxy listenerProxy : splitListeners) {
			listenerProxy.afterSplit();
		}
        
        //if the split exit status hasn't been set, default it to the batch status
        if (currentSplitContext.getExitStatus() != null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Returning split with user-set exit status: " + currentSplitContext.getExitStatus());
            }
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Returning split with default exit status");
            }
            currentSplitContext.setExitStatus(currentSplitContext.getBatchStatus());
        }

        return currentSplitContext.getExitStatus();
        
    }

    public void setStepContext(StepContextImpl<?, ? extends Externalizable> stepContext) {
        throw new BatchContainerRuntimeException("Incorrect usage: step context is not in scope within a flow.");
    }

    public void setSplitContext(SplitContextImpl splitContext) {
    	this.currentSplitContext = splitContext;
    }

    public void setFlowContext(FlowContextImpl flowContext) {
    	throw new BatchContainerRuntimeException("Incorrect usage: flow context is not in scope within a split.");
    }

    public void setAnalyzerProxy(PartitionAnalyzerProxy analyzerProxy) {
        this.analyzerProxy = analyzerProxy;
    }
    
}
