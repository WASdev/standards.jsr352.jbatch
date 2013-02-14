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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator.BatchStatus;

import jsr352.batch.jsl.Flow;
import jsr352.batch.jsl.JSLJob;
import jsr352.batch.jsl.Split;

import com.ibm.batch.container.AbortedBeforeStartException;
import com.ibm.batch.container.IExecutionElementController;
import com.ibm.batch.container.context.impl.StepContextImpl;
import com.ibm.batch.container.exception.BatchContainerRuntimeException;
import com.ibm.batch.container.jobinstance.ParallelJobExecution;
import com.ibm.batch.container.jobinstance.RuntimeJobExecutionImpl;
import com.ibm.batch.container.services.ServicesManager;
import com.ibm.batch.container.services.ServicesManager.ServiceType;
import com.ibm.batch.container.util.PartitionDataWrapper;

public class SplitControllerImpl implements IExecutionElementController {

    private final static String sourceClass = SplitControllerImpl.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);
	
	private final RuntimeJobExecutionImpl jobExecutionImpl;
    
	private volatile List<ParallelJobExecution> parallelJobExecs;

	private final ServicesManager servicesManager;
	private final BatchKernelImpl batchKernel;
    
	final List<JSLJob> subJobs = new ArrayList<JSLJob>();
	
    protected Split split;

    public SplitControllerImpl(RuntimeJobExecutionImpl jobExecutionImpl, Split split) {
        this.jobExecutionImpl = jobExecutionImpl;
        this.split = split;
        
		servicesManager = ServicesManager.getInstance();
		batchKernel = (BatchKernelImpl) servicesManager.getService(ServiceType.BATCH_KERNEL_SERVICE);
        
    }

    @Override
    public void stop() { 

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
        
        List<Flow> flows = this.split.getFlow();
        
		// Build all sub jobs from flows in split
		synchronized (subJobs) {
			
			for (Flow flow : flows) {
				subJobs.add(PartitionedStepBuilder.buildSubJob(jobExecutionImpl.getExecutionId(), this.split, flow, null));
			}
			
			// Then execute all subjobs in parallel
			//FIXME Right now we don't pass any job parameters along for a split!!! I don't think this is right
			//FIXME Each flow in a split should probably get a copy of the job params
			parallelJobExecs = batchKernel.startParallelJobs(subJobs, null, null);

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
			BatchStatus batchStatus = subJob.getJobExecution().getJobContext().getBatchStatus();
			if (batchStatus.equals(BatchStatus.FAILED)) {
				if (logger.isLoggable(Level.FINE)) {
					logger.fine("Subjob " + subJob.getJobExecution().getExecutionId() + "ended with status '" + batchStatus + "'" );
					logger.fine("Starting logical transaction rollback.");
				}
				
				break;
			} else if (batchStatus.equals(BatchStatus.STOPPED)){
				if (logger.isLoggable(Level.FINE)) {
					logger.fine("Subjob " + subJob.getJobExecution().getExecutionId() + "ended with status '" + batchStatus + "'" );
					logger.fine("Starting logical transaction rollback.");
				}
				break;
			}
		}
		
        if (logger.isLoggable(Level.FINER)) {
            logger.exiting(sourceMethod, sourceMethod);
        }

        return "SPLIT_CONTROLLER_RETURN_VALUE";
        
    }

    public void setStepContext(StepContextImpl<?, ? extends Externalizable> stepContext) {
        throw new BatchContainerRuntimeException("Incorrect usage: step context is not in scope within a flow.");
    }

    @Override
    public void setAnalyzerQueue(LinkedBlockingQueue<PartitionDataWrapper> analyzerQueue) {
        // no-op
    }

	public List<ParallelJobExecution> getParallelJobExecs() {
		return parallelJobExecs;
	}

}
