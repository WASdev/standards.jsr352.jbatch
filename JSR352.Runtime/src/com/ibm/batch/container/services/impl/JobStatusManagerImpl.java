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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.runtime.JobInstance;

import com.ibm.batch.container.config.IBatchConfig;
import com.ibm.batch.container.exception.BatchContainerServiceException;
import com.ibm.batch.container.services.IJobStatusManagerService;
import com.ibm.batch.container.services.IPersistenceManagerService;
import com.ibm.batch.container.services.ServicesManager;
import com.ibm.batch.container.status.JobStatus;
import com.ibm.batch.container.status.JobStatusKey;
import com.ibm.batch.container.status.StepStatus;
import com.ibm.batch.container.status.StepStatusKey;
import com.ibm.batch.container.util.ExecutionStatus.BatchStatus;

public class JobStatusManagerImpl implements IJobStatusManagerService {

    private static final String CLASSNAME = JobStatusManagerImpl.class.getName();
    private static Logger logger = Logger.getLogger(JobStatusManagerImpl.class.getPackage().getName());
    private IPersistenceManagerService _persistenceManager;    

    private String _jobId;


    @Override
    public void shutdown() throws BatchContainerServiceException {
        // TODO Auto-generated method stub

    }

    @Override
    public JobStatus createJobStatus(JobInstance jobInstance, long firstJobExecutionId) throws BatchContainerServiceException {
        String method = "createJobStatus";
        if(logger.isLoggable(Level.FINER)) { logger.entering(CLASSNAME, method, " jobid: " + jobInstance.getInstanceId());}

        JobStatus jobStatus = new JobStatus(jobInstance);
        jobStatus.setLatestExecutionId(firstJobExecutionId);        

        _persistenceManager.createData(IPersistenceManagerService.JOB_STATUS_STORE_ID,
                new JobStatusKey(jobInstance.getInstanceId()), jobStatus);           

        if(logger.isLoggable(Level.FINER)) { logger.exiting(CLASSNAME, method);}

        return jobStatus;
    }

    @Override
    public JobStatus getJobStatus(long jobInstanceId) throws BatchContainerServiceException {
        JobStatus retVal = null;
        List<?> data = _persistenceManager.getData(IPersistenceManagerService.JOB_STATUS_STORE_ID,
                new JobStatusKey(jobInstanceId));

        if (data == null) { 
            throw new IllegalStateException("Null entry for JobInstance: " + jobInstanceId);
        } else if (data.size()==0) {
            throw new IllegalStateException("Empty entry for JobInstance: " + jobInstanceId);
        } else if (data.size()!=1) {
            throw new IllegalStateException("Should only be one entry for JobInstance: " + jobInstanceId);
        } else {
            try {
                retVal = (JobStatus)data.get(0);
            } catch (ClassCastException e) {
                throw new IllegalStateException("Expected JobStatus but found" + data.get(0)); 
            }
        }
        return retVal;
    }

    @Override
    public void updateJobBatchStatus(long jobInstanceId, BatchStatus batchStatus) throws BatchContainerServiceException {
        JobStatus js = getJobStatus(jobInstanceId);
        if (js == null) {
            throw new IllegalStateException("Couldn't find entry to update for id = " + jobInstanceId);
        }
        js.setBatchStatus(batchStatus);
        persistJobStatus(jobInstanceId, js);
    }

    @Override
    public void updateJobExecutionStatus(long jobInstanceId, BatchStatus batchStatus, String exitStatus) throws BatchContainerServiceException {
        JobStatus js = getJobStatus(jobInstanceId);
        if (js == null) {
            throw new IllegalStateException("Couldn't find entry to update for id = " + jobInstanceId);
        }
        js.setBatchStatus(batchStatus);
        js.setExitStatus(exitStatus);
        persistJobStatus(jobInstanceId, js);
    }

    @Override
    public void updateJobCurrentStep(long jobInstanceId, String currentStepName) throws BatchContainerServiceException {
        JobStatus js = getJobStatus(jobInstanceId);
        if (js == null) {
            throw new IllegalStateException("Couldn't find entry to update for id = " + jobInstanceId);
        }
        js.setCurrentStepId(currentStepName);
        persistJobStatus(jobInstanceId, js);        
    }


    @Override
    public void updateJobStatusWithNewExecution(long jobInstanceId, long newExecutionId) throws BatchContainerServiceException {
        JobStatus js = getJobStatus(jobInstanceId);
        if (js == null) {
            throw new IllegalStateException("Couldn't find entry to update for id = " + jobInstanceId);
        }
        js.setLatestExecutionId(newExecutionId);
        persistJobStatus(jobInstanceId, js);                
    }

    private void persistJobStatus(long jobInstanceId, JobStatus newJobStatus) throws BatchContainerServiceException {       
        _persistenceManager.updateData(IPersistenceManagerService.JOB_STATUS_STORE_ID,
                new JobStatusKey(jobInstanceId), newJobStatus);

    }

    @Override
    public void createStepStatus(long jobInstanceId, String stepId, StepStatus newStepStatus) throws BatchContainerServiceException {        
        _persistenceManager.createData(IPersistenceManagerService.STEP_STATUS_STORE_ID,
                new StepStatusKey(jobInstanceId, stepId), newStepStatus);            
    }

