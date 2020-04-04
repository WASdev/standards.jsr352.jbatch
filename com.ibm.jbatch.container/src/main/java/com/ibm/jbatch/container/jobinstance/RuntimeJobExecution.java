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

import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobInstance;

import com.ibm.jbatch.container.artifact.proxy.ListenerFactory;
import com.ibm.jbatch.container.context.impl.JobContextImpl;
import com.ibm.jbatch.container.navigator.ModelNavigator;
import com.ibm.jbatch.container.services.IJobExecution;
import com.ibm.jbatch.jsl.model.JSLJob;

public class RuntimeJobExecution {
	
	private ModelNavigator<JSLJob> jobNavigator = null;
	private JobInstance jobInstance;
	private long executionId;
	private String restartOn; 
	private JobContextImpl jobContext = null;
	private ListenerFactory listenerFactory;
	private IJobExecution operatorJobExecution = null;
	private Integer partitionInstance = null;

	public RuntimeJobExecution(JobInstance jobInstance, long executionId) {
		this.jobInstance = jobInstance;
		this.executionId = executionId;
        this.operatorJobExecution = new JobOperatorJobExecution(executionId, jobInstance.getInstanceId());
    }


	/*
	 * Non-spec'd methods (not on the interface, but maybe we should
	 * put on a second interface).
	 */
	
	public void prepareForExecution(JobContextImpl jobContext, String restartOn) {
		this.jobContext = jobContext;
		this.jobNavigator = jobContext.getNavigator();
		jobContext.setExecutionId(executionId);
		jobContext.setInstanceId(jobInstance.getInstanceId());
		this.restartOn = restartOn;
		operatorJobExecution.setJobContext(jobContext);
	}
	
	public void prepareForExecution(JobContextImpl jobContext) {
		prepareForExecution(jobContext, null);
	}
	
	public void setRestartOn(String restartOn) {
		this.restartOn = restartOn;
	}
	public long getExecutionId() {
		return executionId;
	}

	public long getInstanceId() {
		return jobInstance.getInstanceId();
	}

	 public JobInstance getJobInstance() {
		return jobInstance;
	}

	public ModelNavigator<JSLJob> getJobNavigator() {
		return jobNavigator;
	}

	public JobContextImpl getJobContext() {
		return jobContext;
	}    

	public String getRestartOn() {
		return restartOn;
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

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(" executionId: " + executionId);
		buf.append(" restartOn: " + restartOn);        
		buf.append("\n-----------------------\n");
		buf.append("jobInstance: \n   " + jobInstance);
		return buf.toString();
	}

    public Integer getPartitionInstance() {
        return partitionInstance;
    }

    public void setPartitionInstance(Integer partitionInstance) {
        this.partitionInstance = partitionInstance;
    }
}
