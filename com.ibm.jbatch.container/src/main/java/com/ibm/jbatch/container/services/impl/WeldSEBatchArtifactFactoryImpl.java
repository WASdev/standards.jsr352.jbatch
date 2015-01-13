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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Named;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.spi.services.IBatchArtifactFactory;
import com.ibm.jbatch.spi.services.IBatchConfig;

@Named("MyWeldBean")
public class WeldSEBatchArtifactFactoryImpl implements IBatchArtifactFactory {

    private final static Logger logger = Logger.getLogger(WeldSEBatchArtifactFactoryImpl.class.getName());
    private final static String CLASSNAME = WeldSEBatchArtifactFactoryImpl.class.getName();

    // TODO - synchronize appropriately once we learn more about usage
    private boolean loaded = false;
    private Weld weld;
    private WeldContainer container;

    // Uses TCCL
    @Override
    public Object load(String batchId) {
        String methodName = "load";

        if (logger.isLoggable(Level.FINER)) {
            logger.entering(CLASSNAME, methodName, "Loading batch artifact id = " + batchId);
        }

        Object loadedArtifact = getArtifactById(batchId);

        if (loadedArtifact == null) {

            logger.exiting(CLASSNAME, methodName, "Returning null artifact for id: " + batchId);

            return loadedArtifact;

        }

        if (logger.isLoggable(Level.FINER)) {
            logger.exiting(CLASSNAME, methodName, "For batch artifact id = " + batchId + ", loaded artifact instance: " + loadedArtifact
                    + " of type: " + loadedArtifact.getClass().getCanonicalName());
        }
        return loadedArtifact;
    }

    private Object getArtifactById(String id) {

        Object artifactInstance = null;

        try {
            final BeanManager bm = container.getBeanManager();

            final Bean<?> bean = bm.resolve(bm.getBeans(id));

            final Class clazz = bean.getBeanClass();

            artifactInstance = bm.getReference(bean, clazz, bm.createCreationalContext(bean));
        } catch (Exception e) {
            // Don't throw an exception but simply return null;
            logger.fine("Tried but failed to load artifact with id: " + id + ", Exception = " + e);
        }
        return artifactInstance;
    }

    @Override
    public void init(IBatchConfig batchConfig) throws BatchContainerServiceException {
        weld = new Weld();
        container = weld.initialize();
    }

    @Override
    public void shutdown() throws BatchContainerServiceException {
        weld.shutdown();
    }
}
