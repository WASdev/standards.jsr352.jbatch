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
package com.ibm.jbatch.container.artifact.proxy;

import java.io.Serializable;
import java.util.logging.Logger;

import com.ibm.jbatch.container.context.impl.StepContextImpl;

/**
 * An abstract class which contains the common behavior for a batch artifact
 * proxy. This class performs runtime introspection of an artifact instances
 * annotations and handles property injection.
 * 
 */
public abstract class AbstractProxy<T> {

    private final static String sourceClass = AbstractProxy.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);

    protected T delegate;
    protected StepContextImpl<?, ? extends Serializable> stepContext;

    /**
     * @param delegate
     *            An instance of a batch artifact which will back this proxy
     * @param props
     *            The properties directly associated with this batch artifact.
     *            These properties will be injected into fields annotated with
     *            the @BatchProperty annotation in the delegate object.
     */
    AbstractProxy(T delegate) {
        this.delegate = delegate;
    }

    public T getDelegate() {
        return this.delegate;
    }
    
    public void setStepContext(StepContextImpl<?, ? extends Serializable> stepContext){
    	this.stepContext = stepContext;
    }
}
