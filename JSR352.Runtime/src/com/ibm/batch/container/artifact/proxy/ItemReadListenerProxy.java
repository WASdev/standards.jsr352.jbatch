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

import javax.batch.annotation.AfterRead;
import javax.batch.annotation.BeforeRead;
import javax.batch.annotation.OnReadError;

import jsr352.batch.jsl.Property;

import com.ibm.batch.container.exception.BatchContainerRuntimeException;

public class ItemReadListenerProxy extends AbstractProxy {

    private Method beforeReadMethod = null;
    private Method afterReadMethod = null;
    private Method onReadErrorMethod = null;

    ItemReadListenerProxy(Object delegate, List<Property> props) { 
        super(delegate, props);

        for (Method method : this.delegate.getClass().getDeclaredMethods()) {
            Annotation beforeRead = method.getAnnotation(BeforeRead.class);
            if (beforeRead != null) {
                beforeReadMethod = method;
            }

            Annotation afterRead = method.getAnnotation(AfterRead.class);
            if (afterRead != null) {
                afterReadMethod = method;
            }

            Annotation onReadError = method.getAnnotation(OnReadError.class);
            if (onReadError != null) {
                onReadErrorMethod = method;
            }
        }
    }

    public void beforeRead() {
        if (beforeReadMethod != null) {
            try {
                beforeReadMethod.invoke(delegate, (Object[]) null);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
        }
    }

    public void afterRead() {
        if (afterReadMethod != null) {
            try {
                afterReadMethod.invoke(delegate, (Object[]) null);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
    }}

    public void onReadError() {
        if (onReadErrorMethod != null) {
            try {
                onReadErrorMethod.invoke(delegate, (Object[]) null);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
    }}
}
