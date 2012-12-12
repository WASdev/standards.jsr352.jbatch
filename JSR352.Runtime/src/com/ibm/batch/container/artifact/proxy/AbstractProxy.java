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

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.annotation.BatchContext;
import javax.batch.annotation.BatchProperty;
import javax.batch.runtime.context.FlowContext;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.SplitContext;
import javax.batch.runtime.context.StepContext;

import jsr352.batch.jsl.Property;

import com.ibm.batch.container.exception.BatchContainerRuntimeException;
import com.ibm.batch.container.exception.IllegalBatchPropertyException;

/**
 * An abstract class which contains the common behavior for a batch artifact
 * proxy. This class performs runtime introspection of an artifact instances
 * annotations and handles property injection.
 * 
 */
public abstract class AbstractProxy {

    private final static String sourceClass = AbstractProxy.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);

    protected Object delegate;

    private JobContext<?> jobContext;
    private StepContext<?, ?> stepContext;
    private SplitContext splitContext;
    private FlowContext flowContext;

    private ArrayList<Field> jobContextFields;
    private ArrayList<Field> stepContextFields;
    private ArrayList<Field> splitContextFields;
    private ArrayList<Field> flowContextFields;

    protected HashMap<String, Field> propertyMap;

    /**
     * @param delegate
     *            An instance of a batch artifact which will back this proxy
     * @param props
     *            The properties directly associated with this batch artifact.
     *            These properties will be injected into fields annotated with
     *            the @BatchProperty annotation in the delegate object.
     */
    AbstractProxy(Object delegate, List<Property> props) {
        this.delegate = delegate;
        
        this.findPropertyFields(delegate);
        this.findBatchContextFields(delegate);

        if (props != null) {
            injectProperties(props);
        }

    }

    /**
     * Currently unused. May need to remove this.
     * 
     * @param name
     * @param value
     */
    private void setProperty(String name, String value) {
        Field propertyField = this.propertyMap.get(name);
        if (propertyField != null) {
            try {
                propertyField.set(delegate, value);
            } catch (IllegalArgumentException e) {
                throw new IllegalBatchPropertyException("The given property value is not an instance of the declared field.", e);
            } catch (IllegalAccessException e) {
                throw new BatchContainerRuntimeException("Field is not accesible.", e);
            }
        } else {
            throw new IllegalBatchPropertyException("An annotated property by the name, " + name + ", does not exist in this scope.");
        }
    }

    /**
     * May need to remove this. We currently have no need for direct access to
     * the delegate object.
     **/
    public Object getDelegate() {
        return this.delegate;
    }

    public void setJobContext(JobContext<?> jobContext) {
        this.jobContext = jobContext;
        if (this.jobContextFields != null) {
            for (Field field : this.jobContextFields) {
                try {
                    field.set(delegate, jobContext);
                } catch (IllegalArgumentException e) {
                    throw new BatchContainerRuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new BatchContainerRuntimeException(e);
                }
            }
        }
    }

    public JobContext<?> getJobContext() {
        return jobContext;
    }

    public void setStepContext(StepContext<?, ?> stepContext) {
        this.stepContext = stepContext;
        if (this.stepContextFields != null) {
            for (Field field : this.stepContextFields) {
                try {
                    field.set(delegate, stepContext);
                } catch (IllegalArgumentException e) {
                    throw new BatchContainerRuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new BatchContainerRuntimeException(e);
                }
            }
        }
    }

    public void setFlowContext(FlowContext flowContext) {
        this.flowContext = flowContext;
        if (this.flowContextFields != null) {
            for (Field field : this.stepContextFields) {
                try {
                    field.set(delegate, flowContext);
                } catch (IllegalArgumentException e) {
                    throw new BatchContainerRuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new BatchContainerRuntimeException(e);
                }
            }
        }
    }

    public StepContext<?, ?> getStepContext() {
        return stepContext;
    }

    public SplitContext getSplitContext() {
        return splitContext;
    }

    public FlowContext getFlowContext() {
        return flowContext;
    }

    public void setSplitContext(SplitContext splitContext) {
        this.splitContext = splitContext;
    }
    
    /**
     * 
     * @param props
     *            The properties directly associated with this batch artifact.
     */
    void injectProperties(List<Property> props) {

    	//check if jsl properties are null or if 
        //the propertyMap is null. this means there are no annotated fields with @BatchProperty

        if (props == null || this.propertyMap == null) {
            return;
        }
        
        // go through each field marked with @BatchProperty
        for (Entry<String, Field> batchProperty : this.propertyMap.entrySet()) {
            String propValue = this.getPropertyValue(props, batchProperty.getKey());

            // if a property is supplied in the job xml inject the given value
            // into
            // the field otherwise the default value will remain
            if (propValue != null) {
                try {
                    batchProperty.getValue().set(delegate, propValue);
                } catch (IllegalArgumentException e) {
                    throw new IllegalBatchPropertyException("The given property value is not an instance of the declared field.", e);
                } catch (IllegalAccessException e) {
                    throw new BatchContainerRuntimeException(e);
                }
            }

        }

    }

    private String getPropertyValue(List<Property> props, String name) {

        for (Property prop : props) {
            if (name.equals(prop.getName())) {
                return prop.getValue();
            }
        }
        return null;
    }

    /**
     * 
     * @param delegate
     *            An instance of the batch artifact
     * @return an ArrayList<Field> of fields annotated with @JobContext
     */
    private void findBatchContextFields(Object delegate) {

        // Go through declared field annotations
        for (final Field field : delegate.getClass().getDeclaredFields()) {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    field.setAccessible(true); // ignore java accessibility
                    return null;
                }
            });

            BatchContext batchContext = field.getAnnotation(BatchContext.class);
            if (batchContext != null) {

                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "Found @JobContext on (Object, field)", new Object[] { delegate, field.getName() });
                }

                Class<?> clazz = field.getType();
                
                // check the field for the context type
                if (JobContext.class.isAssignableFrom(field.getType())) {
                    if (jobContextFields == null) {
                        jobContextFields = new ArrayList<Field>();
                    }
                    jobContextFields.add(field);
                //} else if (StepContext.class.isInstance(field.getType())) {
                } else if (StepContext.class.isAssignableFrom(field.getType())) {
                    if (stepContextFields == null) {
                        stepContextFields = new ArrayList<Field>();
                    }
                    stepContextFields.add(field);
                } else if (SplitContext.class.isAssignableFrom(field.getType())) {
                    if (splitContextFields == null) {
                        splitContextFields = new ArrayList<Field>();
                    }
                    splitContextFields.add(field);
                } else if (FlowContext.class.isAssignableFrom(field.getType())) {
                    if (flowContextFields == null) {
                        flowContextFields = new ArrayList<Field>();
                    }
                    flowContextFields.add(field);
                }

            }
        }

    }

    /**
     * 
     * @param delegate
     *            An instance of the batch artifact
     * @return A map of Fields annotated with @BatchProperty.
     */
    public void findPropertyFields(Object delegate) {

        // Go through declared field annotations
        for (final Field field : delegate.getClass().getDeclaredFields()) {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    field.setAccessible(true); // ignore java accessibility
                    return null;
                }
            });

            BatchProperty batchPropertyAnnotation = field.getAnnotation(BatchProperty.class);
            if (batchPropertyAnnotation != null) {
                if (propertyMap == null) {
                    propertyMap = new HashMap<String, Field>();
                }
                // If a name is not supplied the batch property name defaults to
                // the field name
                String batchPropName = null;
                if (batchPropertyAnnotation.name().equals("")) {
                    batchPropName = field.getName();
                } else {
                    batchPropName = batchPropertyAnnotation.name();
                }

                // Check if we have already used this name for a property.
                if (propertyMap.containsKey(batchPropName)) {
                    throw new IllegalBatchPropertyException("There is already a batch property with this name: " + batchPropName);
                }

                propertyMap.put(batchPropName, field);
            }

        }

    }
}
