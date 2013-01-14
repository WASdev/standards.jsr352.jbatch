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

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.runtime.context.StepContext;
import javax.batch.runtime.spi.TransactionManagerSPI;

import com.ibm.batch.container.config.IBatchConfig;
import com.ibm.batch.container.exception.BatchContainerServiceException;
import com.ibm.batch.container.exception.TransactionManagementException;
import com.ibm.batch.container.services.ITransactionManagementService;
import com.ibm.batch.container.transaction.impl.DefaultNonTransactionalManager;
import com.ibm.batch.container.transaction.impl.JTAUserTransactionAdapter;

public class BatchTransactionServiceImpl implements ITransactionManagementService {
	
	private static final String CLASSNAME = BatchTransactionServiceImpl.class.getName();
	
	private static final Logger logger = Logger.getLogger(CLASSNAME);
	
	public static final String JAVAX_TRANSACTION_MANAGER_SPI_PROPNAME = "javax.batch.transaction.manager.spi";
	
	
	/**
	 * batch configuration properties.
	 */
	private IBatchConfig batchConfig = null;	
	
	/**
	 * constructor
	 */
	public BatchTransactionServiceImpl() {
	}

	/* (non-Javadoc)
	 * @see com.ibm.batch.container.services.IBatchServiceBase#init(com.ibm.batch.container.IBatchConfig)
	 */
	@Override
	public void init(IBatchConfig batchConfig) throws BatchContainerServiceException {
		logger.entering(CLASSNAME, "init", batchConfig);
		this.batchConfig = batchConfig;
		logger.exiting(CLASSNAME, "init");
	}

	/* (non-Javadoc)
	 * @see com.ibm.batch.container.services.IBatchServiceBase#shutdown()
	 */
	@Override
	public void shutdown() throws BatchContainerServiceException {
		logger.entering(CLASSNAME, "shutdown");
		logger.fine("do nothing");
		logger.exiting(CLASSNAME, "shutdown");
	}

	/* (non-Javadoc)
	 * @see com.ibm.batch.container.services.ITransactionManagementService#getTransactionManager(javax.batch.runtime.context.StepContext)
	 */
	@Override
	public TransactionManagerSPI getTransactionManager(StepContext<?, ?> stepContext) throws TransactionManagementException {
		logger.entering(CLASSNAME, "getTransactionManager", stepContext);

		TransactionManagerSPI transactionManager;
		
			if(this.isStepInLocalTrans(stepContext)) {
				// no global transaction manager requested
				// use default non-transactional manager
				logger.fine("default no-op local transaction manager");
				transactionManager  = new DefaultNonTransactionalManager();
				
			} else {
				
				// global transaction manager requested
				// must be JTA-compliant, set via JVM property or batch.xml
				transactionManager = this.getTransactionManager();

			}
			
			int timeout = getTransactionTimeout(stepContext);
			logger.log(Level.FINE, "transaction timeout {0}", timeout);
			try {
				transactionManager.setTransactionTimeout(timeout);
			} catch (Exception e) {
				throw new TransactionManagementException(e);
			}
			
		
		logger.exiting(CLASSNAME, "getTransactionManager", transactionManager);
		return transactionManager;
	}
	
	/**
	 * @param stepContext
	 * @return transaction global mode defined in step properties.
	 * default value is false. Set to true to request a global transaction manager.
	 * 
	 */
	private boolean isStepInLocalTrans(final StepContext<?, ?> stepContext) {
		logger.entering(CLASSNAME, "isStepInLocalTrans", stepContext);
		Properties p = stepContext.getProperties();
		boolean inLocalTx = true; // local trans by default as per spec.
		if(p != null && !p.isEmpty()) {
			
			String globalMode = p.getProperty("javax.transaction.global.mode");
			logger.log(Level.FINE, "javax.transaction.global.mode = {0}", globalMode);
			if(globalMode != null && !globalMode.isEmpty() && globalMode.equalsIgnoreCase("true")) {
				inLocalTx = false;
			}	
		}
		logger.exiting(CLASSNAME, "isStepInLocalTrans", inLocalTx);
		return inLocalTx;
	}
	
