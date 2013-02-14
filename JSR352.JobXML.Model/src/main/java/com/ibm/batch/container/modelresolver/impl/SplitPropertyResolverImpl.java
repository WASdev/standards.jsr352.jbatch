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

import jsr352.batch.jsl.Flow;
import jsr352.batch.jsl.Listener;
import jsr352.batch.jsl.Split;

import com.ibm.batch.container.modelresolver.PropertyResolverFactory;

public class SplitPropertyResolverImpl extends AbstractPropertyResolver<Split> {



    public SplitPropertyResolverImpl(boolean isPartitionStep) {
		super(isPartitionStep);
	}

	@Override
    public Split substituteProperties(final Split split, final Properties submittedProps, final Properties parentProps) {
        // resolve all the properties used in attributes and update the JAXB model
    	split.setId(this.replaceAllProperties(split.getId(), submittedProps, parentProps));
    	split.setNextFromAttribute(this.replaceAllProperties(split.getNextFromAttribute(), submittedProps, parentProps));
    	
        // Resolve all the properties defined for this step
        Properties currentProps = null;
        if (split.getProperties() != null) {
            currentProps = this.resolveElementProperties(split.getProperties().getPropertyList(), submittedProps, parentProps);
        }

        // Resolve Listener properties, this is list of listeners List<Listener>
        if (split.getListeners() != null) {
            for (final Listener listener : split.getListeners().getListenerList()) {
                PropertyResolverFactory.createListenerPropertyResolver(this.isPartitionedStep).substituteProperties(listener, submittedProps, currentProps);
            }
        }
        
        for (final Flow flow : split.getFlow()) {
        	PropertyResolverFactory.createFlowPropertyResolver(this.isPartitionedStep).substituteProperties(flow, submittedProps, currentProps);
        }
    	
        return split;
    }

}
