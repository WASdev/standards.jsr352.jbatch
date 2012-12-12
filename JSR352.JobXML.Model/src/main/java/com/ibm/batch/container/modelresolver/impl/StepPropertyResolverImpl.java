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
package com.ibm.batch.container.modelresolver.impl;

import java.util.Properties;

import com.ibm.batch.container.modelresolver.PropertyResolverFactory;
import com.ibm.batch.container.xjcl.ControlElement;

import jsr352.batch.jsl.Listener;
import jsr352.batch.jsl.Step;

public class StepPropertyResolverImpl extends AbstractPropertyResolver<Step> {

    @Override
    public Step substituteProperties(final Step step, final Properties submittedProps, final Properties parentProps) {

        // resolve all the properties used in attributes and update the JAXB
        // model
        step.setId(this.replaceAllProperties(step.getId(), submittedProps, parentProps));

        step.setAbstract(this.replaceAllProperties(step.getAbstract(), submittedProps, parentProps));
        step.setAllowStartIfComplete(this.replaceAllProperties(step.getAllowStartIfComplete(), submittedProps, parentProps));
        step.setNextFromAttribute(this.replaceAllProperties(step.getNextFromAttribute(), submittedProps, parentProps));
        step.setStartLimit(this.replaceAllProperties(step.getStartLimit(), submittedProps, parentProps));
        step.setParent(this.replaceAllProperties(step.getParent(), submittedProps, parentProps));

        // Resolve all the properties defined for this step
        Properties currentProps = null;
        if (step.getProperties() != null) {
            currentProps = this.resolveElementProperties(step.getProperties().getPropertyList(), submittedProps, parentProps);
        }

        // Resolve Listener properties, this is list of listeners List<Listener>
        if (step.getListeners() != null) {
            for (final Listener listener : step.getListeners().getListenerList()) {
                PropertyResolverFactory.createListenerPropertyResolver().substituteProperties(listener, submittedProps, currentProps);
            }
        }
        
        if (step.getControlElements() != null) {
            for (final ControlElement controlElement : step.getControlElements()) {
                PropertyResolverFactory.createControlElementPropertyResolver().substituteProperties(controlElement, submittedProps, currentProps);
            }
        }
        
        
        
        

        // Resolve Batchlet properties
        if (step.getBatchlet() != null) {
            PropertyResolverFactory.createBatchletPropertyResolver().substituteProperties(step.getBatchlet(), submittedProps, currentProps);
        }

        // Resolve Chunk properties
        if (step.getChunk() != null) {
            PropertyResolverFactory.createChunkPropertyResolver().substituteProperties(step.getChunk(), submittedProps, currentProps);
        }

        return step;
    }

}
