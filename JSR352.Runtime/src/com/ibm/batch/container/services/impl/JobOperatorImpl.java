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
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator;
import javax.batch.operations.exception.JobExecutionIsRunningException;
import javax.batch.operations.exception.JobExecutionNotRunningException;
import javax.batch.operations.exception.JobInstanceAlreadyCompleteException;
import javax.batch.operations.exception.JobRestartException;
import javax.batch.operations.exception.JobStartException;
import javax.batch.operations.exception.NoSuchJobException;
import javax.batch.operations.exception.NoSuchJobExecutionException;
import javax.batch.operations.exception.NoSuchJobInstanceException;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;

import com.ibm.batch.container.services.IBatchKernelService;
import com.ibm.batch.container.services.IPersistenceManagerService;
import com.ibm.batch.container.services.ServicesManager;
import com.ibm.batch.container.services.ServicesManager.ServiceType;
import com.ibm.batch.container.tck.bridge.IJobEndCallbackService;

public class JobOperatorImpl implements JobOperator {

    private final static String sourceClass = JobOperatorImpl.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);
    
    private ServicesManager servicesManager = null; 
    private IBatchKernelService batchKernel = null;
    
    private IJobEndCallbackService callbackService = null;
    private IPersistenceManagerService persistenceService = null;

    public JobOperatorImpl() {
        servicesManager = ServicesManager.getInstance();
        batchKernel = (IBatchKernelService) servicesManager.getService(ServiceType.BATCH_KERNEL_SERVICE);
        callbackService = (IJobEndCallbackService) servicesManager.getService(ServiceType.CALLBACK_SERVICE);
        persistenceService = (IPersistenceManagerService) servicesManager.getService(ServiceType.PERSISTENCE_MANAGEMENT_SERVICE);
    }

    @Override
    public List<Long> getExecutions(long instanceId) throws NoSuchJobInstanceException {
        
    	return batchKernel.getExecutionIds(instanceId);
    }

    @Override
    public int getJobInstanceCount(String jobName) throws NoSuchJobException {
        
    	int jobInstanceCount = 0;
    	
    	jobInstanceCount = persistenceService.jobOperatorGetJobInstanceCount(jobName);
    	
    	if (jobInstanceCount > 0) {
    		return jobInstanceCount;
    	}
    	else throw new NoSuchJobException(null, "Job " + jobName + " not found");
    }

    @Override
    public Set<String> getJobNames() {
    	return persistenceService.jobOperatorgetJobNames();
    }

    @Override
    public Properties getParameters(long executionId) throws NoSuchJobExecutionException {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public Long restart(long instanceId, Properties jobParameters) throws JobInstanceAlreadyCompleteException,
            NoSuchJobExecutionException, NoSuchJobException, JobRestartException {

        if (logger.isLoggable(Level.FINE)) {            
            logger.fine("Restarting job with instanceID: " + instanceId);
        }
        
        JobExecution execution = batchKernel.restartJob(instanceId, jobParameters);
        
        long newExecutionId = execution.getExecutionId();
        
        if (logger.isLoggable(Level.FINE)) {            
            logger.fine("Restarted job with instanceID: " + instanceId + ", and new executionID: " + newExecutionId);
        }
        
        return newExecutionId;
    }

    /*
     * Call into JobInstanceController to start job This call will return
     * immediate and the execution will on on java executor thread.
     * 
     * return executionId
     */
    public Long start(String jobXML, Properties jobParameters) throws JobStartException {

        Long retExecID = null;
        
        if (logger.isLoggable(Level.FINE)) {            
            int concatLen = jobXML.length() > 200 ? 200 : jobXML.length();
            logger.fine("Starting job: " + jobXML.substring(0, concatLen) + "... truncated ...");
        }
        
        JobExecution execution = batchKernel.startJob(jobXML, jobParameters);
        retExecID = execution.getExecutionId();
        
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("JobOperator start received executionId: " + retExecID);
        }

        return retExecID;
    }

    @Override
    public void stop(long instanceId) throws NoSuchJobExecutionException, JobExecutionNotRunningException {
        batchKernel.stopJob(instanceId);
    }

    @Override
    public JobExecution getJobExecution(long executionId) {
        // TODO Auto-generated method stub
        return batchKernel.getJobExecution(executionId);
    	// go to persistence, get the stuff for a new BatchJobExecutionImpl
    	// add setters to BatchJobExecutionImpl, set all the fields and return
    	
    	/*
    	BatchJobExecutionImpl jobExImpl = new BatchJobExecutionImpl();
    	
    	// get from db all components of a JobExecutionImpl instance based on executionID
    	// set into new BatchJobExecutionImpl obj:
    	if (persistenceService instanceof JDBCPersistenceManagerImpl){
    		JobInformationKey jobinfoKey = new JobInformationKey(executionId);
			jobExImpl.setBatchStatus(((JDBCPersistenceManagerImpl)persistenceService).jobOperatorQueryJobExecutionStatus(executionId, JDBCPersistenceManagerImpl.BATCH_STATUS));
			jobExImpl.setExitStatus(((JDBCPersistenceManagerImpl)persistenceService).jobOperatorQueryJobExecutionStatus(executionId, JDBCPersistenceManagerImpl.EXIT_STATUS));
			jobExImpl.setCreateTime(((JDBCPersistenceManagerImpl)persistenceService).jobOperatorQueryJobExecutionTimestamp(executionId, JDBCPersistenceManagerImpl.CREATE_TIME));
			jobExImpl.setStartTime(((JDBCPersistenceManagerImpl)persistenceService).jobOperatorQueryJobExecutionTimestamp(executionId, JDBCPersistenceManagerImpl.START_TIME));
			jobExImpl.setEndTime(((JDBCPersistenceManagerImpl)persistenceService).jobOperatorQueryJobExecutionTimestamp(executionId, JDBCPersistenceManagerImpl.END_TIME));
			jobExImpl.setLastUpdateTime(((JDBCPersistenceManagerImpl)persistenceService).jobOperatorQueryJobExecutionTimestamp(executionId, JDBCPersistenceManagerImpl.UPDATE_TIME));
			jobExImpl.setJobInstanceId(((JDBCPersistenceManagerImpl)persistenceService).jobOperatorQueryJobExecutionJobInstanceId(executionId));
		}
    	jobExImpl.setExecutionId(executionId);
    	
    	return jobExImpl;
    	*/
    }
    
    @Override
    public StepExecution getStepExecution(long jobExecutionId, long stepExecutionId) {
    	return batchKernel.getStepExecution(jobExecutionId, stepExecutionId);
    }
    
    @Override
    public List<StepExecution> getJobSteps(long jobExecutionId) {
    	return batchKernel.getJobSteps(jobExecutionId);
    }

    @Override
    public List<JobExecution> getJobExecutions(long instanceId) {
        // TODO Auto-generated method stub
        return null;
    }

	/* (non-Javadoc)
	 * @see javax.batch.operations.JobOperator#getJobInstanceIds(java.lang.String, int, int)
	 */
	@Override
	public List<Long> getJobInstanceIds(String jobName, int start, int count)
			throws NoSuchJobException {
		return persistenceService.jobOperatorgetJobInstanceIds(jobName, start, count);
	}

	/* (non-Javadoc)
	 * @see javax.batch.operations.JobOperator#getRunningInstanceIds(java.lang.String)
	 */
	@Override
	public Set<Long> getRunningInstanceIds(String jobName)
			throws NoSuchJobException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.batch.operations.JobOperator#abandon(long)
	 */
	@Override
	public void abandon(long instanceId) throws NoSuchJobExecutionException,
			JobExecutionIsRunningException {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see javax.batch.operations.JobOperator#getJobInstance(long)
	 */
	@Override
	public JobInstance getJobInstance(long instanceId) {
		// TODO Auto-generated method stub
		return null;
	}



}