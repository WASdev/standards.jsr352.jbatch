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

import jsr352.batch.jsl.Decision;
import jsr352.batch.jsl.Flow;
import jsr352.batch.jsl.Listener;
import jsr352.batch.jsl.Step;

import com.ibm.batch.container.modelresolver.PropertyResolverFactory;
import com.ibm.batch.container.xjcl.ExecutionElement;

public class FlowPropertyResolverImpl extends AbstractPropertyResolver<Flow>  {


    @Override
    public Flow substituteProperties(final Flow flow, final Properties submittedProps, final Properties parentProps) {

        // resolve all the properties used in attributes and update the JAXB model
    	flow.setId(this.replaceAllProperties(flow.getId(), submittedProps, parentProps));
    	flow.setNextFromAttribute(this.replaceAllProperties(flow.getNextFromAttribute(), submittedProps, parentProps));
    	flow.setAbstract(this.replaceAllProperties(flow.getAbstract(), submittedProps, parentProps));
    	flow.setParent(this.replaceAllProperties(flow.getParent(), submittedProps, parentProps));
    	
        // Resolve all the properties defined for this step
        Properties currentProps = null;
        if (flow.getProperties() != null) {
            currentProps = this.resolveElementProperties(flow.getProperties().getPropertyList(), submittedProps, parentProps);
        }
    	
        // Resolve Listener properties, this is list of listeners List<Listener>
        if (flow.getListeners() != null) {
            for (final Listener listener : flow.getListeners().getListenerList()) {
                PropertyResolverFactory.createListenerPropertyResolver().substituteProperties(listener, submittedProps, currentProps);
            }
        }
        
        for (final ExecutionElement next : flow.getExecutionElements()) {
            if (next instanceof Step) {
                PropertyResolverFactory.createStepPropertyResolver().substituteProperties((Step)next, submittedProps, currentProps);
            } else if (next instanceof Decision) {
                PropertyResolverFactory.createDecisionPropertyResolver().substituteProperties((Decision)next, submittedProps, currentProps);
            } 
        }
    	
        return flow;
    }

}
