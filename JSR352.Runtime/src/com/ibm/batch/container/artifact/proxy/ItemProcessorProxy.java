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

import javax.batch.annotation.ProcessItem;

import jsr352.batch.jsl.Property;

public class ItemProcessorProxy  extends AbstractProxy {

    private Method processItemMethod = null;

    ItemProcessorProxy(Object delegate, List<Property> props) { 
        super(delegate, props);
        
        for (Method method : this.delegate.getClass().getDeclaredMethods()) {
            Annotation processItem = method.getAnnotation(ProcessItem.class);
            if (processItem != null) {
                processItemMethod = method;
            }
        }
    }

    public Object processItem(Object inputItem) throws Throwable {
        
        Object[] itemParam = {inputItem};
        
        Object outputItem = null;
                
        if (processItemMethod != null) {
            try {
                outputItem = processItemMethod.invoke(delegate, itemParam);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
        
        return outputItem;
    }

    public Method getProcessItemMethod() {
        return processItemMethod;
    }

}
