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
package com.ibm.jbatch.container.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.operations.exception.JobExecutionNotRunningException;
import javax.batch.operations.exception.JobRestartException;
import javax.batch.operations.exception.JobStartException;
import javax.batch.operations.exception.NoSuchJobExecutionException;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;


import com.ibm.jbatch.container.config.ServicesManager;
import com.ibm.jbatch.spi.IBatchConfig;
import com.ibm.jbatch.spi.services.ServiceType;
import com.ibm.jbatch.container.config.impl.ServicesManagerImpl;
import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.jobinstance.JobExecutionHelper;
import com.ibm.jbatch.container.jobinstance.ParallelJobExecution;
import com.ibm.jbatch.container.jobinstance.RuntimeJobExecutionImpl;
import com.ibm.jbatch.container.services.IBatchKernelService;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.tck.bridge.IJobEndCallbackService;
import com.ibm.jbatch.container.util.BatchWorkUnit;
import com.ibm.jbatch.container.util.PartitionDataWrapper;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.jbatch.spi.services.IBatchThreadPoolService;
import com.ibm.jbatch.spi.services.IJobIdManagementService;
import com.ibm.jbatch.spi.services.ParallelTaskResult;

public class BatchKernelImpl implements IBatchKernelService {

    private final static String sourceClass = BatchKernelImpl.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);
    private static IJobIdManagementService _jobIdManagementService = null;

    private Map<Long, JobControllerImpl> instanceId2jobControllerMap = new ConcurrentHashMap<Long, JobControllerImpl>();
    private Map<Long, RuntimeJobExecutionImpl> jobExecutionInstancesMap = new ConcurrentHashMap<Long, RuntimeJobExecutionImpl>();

    ServicesManager servicesManager = ServicesManagerImpl.getInstance();

    private IBatchThreadPoolService executorService = null;
    
    private IJobEndCallbackService callbackService = null;
    
    private IPersistenceManagerService persistenceService = null;

    // TODO - assuming we have a IBatchConfig, maybe we should get size from
    // there.
    public final static int THREAD_POOL_SIZE = 5;

    public BatchKernelImpl() {
        // get the JobId service
        _jobIdManagementService = (IJobIdManagementService) servicesManager.getService(ServiceType.JOB_ID_MANAGEMENT_SERVICE);
        executorService = (IBatchThreadPoolService)servicesManager.getService(ServiceType.BATCH_THREADPOOL_SERVICE);
        callbackService = (IJobEndCallbackService) servicesManager.getService(ServiceType.CALLBACK_SERVICE);
        persistenceService = (IPersistenceManagerService) servicesManager.getService(ServiceType.PERSISTENCE_MANAGEMENT_SERVICE);
    }

    public void init(IBatchConfig pgcConfig) throws BatchContainerServiceException {

    }

    @Override
    public void shutdown() throws BatchContainerServiceException {
        // TODO Auto-generated method stub

    }

    @Override
    public JobExecution startJob(String jobXML) throws JobStartException {
        return startJob(jobXML, null);
    }

    @Override
    public JobExecution startJob(String jobXML, Properties jobParameters) throws JobStartException {
        String method = "startJob";

        if (logger.isLoggable(Level.FINER)) {
            logger.entering(sourceClass, method, new Object[] { jobXML, jobParameters });
        }

        RuntimeJobExecutionImpl jobExecution = JobExecutionHelper.startJob(jobXML, jobParameters);

        // TODO - register with status manager
        
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("JobExecution constructed: " + jobExecution);
        }

        BatchWorkUnit batchWork = new BatchWorkUnit(this, jobExecution);
        this.instanceId2jobControllerMap.put(jobExecution.getInstanceId(), batchWork.getController());
        //AJM: not needed anymore : jobExecutionInstancesMap.put(jobExecution.getExecutionId(), jobExecution);

        executorService.executeTask(batchWork, null);

        if (logger.isLoggable(Level.FINER)) {
            logger.exiting(sourceClass, method, jobExecution);
        }

        return jobExecution.getJobOperatorJobExecution();
    }

    @Override
    public void stopJob(long executionId) throws NoSuchJobExecutionException, JobExecutionNotRunningException {

    	long jobInstanceId = persistenceService.jobOperatorQueryJobExecutionJobInstanceId(executionId);
    	
        JobControllerImpl controller = this.instanceId2jobControllerMap.get(jobInstanceId);
        if (controller == null) {
    	//TODO - should  be more discriminating w/ exception  to see if it's just not running or if it doesn't exist at all in the DB !
        	String msg = "JobExecution with execution id of " + jobInstanceId + "is not running.";
            throw new JobExecutionNotRunningException(null, msg);
        }

        controller.stop();
    }

    @Override
    public JobExecution restartJob(long executionId) throws JobRestartException {
        String method = "restartJob";

        if (logger.isLoggable(Level.FINER)) {
            logger.entering(sourceClass, method);
        }

        Properties dummyPropObj = new Properties();
        return this.restartJob(executionId, dummyPropObj);
    }
    
    @Override
    public JobExecution restartJob(long executionId, Properties jobOverrideProps) throws JobRestartException {
        String method = "restartJob";

        if (logger.isLoggable(Level.FINER)) {
            logger.entering(sourceClass, method);
        }

        RuntimeJobExecutionImpl jobExecution = JobExecutionHelper.restartJob(executionId, jobOverrideProps, false);

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("JobExecution constructed: " + jobExecution);
        }

        BatchWorkUnit batchWork = new BatchWorkUnit(this, jobExecution);
        this.instanceId2jobControllerMap.put(jobExecution.getInstanceId(), batchWork.getController());
        //AJM not needed any longer jobExecutionInstancesMap.put(jobExecution.getExecutionId(), jobExecution);

        executorService.executeTask(batchWork, null);

        if (logger.isLoggable(Level.FINER)) {
            logger.exiting(sourceClass, method, jobExecution);
        }

        return jobExecution.getJobOperatorJobExecution();
    }

    @Override
    public void jobExecutionDone(RuntimeJobExecutionImpl jobExecution) {

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("JobExecution done, getting ready to invoke callbacks for JobExecution: " + jobExecution.getExecutionId());
        }

        callbackService.done(jobExecution.getExecutionId());

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Done invoking callbacks for JobExecution: " + jobExecution.getExecutionId());
        }

        // Remove from map after job is done        
        this.instanceId2jobControllerMap.remove(jobExecution.getInstanceId());
        
        // AJM: ah - purge jobExecution from map here and flush to DB?
        // edit: no long want a 2 tier for the jobexecution...do want it for step execution
        // renamed method to flushAndRemoveStepExecution

    }

    public JobExecution getJobExecution(long executionId) {
    	// this method will retrieve a 'live' JobOperatorJobExecutionImpl from the in memory map OR
    	// hydrate a previously run job's JobOperatorJobExecutionImpl from the backing store ...
    	
        String method = "getJobExecution";
        if (logger.isLoggable(Level.FINER)) {
            logger.entering(sourceClass, method, executionId);
        }

        JobExecution retVal = null;
        
        /*
        RuntimeJobExecutionImpl rtJobExecution = jobExecutionInstancesMap.get(executionId);
        
        if (rtJobExecution != null){       	
        // get JobExecution out of map, send back a JOJobExecution
        	retVal = rtJobExecution.getJobOperatorJobExecution();
        }
        else { 
        	// need to look in the backing store
        	retVal = JobExecutionHelper.getPersistedJobOperatorJobExecution(executionId);
        }
        */
        
        retVal = JobExecutionHelper.getPersistedJobOperatorJobExecution(executionId);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.exiting(sourceClass, method, retVal);
        }

        return retVal;
        
    }

    
    @Override
    public List<StepExecution> getStepExecutions(long executionId) {
    	return JobExecutionHelper.getstepExecutionIDInfoList(executionId);
    }
    
    @Override    
    public StepExecution getStepExecution(long stepExecutionId) {
		return JobExecutionHelper.getStepExecutionIDInfo(stepExecutionId);
    	
    }
       
    
    
    
    /*
     * creates unique key to get StepExecution
     */
    private String getJobStepExecId(long jobExecutionId, long stepExecutionId) {
    	return String.valueOf(jobExecutionId) + ':' + String.valueOf(stepExecutionId);
    }


    private ParallelJobExecution startGeneratedJob(JSLJob jobModel, Properties partitionProps, LinkedBlockingQueue<PartitionDataWrapper> analyzerQueue, Stack<String> subJobExitStatusQueue) {
        String method = "startGeneratedJob";

        if (logger.isLoggable(Level.FINER)) {
            logger.entering(sourceClass, method, new Object[] { jobModel, partitionProps });
        }

        RuntimeJobExecutionImpl jobExecution = JobExecutionHelper.startJob(jobModel, partitionProps, true);

        // TODO - register with status manager

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("JobExecution constructed: " + jobExecution);
        }

        BatchWorkUnit batchWork = new BatchWorkUnit(this, jobExecution, analyzerQueue, subJobExitStatusQueue, false);
        this.instanceId2jobControllerMap.put(jobExecution.getInstanceId(), batchWork.getController());
        // re-enabling while i work out the persistence
        jobExecutionInstancesMap.put(jobExecution.getExecutionId(), jobExecution);

        //This call is non-blocking
        ParallelTaskResult result = executorService.executeParallelTask(batchWork, null);
        
        ParallelJobExecution parallelJobExec = new ParallelJobExecution(jobExecution, result);

        if (logger.isLoggable(Level.FINER)) {
            logger.exiting(sourceClass, method, jobExecution);
        }

        return parallelJobExec;
    }
    
    public List<ParallelJobExecution> startParallelJobs(List<JSLJob> jobModels, Properties[] partitionProperties, LinkedBlockingQueue<PartitionDataWrapper> analyzerQueue, Stack<String> subJobExitStatusQueue) {
        
        List<ParallelJobExecution> parallelJobExecs = new ArrayList<ParallelJobExecution>(jobModels.size());
        
        //for now let always use a Properties array. We can add some more convenience methods later for null properties and what not
        
        
        
        
        int instance = 0;
        for (JSLJob parallelJob  : jobModels){
            
            
            Properties partitionProps = (partitionProperties == null) ? null : partitionProperties[instance];    
            
            ParallelJobExecution parallelJobExec = this.startGeneratedJob(parallelJob, partitionProps, analyzerQueue, subJobExitStatusQueue);
            parallelJobExecs.add(parallelJobExec);

            instance++;
        }
        
        
        return parallelJobExecs;
        
    }

	@Override
	public List<Long> getExecutionIds(long jobInstance) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getJobInstanceCount(String jobName) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public JobInstance getJobInstance(long instanceId){
		return JobExecutionHelper.getJobInstance(instanceId);
	}

	public List<ParallelJobExecution> restartParallelJobs(List<JSLJob> jobModels,
			Properties[] partitionProperties,
			LinkedBlockingQueue<PartitionDataWrapper> analyzerQueue, Stack<String> subJobExitStatusQueue) throws JobRestartException {
        List<ParallelJobExecution> parallelJobExecs = new ArrayList<ParallelJobExecution>(jobModels.size());
        
        //for now let always use a Properties array. We can add some more convenience methods later for null properties and what not
        
        int instance = 0;
        for (JSLJob parallelJob  : jobModels){
            
            
            Properties partitionProps = (partitionProperties == null) ? null : partitionProperties[instance];    
            
            ParallelJobExecution parallelJobExec = this.restartGeneratedJob(parallelJob, partitionProps, analyzerQueue, subJobExitStatusQueue);
            parallelJobExecs.add(parallelJobExec);

            instance++;
        }
        
        
        return parallelJobExecs;
	}

	private ParallelJobExecution restartGeneratedJob(JSLJob jobModel,
			Properties partitionProps, LinkedBlockingQueue<PartitionDataWrapper> analyzerQueue, Stack<String> subJobExitStatusQueue) throws JobRestartException {
        String method = "restartGeneratedJob";

        if (logger.isLoggable(Level.FINER)) {
            logger.entering(sourceClass, method, new Object[] { jobModel, partitionProps });
        }

        //There can only be one instance associated with a subjob's id since it is generated from an unique
        //job instance id. So there should be no way to directly start a subjob with particular
        List<Long> instanceIds = persistenceService.jobOperatorgetJobInstanceIds(jobModel.getId(), 0, 2);
        
        assert(instanceIds.size() == 1);
        
        
        List<JobExecution> partitionExecs = persistenceService.jobOperatorGetJobExecutions(instanceIds.get(0));
        
        Long execId = Long.MIN_VALUE;
        
        for (JobExecution partitionExec : partitionExecs ) {
            if (partitionExec.getExecutionId() > execId ) {
                execId = partitionExec.getExecutionId();
            }
        }
        
        RuntimeJobExecutionImpl jobExecution = JobExecutionHelper.restartJob(execId, partitionProps, true);

        // TODO - register with status manager

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("JobExecution constructed: " + jobExecution);
        }

        BatchWorkUnit batchWork = new BatchWorkUnit(this, jobExecution, analyzerQueue, subJobExitStatusQueue, false);
        this.instanceId2jobControllerMap.put(jobExecution.getInstanceId(), batchWork.getController());
        // re-enabling while i work out the persistence
        jobExecutionInstancesMap.put(jobExecution.getExecutionId(), jobExecution);

        //This call is non-blocking
        ParallelTaskResult result = executorService.executeParallelTask(batchWork, null);
        
        ParallelJobExecution parallelJobExec = new ParallelJobExecution(jobExecution, result);

        if (logger.isLoggable(Level.FINER)) {
            logger.exiting(sourceClass, method, jobExecution);
        }

        return parallelJobExec;
	}

}
