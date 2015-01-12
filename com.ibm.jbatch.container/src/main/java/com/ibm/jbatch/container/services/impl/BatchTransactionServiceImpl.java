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

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.runtime.context.StepContext;

import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.exception.TransactionManagementException;
import com.ibm.jbatch.container.transaction.impl.DefaultNonTransactionalManager;
import com.ibm.jbatch.container.transaction.impl.JTAUserTransactionAdapter;
import com.ibm.jbatch.spi.services.IBatchConfig;
import com.ibm.jbatch.spi.services.ITransactionManagementService;
import com.ibm.jbatch.spi.services.TransactionManagerAdapter;

public class BatchTransactionServiceImpl implements ITransactionManagementService {

    private static final String CLASSNAME = BatchTransactionServiceImpl.class.getName();
    private static final Logger logger = Logger.getLogger(CLASSNAME);
    
    /**
     * batch configuration properties.
     */
    private IBatchConfig batchConfig = null;

    /**
     * constructor
     */
    public BatchTransactionServiceImpl() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.jbatch.container.services.IBatchServiceBase#init(com.ibm.batch
     * .container.IBatchConfig)
     */
    @Override
    public void init(IBatchConfig batchConfig) throws BatchContainerServiceException {
        logger.entering(CLASSNAME, "init", batchConfig);
        this.batchConfig = batchConfig;
        logger.exiting(CLASSNAME, "init");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.jbatch.container.services.IBatchServiceBase#shutdown()
     */
    @Override
    public void shutdown() throws BatchContainerServiceException {
        logger.entering(CLASSNAME, "shutdown");
        logger.fine("do nothing");
        logger.exiting(CLASSNAME, "shutdown");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.jbatch.container.services.ITransactionManagementService#
     * getTransactionManager(javax.batch.runtime.context.StepContext)
     */
    @Override
    public TransactionManagerAdapter getTransactionManager(StepContext stepContext) throws TransactionManagementException {
        logger.entering(CLASSNAME, "getTransactionManager", stepContext);

        TransactionManagerAdapter transactionManager = null;

        // get the JTA tran manager if we are in Java EE
        if (! this.batchConfig.isJ2seMode()) {
            // use the container JNDI java:comp/UserTransaction
            logger.fine("getting transaction object from JNDI java:comp/UserTransaction");
            transactionManager = new JTAUserTransactionAdapter("java:comp/UserTransaction");
        } else if (this.batchConfig.isJ2seMode()) {
        // If we are in J2SE mode use the non-transactional manager
        
            // java environment is Java SE
            // NoOp transaction manager
            logger.fine("J2SE mode non-transactional manager");
            transactionManager = new DefaultNonTransactionalManager();
        }

        logger.exiting(CLASSNAME, "getTransactionManager", transactionManager);
        return transactionManager;
    }



}