    @Override
    /*
     * @return - StepStatus or null if one is unknown
     */
    public StepStatus getStepStatus(long jobInstanceId, String stepId) throws BatchContainerServiceException {
        String method = "getStepStatus";
        if(logger.isLoggable(Level.FINER)) { logger.entering(CLASSNAME, method, " jobid: " + jobInstanceId + ", stepId: " + stepId);}

        StepStatus retVal = null;
        List<?> data = _persistenceManager.getData(IPersistenceManagerService.STEP_STATUS_STORE_ID,
                new StepStatusKey(jobInstanceId, stepId));
        if (data.size() == 0) {
            if(logger.isLoggable(Level.FINER)) { logger.exiting(CLASSNAME, method, "No step status found in store."); }
            return null;
        }
        if (data.size() > 1) {
            if(logger.isLoggable(Level.FINER)) { logger.exiting(CLASSNAME, method, "Found more than 1 (number=" + data.size() + ") StepStatus in store.");}
            throw new IllegalStateException("Should only be one entry for job/step with JobInstance: " + jobInstanceId + 
                    ", and step id = " + stepId);
        } else {
            try {
                retVal = (StepStatus)data.get(0);                            
            } catch (ClassCastException e) {
                throw new IllegalStateException("Expected JobStatus but found" + data.get(0)); 
            }
        }
        if(logger.isLoggable(Level.FINER)) { logger.exiting(CLASSNAME, method, "Found step status in store: " + retVal);}
        return retVal;
    }

    @Override 
    public void updateEntireStepStatus(long jobInstanceId, String stepId, StepStatus newStepStatus) {
        _persistenceManager.updateData(IPersistenceManagerService.STEP_STATUS_STORE_ID,
                new StepStatusKey(jobInstanceId, stepId), newStepStatus);
    }


    /*
    @Override
    public void updateStepStatus(long jobInstanceId, String stepId, BatchStatus stepBatchStatus) throws BatchContainerServiceException {

        StepStatus s = getStepStatus(jobInstanceId, stepId);
        if (s == null) {
            throw new IllegalStateException("Couldn't find entry to update for key with jobInstance id = " + 
                    jobInstanceId + ", and stepId = " + stepId);
        }
        s.setBatchStatus(stepBatchStatus);
        updateStepStatus(jobInstanceId, stepId, s);
    }

    @Override
    public void updateStepStatusWithStarting(long jobInstanceId, String stepId) throws BatchContainerServiceException {

        StepStatus s = getStepStatus(jobInstanceId, stepId);        
        if (s == null) {
            throw new IllegalStateException("Couldn't find entry to update for key with jobInstance id = " + 
                    jobInstanceId + ", and stepId = " + stepId);
        }
        s.incrementStartCount();
        s.setBatchStatus(BatchStatus.STARTING);
        updateStepStatus(jobInstanceId, stepId, s);
    }
     */



    @Override
    public void init(IBatchConfig batchConfig)
    throws BatchContainerServiceException {
        String method = "init";
        if(logger.isLoggable(Level.FINER)) { logger.entering(CLASSNAME, method);}

        ServicesManager sm = ServicesManager.getInstance();

        _persistenceManager = (IPersistenceManagerService)sm.getService(ServicesManager.ServiceType.PERSISTENCE_MANAGEMENT_SERVICE);

        /*
        if(!_isInited ) {

            ServicesManager sm = ServicesManager.getInstance();

            _persistenceManager = (IPersistenceManagerService)sm.getService(ServicesManager.ServiceType.PERSISTENCE_MANAGEMENT_SERVICE);

            List jobStatusList = _persistenceManager.getData(
                    IPersistenceManagerService.JOB_STATUS_STORE_ID,
                    new JobStatusKey(_jobId));

            if(jobStatusList != null && jobStatusList.size() == 1) {
                // if the job is in restartable state it will already have
                // a status so load it into the cache
                _cachedJobStatus = (JobStatus)jobStatusList.get(0);
                int upCnt = _cachedJobStatus.getUpdateCount();
                // increment the count because in case of z the cr may have sent a job status update with 100 as the update count
                // because it can't obtain the actual no from DB, the scheduler would then realizing the cnt is 100 set the cnt on
                // jsdo as <last known cnt? + 1
                _cachedJobStatus.setUpdateCount(++upCnt);
            }

            _isInited = true;
        }
         */
        if(logger.isLoggable(Level.FINER)) { logger.exiting(CLASSNAME, method);}
    }


    @Override
    public List<JobStatus> getAllJobStatus() throws BatchContainerServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    /*
     * Inefficient, since we've already updated the status to stopped.. would be better to have a single update.
     */
    public void updateJobStatusFromJSLStop(long jobInstanceId, String restartOn) throws BatchContainerServiceException {       
        JobStatus js = getJobStatus(jobInstanceId);        
        if (js == null) {
            throw new IllegalStateException("Couldn't find entry to update for id = " + jobInstanceId);
        }
        js.setRestartOn(restartOn);
        persistJobStatus(jobInstanceId, js);   
    }
}
