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
package com.ibm.batch.container.jobinstance;

import java.io.Externalizable;
import java.io.Serializable;
import java.sql.Timestamp;

import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;

import com.ibm.batch.container.context.impl.MetricImpl;
import com.ibm.batch.container.context.impl.StepContextImpl;

public class StepExecutionImpl implements StepExecution, Serializable {

    
    private long commitCount = 0;
    private Timestamp endTime = null;
    private String exitStatus = null;
    
    private long filterCount = 0;
    private long jobExecutionId = 0;
    private Timestamp lastUpdateTime = null;
    private long processSkipCount = 0;
    private long readCount = 0;
    private long readSkipCount = 0;
    private long rollbackCount = 0;
    private Timestamp startTime = null;
    private long stepExecutionId = 0;
    private String stepName = null;
    long ExecutionId = 0;

    private long writeCount = 0;
    private long writeSkipCount = 0;
    
    private StepContextImpl<?, ? extends Externalizable> stepContext = null;
    
    public StepExecutionImpl(long jobExecutionId, long stepExecutionId) {
    	this.jobExecutionId = jobExecutionId;
    	this.stepExecutionId = stepExecutionId;
    }
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public long getCommitCount() {
        return commitCount;
    }

    @Override
    public Timestamp getEndTime() {
        return endTime;
    }

    @Override
    public String getExitStatus() {
    	return this.stepContext.getExitStatus();
    }

    public long getFilterCount() {
        return filterCount;
    }

    public long getJobExecutionId() {
        return jobExecutionId;
    }

    public Timestamp getLastUpdateTime() {
        return lastUpdateTime;
    }

    public long getProcessSkipCount() {
        return processSkipCount;
    }

    public long getReadCount() {
        return readCount = stepContext.getMetric(MetricImpl.Counter.valueOf("READ_COUNT")).getValue();
    }

    public long getReadSkipCount() {
        return readSkipCount;
    }

    public long getRollbackCount() {
        return rollbackCount;
    }

    @Override
    public Timestamp getStartTime() {
        return startTime;
    }

    public long getStepExecutionId() {
        return stepExecutionId;
    }

    public String getStepName() {     
        return stepName;
    }
        
    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public long getWriteCount() {     
        return writeCount = stepContext.getMetric(MetricImpl.Counter.valueOf("WRITE_COUNT")).getValue();
    }

    public long getWriteSkipCount() {        
        return writeSkipCount;
    }
    
    public StepContextImpl<?, ? extends Externalizable> getJobContext() {
        return stepContext;
    }    
    
    public <T> void setStepContext(StepContextImpl<?, ? extends Externalizable> stepContext) {
        this.stepContext = stepContext;
    }

	public String getBatchStatus() {
	    return this.stepContext.getBatchStatus();
	}
	
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
		buf.append("---------------------------------------------------------------------------------");
		buf.append("getStepName(): " + this.getStepName() + "\n");
		buf.append("getJobExecutionId(): " + this.getJobExecutionId() + "\n");
		buf.append("getStepExecutionId(): " + this.getStepExecutionId() + "\n");			
		buf.append("getCommitCount(): " + this.getCommitCount() + "\n");
		buf.append("getFilterCount(): " + this.getFilterCount() + "\n");
		buf.append("getProcessSkipCount(): " + this.getProcessSkipCount() + "\n");
		buf.append("getReadCount(): " + this.getReadCount() + "\n");
		buf.append("getReadSkipCount(): " + this.getReadSkipCount() + "\n");
		buf.append("getRollbackCount(): " + this.getRollbackCount() + "\n");
		buf.append("getWriteCount(): " + this.getWriteCount() + "\n");
		buf.append("getWriteSkipCount(): " + this.getWriteSkipCount() + "\n");
		buf.append("getStartTime(): " + this.getStartTime() + "\n");
		buf.append("getEndTime(): " + this.getEndTime() + "\n");
		buf.append("getLastUpdateTime(): " + this.getLastUpdateTime() + "\n");
		buf.append("getBatchStatus(): " + this.getBatchStatus() + "\n");
		buf.append("getExitStatus(): " + this.getExitStatus());
		buf.append("---------------------------------------------------------------------------------");
        return buf.toString();
    }
    
    public void setJobExecutionId(long jobexecID){
    	this.jobExecutionId = jobexecID;
    }
    
    public void setStepExecutionId(long stepexecID){
    	this.ExecutionId = stepexecID;
    }

	@Override
	public Metric[] getMetrics() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getStatus() {
		// TODO Auto-generated method stub
		return this.stepContext.getBatchStatus();
	}

	@Override
	public Object getUserPersistentData() {
		// TODO Auto-generated method stub
		return null;
	}
    

}
