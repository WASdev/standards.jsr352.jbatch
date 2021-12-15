/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

// Taken and reworked from source at: 
//   https://github.com/apache/bval/blob/master/bval-jsr/src/main/java/org/apache/bval/cdi/BValInterceptorBean.java
//
package com.ibm.jbatch.container.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.util.AnnotationLiteral;

public class JobOpProducerBean implements Bean {
	
	    private final Set<Type> types;
	    private final Set<Annotation> qualifiers;

	    public JobOpProducerBean() {

	        final Set<Type> t = new HashSet<>();
	        t.add(JobOperator.class);
	        t.add(Object.class);
	        types = Collections.unmodifiableSet(t);

	        final Set<Annotation> q = new HashSet<>();
	        q.add(new DefaultLiteral());
	        q.add(new AnyLiteral());
	        qualifiers = Collections.unmodifiableSet(q);
	    }
	
		@Produces
		@Dependent
		public JobOperator produceJobOperator() {
			return BatchRuntime.getJobOperator();
		}
		
	    @Override
	    public Set<Type> getTypes() {
	        return types;
	    }

	    @Override
	    public Set<Annotation> getQualifiers() {
	        return qualifiers;
	    }

	    @Override
	    public Class<? extends Annotation> getScope() {
	        return Dependent.class;
	    }

	    @Override
	    public String getName() {
	        return null;
	    }

	    @Override
	    public boolean isNullable() {
	        return false;
	    }

	    @Override
	    public Set<InjectionPoint> getInjectionPoints() {
	        return Collections.emptySet();
	    }

	    @Override
	    public Class<?> getBeanClass() {
	        return JobOpProducerBean.class;
	    }

	    @Override
	    public Set<Class<? extends Annotation>> getStereotypes() {
	        return Collections.emptySet();
	    }

	    @Override
	    public boolean isAlternative() {
	        return false;
	    }

	    public String getId() {
	        return String.format("JBatch JobOpProducer: %d", hashCode());
	    }

	    public class AnyLiteral extends AnnotationLiteral<Any> implements Any {
	    	AnyLiteral() {super(); }
	    }

	    public class DefaultLiteral extends AnnotationLiteral<Default> implements Default {
	    	DefaultLiteral() {super(); }
	    }

		@Override
		public Object create(CreationalContext creationalContext) {
			return BatchRuntime.getJobOperator();
		}

		@Override
		public void destroy(Object instance, CreationalContext creationalContext) {
			// No-op
		}

}
