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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator;
import javax.batch.operations.exception.JobExecutionAlreadyCompleteException;
import javax.batch.operations.exception.JobExecutionIsRunningException;
import javax.batch.operations.exception.JobExecutionNotMostRecentException;
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

import com.ibm.jbatch.container.config.ServicesManager;
import com.ibm.jbatch.container.config.impl.ServicesManagerImpl;
import com.ibm.jbatch.container.services.IBatchKernelService;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.tck.bridge.IJobEndCallbackService;
import com.ibm.jbatch.spi.services.IJobXMLLoaderService;
import com.ibm.jbatch.spi.services.ServiceType;


public class JobOperatorImpl implements JobOperator {

    private final static String sourceClass = JobOperatorImpl.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);
    
    private ServicesManager servicesManager = null; 
    private IBatchKernelService batchKernel = null;
    private IJobEndCallbackService callbackService = null;
    private IPersistenceManagerService persistenceService = null;
    private IJobXMLLoaderService jobXMLLoaderService = null;
	
    public JobOperatorImpl() {
        servicesManager = ServicesManagerImpl.getInstance();
        batchKernel = (IBatchKernelService) servicesManager.getService(ServiceType.BATCH_KERNEL_SERVICE);
        callbackService = (IJobEndCallbackService) servicesManager.getService(ServiceType.CALLBACK_SERVICE);
        persistenceService = (IPersistenceManagerService) servicesManager.getService(ServiceType.PERSISTENCE_MANAGEMENT_SERVICE);
        jobXMLLoaderService = (IJobXMLLoaderService) servicesManager.getService(ServiceType.JOBXML_LOADER_SERVICE);
    }
    
	@Override
	public long start(String jobXMLName, Properties submittedProps)	throws JobStartException {
	    
	    String jobXML = jobXMLLoaderService.loadJob(jobXMLName);
	    
		long executionId = 0;
        
        if (logger.isLoggable(Level.FINE)) {            
            int concatLen = jobXML.length() > 200 ? 200 : jobXML.length();
            logger.fine("Starting job: " + jobXML.substring(0, concatLen) + "... truncated ...");
        }
        
        JobExecution execution = batchKernel.startJob(jobXML, submittedProps);
        executionId = execution.getExecutionId();
        
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("JobOperator start received executionId: " + executionId);
        }

        return executionId;
	}


	@Override
	public void abandon(JobExecution jobExecution)
			throws NoSuchJobExecutionException, JobExecutionIsRunningException {
		// TODO Auto-generated method stub
		
		boolean abandoned = false;
		long executionId = jobExecution.getExecutionId();
		
		// get the job executions associated with the job instance
		//List<JobExecution> jobExecutions = persistenceService.jobOperatorGetJobExecutionsByJobInstanceID(instanceId);
		
		JobExecution jobEx = persistenceService.jobOperatorGetJobExecution(executionId);
		
		// if there are none found, throw exception saying so
		if (jobEx == null){
			throw new NoSuchJobInstanceException(null, "Job Execution: " + executionId + " not found");
		}
		
		// for every job execution associated with the job
		// if it is not in STARTED state, mark it as ABANDONED
		//for (JobExecution jobEx : jobExecutions){
			if (!jobEx.getBatchStatus().equals(BatchStatus.STARTED) || !jobEx.getBatchStatus().equals(BatchStatus.STARTING)){
				// update table to reflect ABANDONED state
		        long time = System.currentTimeMillis();
		    	Timestamp timestamp = new Timestamp(time);
				persistenceService.jobOperatorUpdateBatchStatusWithSTATUSandUPDATETSonly(jobEx.getExecutionId(), "batchstatus", BatchStatus.ABANDONED.name(), timestamp);
			}
			else {
				// If one of the JobExecutions is still running, throw an exception
				throw new JobExecutionIsRunningException(null, "Job Execution: " + executionId + " is still running");
			}
		//}
		
	}

	@Override
	public List<JobExecution> getExecutions(JobInstance instance)
			throws NoSuchJobInstanceException {
		
		List<JobExecution> executions = persistenceService.jobOperatorGetJobExecutionsByJobInstanceID(instance.getInstanceId());
		if (executions.size() == 0){
			throw new NoSuchJobInstanceException(null, "Job: " + instance.getJobName() + " has no executions");
		}
		else{
			return persistenceService.jobOperatorGetJobExecutionsByJobInstanceID(instance.getInstanceId());
		}
	}

	@Override
	public JobExecution getJobExecution(long executionId)
			throws NoSuchJobExecutionException {
		
		JobExecution execution = persistenceService.jobOperatorGetJobExecution(executionId);
		
		if (execution == null){
			throw new NoSuchJobExecutionException(null, "No job execution exists for job execution id: " + executionId);
		}
		else {
			return batchKernel.getJobExecution(executionId);
		}
	}

	@Override
	public List<JobExecution> getJobExecutions(JobInstance instance)
			throws NoSuchJobInstanceException {
		
		List<JobExecution> executions = persistenceService.jobOperatorGetJobExecutions(instance.getInstanceId());
		
		if (executions.size() == 0 ){
			throw new NoSuchJobInstanceException(null, "Job: " + instance.getJobName() + " does not exist");
		}
		else {
			return persistenceService.jobOperatorGetJobExecutions(instance.getInstanceId());
		}
	}

	@Override
	public JobInstance getJobInstance(long executionId)
			throws NoSuchJobExecutionException {
		// will have to look at t he persistence layer - 
		// this used to take in an instanceid, now takes 
		// in an executionId. Will have to adapt to that fact
		return this.batchKernel.getJobInstance(executionId);
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
	public List<JobInstance> getJobInstances(String jobName, int start,
			int count) throws NoSuchJobException {
		
		List<JobInstance> jobInstances = new ArrayList<JobInstance>();
		
		// get the jobinstance ids associated with this job name
		List<Long> instanceIds = persistenceService.jobOperatorgetJobInstanceIds(jobName, start, count);
		
		if (instanceIds.size() > 0){
			// for every job instance id
			for (long id : instanceIds){
				// get the job instance obj, add it to the list
				jobInstances.add(batchKernel.getJobInstance(id));
			}
			// send the list of objs back to caller
			return jobInstances;
		}
		else throw new NoSuchJobException(null, "Job Name " + jobName + " not found");
	}

	@Override
	public Set<String> getJobNames() {
		return persistenceService.jobOperatorgetJobNames();
	}

	@Override
	public Properties getParameters(JobInstance instance)
			throws NoSuchJobExecutionException {
		
		Properties props = persistenceService.getParameters(instance.getInstanceId());
		
		if (props == null){
			throw new NoSuchJobExecutionException(null, "");
		}
		else {
			return persistenceService.getParameters(instance.getInstanceId());
		}
	}

	@Override
	public List<JobExecution> getRunningExecutions(String jobName)
			throws NoSuchJobException {
		
		List<JobExecution> jobExecutions = new ArrayList<JobExecution>();
		
		// get the jobexecution ids associated with this job name
		Set<Long> executionIds = persistenceService.jobOperatorGetRunningExecutions(jobName);
		
		if (executionIds.size() > 0){
			// for every job instance id
			for (long id : executionIds){
				// get the job instance obj, add it to the list
				jobExecutions.add(batchKernel.getJobExecution(id));
			}
			// send the list of objs back to caller
			return jobExecutions;
		}
		else throw new NoSuchJobException(null, "Job Name " + jobName + " not found");
	}

	@Override
	public List<StepExecution> getStepExecutions(long executionId)
			throws NoSuchJobExecutionException {
		
		// now need to return a set of StepExecution Objs - 
		return persistenceService.getStepExecutionIDListQueryByJobID(executionId);
	}

	@Override
	public long restart(long executionId)
			throws JobExecutionAlreadyCompleteException,
			NoSuchJobExecutionException, JobExecutionNotMostRecentException,
			JobRestartException {
		
        if (logger.isLoggable(Level.FINE)) {            
            logger.fine("Restarting job with instanceID: " + executionId);
        }
        
        JobExecution execution = batchKernel.restartJob(executionId);
        
        long newExecutionId = execution.getExecutionId();
        
        if (logger.isLoggable(Level.FINE)) {            
            logger.fine("Restarted job with instanceID: " + executionId + ", and new executionID: " + newExecutionId);
        }
        
        return newExecutionId;
	}
	
    @Override
    public long restart(long instanceId, Properties jobParameters) throws JobInstanceAlreadyCompleteException,
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
	
	@Override
	public void stop(long executionId) throws NoSuchJobExecutionException,
			JobExecutionNotRunningException {
		
		batchKernel.stopJob(executionId);
	}

}