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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.runtime.BatchStatus;


import com.ibm.jbatch.container.artifact.proxy.BatchletProxy;
import com.ibm.jbatch.container.artifact.proxy.InjectionReferences;
import com.ibm.jbatch.container.artifact.proxy.ProxyFactory;
import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.jobinstance.RuntimeJobContextJobExecutionBridge;
import com.ibm.jbatch.container.validation.ArtifactValidationException;
import com.ibm.jbatch.jsl.model.Batchlet;
import com.ibm.jbatch.jsl.model.Partition;
import com.ibm.jbatch.jsl.model.Property;
import com.ibm.jbatch.jsl.model.Step;

public class BatchletStepControllerImpl extends SingleThreadedStepControllerImpl {

	private final static String sourceClass = BatchletStepControllerImpl.class.getName();
	private final static Logger logger = Logger.getLogger(sourceClass);

	private BatchletProxy batchletProxy;

	public BatchletStepControllerImpl(RuntimeJobContextJobExecutionBridge jobExecutionImpl, Step step) {
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

		InjectionReferences injectionRef = new InjectionReferences(jobExecutionImpl.getJobContext(), stepContext, 
				propList);

		try {
			batchletProxy = ProxyFactory.createBatchletProxy(batchletId, injectionRef, stepContext);
		} catch (ArtifactValidationException e) {
			throw new BatchContainerServiceException("Cannot create the batchlet [" + batchletId + "]", e);
		}

		if (logger.isLoggable(Level.FINE))
			logger.fine("Batchlet is loaded and validated: " + batchletProxy);


		if (jobExecutionImpl.getJobContext().getBatchStatus().equals(BatchStatus.STOPPING)){
			this.statusStopped();
		} else {

			String processRetVal = batchletProxy.process();

			logger.fine("Set process() return value = " + processRetVal + " for possible use as exitStatus");
			stepContext.setBatchletProcessRetVal(processRetVal);

			logger.exiting(sourceClass, sourceMethod, exitStatus==null ? "<null>" : exitStatus);
		}
	}

	@Override
	protected void invokeCoreStep() throws BatchContainerServiceException {

		//TODO If this step is partitioned create partition artifacts
		Partition partition = step.getPartition();
		if (partition != null) {
			//partition.getConcurrencyElements();
		}
		try {
			invokeBatchlet(step.getBatchlet());
		} finally {
			invokeCollectorIfPresent();
		}

	}

	@Override
	public synchronized void stop() { 

		if (BatchStatus.STARTING.equals(stepContext.getBatchStatus()) ||
				BatchStatus.STARTED.equals(stepContext.getBatchStatus())) {

			stepContext.setBatchStatus(BatchStatus.STOPPING);

			if (batchletProxy != null) {
				batchletProxy.stop();	
			}
		} else {
			//TODO do we need to throw an error if the batchlet is already stopping/stopped
			//a stop gets issued twice
		}
	}


}
