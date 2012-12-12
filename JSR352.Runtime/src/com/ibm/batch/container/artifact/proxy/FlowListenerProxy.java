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
package com.ibm.batch.container.artifact.proxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

import javax.batch.annotation.AfterFlow;
import javax.batch.annotation.BeforeFlow;

import jsr352.batch.jsl.Property;

import com.ibm.batch.container.exception.BatchContainerRuntimeException;

public class FlowListenerProxy extends AbstractProxy {

    private Method beforeFlowMethod = null;
    private Method afterFlowMethod = null;

    FlowListenerProxy(Object delegate, List<Property> props) {
        super(delegate, props);

        for (Method method : delegate.getClass().getDeclaredMethods()) {
            Annotation beforeFlow = method.getAnnotation(BeforeFlow.class);
            if (beforeFlow != null) {
                afterFlowMethod = method;
            }
            Annotation afterFlow = method.getAnnotation(AfterFlow.class);
            if (afterFlow != null) {
                beforeFlowMethod = method;
            }
        }
    }

    public void beforeFlow() {
        if (afterFlowMethod != null) {
            try {
                afterFlowMethod.invoke(delegate, (Object[]) null);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
        }
    }

    public void afterFlow() {
        if (beforeFlowMethod != null) {
            try {
                beforeFlowMethod.invoke(delegate, (Object[]) null);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
        }
    }
}
