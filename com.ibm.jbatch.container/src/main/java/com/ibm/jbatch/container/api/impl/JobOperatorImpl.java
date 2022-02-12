/*
 * Copyright 2022 International Business Machines Corp.
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
package com.ibm.jbatch.container.api.impl;

import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.ibm.jbatch.container.exception.TransactionManagementException;

import jakarta.batch.operations.BatchRuntimeException;
import jakarta.batch.operations.JobExecutionAlreadyCompleteException;
import jakarta.batch.operations.JobExecutionIsRunningException;
import jakarta.batch.operations.JobExecutionNotMostRecentException;
import jakarta.batch.operations.JobExecutionNotRunningException;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.operations.JobRestartException;
import jakarta.batch.operations.JobSecurityException;
import jakarta.batch.operations.JobStartException;
import jakarta.batch.operations.NoSuchJobException;
import jakarta.batch.operations.NoSuchJobExecutionException;
import jakarta.batch.operations.NoSuchJobInstanceException;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.JobInstance;
import jakarta.batch.runtime.StepExecution;
import jakarta.inject.Inject;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;



/**
 * Thin wrapper around the real JobOperatorImpl that suspends the active transaction
 * for every JobOperator API method.
 */
public class JobOperatorImpl implements JobOperator {

	private final static String sourceClass = JobOperatorImpl.class.getName();
	private final static Logger logger = Logger.getLogger(sourceClass);

    /**
     * For suspending/resuming transactions
     */
    private TransactionManager tranMgr;
    
    /**
     * The real JobOperator.  All calls forwarded to this guy.
     */
    private JobOperator jobOperator;
    
	public JobOperatorImpl() {
		jobOperator = new JobOperatorImplDelegate();
		tranMgr = jndiLookup();
	}


	private TransactionManager jndiLookup() {
		logger.entering(sourceClass, "jndiLookup");
		String jndi = "java:appserver/TransactionManager";
		InitialContext ctxt;
		TransactionManager retVal = null;
		try {
			ctxt = new InitialContext();
			retVal = (TransactionManager) ctxt.lookup(jndi);
			logger.fine("JNDI transaction manager found");
		} catch (NamingException ne) {
			logger.info("JNDI transaction manager not found at: " + jndi);
		}
		logger.exiting(sourceClass, "jndiLookup");
		return retVal;
	}

	 
    /**
     * @return the just-suspended tran, or null if there wasn't one
     */
	 private Transaction suspendTran() {
		 if (tranMgr == null) {
			 if (logger.isLoggable(Level.FINE)) {            
				 logger.fine("JobOperator suspend, return null since tranManager is null");
			 }
			 return null;
		 } else {
			 if (logger.isLoggable(Level.FINE)) {            
				 logger.fine("JobOperator suspending transaction");
			 }
		 }
		 try {
			 Transaction suspended = tranMgr.suspend();
			 if (logger.isLoggable(Level.FINER)) {           
				 logger.finer("JobOperator suspending transaction: " + suspended);
			 }
             return suspended;
		 } catch (SystemException se) {
			 logger.severe("Failed to suspend current transaction before JobOperator method");
			 throw new BatchRuntimeException("Failed to suspend current transaction before JobOperator method", se);
		 }
	 }
    
    /**
     * Resume the given tran.
     */
    private void resumeTran(Transaction tran) {
        if (tran != null) {
			 if (logger.isLoggable(Level.FINE)) {            
				 logger.fine("JobOperator resuming transaction = " + tran);
			 }
            try {
                tranMgr.resume(tran);
            } catch (Exception e) {
			    logger.severe("Failed to resume transaction before JobOperator method, tran = " + tran);
                throw new BatchRuntimeException("Failed to resume transaction after JobOperator method", e);
            } 
        } else {
			 if (logger.isLoggable(Level.FINER)) {            
				 logger.finer("No-op on JobOperator resume, transaction = <null>");
			 }
        }
    }
    
    @Override
    public Set<String> getJobNames() throws JobSecurityException {
        Transaction tran = suspendTran();
        try {
            return jobOperator.getJobNames();
        } finally {
            resumeTran(tran);
        }
    }

    @Override
    public int getJobInstanceCount(String jobName) throws NoSuchJobException, JobSecurityException {
        Transaction tran = suspendTran();
        try {
            return jobOperator.getJobInstanceCount(jobName);
        } finally {
            resumeTran(tran);
        }
    }

    @Override
    public List<JobInstance> getJobInstances(String jobName, int start, int count) throws NoSuchJobException, JobSecurityException {
        Transaction tran = suspendTran();
        try {
            return jobOperator.getJobInstances(jobName, start, count);
        } finally {
            resumeTran(tran);
        }
    }

    @Override
    public List<Long> getRunningExecutions(String jobName) throws NoSuchJobException, JobSecurityException {
        Transaction tran = suspendTran();
        try {
            return jobOperator.getRunningExecutions(jobName);
        } finally {
            resumeTran(tran);
        }
    }

    @Override
    public Properties getParameters(long executionId) throws NoSuchJobExecutionException, JobSecurityException {
        Transaction tran = suspendTran();
        try {
            return jobOperator.getParameters(executionId);
        } finally {
            resumeTran(tran);
        }
    }

    @Override
    public long start(String jobXMLName, Properties jobParameters) throws JobStartException, JobSecurityException {
        Transaction tran = suspendTran();
        try {
            return jobOperator.start(jobXMLName, jobParameters);
        } finally {
            resumeTran(tran);
        }
    }

    @Override
    public long restart(long executionId, Properties restartParameters) throws JobExecutionAlreadyCompleteException,
                                                                               NoSuchJobExecutionException, 
                                                                               JobExecutionNotMostRecentException,
                                                                               JobRestartException, 
                                                                               JobSecurityException {
        Transaction tran = suspendTran();
        try {
            return jobOperator.restart(executionId, restartParameters);
        } finally {
            resumeTran(tran);
        }
    }

    @Override
    public void stop(long executionId) throws NoSuchJobExecutionException, JobExecutionNotRunningException, JobSecurityException {
        Transaction tran = suspendTran();
        try {
            jobOperator.stop(executionId);
        } finally {
            resumeTran(tran);
        }
    }

    @Override
    public void abandon(long executionId) throws NoSuchJobExecutionException, JobExecutionIsRunningException, JobSecurityException {
        Transaction tran = suspendTran();
        try {
            jobOperator.abandon(executionId);
        } finally {
            resumeTran(tran);
        }
    }

    @Override
    public JobInstance getJobInstance(long executionId) throws NoSuchJobExecutionException, JobSecurityException {
        Transaction tran = suspendTran();
        try {
            return jobOperator.getJobInstance(executionId);
        } finally {
            resumeTran(tran);
        }
    }

    @Override
    public List<JobExecution> getJobExecutions(JobInstance instance) throws NoSuchJobInstanceException, JobSecurityException {
        Transaction tran = suspendTran();
        try {
            return jobOperator.getJobExecutions(instance);
        } finally {
            resumeTran(tran);
        }
    }

    @Override
    public JobExecution getJobExecution(long executionId) throws NoSuchJobExecutionException, JobSecurityException {
        Transaction tran = suspendTran();
        try {
            return jobOperator.getJobExecution(executionId);
        } finally {
            resumeTran(tran);
        }
    }

    @Override
    public List<StepExecution> getStepExecutions(long jobExecutionId) throws NoSuchJobExecutionException, JobSecurityException {
        Transaction tran = suspendTran();
        try {
            return jobOperator.getStepExecutions(jobExecutionId);
        } finally {
            resumeTran(tran);
        }
    }

}