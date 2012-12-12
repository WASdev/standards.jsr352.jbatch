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

import javax.batch.annotation.AfterProcess;
import javax.batch.annotation.BeforeProcess;
import javax.batch.annotation.OnProcessError;

import jsr352.batch.jsl.Property;

import com.ibm.batch.container.exception.BatchContainerRuntimeException;

public class ItemProcessListenerProxy extends AbstractProxy {

    private Method beforeProcessMethod = null;
    private Method afterProcessMethod = null;
    private Method onProcessorErrorMethod = null;

    ItemProcessListenerProxy(Object delegate, List<Property> props) {
        super(delegate, props);

        // find annotations
        for (Method method : delegate.getClass().getDeclaredMethods()) {
            Annotation beforeProcess = method.getAnnotation(BeforeProcess.class);
            if (beforeProcess != null) {
                beforeProcessMethod = method;
            }
            Annotation afterProcess = method.getAnnotation(AfterProcess.class);
            if (afterProcess != null) {
                afterProcessMethod = method;
            }

            Annotation onProcessorError = method.getAnnotation(OnProcessError.class);
            if (onProcessorError != null) {
                onProcessorErrorMethod = method;
            }
        }
    }

    public void beforeProcess() {
        if (beforeProcessMethod != null) {
            try {
                beforeProcessMethod.invoke(delegate, (Object[]) null);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
        }
    }

    public void onProcessorError() {
        if (onProcessorErrorMethod != null) {
            try {
                onProcessorErrorMethod.invoke(delegate, (Object[]) null);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
        }
    }

    public void afterProcess() {
        if (afterProcessMethod != null) {
            try {
                afterProcessMethod.invoke(delegate, (Object[]) null);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
        }
    }
}
