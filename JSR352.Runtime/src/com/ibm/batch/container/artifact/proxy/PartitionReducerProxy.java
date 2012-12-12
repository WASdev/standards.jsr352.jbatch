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

import javax.batch.annotation.AfterJob;
import javax.batch.annotation.AfterStep;
import javax.batch.annotation.BeforeJob;
import javax.batch.annotation.BeforeStep;

import jsr352.batch.jsl.Property;

import com.ibm.batch.container.exception.BatchContainerRuntimeException;

public class PartitionReducerProxy extends AbstractProxy {

    private Method partitionReducerBeginMethod = null;
    private Method partitionReducerBeforeCompletionMethod = null;
    private Method partitionReducerRollbackMethod = null;
    private Method partitionReducerAfterCompletionMethod = null;

    PartitionReducerProxy(Object delegate, List<Property> props) { 
        super(delegate, props);

        //find annotations: beforeJob, afterJob
        for (Method method: delegate.getClass().getDeclaredMethods()) { 
            Annotation afterJob= method.getAnnotation(AfterJob.class);
            if ( afterJob != null ) { 
                partitionReducerBeginMethod = method;
            }
        	
        	Annotation beforeJob= method.getAnnotation(BeforeJob.class);
            if ( beforeJob != null ) { 
                partitionReducerBeforeCompletionMethod= method;
            }

            Annotation afterStep= method.getAnnotation(AfterStep.class);
            if ( afterStep != null ) { 
                partitionReducerRollbackMethod = method;
            }

            Annotation beforeStep= method.getAnnotation(BeforeStep.class);
            if ( beforeStep != null ) { 
                partitionReducerAfterCompletionMethod = method;
            }
        }
        
    }

    public void partitionReducerBegin() {
        if ( partitionReducerBeginMethod != null ) {
            try {
                partitionReducerBeginMethod.invoke(delegate, (Object[]) null);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
        }
    }
    
    public void partitionReducerBeforeCompletion() {
        if ( partitionReducerBeforeCompletionMethod != null ) {
            try {
                partitionReducerBeforeCompletionMethod.invoke(delegate, (Object[]) null);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
        }
    }

    public void partitionReducerRollback() {
        if ( partitionReducerRollbackMethod != null ) {
            try {
                partitionReducerRollbackMethod.invoke(delegate, (Object[]) null);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
        }
    }
    
    public void partitionReducerAfterCompletion(String status) {
        if ( partitionReducerAfterCompletionMethod != null ) {
            try {
                partitionReducerAfterCompletionMethod.invoke(delegate, status);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
        }
    }
}


