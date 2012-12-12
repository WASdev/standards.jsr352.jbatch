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

import javax.batch.annotation.OnRetryProcessException;
import javax.batch.annotation.OnRetryProcessItem;
import javax.batch.annotation.OnRetryReadException;
import javax.batch.annotation.OnRetryReadItem;
import javax.batch.annotation.OnRetryWriteException;
import javax.batch.annotation.OnRetryWriteItem;

import jsr352.batch.jsl.Property;

import com.ibm.batch.container.exception.BatchContainerRuntimeException;

public class RetryListenerProxy extends AbstractProxy {

    private Method onRetryReadExceptionMethod = null;
    private Method onRetryReadItemMethod = null;
    private Method onRetryProcessExceptionMethod = null;
    private Method onRetryProcessItemMethod = null;
    private Method onRetryWriteExceptionMethod = null;
    private Method onRetryWriteItemMethod = null;

    RetryListenerProxy(Object delegate, List<Property> props) { 
        super(delegate, props);

        for (Method method : this.delegate.getClass().getDeclaredMethods()) {
            Annotation onRetryReadException = method.getAnnotation(OnRetryReadException.class);
            if (onRetryReadException != null) {
                onRetryReadExceptionMethod = method;
            }
            Annotation onRetryReadItem = method.getAnnotation(OnRetryReadItem.class);
            if (onRetryReadItem != null) {
                onRetryReadItemMethod = method;
            }

            Annotation onRetryProcessException = method.getAnnotation(OnRetryProcessException.class);
            if (onRetryProcessException != null) {
                onRetryProcessExceptionMethod = method;
            }
            
            Annotation onRetryProcessItem = method.getAnnotation(OnRetryProcessItem.class);
            if (onRetryProcessItem != null) {
            	onRetryProcessItemMethod = method;
            }
            Annotation onRetryWriteException = method.getAnnotation(OnRetryWriteException.class);
            if (onRetryWriteException != null) {
            	onRetryWriteExceptionMethod = method;
            }

            Annotation onRetryWriteItem = method.getAnnotation(OnRetryWriteItem.class);
            if (onRetryWriteItem != null) {
            	onRetryWriteItemMethod = method;
            }
            
        }
    }

    public void onRetryReadException(Exception ex) {
        if (onRetryReadExceptionMethod != null) {
            try {
                onRetryReadExceptionMethod.invoke(delegate, ex);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
        }
    }
    
    public void onRetryReadItem(Exception ex) {
        if (onRetryReadItemMethod != null) {
            try {
                onRetryReadItemMethod.invoke(delegate, ex);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
        }
    }
    
    public void onRetryProcessException(Exception ex, Object item) {
        if (onRetryProcessExceptionMethod != null) {
            try {
                onRetryProcessExceptionMethod.invoke(delegate, ex, item);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
        }
    }
    
    public void onRetryProcessItem(Exception ex, Object item) {
        if (onRetryProcessItemMethod != null) {
            try {
            	onRetryProcessItemMethod.invoke(delegate, ex, item);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
        }
    }
    
    public void onRetryWriteException(Exception ex, List<Object> items) {
        if (onRetryWriteExceptionMethod != null) {
            try {
            	onRetryWriteExceptionMethod.invoke(delegate, ex, items);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
        }
    }
    
    public void onRetryWriteItem(Exception ex, Object item) {
        if (onRetryWriteItemMethod != null) {
            try {
            	onRetryWriteItemMethod.invoke(delegate, ex, item);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
        }
    }
}
