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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.batch.api.BatchProperty;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.AnnotationLiteral;

import com.ibm.jbatch.container.artifact.proxy.ProxyFactory;
import com.ibm.jbatch.container.util.DependencyInjectionUtility;
import com.ibm.jbatch.jsl.model.Property;

public class BatchProducerBean implements Bean<BatchProducerBean> {
    
    private final static String sourceClass = BatchProducerBean.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);

    @Produces
    @Dependent
    @BatchProperty
    public String produceProperty(InjectionPoint injectionPoint) {

        //Seems like this is a CDI bug where null injection points are getting passed in. 
        //We should be able to ignore these as a workaround.
        if (injectionPoint != null) {

            BatchProperty batchPropAnnotation = injectionPoint.getAnnotated().getAnnotation(BatchProperty.class);

            // If a name is not supplied the batch property name defaults to
            // the field name
            String batchPropName = null;
            if (batchPropAnnotation.name().equals("")) {
                batchPropName = injectionPoint.getMember().getName();
            } else {
                batchPropName = batchPropAnnotation.name();
            }

            List<Property> propList = ProxyFactory.getInjectionReferences().getProps();

            String propValue =  DependencyInjectionUtility.getPropertyValue(propList, batchPropName);
            
            return propValue;
            
        }

        return null;

    }

    @Produces
    @Dependent
    public JobContext getJobContext() {
        return ProxyFactory.getInjectionReferences().getJobContext();
    }

    @Produces
    @Dependent
    public StepContext getStepContext() {
        return ProxyFactory.getInjectionReferences().getStepContext();
    }

    @Override
    public Class<?> getBeanClass() {

        return BatchProducerBean.class;

    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {

        return Collections.emptySet();

    }

    @Override
    public String getName() {

        return "batchProducerBean";

    }

    @Override
    public Set<Annotation> getQualifiers() {

        Set<Annotation> qualifiers = new HashSet<Annotation>();

        qualifiers.add(new AnnotationLiteral<Default>() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Default.class;
            }
        });

        qualifiers.add(new AnnotationLiteral<Any>() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Any.class;
            }
        });

        return qualifiers;
    }

    @Override
    public Class<? extends Annotation> getScope() {

        return Dependent.class;

    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {

        return Collections.emptySet();

    }

    @Override
    public Set<Type> getTypes() {

        Set<Type> types = new HashSet<Type>();

        types.add(BatchProducerBean.class);

        types.add(Object.class);

        return types;

    }

    @Override
    public boolean isAlternative() {

        return false;

    }

    @Override
    public boolean isNullable() {

        return false;

    }

    @Override
    public BatchProducerBean create(CreationalContext<BatchProducerBean> ctx) {

        return new BatchProducerBean();

    }

    @Override
    public void destroy(BatchProducerBean instance, CreationalContext<BatchProducerBean> ctx) {

        ctx.release();

    }

}
