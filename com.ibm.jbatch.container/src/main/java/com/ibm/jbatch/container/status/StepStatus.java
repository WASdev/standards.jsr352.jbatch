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

import java.io.ByteArrayInputStream;
import java.io.Serializable;

import jakarta.batch.runtime.BatchStatus;

import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.persistence.PersistentDataWrapper;
import com.ibm.jbatch.container.util.TCCLObjectInputStream;

public class StepStatus implements Serializable {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private long stepExecutionId;
    private BatchStatus batchStatus;
    private String exitStatus;
    private int startCount;
    private PersistentDataWrapper persistentUserData;
    private Integer numPartitions;
    
    private long lastRunStepExecutionId;

    public StepStatus(long stepExecutionId) {
        this.startCount = 1;
        this.stepExecutionId = stepExecutionId;
        this.lastRunStepExecutionId = stepExecutionId;
        this.batchStatus = BatchStatus.STARTING;
    }

    public void setBatchStatus(BatchStatus batchStatus) {
        this.batchStatus = batchStatus;
    }

    public BatchStatus getBatchStatus() {
        return batchStatus;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("stepExecutionId: " + stepExecutionId);
        buf.append(",batchStatus: " + batchStatus);
        buf.append(",exitStatus: " + exitStatus);
        buf.append(",startCount: " + startCount);
        buf.append(",persistentUserData: " + persistentUserData);
        buf.append(",numPartitions: " + numPartitions);
        return buf.toString();
    }

    public long getStepExecutionId() {
        return stepExecutionId;
    }

    public int getStartCount() {
        return startCount;
    }

    public void incrementStartCount() {
        startCount++;
    }

    public void setExitStatus(String exitStatus) {
        this.exitStatus = exitStatus;
    }

    public String getExitStatus() {
        return exitStatus;
    }

    public void setPersistentUserData(PersistentDataWrapper persistentUserData) {
        this.persistentUserData = persistentUserData;
    }

    public Serializable getPersistentUserData() {
        if (this.persistentUserData != null) {
            byte[] persistentToken = this.persistentUserData.getPersistentDataBytes();
            ByteArrayInputStream persistentByteArrayInputStream = new ByteArrayInputStream(persistentToken);
            TCCLObjectInputStream persistentOIS = null;

            Serializable persistentObject = null;

            try {
                persistentOIS = new TCCLObjectInputStream(persistentByteArrayInputStream);
                persistentObject = (Serializable) persistentOIS.readObject();
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }

            return persistentObject;
        } else {
            return null;
        }
    }

    public Integer getNumPartitions() {
        return numPartitions;
    }

    public void setNumPartitions(Integer numPartitions) {
        this.numPartitions = numPartitions;
    }

    public void setStepExecutionId(long stepExecutionId) {
        this.stepExecutionId = stepExecutionId;
        this.lastRunStepExecutionId = this.stepExecutionId;
    }

    public long getLastRunStepExecutionId() {
        return lastRunStepExecutionId;
    }

    public void setLastRunStepExecutionId(long lastRunStepExecutionId) {
        this.lastRunStepExecutionId = lastRunStepExecutionId;
    }

}
