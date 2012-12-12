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

import java.io.Externalizable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import javax.batch.annotation.Close;
import javax.batch.annotation.CheckpointInfo;
import javax.batch.annotation.Open;
import javax.batch.annotation.WriteItems;

import jsr352.batch.jsl.Property;

import com.ibm.batch.container.exception.BatchContainerRuntimeException;

public class ItemWriterProxy extends AbstractProxy {

    private Method openMethod = null;
    private Method closeMethod = null;
    private Method writeItemsMethod = null;
    private Method checkpointInfoMethod= null;

    ItemWriterProxy(Object delegate, List<Property> props) {     	
        super(delegate, props);
     
        for (Method method : this.delegate.getClass().getDeclaredMethods()) {
            Annotation openWriter = method.getAnnotation(Open.class);
            if (openWriter != null) {
                openMethod = method;
            }
            Annotation closeWriter = method.getAnnotation(Close.class);
            if (closeWriter != null) {
                closeMethod = method;
            }
            Annotation writeItemsWriter= method.getAnnotation(WriteItems.class);
            if ( writeItemsWriter != null ) { 
                writeItemsMethod = method;
            }
            Annotation checkpointInfoWriter= method.getAnnotation(CheckpointInfo.class);
            if ( checkpointInfoWriter != null ) { 
                checkpointInfoMethod = method;
            }
        }
    }

    public void openWriter(Object checkpoint) {
        if (openMethod != null) {
			try {
				openMethod.invoke(delegate, checkpoint);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
        }
    }

    public void closeWriter() {
        if (closeMethod != null) {
			try {
				closeMethod.invoke(delegate, (Object[]) null);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
        }
    }
    
    public void writeItems(List<Object> items) throws Throwable {
        if ( writeItemsMethod != null ) {
			try {
				writeItemsMethod.invoke(delegate, items);
			} catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }        
    }

    public Externalizable checkpointInfo() {
        Externalizable chkpoint = null;
        if ( checkpointInfoMethod != null ) {
			try {
				chkpoint  = (Externalizable) checkpointInfoMethod.invoke(delegate, (Object[]) null);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }                        
        }
        return chkpoint;
    }

    public Method getOpenMethod() {
        return openMethod;
    }

    public Method getCloseMethod() {
        return closeMethod;
    }

    public Method getWriteItemsMethod() {
        return writeItemsMethod;
    }

    public Method getCheckpointInfoMethod() {
        return checkpointInfoMethod;
    }

}
