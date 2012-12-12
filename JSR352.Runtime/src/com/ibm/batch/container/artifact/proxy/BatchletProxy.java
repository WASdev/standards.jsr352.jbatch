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

import javax.batch.annotation.Process;
import javax.batch.annotation.Stop;

import jsr352.batch.jsl.Property;

import com.ibm.batch.container.exception.BatchContainerRuntimeException;

public class BatchletProxy extends AbstractProxy {

    private Method processMethod = null;
    private Method stopMethod = null;

    BatchletProxy(Object delegate, List<Property> props) {
        super(delegate, props);

        // find method level annotations
        for (Method method : delegate.getClass().getDeclaredMethods()) {
            Annotation process = method.getAnnotation(Process.class);
            if (process != null) {
                processMethod = method;
            }

            Annotation stop = method.getAnnotation(Stop.class);
            if (stop != null) {
                stopMethod = method;
            }

        }
    }

    public String process() {
        String retVal = null;
        if (processMethod != null) {
            try {
                retVal = (String) processMethod.invoke(delegate, (Object[]) null);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
        }
        return retVal;
    }

    public void stop() {
        if (stopMethod != null) {
            try {
                stopMethod.invoke(delegate, (Object[]) null);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
        }
    }

    public Object getDelegate() {
        return delegate;
    }


    public Method getProcessMethod() {
        return processMethod;
    }

    public Method getStopMethod() {
        return stopMethod;
    }

}
