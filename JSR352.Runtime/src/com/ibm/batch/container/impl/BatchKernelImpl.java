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
package com.ibm.batch.container.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.operations.exception.JobExecutionNotRunningException;
import javax.batch.operations.exception.NoSuchJobExecutionException;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.StepExecution;

import jsr352.batch.jsl.JSLJob;

import com.ibm.batch.container.IBatchConfig;
import com.ibm.batch.container.artifact.proxy.PartitionAnalyzerProxy;
import com.ibm.batch.container.exception.BatchContainerServiceException;
import com.ibm.batch.container.jobinstance.JobExecutionHelper;
import com.ibm.batch.container.jobinstance.ParallelJobExecution;
import com.ibm.batch.container.jobinstance.RuntimeJobExecutionImpl;
import com.ibm.batch.container.services.IBatchKernelService;
import com.ibm.batch.container.services.IBatchThreadPoolService;
import com.ibm.batch.container.services.IJobIdManagementService;
import com.ibm.batch.container.services.ParallelTaskResult;
import com.ibm.batch.container.services.ServicesManager;
import com.ibm.batch.container.services.ServicesManager.ServiceType;
import com.ibm.batch.container.services.impl.JDBCPersistenceManagerImpl;
import com.ibm.batch.container.tck.bridge.IJobEndCallbackService;
import com.ibm.batch.container.util.BatchWorkUnit;

public class BatchKernelImpl implements IBatchKernelService {

