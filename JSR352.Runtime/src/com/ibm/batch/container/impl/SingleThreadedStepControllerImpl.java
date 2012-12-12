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

import java.util.List;
import java.util.logging.Logger;

import jsr352.batch.jsl.Collector;
import jsr352.batch.jsl.Property;
import jsr352.batch.jsl.Step;

import com.ibm.batch.container.IExecutionElementController;
import com.ibm.batch.container.artifact.proxy.ProxyFactory;
import com.ibm.batch.container.artifact.proxy.StepListenerProxy;
import com.ibm.batch.container.exception.BatchContainerServiceException;
import com.ibm.batch.container.jobinstance.RuntimeJobExecutionImpl;
import com.ibm.batch.container.validation.ArtifactValidationException;

public abstract class SingleThreadedStepControllerImpl extends BaseStepControllerImpl implements IExecutionElementController {

	private final static String sourceClass = SingleThreadedStepControllerImpl.class.getName();
	private final static Logger logger = Logger.getLogger(sourceClass);

	protected SingleThreadedStepControllerImpl(RuntimeJobExecutionImpl jobExecutionImpl, Step step) {
		super(jobExecutionImpl, step);

	}

	List<StepListenerProxy> stepListeners = null;

	protected void setupStepArtifacts() {
		// set up listeners
		this.stepListeners = jobExecutionImpl.getListenerFactory().getStepListeners(step);

		// set up collectors if we are running a partitioned step
		if (step.getPartition() != null) {
			Collector collector = step.getPartition().getCollector();
			if (collector != null) {
				List<Property> propList = (collector.getProperties() == null) ? null : collector.getProperties().getPropertyList();
				try {
					this.collectorProxy = ProxyFactory.createPartitionCollectorProxy(collector.getRef(), propList);
				} catch (ArtifactValidationException e) {
					throw new BatchContainerServiceException("Cannot create the collector [" + collector.getRef() + "]", e);
				}
			}
			
		}

	}

	@Override
	protected void invokePreStepArtifacts() {
		if (stepListeners == null) {
			return;
		}

		/**
		 * Inject job flow, split, and step contexts into partition artifacts
		 * like collectors and listeners some of these contexts may be null
		 */
		if (this.collectorProxy != null) {
			this.collectorProxy.setJobContext(jobExecutionImpl.getJobContext());
			this.collectorProxy.setSplitContext(splitContext);
			this.collectorProxy.setFlowContext(flowContext);
			this.collectorProxy.setStepContext(stepContext);
		}

		for (StepListenerProxy listenerProxy : stepListeners) {
			listenerProxy.setJobContext(jobExecutionImpl.getJobContext());
			listenerProxy.setSplitContext(splitContext);
			listenerProxy.setFlowContext(flowContext);
			listenerProxy.setStepContext(stepContext);
		}

		// Call @BeforeStep on all the job listeners
		for (StepListenerProxy listenerProxy : stepListeners) {
			listenerProxy.beforeStep();
		}
		
		// 
		//ServicesManager.getInstance().setStepForThread(stepContext);

	}

	@Override
	protected void invokePostStepArtifacts() {
		
		// Invoke the subjob analayzer at the end of each step
		if (this.analyzerProxy != null) {
			this.analyzerProxy.analyzeExitStatus(stepStatus.getExitStatus());
		}
		
		// Call @AfterStep on all the step listeners
		if (stepListeners != null) {
			for (StepListenerProxy listenerProxy : stepListeners) {
				listenerProxy.afterStep();
			}
		}

	}

}
