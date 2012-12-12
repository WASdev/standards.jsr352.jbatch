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

import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;

import jsr352.batch.jsl.JSLJob;

import com.ibm.batch.container.artifact.proxy.ListenerFactory;
import com.ibm.batch.container.context.impl.JobContextImpl;
import com.ibm.batch.container.xjcl.Navigator;

public class RuntimeJobExecutionImpl {
    
    private Navigator jobNavigator = null;
    private JobInstance jobInstance;
    private long executionId;
    private String restartOn; 
    private JobContextImpl<?> jobContext = null;
    private ListenerFactory listenerFactory;
    
    private JobExecution operatorJobExecution = null;
    
    
    RuntimeJobExecutionImpl(Navigator jobNavigator, JobInstance jobInstance, long executionId) {
        this.jobNavigator = jobNavigator;
        this.jobInstance = jobInstance;
        this.executionId = executionId;
        jobContext = new JobContextImpl(jobNavigator.getId());
        this.operatorJobExecution = 
        		new JobOperatorJobExecutionImpl(jobInstance.getInstanceId(), executionId, jobContext);
    }
    
    RuntimeJobExecutionImpl(Navigator jobNavigator, JobInstance jobInstance, long executionId, String restartOn) {
        this(jobNavigator, jobInstance, executionId);
        this.restartOn = restartOn;
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
    
    public JobExecution getJobOperatorJobExecution() {
    	return operatorJobExecution;
    }
    
    public String getStatus() {
    	return this.jobContext.getBatchStatus();
    }
}
