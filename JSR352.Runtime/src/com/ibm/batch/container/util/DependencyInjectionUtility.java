package com.ibm.batch.container.util;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.annotation.BatchProperty;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

import jsr352.batch.jsl.Property;

import com.ibm.batch.container.artifact.proxy.InjectionReferences;
import com.ibm.batch.container.exception.BatchContainerRuntimeException;
import com.ibm.batch.container.exception.IllegalBatchPropertyException;

public class DependencyInjectionUtility {

    private final static String sourceClass = DependencyInjectionUtility.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);

    
    public static void injectReferences(Object artifact, InjectionReferences injectionRefs) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Injecting references into: " + artifact);
            logger.fine("InjectionReferences = " + injectionRefs);
        }

        Map<String, Field> propertyMap = findPropertyFields(artifact);

        if (injectionRefs.getProps() != null) {
            injectProperties(artifact, injectionRefs.getProps(), propertyMap);
        }

        injectBatchContextFields(artifact, injectionRefs.getJobContext(), 
                injectionRefs.getStepContext());
        
        if (logger.isLoggable(Level.FINE)) {
            logger.exiting(sourceClass, "injectReferences");
        }

    }
    

    /**
     * 
     * @param props
     *            The properties directly associated with this batch artifact.
     */
    private static void injectProperties(Object artifact , List<Property> props, Map<String, Field> propertyFieldMap) {

        //check if jsl properties are null or if 
        //the propertyMap is null. this means there are no annotated fields with @BatchProperty

        if (props == null || propertyFieldMap == null) {
            return;
        }
        
        // go through each field marked with @BatchProperty
        for (Entry<String, Field> batchProperty : propertyFieldMap.entrySet()) {
            String propValue = getPropertyValue(props, batchProperty.getKey());

            // if a property is supplied in the job xml inject the given value
            // into
            // the field otherwise the default value will remain
            try {
                batchProperty.getValue().set(artifact, propValue);

            } catch (IllegalArgumentException e) {
                throw new IllegalBatchPropertyException("The given property value is not an instance of the declared field.", e);
            } catch (IllegalAccessException e) {
                throw new BatchContainerRuntimeException(e);
            }

        }

    }

    
    /**
     * 
     * @param props list of properties from job xml
     * @param name name of the property
     * @return null if no matching property found 
     */
    public static String getPropertyValue(List<Property> props, String name) {
        if (props == null) {
            return null;
        }
        
        for (Property prop : props) {
            if (name.equals(prop.getName())) {
                return prop.getValue();
            }
        }
        return null;
    }

    /**
     * 
     * @param artifact
     *            An instance of the batch artifact
     * @return an ArrayList<Field> of fields annotated with @JobContext
     */
    private static void injectBatchContextFields(Object artifact, JobContext<?> jobCtx, StepContext<?,?> stepCtx) {

        // Go through declared field annotations
        for (final Field field : artifact.getClass().getDeclaredFields()) {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    field.setAccessible(true); // ignore java accessibility
                    return null;
                }
            });

            Inject injectAnnotation = field.getAnnotation(Inject.class);
            if (injectAnnotation != null) {

                if (logger.isLoggable(Level.FINER)) {
                    logger.log(Level.FINER, "Found Context on (Object, field)", new Object[] { artifact, field.getName() });
                }

                try {
                    // check the field for the context type
                    if (JobContext.class.isAssignableFrom(field.getType())) {
                        field.set(artifact, jobCtx);
                    } else if (StepContext.class.isAssignableFrom(field.getType())) {
                        field.set(artifact, stepCtx);
                    }
                } catch (IllegalArgumentException e) {
                    throw new BatchContainerRuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new BatchContainerRuntimeException(e);
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
    private static Map<String, Field> findPropertyFields(Object delegate) {

        HashMap<String, Field> propertyMap = null;
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
        return propertyMap;
    }
    
}
