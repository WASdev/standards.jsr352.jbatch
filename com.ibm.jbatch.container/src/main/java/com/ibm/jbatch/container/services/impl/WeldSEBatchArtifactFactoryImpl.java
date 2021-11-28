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

import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Named;

import jakarta.enterprise.inject.se.*;

import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.spi.services.IBatchArtifactFactory;
import com.ibm.jbatch.spi.services.IBatchConfig;

@Named("MyWeldBean")
public class WeldSEBatchArtifactFactoryImpl extends CDIBatchArtifactFactoryImpl {

    private final static Logger logger = Logger.getLogger(WeldSEBatchArtifactFactoryImpl.class.getName());

    private SeContainer container;

    @Override
    protected BeanManager obtainBeanManager() {
    	return container.getBeanManager();
    }

    @Override
    public void init(IBatchConfig batchConfig) throws BatchContainerServiceException {
		logger.fine("Initializing WeldSEBatchArtifactFactoryImpl");
        container = SeContainerInitializer.newInstance().initialize();
    }

    @Override
    public void shutdown() throws BatchContainerServiceException {
		logger.fine("Shutdown WeldSEBatchArtifactFactoryImpl");
        container.close();
    }
}
