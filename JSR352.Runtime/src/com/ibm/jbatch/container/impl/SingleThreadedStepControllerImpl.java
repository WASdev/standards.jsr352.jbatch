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
import java.util.logging.Logger;


import com.ibm.jbatch.container.IExecutionElementController;
import com.ibm.jbatch.container.artifact.proxy.InjectionReferences;
import com.ibm.jbatch.container.artifact.proxy.ProxyFactory;
import com.ibm.jbatch.container.artifact.proxy.StepListenerProxy;
import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.jobinstance.RuntimeJobExecutionHelper;
import com.ibm.jbatch.container.validation.ArtifactValidationException;
import com.ibm.jbatch.jsl.model.Collector;
import com.ibm.jbatch.jsl.model.Property;
import com.ibm.jbatch.jsl.model.Step;

public abstract class SingleThreadedStepControllerImpl extends BaseStepControllerImpl implements IExecutionElementController {

    private final static String sourceClass = SingleThreadedStepControllerImpl.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);

    protected SingleThreadedStepControllerImpl(RuntimeJobExecutionHelper jobExecutionImpl, Step step) {
        super(jobExecutionImpl, step);

    }

    List<StepListenerProxy> stepListeners = null;

    protected void setupStepArtifacts() {
        // set up listeners

        InjectionReferences injectionRef = new InjectionReferences(jobExecutionImpl.getJobContext(), stepContext, null);
        this.stepListeners = jobExecutionImpl.getListenerFactory().getStepListeners(step, injectionRef, stepContext);

        // set up collectors if we are running a partitioned step
        if (step.getPartition() != null) {
            Collector collector = step.getPartition().getCollector();
            if (collector != null) {
                List<Property> propList = (collector.getProperties() == null) ? null : collector.getProperties().getPropertyList();
                /**
                 * Inject job flow, split, and step contexts into partition
                 * artifacts like collectors and listeners some of these
                 * contexts may be null
                 */
                injectionRef = new InjectionReferences(jobExecutionImpl.getJobContext(), stepContext, propList);

                try {
                    this.collectorProxy = ProxyFactory.createPartitionCollectorProxy(collector.getRef(), injectionRef, this.stepContext);
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

        // Call @BeforeStep on all the step listeners
        for (StepListenerProxy listenerProxy : stepListeners) {
            listenerProxy.beforeStep();
        }
    }

    @Override
    protected void invokePostStepArtifacts() {

        // Call @AfterStep on all the step listeners
        if (stepListeners != null) {
            for (StepListenerProxy listenerProxy : stepListeners) {
                listenerProxy.afterStep();
            }
        }

    }

}
