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

import javax.batch.annotation.OnSkipProcessItem;
import javax.batch.annotation.OnSkipReadItem;
import javax.batch.annotation.OnSkipWriteItem;

import jsr352.batch.jsl.Property;

import com.ibm.batch.container.exception.BatchContainerRuntimeException;

public class SkipListenerProxy extends AbstractProxy {

    private Method onSkipInReadMethod = null;
    private Method onSkipInWriteMethod = null;
    private Method onSkipInProcessMethod = null;

    SkipListenerProxy(Object delegate, List<Property> props) { 
        super(delegate, props);

        for (Method method : this.delegate.getClass().getDeclaredMethods()) {
            Annotation onSkipInRead = method.getAnnotation(OnSkipReadItem.class);
            if (onSkipInRead != null) {
                onSkipInReadMethod = method;
            }
            Annotation onSkipInWrite = method.getAnnotation(OnSkipWriteItem.class);
            if (onSkipInWrite != null) {
                onSkipInWriteMethod = method;
            }

            Annotation onSkipInProcess = method.getAnnotation(OnSkipProcessItem.class);
            if (onSkipInProcess != null) {
                onSkipInProcessMethod = method;
            }
        }
    }


    public void onSkipInRead(Exception ex) {
        if (onSkipInReadMethod != null) {
            try {
                onSkipInReadMethod.invoke(delegate, ex);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
        }
    }
    
    public void onSkipInWrite(Exception ex, Object record) {
        if (onSkipInWriteMethod != null) {
            try {
                onSkipInWriteMethod.invoke(delegate, ex, record);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
        }
    }
    
    public void onSkipInProcess(Exception ex, Object record) {
        if (onSkipInProcessMethod != null) {
            try {
                onSkipInProcessMethod.invoke(delegate, ex, record);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
        }
    }
    
}
