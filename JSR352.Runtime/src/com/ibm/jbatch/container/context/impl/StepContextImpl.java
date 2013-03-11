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

import javax.batch.operations.JobOperator.BatchStatus;
import javax.batch.runtime.Metric;
import javax.batch.runtime.context.StepContext;

public class StepContextImpl<T, P extends Serializable> implements StepContext<T, P> {

    private final static String sourceClass = StepContextImpl.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);
    
    private String stepId = null;
    private BatchStatus batchStatus = null;
    private String exitStatus = null;
    private T transientUserData = null;
    private P persistentUserData = null;
    private Exception exception = null;
    Timestamp starttime = null;
    Timestamp endtime = null;
    
    private long stepExecID = 0;
    
    private Properties properties = new Properties(); 

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
    public P getPersistentUserData() {
        return persistentUserData;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    public T getTransientUserData() {
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
        this.batchStatus = status;
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Batch status set to: " + status + " for step id:" + getStepName());
        }
    }

    @Override
    public void setPersistentUserData(P data) {
        persistentUserData = data;

    }

    public void setTransientUserData(T data) {
        transientUserData = data;        
    }

    @Override 
    public String toString() {    
        StringBuffer buf = new StringBuffer();
        buf.append(" stepId: " + stepId);
        buf.append(", batchStatus: " + batchStatus);        
        buf.append(", exitStatus: " + exitStatus);
        buf.append(", transientUserData: " + transientUserData);
        buf.append(", persistentUserData: " +     persistentUserData);
        return buf.toString();
    }

	@Override
	public long getStepExecutionId() {
		// TODO Auto-generated method stub
		return stepExecID;
	}

	/*
	@Override
	public List<FlowContext<T>> getBatchContexts() {
		// TODO Auto-generated method stub
		return null;
	}
    */
	
	/*
	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		// TODO Auto-generated method stub
		
	}
    */
	
	/*
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		// TODO Auto-generated method stub
		
	}
	*/
	
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

}
