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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import javax.batch.annotation.Close;
import javax.batch.annotation.CheckpointInfo;
import javax.batch.annotation.Open;
import javax.batch.annotation.ReadItem;

import jsr352.batch.jsl.Property;

import com.ibm.batch.container.exception.BatchContainerRuntimeException;

public class ItemReaderProxy extends AbstractProxy {

    private Method openMethod = null;
    private Method closeMethod = null;
    private Method readItemMethod = null;
    private Method checkpointInfoMethod = null;

    ItemReaderProxy(Object delegate, List<Property> props) {
        super(delegate, props);

        for (Method method : this.delegate.getClass().getDeclaredMethods()) {
            Annotation openReader = method.getAnnotation(Open.class);
            if (openReader != null) {
                openMethod = method;
            }
            Annotation closeReader = method.getAnnotation(Close.class);
            if (closeReader != null) {
                closeMethod = method;
            }
            Annotation readItemReader = method.getAnnotation(ReadItem.class);
            if (readItemReader != null) {
                readItemMethod = method;
            }
            Annotation checkpointInfoReader = method.getAnnotation(CheckpointInfo.class);
            if (checkpointInfoReader != null) {
                checkpointInfoMethod = method;
            }
        }
    }

    public void openReader(Object checkpoint) {
        if (openMethod != null) {
            try {
            	Object[] args = new Object[1];
            	args[0] = checkpoint;
            	openMethod.invoke(delegate, args);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
        }

    }

    public void closeReader() {
        if (closeMethod != null) {
            try {
                closeMethod.invoke(delegate, (Object[]) null);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
        }

    }

    public Object readItem() throws Throwable {
        Object item = null;

        if (readItemMethod != null) {
            try {
                item = readItemMethod.invoke(delegate, (Object[]) null);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            } 
        }

        return item;
    }

    public Object checkpointInfo() {
    	Object checkpointData = null;
        if (checkpointInfoMethod != null)
            try {
            	checkpointData = checkpointInfoMethod.invoke(delegate, (Object[]) null);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }

            return checkpointData;
    }

    public Method getOpenMethod() {
        return openMethod;
    }

    public Method getCloseMethod() {
        return closeMethod;
    }

    public Method getReadItemMethod() {
        return readItemMethod;
    }

    public Method getCheckpointInfoMethod() {
        return checkpointInfoMethod;
    }

}
