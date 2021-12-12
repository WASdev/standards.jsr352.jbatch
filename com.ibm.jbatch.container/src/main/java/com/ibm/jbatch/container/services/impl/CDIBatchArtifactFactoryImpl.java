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
package com.ibm.jbatch.container.services.impl;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.spi.services.IBatchArtifactFactory;
import com.ibm.jbatch.spi.services.IBatchConfig;
import com.ibm.jbatch.container.cdi.BatchXMLMapper;

import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;

public class CDIBatchArtifactFactoryImpl implements IBatchArtifactFactory {

    private final static Logger logger = Logger.getLogger(CDIBatchArtifactFactoryImpl.class.getName());
    private final static String CLASSNAME = CDIBatchArtifactFactoryImpl.class.getName();

    @Override
    public Object load(String batchId) {
        String methodName = "load";

        if (logger.isLoggable(Level.FINER)) {
            logger.entering(CLASSNAME, methodName, "Loading batch artifact id = " + batchId);
        }

        Object loadedArtifact = getArtifactById(batchId);

        if (loadedArtifact != null) {
            if (logger.isLoggable(Level.FINER)) {
                logger.exiting(CLASSNAME, methodName, "For batch artifact id = " + batchId + ", loaded artifact instance: " + loadedArtifact
                    + " of type: " + loadedArtifact.getClass().getCanonicalName());
            }
        } else {
            if (logger.isLoggable(Level.FINER)) {
                logger.exiting(CLASSNAME, methodName, "For batch artifact id = " + batchId + ", FAILED to load artifact instance");
            }
        }
        return loadedArtifact;
    }

    protected BeanManager obtainBeanManager() throws NamingException {
        InitialContext initialContext = new InitialContext();
        return (BeanManager) initialContext.lookup("java:comp/BeanManager");
    }
            
    private Object getArtifactById(String id) {

        Object artifactInstance = null;

        try {
            final BeanManager bm = obtainBeanManager();

            final Bean<?> bean = (bm != null) ? getBeanById(bm, id) : null;
            
            final Class<?> clazz = bean.getBeanClass();
            artifactInstance = bm.getReference(bean, clazz, bm.createCreationalContext(bean));
        } catch (Exception e) {
            // Don't throw an exception but simply return null;
            logger.fine("Tried but failed to load artifact with id: " + id + ", Exception = " + e);
        }

        return artifactInstance;
    }
    
    /**
     * @param id Either the EL name of the bean, its id in batch.xml, or its fully qualified class name.
     *
     * @return the bean for the given artifact id.
     */
    protected Bean<?> getBeanById(BeanManager bm, String id) {

        Bean<?> match = getUniqueBeanByBeanName(bm, id);

        if (match == null) {
            match = getUniqueBeanForBatchXMLEntry(bm, id);
        }

        if (match == null) {
            match = getUniqueBeanForClassName(bm, id);
        }

        return match;
    }
    
    /**
     * Use the given BeanManager to lookup a unique CDI-registered bean
     * with bean name equal to 'batchId', using EL matching rules.
     *
     * @return the bean with the given bean name, or 'null' if there is an ambiguous resolution
     */
    protected Bean<?> getUniqueBeanByBeanName(BeanManager bm, String batchId) {
        Bean<?> match = null;

        // Get all beans with the given EL name (id).  EL names are applied via @Named.
        // If the bean is not annotated with @Named, then it does not have an EL name
        // and therefore can't be looked up that way.
        Set<Bean<?>> beans = bm.getBeans(batchId);

        try {
            match = bm.resolve(beans);
        } catch (AmbiguousResolutionException e) {
            return null;
        }
        return match;
    }
    
    /**
     * Use the given BeanManager to lookup a unique CDI-registered bean
     * with bean class equal to the batch.xml entry mapped to be the batchId parameter
     *
     * @return the bean with the given className. It returns null if there are zero matches or if there is no umabiguous resolution (i.e. more than 1 match)
     */
    protected Bean<?> getUniqueBeanForBatchXMLEntry(BeanManager bm, String batchId) {
        ClassLoader loader = getContextClassLoader();
        BatchXMLMapper batchXMLMapper = new BatchXMLMapper(loader);
        Class<?> clazz = batchXMLMapper.getArtifactById(batchId);
        if (clazz != null) {
            try {
                return findUniqueBeanForClass(bm, clazz);
            } catch (AmbiguousResolutionException e) {
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("getBeanForBatchXML: BatchCDIAmbiguousResolutionCheckedException: " + e.getMessage());
                }
                return null;
            }
        } else {
            return null;
        }
    }
    
    protected Bean<?> getUniqueBeanForClassName(BeanManager bm, String className) {
        // Ignore exceptions since will just failover to another loading mechanism
        try {
            Class<?> clazz = getContextClassLoader().loadClass(className);
            return findUniqueBeanForClass(bm, clazz);
        } catch (AmbiguousResolutionException e) {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("getBeanForClassName: BatchCDIAmbiguousResolutionCheckedException: " + e.getMessage());
            }
            return null;
        } catch (ClassNotFoundException cnfe) {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("getBeanForClassName: ClassNotFoundException for " + className + ": " + cnfe);
            }
            return null;
        }
    }
    
    /**
     * @return the bean within the given set whose class matches the given clazz.
     * @throws BatchCDIAmbiguousResolutionCheckedException if more than one match is found
     */
    protected Bean<?> findUniqueBeanForClass(BeanManager beanManager, Class<?> clazz) throws AmbiguousResolutionException {
        Set<Bean<?>> matches = new HashSet<Bean<?>>();
        Bean<?> retVal = null;
        Set<Bean<?>> beans = beanManager.getBeans(clazz);
        if (beans == null || beans.isEmpty()) {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("In findBeanForClass: found empty set or null for class: " + clazz);
            }
            return null;
        }
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("In findBeanForClass: found non-empty set: " + beans + " for class: " + clazz);
        }
        for (Bean<?> bean : beans) {
            if (bean.getBeanClass().equals(clazz)) {
                matches.add(bean);
            }
        }
        try {
            retVal = beanManager.resolve(matches);
        } catch (AmbiguousResolutionException e) {
            throw new AmbiguousResolutionException("Found beans = " + matches + ", and could not resolve unambiguously");
        }

        return retVal;
    }


    /**
     * @return thread context classloader
     */
    protected ClassLoader getContextClassLoader() {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
    }

    @Override
    public void init(IBatchConfig batchConfig) throws BatchContainerServiceException {
        logger.fine("Initializing CDIBatchArtifactFactoryImpl");
    }

    @Override
    public void shutdown() throws BatchContainerServiceException {
        logger.fine("Shutdown CDIBatchArtifactFactoryImpl");
    }


}