	/**
	 * @param stepContext
	 * @return global transaction timeout defined in step properties.
	 * default timeout value is 180
	 * 
	 */
	private int getTransactionTimeout(final StepContext<?, ?> stepContext) {
		logger.entering(CLASSNAME, "getTransactionTimeout", stepContext);
		Properties p = stepContext.getProperties();
		int timeout = 180; // default as per spec.
		if(p != null && !p.isEmpty()) {
			
			String to =  p.getProperty("javax.transaction.global.timeout");
			logger.log(Level.FINE, "javax.transaction.global.timeout = {0}", to);
			if(to != null && !to.isEmpty() ) {
				timeout =  Integer.parseInt(to, 10);
			}	
		}
		logger.exiting(CLASSNAME, "getTransactionTimeout", timeout);
		return timeout;
	}
	
	/**
	 * @return transaction object defined. 
	 *  JSE defaults back to DefaultLocalTransactionManager (no op)
	 *  J2EE defaults back to the container JNDI java:comp/UserTransaction
	 * 
	 */
	private TransactionManagerSPI getTransactionManager() {
		logger.entering(CLASSNAME, "getTransactionObject");
		TransactionManagerSPI transactionManager = null;
		
		// get global transaction manager object from JVM properties
		transactionManager = getTransactionObjectFromJvmProperties();

		//If on Tran Manager is defined in the JVM props and we are in J2EE mode
		//get the JTA tran manager
		if (transactionManager == null && !this.batchConfig.isJ2seMode()) {
			// no transaction manager found and the java environment is Java EE 
			// use the container JNDI java:comp/UserTransaction
			logger.fine("getting transaction object from JNDI java:comp/UserTransaction");
			transactionManager = new JTAUserTransactionAdapter("java:comp/UserTransaction");
		}
		
		//If we are in J2SE mode and no tran manager is defined in the JVM
		//props use the non-transactional manager
		if (transactionManager == null && this.batchConfig.isJ2seMode()) {
			// not transaction manager found and the java environment is Java SE 
			// default back to NoOp transaction manager
			logger.fine("defaulting back to no op transaction manager");
			transactionManager = new DefaultNonTransactionalManager();
		}
		logger.exiting(CLASSNAME, "getTransactionObject", transactionManager);
		return transactionManager;
	}
	
	/**
	 * @return transaction object from class name defined in JVM properties
	 * 
	 */
	private TransactionManagerSPI getTransactionObjectFromJvmProperties() {
		logger.entering(CLASSNAME, "getTransactionObjectFromJvmProperties");
		TransactionManagerSPI transactionObj = null;
		
		String className = System.getProperty(JAVAX_TRANSACTION_MANAGER_SPI_PROPNAME);
		logger.log(Level.FINE, "javax.batch.transaction.manager = {0}", className);
		
		if (className != null) {
			Class<?> clazz;
			try {
				clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
				transactionObj = (TransactionManagerSPI)clazz.newInstance();
			} catch (ClassNotFoundException e) {
				throw new TransactionManagementException(e);
			} catch (IllegalAccessException e) {
				throw new TransactionManagementException(e);
			} catch (InstantiationException e) {
				throw new TransactionManagementException(e);
			} catch (ClassCastException e) {
				//JVM properties CAN ONLY be of this type TransactionManagerSPI
				throw new TransactionManagementException("Class:" + className + " from JVM property " + JAVAX_TRANSACTION_MANAGER_SPI_PROPNAME + "must implement the TransactionManagerSPI interface.", e);   
			}
			
		}
		logger.exiting(CLASSNAME, "getTransactionObjectFromJvmProperties", transactionObj);
		return transactionObj;
	}
	
}
