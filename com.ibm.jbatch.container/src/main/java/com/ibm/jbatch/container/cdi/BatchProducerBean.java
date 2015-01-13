/**
 * Copyright 2013 International Business Machines Corp.
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
package com.ibm.jbatch.container.cdi;

import java.util.List;

import javax.batch.api.BatchProperty;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

import com.ibm.jbatch.container.artifact.proxy.ProxyFactory;
import com.ibm.jbatch.container.util.DependencyInjectionUtility;
import com.ibm.jbatch.jsl.model.Property;

public class BatchProducerBean {

    @Produces
    @Dependent
    @BatchProperty
    public String produceProperty(InjectionPoint injectionPoint) {
        if (ProxyFactory.getInjectionReferences() == null) {
            return null;
        }

        BatchProperty batchPropAnnotation = injectionPoint.getAnnotated().getAnnotation(BatchProperty.class);

        // If a name is not supplied the batch property name defaults to
        // the field name
        String batchPropName;
        if (batchPropAnnotation.name().equals("")) {
            batchPropName = injectionPoint.getMember().getName();
        } else {
            batchPropName = batchPropAnnotation.name();
        }

        List<Property> propList = ProxyFactory.getInjectionReferences().getProps();

        return DependencyInjectionUtility.getPropertyValue(propList, batchPropName);
    }

    @Produces
    @Dependent
    public JobContext getJobContext() {
        if (ProxyFactory.getInjectionReferences() != null) {
            return ProxyFactory.getInjectionReferences().getJobContext();
        } else {
            return null;
        }
    }

    @Produces
    @Dependent
    public StepContext getStepContext() {
        if (ProxyFactory.getInjectionReferences() != null) {
            return ProxyFactory.getInjectionReferences().getStepContext();
        } else {
            return null;
        }
    }
}
