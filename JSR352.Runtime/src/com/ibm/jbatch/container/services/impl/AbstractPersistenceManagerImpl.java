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

import java.io.Serializable;
import java.util.List;

import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.exception.PersistenceException;
import com.ibm.jbatch.container.persistence.CheckpointData;
import com.ibm.jbatch.container.persistence.CheckpointDataKey;
import com.ibm.jbatch.container.services.IPersistenceDataKey;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.status.JobStatus;
import com.ibm.jbatch.container.status.JobStatusKey;
import com.ibm.jbatch.container.status.StepStatus;
import com.ibm.jbatch.container.status.StepStatusKey;
import com.ibm.jbatch.spi.IBatchConfig;

public abstract class AbstractPersistenceManagerImpl  implements IPersistenceManagerService {

    IBatchConfig batchConfig = null;

    @Override
    public void createData(int storeDestination, IPersistenceDataKey key,
            Serializable value) throws PersistenceException {
        if (storeDestination == IPersistenceManagerService.JOB_STATUS_STORE_ID) {
            _createJobStatus((JobStatusKey)key, (JobStatus)value);
        } else if (storeDestination == IPersistenceManagerService.STEP_STATUS_STORE_ID) {
            _createStepStatus((StepStatusKey)key, (StepStatus)value);
        } else if (storeDestination == IPersistenceManagerService.CHECKPOINT_STORE_ID) {
            _createCheckpointData((CheckpointDataKey)key, (CheckpointData)value);
        } else {
            throw new IllegalArgumentException("Only support job status & step status & checkpoint persistence through this service.");
        }
    }

    @Override
    public void deleteData(int storeDestination, IPersistenceDataKey key)
    throws PersistenceException {
        if (storeDestination == IPersistenceManagerService.JOB_STATUS_STORE_ID) {
            _deleteJobStatus((JobStatusKey)key);
        } else if (storeDestination == IPersistenceManagerService.STEP_STATUS_STORE_ID) {
            _deleteStepStatus((StepStatusKey)key);
        } else if (storeDestination == IPersistenceManagerService.CHECKPOINT_STORE_ID) {
            _deleteCheckpointData((CheckpointDataKey)key);
        } else {
            throw new IllegalArgumentException("Only support job status & step status & checkpoint persistence through this service.");
        }
    }

    @Override
    public List getData(int storeDestination, IPersistenceDataKey key)
    throws PersistenceException {
        if (storeDestination == IPersistenceManagerService.JOB_STATUS_STORE_ID) {
            return _getJobStatus((JobStatusKey)key);
        } else if (storeDestination == IPersistenceManagerService.STEP_STATUS_STORE_ID) {
            return _getStepStatus((StepStatusKey)key);
        } else if (storeDestination == IPersistenceManagerService.CHECKPOINT_STORE_ID) {
            return _getCheckpointData((CheckpointDataKey)key);
        } else {
            throw new IllegalArgumentException("Only support job status & step status & checkpoint persistence through this service.");
        }
    }

    @Override
    public void updateData(int storeDestination, IPersistenceDataKey key,
            Serializable value) throws PersistenceException {
        if (storeDestination == IPersistenceManagerService.JOB_STATUS_STORE_ID) {
            _updateJobStatus((JobStatusKey)key, (JobStatus)value);
        } else if (storeDestination == IPersistenceManagerService.STEP_STATUS_STORE_ID) {
            _updateStepStatus((StepStatusKey)key, (StepStatus)value);
        } else if (storeDestination == IPersistenceManagerService.CHECKPOINT_STORE_ID) {
            _updateCheckpointData((CheckpointDataKey)key, (CheckpointData)value);
        } else {
            throw new IllegalArgumentException("Only support job status & step status & checkpoint persistence through this service.");
        }
    }

    @Override
    public void init(IBatchConfig batchConfig)
    throws BatchContainerServiceException {

        this.batchConfig = batchConfig;

    }

    @Override
    public void shutdown() throws BatchContainerServiceException {
        // TODO Auto-generated method stub

    }

    protected abstract void _createStepStatus(StepStatusKey key, StepStatus value) ;

    protected abstract void _createJobStatus(JobStatusKey key, JobStatus value) ;
    
    protected abstract void _createCheckpointData(CheckpointDataKey key, CheckpointData value) ;

    protected abstract List<StepStatus> _getStepStatus(StepStatusKey key);

    protected abstract List<JobStatus> _getJobStatus(JobStatusKey key) ;
    
    protected abstract List<CheckpointData> _getCheckpointData(CheckpointDataKey key) ;

    protected abstract  void _updateStepStatus(StepStatusKey key, StepStatus value) ;

    protected abstract  void _updateJobStatus(JobStatusKey key, JobStatus value) ;
    
    protected abstract  void _updateCheckpointData(CheckpointDataKey key, CheckpointData value) ;

    protected abstract void _deleteStepStatus(StepStatusKey key) ;

    protected abstract void _deleteJobStatus(JobStatusKey key) ;
    
    protected abstract void _deleteCheckpointData(CheckpointDataKey key) ;
    



}
