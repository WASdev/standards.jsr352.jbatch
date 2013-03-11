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
package com.ibm.jbatch.container.jobinstance;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Properties;

import javax.batch.operations.JobOperator.BatchStatus;
import javax.batch.runtime.JobInstance;

import com.ibm.jbatch.container.artifact.proxy.ListenerFactory;
import com.ibm.jbatch.container.context.impl.JobContextImpl;
import com.ibm.jbatch.container.jsl.Navigator;
import com.ibm.jbatch.container.services.IJobExecution;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.jbatch.jsl.model.JSLProperties;

public class RuntimeJobExecutionHelper {
    
    private Navigator jobNavigator = null;
    private JobInstance jobInstance;
    private long executionId;
    private String restartOn; 
    private JobContextImpl<?> jobContext = null;
    private ListenerFactory listenerFactory;
    
    private IJobExecution operatorJobExecution = null;
    
    
    RuntimeJobExecutionHelper(Navigator jobNavigator, JobInstance jobInstance, long executionId) {
        this.jobNavigator = jobNavigator;
        this.jobInstance = jobInstance;
        this.executionId = executionId;
        
        JSLProperties jslProperties = new JSLProperties();
        if(jobNavigator.getJSL() != null && jobNavigator.getJSL() instanceof JSLJob) {
        	jslProperties = ((JSLJob)jobNavigator.getJSL()).getProperties();
        }
        jobContext = new JobContextImpl(jobNavigator.getId(), jslProperties);
        
        this.operatorJobExecution = 
        		new JobOperatorJobExecutionImpl(executionId, jobInstance.getInstanceId(), jobContext);
    }
    
    RuntimeJobExecutionHelper(Navigator jobNavigator, JobInstance jobInstance, long executionId, String restartOn) {
        this(jobNavigator, jobInstance, executionId);
        this.restartOn = restartOn;
    }

    public RuntimeJobExecutionHelper(Navigator jobNavigator, JobInstance jobInstance, long executionId, JobContextImpl jobContext) {
        this.jobNavigator = jobNavigator;
        this.jobInstance = jobInstance;
        this.executionId = executionId;
        this.jobContext = jobContext;
        
        this.operatorJobExecution = new JobOperatorJobExecutionImpl(executionId, jobInstance.getInstanceId(), jobContext);
    }
    

    public long getExecutionId() {
        return executionId;
    }


    public long getInstanceId() {
        return jobInstance.getInstanceId();
    }
    
    /*
     * Non-spec'd methods (not on the interface, but maybe we should
     * put on a second interface).
     */
    public JobInstance getJobInstance() {
        return jobInstance;
    }
    
    public Navigator<JSLJob> getJobNavigator() {
        return jobNavigator;
    }

    public JobContextImpl<?> getJobContext() {
        return jobContext;
    }    
    
    //public <T> void setJobContext(JobContextImpl<T> jobContext) {
    //     this.jobContext = jobContext;
    //}
    
    public String getRestartOn() {
        return restartOn;
    }
    
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(" executionId: " + executionId);
        buf.append(" restartOn: " + restartOn);        
        buf.append("\n-----------------------\n");
        buf.append("jobInstance: \n   " + jobInstance);
        return buf.toString();
    }

    public ListenerFactory getListenerFactory() {
        return listenerFactory;
    }

    public void setListenerFactory(ListenerFactory listenerFactory) {
        this.listenerFactory = listenerFactory;
    }
    
    public IJobExecution getJobOperatorJobExecution() {
    	return operatorJobExecution;
    }
    
    public BatchStatus getBatchStatus() {
    	return this.jobContext.getBatchStatus();
    }
    
    public String getExitStatus() {
    	return this.jobContext.getExitStatus();
    }
    
	public void setBatchStatus(String status) {
		operatorJobExecution.setBatchStatus(status);
	}

	public void setCreateTime(Timestamp ts) {
		operatorJobExecution.setCreateTime(ts);
	}

	public void setEndTime(Timestamp ts) {
		operatorJobExecution.setEndTime(ts);
	}

	public void setExitStatus(String status) {
		//exitStatus = status;
		operatorJobExecution.setExitStatus(status);
	
	}

	// do we need this?
	//public void setInstanceId(long id) {
	//	instanceID = id;
	//}

	public void setLastUpdateTime(Timestamp ts) {
		operatorJobExecution.setLastUpdateTime(ts);
	}

	public void setStartTime(Timestamp ts) {
		operatorJobExecution.setStartTime(ts);
	}
	
	public void setJobParameters(Properties jProps){
		operatorJobExecution.setJobParameters(jProps);
	}
	
	public Properties getJobParameters(){
		return operatorJobExecution.getJobParameters();
	}
	
	public Date getStartTime(){
		return operatorJobExecution.getStartTime();
	}
	
	public Date getEndTime(){
		return operatorJobExecution.getEndTime();
	}
	
	public Date getLastUpdatedTime(){
		return operatorJobExecution.getLastUpdatedTime();
	}
	
	public Date getCreateTime(){
		return operatorJobExecution.getCreateTime();
	}

}
