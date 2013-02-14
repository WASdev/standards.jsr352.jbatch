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
package com.ibm.batch.container.status;

import java.io.Externalizable;
import java.io.Serializable;

import javax.batch.api.parameters.PartitionPlan;
import javax.batch.operations.JobOperator.BatchStatus;

public class StepStatus implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private String stepId;
    private BatchStatus batchStatus;
    private String exitStatus;
    private int startCount;    
    private Externalizable persistentUserData;
    private PartitionPlan plan;
    
    public StepStatus(String stepId) {
        this.startCount = 1;
        this.stepId = stepId;
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
        buf.append("stepId: " + stepId);        
        buf.append(",batchStatus: " + batchStatus);
        buf.append(",startCount: " + startCount);
        return buf.toString();
    }

    public String getStepId() {
        return stepId;
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

    public void setPersistentUserData(Externalizable persistentUserData) {
        this.persistentUserData = persistentUserData;
    }

    public Externalizable getPersistentUserData() {
        return persistentUserData;
    }

	public void setPlan(PartitionPlan plan) {
		this.plan = plan;
	}

	public PartitionPlan getPlan() {
		return plan;
	}

}
