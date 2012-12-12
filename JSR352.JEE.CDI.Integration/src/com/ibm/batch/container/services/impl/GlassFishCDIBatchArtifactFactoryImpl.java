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
package com.ibm.batch.container.services.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import com.ibm.batch.container.IBatchConfig;
import com.ibm.batch.container.exception.BatchContainerRuntimeException;
import com.ibm.batch.container.exception.BatchContainerServiceException;
import com.ibm.batch.container.services.IBatchArtifactFactory;


public class GlassFishCDIBatchArtifactFactoryImpl implements IBatchArtifactFactory {

	private final static Logger logger = Logger.getLogger(GlassFishCDIBatchArtifactFactoryImpl.class.getName());
	private final static String CLASSNAME = GlassFishCDIBatchArtifactFactoryImpl.class.getName();

	@Override
	public Object load(String batchId) {
		String methodName = "load";

		if (logger.isLoggable(Level.FINER)) {
			logger.entering(CLASSNAME, methodName, "Loading batch artifact id = " + batchId);
		}
		
		Object loadedArtifact = getArtifactById(batchId);

		if (loadedArtifact == null) {
			throw new IllegalArgumentException("Could not load any artifacts with batch id=" + batchId);
		}

		if (logger.isLoggable(Level.FINER)) {
			logger.exiting(CLASSNAME, methodName, "For batch artifact id = " + batchId + ", loaded artifact instance: " +
					loadedArtifact + " of type: " + loadedArtifact.getClass().getCanonicalName());
		}
		return loadedArtifact;
	}


	private Object getArtifactById(String id) {

		Object artifactInstance = null;
		
		 try {
			 InitialContext initialContext = new InitialContext();
			 BeanManager bm = (BeanManager) initialContext.lookup("java:comp/BeanManager");	 
			 Bean bean = bm.getBeans(id).iterator().next();		
			 artifactInstance = bm.getReference(bean, bean.getClass(), bm.createCreationalContext(bean));
		 } catch (Exception e) {			 
			 throw new BatchContainerRuntimeException("Tried but failed to load artifact with id: " + id, e);
		 }

		return artifactInstance;
	}

	

	@Override
	public void init(IBatchConfig batchConfig) throws BatchContainerServiceException {
		// TODO Auto-generated method stub

	}

	@Override
	public void shutdown() throws BatchContainerServiceException {
		// TODO Auto-generated method stub

	}
}

