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
package com.ibm.jbatch.container.status;
import java.io.Serializable;

import javax.batch.operations.JobOperator.BatchStatus;
import javax.batch.runtime.JobInstance;

import com.ibm.jbatch.container.jobinstance.JobInstanceImpl;

public class JobStatus implements Serializable, Cloneable{

    private static final long serialVersionUID = 1L;

    private JobInstance jobInstance;

    private long jobInstanceId;

    private String currentStepId;

    private BatchStatus batchStatus;  // Might be nice to know.

    private String exitStatus;

    // Assume this will be needed.
    private long latestExecutionId;

    // How many times the status has been updated.

    //TODO - reset to 0?
    //private int updateCount;

    // TODO - Maybe a job operator would use this?
    //private int restartCount;

    private String restartOn;

    public JobStatus(long jobInstanceId) {
        this.jobInstanceId = jobInstanceId;
    }
    
    public JobStatus(JobInstance jobInstance) {
        this.batchStatus = BatchStatus.STARTING;
        //this.restartCount = 0;
       // this.updateCount = 0;  
        this.jobInstance = jobInstance;
        this.jobInstanceId = jobInstance.getInstanceId();
    }

    public long getJobInstanceId() {
        return this.jobInstanceId;
    }

    public void setJobInstance(JobInstance jobInstance) {
        this.jobInstance = jobInstance;
    }
    
    public JobInstanceImpl getJobInstance() {
        return (JobInstanceImpl)jobInstance;
    }

    public String getCurrentStepId() {
        return currentStepId;
    }

    public void setCurrentStepId(String currentStepId) {
        this.currentStepId = currentStepId;
    }

    public BatchStatus getBatchStatus() {
        return batchStatus;
    }

    public void setBatchStatus(BatchStatus batchStatus) {
        this.batchStatus = batchStatus;
    }

    public long getLatestExecutionId() {
        return latestExecutionId;
    }

    public void setLatestExecutionId(long latestExecutionId) {
        this.latestExecutionId = latestExecutionId;
    }

    /*
    public int getUpdateCount() {
        return updateCount;
    }

    public void setUpdateCount(int updateCount) {
        this.updateCount = updateCount;
    }

    public int getRestartCount() {
        return restartCount;
    }

    public void setRestartCount(int restartCount) {
        this.restartCount = restartCount;
    }
    */
    @Override
    public String toString() {        
        
        StringBuffer buf = new StringBuffer();
        buf.append(",currentStepId: " + currentStepId);
        buf.append(",batchStatus: " + batchStatus);
        buf.append(",latestExecutionId: " + latestExecutionId);
        //buf.append(",updateCount: " + updateCount);
        //buf.append(",restartCount: " + restartCount);
        buf.append(",restartOn: " + restartOn);
        buf.append("\n-----------------------\n");
       // buf.append("jobInstance: " + jobInstance.toString());
        return buf.toString();
    }

    public void setExitStatus(String exitStatus) {
        this.exitStatus = exitStatus;
    }

    public String getExitStatus() {
        return exitStatus;
    }

    public String getRestartOn() {
        return restartOn;
    }

    public void setRestartOn(String restartOn) {
        this.restartOn = restartOn;
    }
}
