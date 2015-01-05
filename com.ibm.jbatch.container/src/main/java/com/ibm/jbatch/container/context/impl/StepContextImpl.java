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

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.Metric;
import javax.batch.runtime.context.StepContext;

import com.ibm.jbatch.container.annotation.TCKExperimentProperty;

public class StepContextImpl implements StepContext {

    private final static String sourceClass = StepContextImpl.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);
    
    @TCKExperimentProperty
    private final static boolean cloneContextProperties = Boolean.getBoolean("clone.context.properties");
    
    private String stepId = null;
    private BatchStatus batchStatus = null;
    private String exitStatus = null;
    private Object transientUserData = null;
    private Serializable persistentUserData = null;
    private Exception exception = null;
    Timestamp starttime = null;
    Timestamp endtime = null;
    
    private long stepExecID = 0;
    
    private Properties properties = new Properties(); 

    private String batchletProcessRetVal = null;
    
	public final static String TOP_LEVEL_STEP_EXECUTION_ID_PROP = "com.ibm.jbatch.container.context.impl.StepContextImpl#getExecutionId";

	private ConcurrentHashMap<String, Metric> metrics = new ConcurrentHashMap<String, Metric>();

    public StepContextImpl(String stepId) {
        this.stepId = stepId;        
    }

    @Override
    public BatchStatus getBatchStatus() {
        return batchStatus;
    }

    @Override
    public Exception getException() {
        // TODO Auto-generated method stub
        return exception;
    }
    
    public void setException(Exception exception){
    	this.exception = exception;
    }

    @Override
    public String getExitStatus() {
        return exitStatus;
    }

    @Override
    public String getStepName() {
        return stepId;
    }

    @Override
    public Metric[] getMetrics() {
        return metrics.values().toArray(new Metric[0]);
    }
    
    public MetricImpl getMetric(MetricImpl.MetricType metricType) {
        return (MetricImpl)metrics.get(metricType.name());
    }
    
    public void addMetric(MetricImpl.MetricType metricType, long value) {
    	metrics.putIfAbsent(metricType.name(), new MetricImpl(metricType, value));
    }

    @Override
    public Serializable getPersistentUserData() {
        return persistentUserData;
    }

    @Override
    public Properties getProperties() {
    	if (cloneContextProperties) {
    		logger.fine("Cloning job context properties");
    		return (Properties)properties.clone();
    	} else {
    		logger.fine("Returing ref (non-clone) to job context properties");
    		return properties;
    	}
    }
    
    public Properties getJSLProperties() {
    	return properties;
    }

    public Object getTransientUserData() {
        return transientUserData;
    }

    @Override
    public void setExitStatus(String status) {
        this.exitStatus = status;
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "Exit status set to: " + status + " for step id:" + getStepName());
        }
    }

    public void setBatchStatus(BatchStatus status) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Batch status set to: " + status + " from " + batchStatus + " for step id:" + getStepName());
        }
        this.batchStatus = status;
    }

    @Override
    public void setPersistentUserData(Serializable data) {
        persistentUserData = data;

    }

    public void setTransientUserData(Object data) {
        transientUserData = data;        
    }

    @Override 
    public String toString() {    
        StringBuffer buf = new StringBuffer();
        buf.append(" stepId: " + stepId);
        buf.append(", batchStatus: " + batchStatus);        
        buf.append(", exitStatus: " + exitStatus);
        buf.append(", batchletProcessRetVal: " + batchletProcessRetVal);
        buf.append(", transientUserData: " + transientUserData);
        buf.append(", persistentUserData: " +     persistentUserData);
        return buf.toString();
    }

	@Override
	public long getStepExecutionId() {
		if (properties.containsKey(TOP_LEVEL_STEP_EXECUTION_ID_PROP)) {
			return Long.parseLong(properties.getProperty(TOP_LEVEL_STEP_EXECUTION_ID_PROP));
		} else {
			return this.stepExecID;
		}
	}
	
	public long getInternalStepExecutionId() {
		return stepExecID;
	}


	public void setStepExecutionId(long stepExecutionId){
		stepExecID = stepExecutionId;
	}

	public void setStartTime(Timestamp startTS) {
		starttime = startTS;
		
	}

	public void setEndTime(Timestamp endTS) {
		endtime = endTS;
		
	}
	
	public Timestamp getStartTimeTS(){
		return starttime;
	}
	
	public Timestamp getEndTimeTS(){
		return endtime;
	}
	
    public String getBatchletProcessRetVal() {
		return batchletProcessRetVal;
	}

	public void setBatchletProcessRetVal(String batchletProcessRetVal) {
		this.batchletProcessRetVal = batchletProcessRetVal;
	}

}
