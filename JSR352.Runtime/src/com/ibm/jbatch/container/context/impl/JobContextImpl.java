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
package com.ibm.jbatch.container.context.impl;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator.BatchStatus;
import javax.batch.runtime.Metric;
import javax.batch.runtime.context.JobContext;

import com.ibm.jbatch.jsl.model.JSLProperties;
import com.ibm.jbatch.jsl.model.Property;


public class JobContextImpl<T> implements JobContext<T> {

    private final static String sourceClass = JobContextImpl.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);

    private BatchStatus batchStatus = null;
    private String exitStatus = null;
    
    private T transientUserData = null;
    private String id; //will this change to long?
    private Properties properties = new Properties();
    

    private ConcurrentHashMap<String, Metric> metrics = new ConcurrentHashMap<String, Metric>();

//    public JobContextImpl(String id) {
//        this.id = id;
//    }
    
    public JobContextImpl(String id, JSLProperties jslProperties) {
    	this.id = id;
    	this.batchStatus = BatchStatus.STARTING;
    	this.properties = convertJSProperties(jslProperties);
    }
    
    private Properties convertJSProperties(JSLProperties jslProperties) {
    	
        Properties jobProperties = new Properties();
        if(jslProperties != null) { // null if not job properties defined.
        	for (Property property : jslProperties.getPropertyList()) {
        		jobProperties.setProperty(property.getName(), property.getValue());
        	}
        }
        return jobProperties;
    }
    
    /*
     * Copy Constructor returns a new JobContextImpl with the same properties as the original context
     */
//    public JobContextImpl(JobContextImpl<T> jobContext) {
//    	//jobContext.getProperties().
//    	
//    }
    
    public String getExitStatus() {
        return exitStatus;
    }


    public void setExitStatus(String exitStatus) {
        this.exitStatus = exitStatus;
    }


    public String getJobName() {
        return id;
    }


    public BatchStatus getBatchStatus() {
        return batchStatus;
    }


    public void setBatchStatus(BatchStatus batchStatus) {
        this.batchStatus = batchStatus;
    }


    public T getTransientUserData() {
        return transientUserData;
    }


    public Properties getProperties() {
        return properties;
    }

    public void setTransientUserData(T data) {

        this.transientUserData = data;
        
    }

	@Override
	public long getExecutionId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getInstanceId() {
		// TODO Auto-generated method stub
		return 0;
	}
}