    private final static String sourceClass = BatchKernelImpl.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);
    private static IJobIdManagementService _jobIdManagementService = null;

    private Map<Long, JobControllerImpl> instanceId2jobControllerMap = new ConcurrentHashMap<Long, JobControllerImpl>();
    private Map<Long, RuntimeJobExecutionImpl> jobExecutionInstancesMap = new ConcurrentHashMap<Long, RuntimeJobExecutionImpl>();
    private Map<String, StepExecution> stepExecutionInstancesMap = new ConcurrentHashMap<String, StepExecution>();

    ServicesManager servicesManager = ServicesManager.getInstance();

    private IBatchThreadPoolService executorService = null;
    
    private IJobEndCallbackService callbackService = null;

    // TODO - assuming we have a IBatchConfig, maybe we should get size from
    // there.
    public final static int THREAD_POOL_SIZE = 5;

    public BatchKernelImpl() {
        // get the JobId service
        _jobIdManagementService = (IJobIdManagementService) servicesManager.getService(ServiceType.JOB_ID_MANAGEMENT_SERVICE);
        executorService = ServicesManager.getInstance().getThreadpoolService(null, THREAD_POOL_SIZE);
        callbackService = (IJobEndCallbackService) servicesManager.getService(ServiceType.CALLBACK_SERVICE);
    }

    public void init(IBatchConfig pgcConfig) throws BatchContainerServiceException {

    }

    @Override
    public void shutdown() throws BatchContainerServiceException {
        // TODO Auto-generated method stub

    }

    @Override
    public JobExecution restartJob(long jobInstanceId) {
        return restartJob(jobInstanceId, null);
    }

    @Override
    public JobExecution startJob(String jobXML) {
        return startJob(jobXML, null);
    }

    @Override
    public JobExecution startJob(String jobXML, Properties jobParameters) {
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
        jobExecutionInstancesMap.put(jobExecution.getExecutionId(), jobExecution);

        executorService.executeTask(batchWork, null);

        if (logger.isLoggable(Level.FINER)) {
            logger.exiting(sourceClass, method, jobExecution);
        }

        return jobExecution.getJobOperatorJobExecution();
    }

    @Override
    public void stopJob(long jobInstanceId) throws NoSuchJobExecutionException, JobExecutionNotRunningException {

        JobControllerImpl controller = this.instanceId2jobControllerMap.get(jobInstanceId);
        if (controller == null) {
    	//TODO - should  be more discriminating w/ exception  to see if it's just not running or if it doesn't exist at all in the DB !
        	String msg = "JobExecution with execution id of " + jobInstanceId + "is not running.";
            throw new JobExecutionNotRunningException(null, msg);
        }

        controller.stop();
    }

    @Override
    public JobExecution restartJob(long jobInstanceId, Properties overrideJobParameters) {
        String method = "restartJob";

        if (logger.isLoggable(Level.FINER)) {
            logger.entering(sourceClass, method, new Object[] { jobInstanceId, overrideJobParameters });
        }

        RuntimeJobExecutionImpl jobExecution = JobExecutionHelper.restartJob(jobInstanceId, overrideJobParameters);

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("JobExecution constructed: " + jobExecution);
        }

        BatchWorkUnit batchWork = new BatchWorkUnit(this, jobExecution);
        this.instanceId2jobControllerMap.put(jobInstanceId, batchWork.getController());
        jobExecutionInstancesMap.put(jobExecution.getExecutionId(), jobExecution);

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

    }

    public JobExecution getJobExecution(long executionId) {
        String method = "getJobExecution";
        if (logger.isLoggable(Level.FINER)) {
            logger.entering(sourceClass, method, executionId);
        }

        RuntimeJobExecutionImpl rtJobExecution = jobExecutionInstancesMap.get(executionId);
        
        // get JobExecution out of map, send back a JOJobExecution
        JobExecution retVal = rtJobExecution.getJobOperatorJobExecution();
        //((JobOperatorJobExecutionImpl)retVal).setBatchStatus(rtJobExecution.getStatus());
        
        if (logger.isLoggable(Level.FINER)) {
            logger.exiting(sourceClass, method, retVal);
        }

        return retVal;
        
        //return null;
    }
    
    @Override
    public List<StepExecution> getJobSteps(long jobExecutionId) { // NEXT: Get the step executions out of the db, based on the jobexecution id and put them in this list!!!
    
    	ArrayList<StepExecution> steps = new ArrayList<StepExecution>();
    	
        for (StepExecution stepExecution: stepExecutionInstancesMap.values()) {
        	if (stepExecution.getJobExecutionId() == jobExecutionId) {
        		steps.add(stepExecution);
        	}
        }
    	
    	return steps;
    
    	//return JobExecutionHelper.getstepExecutionIDInfoList(jobExecutionId, JDBCPersistenceManagerImpl.JOBEXEC_ID);
    }
    
    @Override    
    public StepExecution getStepExecution(long jobExecutionId, long stepExecutionId) {
    	String method = "getStepExecution";
        if (logger.isLoggable(Level.FINER)) {
            logger.entering(sourceClass, method, "jobExecutionId " + String.valueOf(jobExecutionId) + " : stepExecutionId " + stepExecutionId);
        }
        
        String key = getJobStepExecId(jobExecutionId, stepExecutionId);  
        StepExecution retVal = stepExecutionInstancesMap.get(key);
        
        long jobExecID = JobExecutionHelper.getstepExecutionIDInfo(key, JDBCPersistenceManagerImpl.JOBEXEC_ID);
        long stepExecID = JobExecutionHelper.getstepExecutionIDInfo(key, JDBCPersistenceManagerImpl.STEPEXEC_ID);
        
        //StepExecution retVal = new StepExecutionImpl(jobExecID, stepExecID);
        
        if (logger.isLoggable(Level.FINER)) {
            logger.exiting(sourceClass, method, retVal);
        }
        return retVal;
       
    }
    
    
    
    public void registerStepExecution(long jobExecutionId, long stepExecutionId, StepExecution stepExecution) {
    	String method = "registerStepExecution";
        if (logger.isLoggable(Level.FINER)) {
            logger.entering(sourceClass, method, "jobExecutionId " + String.valueOf(jobExecutionId) + " : stepExecutionId " + stepExecutionId);
        }
        
        String key = getJobStepExecId(jobExecutionId, stepExecutionId);
        
        // put it into the map
        stepExecutionInstancesMap.put(key, stepExecution);
        //JobExecutionHelper.persistStepExecution(jobExecutionId, stepExecution.
        if (logger.isLoggable(Level.FINER)) {
            logger.exiting(sourceClass, method);
        }
    }
    
    
    /*
     * creates unique key to get StepExecution
     */
    private String getJobStepExecId(long jobExecutionId, long stepExecutionId) {
    	return String.valueOf(jobExecutionId) + ':' + String.valueOf(stepExecutionId);
    }



    
    /*@Override
    public void setJobExecution(Long execID, JobExecution execution) {
        jobExecutionInstancesMap.put(execID, execution);

    }
    */

    private ParallelJobExecution startGeneratedJob(JSLJob jobModel, Properties jobParameters, PartitionAnalyzerProxy analyzerProxy) {
        String method = "startGeneratedJob";

        if (logger.isLoggable(Level.FINER)) {
            logger.entering(sourceClass, method, new Object[] { jobModel, jobParameters });
        }

        RuntimeJobExecutionImpl jobExecution = JobExecutionHelper.startJob(jobModel, jobParameters);

        // TODO - register with status manager

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("JobExecution constructed: " + jobExecution);
        }

        BatchWorkUnit batchWork = new BatchWorkUnit(this, jobExecution, analyzerProxy, false);
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
    
    public List<ParallelJobExecution> startParallelJobs(List<JSLJob> jobModels, Properties[] jobParameters, PartitionAnalyzerProxy analyzerProxy) {
        
        List<ParallelJobExecution> parallelJobExecs = new ArrayList<ParallelJobExecution>(jobModels.size());
        
        //for now let always use a Properties array. We can add some more convenience methods later for null properties and what not
        
        
        
        
        int instance = 0;
        for (JSLJob parallelJob  : jobModels){
            
            Properties jobProperties = (jobParameters == null) ? null : jobParameters[instance];    
            
            ParallelJobExecution parallelJobExec = this.startGeneratedJob(parallelJob, jobProperties, analyzerProxy);
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

	/*
	@Override
	public void setJobExecution(Long executionID, JobExecution execution) {
		// TODO Auto-generated method stub
		
	}
	*/

}
