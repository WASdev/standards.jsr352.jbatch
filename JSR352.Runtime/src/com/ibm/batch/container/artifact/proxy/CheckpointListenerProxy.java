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

import javax.batch.annotation.AfterCheckpoint;
import javax.batch.annotation.BeforeCheckpoint;

import jsr352.batch.jsl.Property;

import com.ibm.batch.container.exception.BatchContainerRuntimeException;

public class CheckpointListenerProxy extends AbstractProxy {

    private Method beforeCheckpointMethod = null;
    private Method afterCheckpointMethod = null;

    CheckpointListenerProxy(final Object delegate, final List<Property> props) {
        super(delegate, props);

        // find annotations
        for (final Method method : this.delegate.getClass().getDeclaredMethods()) {
            final Annotation openReader = method.getAnnotation(BeforeCheckpoint.class);
            if (openReader != null) {
                beforeCheckpointMethod = method;
            }
            final Annotation closeReader = method.getAnnotation(AfterCheckpoint.class);
            if (closeReader != null) {
                afterCheckpointMethod = method;
            }
        }
    }

    public void beforeCheckpoint() {
        if (beforeCheckpointMethod != null) {
            try {
                beforeCheckpointMethod.invoke(delegate, (Object[]) null);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
        }
    }

    public void afterCheckpoint() {
        if (afterCheckpointMethod != null) {
            try {
                afterCheckpointMethod.invoke(delegate, (Object[]) null);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
        }
    }

}
