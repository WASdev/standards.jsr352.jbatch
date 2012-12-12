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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jsr352.batch.jsl.Batchlet;
import jsr352.batch.jsl.Partition;
import jsr352.batch.jsl.Property;
import jsr352.batch.jsl.Step;

import com.ibm.batch.container.artifact.proxy.BatchletProxy;
import com.ibm.batch.container.artifact.proxy.ProxyFactory;
import com.ibm.batch.container.exception.BatchContainerServiceException;
import com.ibm.batch.container.jobinstance.RuntimeJobExecutionImpl;
import com.ibm.batch.container.util.ExecutionStatus;
import com.ibm.batch.container.util.ExecutionStatus.BatchStatus;
import com.ibm.batch.container.validation.ArtifactValidationException;

public class BatchletStepControllerImpl extends SingleThreadedStepControllerImpl {

    private final static String sourceClass = BatchletStepControllerImpl.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);
    
    private BatchletProxy proxy;

    public BatchletStepControllerImpl(RuntimeJobExecutionImpl jobExecutionImpl, Step step) {
        super(jobExecutionImpl, step);
    }

    private void invokeBatchlet(Batchlet batchlet) throws BatchContainerServiceException {

        String batchletId = batchlet.getRef();
        List<Property> propList = (batchlet.getProperties() == null) ? null : batchlet.getProperties().getPropertyList();

        String sourceMethod = "invokeBatchlet";
        if (logger.isLoggable(Level.FINER)) {
            logger.entering(sourceClass, sourceMethod, batchletId);
        }

        String exitStatus = null;
       
        try {
            proxy = ProxyFactory.createBatchletProxy(batchletId, propList);
        } catch (ArtifactValidationException e) {
            throw new BatchContainerServiceException("Cannot create the batchlet [" + batchletId + "]", e);
        }


        
        proxy.setJobContext(jobExecutionImpl.getJobContext());
        proxy.setStepContext(stepContext);

        if (logger.isLoggable(Level.FINE))
            logger.fine("Batchlet is loaded and validated: " + proxy);

        
        if (jobExecutionImpl.getJobContext().getBatchStatus().equals(ExecutionStatus.getStringValue(BatchStatus.STOPPING))){
            this.stepContext.setBatchStatus(ExecutionStatus.getStringValue(BatchStatus.STOPPED));
        } else {
            exitStatus = proxy.process();
            //Set the exist status on the step context even if its null.
            //We'll check for null later and default it to the batch status
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("StepContext exit status set to process() return value.");
                logger.exiting(sourceClass, sourceMethod, exitStatus);
            }
            stepContext.setExitStatus(exitStatus);
        }
    }

    @Override
    protected void invokeCoreStep() throws BatchContainerServiceException {
    	
        //TODO If this step is partitioned create partition artifacts
        Partition partition = step.getPartition();
        if (partition != null) {
        	//partition.getConcurrencyElements();
        }
    	
        invokeBatchlet(step.getBatchlet());


        if (collectorProxy != null) {
        	
        	Externalizable data = this.collectorProxy.collectPartitionData();
        	
        	if (this.analyzerProxy != null) {
        		this.analyzerProxy.analyzeCollectorData(data);
        	}
        }
        
    }

    @Override
    public void stop() { 
        stepContext.setBatchStatus(ExecutionStatus.getStringValue(BatchStatus.STOPPING));
        
        if (proxy != null) {
        	proxy.stop();	
        }
       	       

    }


}
