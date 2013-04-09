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
import java.util.logging.Logger;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.context.JobContext;

import com.ibm.jbatch.container.navigator.ModelNavigator;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.jbatch.jsl.model.JSLProperties;
import com.ibm.jbatch.jsl.model.Property;


public class JobContextImpl implements JobContext {

	private final static String sourceClass = JobContextImpl.class.getName();
	private final static Logger logger = Logger.getLogger(sourceClass);

	private BatchStatus batchStatus = null;
	private String exitStatus = null;

	private Object transientUserData = null;
	private ModelNavigator<JSLJob> navigator = null;
	public ModelNavigator<JSLJob> getNavigator() {
		return navigator;
	}

	private String id;  // Name
	private Properties properties = new Properties();

	private long executionId;
	private long instanceId;
	protected String restartOn;




	public JobContextImpl(ModelNavigator<JSLJob> navigator, JSLProperties jslProperties) {
		this.navigator = navigator;
		this.id = navigator.getRootModelElement().getId();
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


	public String getExitStatus() {
		return exitStatus;
	}


	public void setExitStatus(String exitStatus) {
		logger.fine("Setting exitStatus = " + exitStatus);
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


	public Object getTransientUserData() {
		return transientUserData;
	}


	public Properties getProperties() {
		return properties;
	}

	public void setTransientUserData(Object data) {
		this.transientUserData = data;
	}

	@Override
	public long getExecutionId() {
		// TODO Auto-generated method stub
		return this.executionId;
	}

	@Override
	public long getInstanceId() {
		return this.instanceId;
	}

	public void setExecutionId(long executionId){
		this.executionId = executionId;
	}

	public void setInstanceId(long instanceId){
		this.instanceId = instanceId;
	}

	public String getRestartOn() {
		return restartOn;
	}

	public void setRestartOn(String restartOn) {
		logger.fine("Setting restartOn = " + restartOn);
		this.restartOn = restartOn;
	}

	public String toString() {

		StringBuffer buf = new StringBuffer();
		buf.append("batchStatus = " + batchStatus); 
		buf.append(" , exitStatus = " + exitStatus); 
		buf.append(" , id = " + id); 
		buf.append(" , executionId = " + executionId); 
		buf.append(" , instanceId = " + instanceId);
		buf.append(" , restartOn = " + restartOn);
		return buf.toString();
	}
}

