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
package com.ibm.jbatch.container.modelresolver.impl;

import java.util.Properties;


import com.ibm.jbatch.container.jsl.ControlElement;
import com.ibm.jbatch.container.modelresolver.PropertyResolverFactory;
import com.ibm.jbatch.jsl.model.Decision;

public class DecisionPropertyResolverImpl extends AbstractPropertyResolver<Decision> {

    public DecisionPropertyResolverImpl(boolean isPartitionStep) {
		super(isPartitionStep);
	}

	public Decision substituteProperties(final Decision decision, final Properties submittedProps, final Properties parentProps) {

        // resolve all the properties used in attributes and update the JAXB
        // model
        decision.setId(this.replaceAllProperties(decision.getId(), submittedProps, parentProps));
        decision.setRef(this.replaceAllProperties(decision.getRef(), submittedProps, parentProps));
        
        // Resolve all the properties defined for this decision
        Properties currentProps = null;
        if (decision.getProperties() != null) {
            currentProps = this.resolveElementProperties(decision.getProperties().getPropertyList(), submittedProps, parentProps);
        }
        
        if (decision.getControlElements() != null) {
            for (final ControlElement controlElement : decision.getControlElements()) {
                PropertyResolverFactory.createControlElementPropertyResolver(this.isPartitionedStep).substituteProperties(controlElement, submittedProps, currentProps);
            }
        }
        
        return decision;
    }

}
