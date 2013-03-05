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
package com.ibm.jbatch.tck.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.batch.operations.JobOperator;
import javax.batch.operations.exception.JobExecutionAlreadyCompleteException;
import javax.batch.operations.exception.JobExecutionIsRunningException;
import javax.batch.operations.exception.JobExecutionNotMostRecentException;
import javax.batch.operations.exception.JobExecutionNotRunningException;
import javax.batch.operations.exception.JobInstanceAlreadyCompleteException;
import javax.batch.operations.exception.JobRestartException;
import javax.batch.operations.exception.JobStartException;
import javax.batch.operations.exception.NoSuchJobException;
import javax.batch.operations.exception.NoSuchJobExecutionException;
import javax.batch.operations.exception.NoSuchJobInstanceException;
import javax.batch.operations.exception.SecurityException;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;

import com.ibm.jbatch.tck.spi.JobEndCallbackManager;


public class JobOperatorBridge {

    //private JobOperator jobOp = ServiceGateway.getServices().getJobOperator();
    private JobOperator jobOp = BatchRuntime.getJobOperator();
    private JobEndCallbackManager callbackMgr = ServiceGateway.getServices().getCallbackManager();

    private Set<Long> completedExecutions = new HashSet<Long>();

    private int sleepTime = Integer.parseInt(System.getProperty("junit.jobOperator.sleep.time", "900000"));

    public JobOperatorBridge() {
        super();        
    }
    
    //public List<StepExecution> getJobSteps(long jobExecutionId) {
    //	return jobOp.getJobSteps(jobExecutionId);
    //}
    
    public List<String> getJobNames() {
    	return new ArrayList(jobOp.getJobNames());
    }
    
    public int getJobInstanceCount(String jobName) throws NoSuchJobException, SecurityException {
    	return jobOp.getJobInstanceCount(jobName);
    }
    
    public List<JobExecution> getRunningExecutions(String jobName) throws NoSuchJobException{
    	return jobOp.getRunningExecutions(jobName);
    }
    
    public List<JobExecution> getExecutions(JobInstance instanceId) throws NoSuchJobInstanceException, SecurityException {
    	return jobOp.getExecutions(instanceId);
    }
    
    public List<JobExecution> getJobExecutions(JobInstance instanceId) throws NoSuchJobInstanceException, SecurityException {
    	return jobOp.getJobExecutions(instanceId);
    }
    
    //public StepExecution getStepExecution(long stepExecutionId) {
    //	return jobOp.getStepExecution(stepExecutionId);
    //}
    
    public JobExecution restartJobAndWaitForResult(long executionId, Properties restartJobParameters) throws JobInstanceAlreadyCompleteException, NoSuchJobExecutionException, NoSuchJobException, JobRestartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException, SecurityException {    	
    	//throw new UnsupportedOperationException("Waiting for spec discussion to settle down regarding restart parameters.");
        // Register callback first in case job completes before we get control back 
        JobEndCallbackImpl callback = new JobEndCallbackImpl();
        
        callbackMgr.registerJobEndCallback(callback);        
        Long execID = (Long)jobOp.restart(executionId, restartJobParameters);        
        
        return jobExecutionResult(execID, callback);
    }
    
    public JobExecution restartJobAndWaitForResult(long executionId) throws JobInstanceAlreadyCompleteException, NoSuchJobExecutionException, NoSuchJobException, JobRestartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException, SecurityException {

        // Register callback first in case job completes before we get control back 
        JobEndCallbackImpl callback = new JobEndCallbackImpl();
        
        callbackMgr.registerJobEndCallback(callback);        
        Long execID = (Long)jobOp.restart(executionId);        
        
        return jobExecutionResult(execID, callback);
    }
    
    public void abandonJobInstance(JobExecution jobExecution) throws NoSuchJobInstanceException, JobExecutionIsRunningException, SecurityException {
           
        jobOp.abandon(jobExecution);        
       
    }

    public JobExecution startJobAndWaitForResult(String jobName) throws JobStartException, NoSuchJobExecutionException, SecurityException {
        return startJobAndWaitForResult(jobName, null);
    }

    public JobExecution startJobWithoutWaitingForResult(String jobName, Properties jobParameters) throws JobStartException, NoSuchJobExecutionException, SecurityException {
        Long execID = (Long)jobOp.start(jobName, jobParameters);
        return jobOp.getJobExecution(execID);
    }
    
    public void stopJobWithoutWaitingForResult(long jobInstanceId) throws NoSuchJobExecutionException, JobExecutionNotRunningException, SecurityException {
        jobOp.stop(jobInstanceId);
    }
            
    /*
     * I haven't mentally proven it to myself but I'm assuming this can ONLY be used
     * after startJobWithoutWaitingForResult(), not after startJobAndWaitForResult().
     */
    public JobExecution stopJobAndWaitForResult(JobExecution jobExecution) throws NoSuchJobExecutionException, JobExecutionNotRunningException, SecurityException {
        JobEndCallbackImpl callback = new JobEndCallbackImpl();

        long execID = jobExecution.getExecutionId();
        callbackMgr.registerJobEndCallback(callback);
        jobOp.stop(execID);

        return jobExecutionResult(execID, callback);
    }

    public JobExecution startJobAndWaitForResult(String jobName, Properties jobParameters) throws JobStartException, NoSuchJobExecutionException, SecurityException {

        JobEndCallbackImpl callback = new JobEndCallbackImpl();

        callbackMgr.registerJobEndCallback(callback);
        Long executionId = (Long)jobOp.start(jobName, jobParameters);
        
        return jobExecutionResult(executionId, callback);
    }
        


    public Properties getParameters(JobInstance jobExecutionId) throws NoSuchJobInstanceException, SecurityException{
    	return jobOp.getParameters(jobExecutionId);
    }
    
    public JobInstance getJobInstance(long instanceId) throws NoSuchJobExecutionException, SecurityException{
    	return jobOp.getJobInstance(instanceId);
    }
    
    public JobExecution getJobExecution(long executionId) throws NoSuchJobExecutionException, SecurityException{
    	return jobOp.getJobExecution(executionId);
    }
    
    //TODO - when JobOperator introduces a deregister we should call it.
    public void destroy() {

    }

    private class JobEndCallbackImpl implements com.ibm.jbatch.tck.spi.JobEndCallback {
    	
    	// The wrapper around long is chosen so that 'null' clearly signifies 'unset',
    	// since '0' does not.
    	private Long execIdObj = null;

		public Long getExecIdObj() {
			return execIdObj;
		}

		public void setExecIdObj(Long execIdObj) {
			this.execIdObj = execIdObj;
		}

		@Override
        public void done(long jobExecutionId) {
            synchronized(this) {
                completedExecutions.add(jobExecutionId);
                
                // If we have set an execution id into the callback,
                // then only wake up the sleep if we have matched the
                // execution id.
                if (execIdObj != null) {
                	if (execIdObj.longValue() == jobExecutionId) {
                		this.notify();
                	}
                } 
                
                // otherwise there is nothing to do.   We will only be sleeping
                // with an already-set execution id.
            }
        }
    }

    protected JobExecution jobExecutionResult(long execID, JobEndCallbackImpl callback) throws NoSuchJobExecutionException, SecurityException {
    	// First get the lock on the callback
        synchronized (callback) {          
        	// If this execution is already complete, then there's no need to wait
            if (!completedExecutions.contains(execID)) {
            	// While we have the lock we'll associate this callback with the execution id
            	// so we can only get notified when this particular execution id completes.
            	callback.setExecIdObj(new Long(execID));
                try {
                    callback.wait(sleepTime);
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
                // Now either we have the result, or we've waiting long enough and are going to bail.
                if (!completedExecutions.contains(execID)) {
                	throw new IllegalStateException("Still didn't see a result for executionId: " + execID + 
                        ".  Perhaps try increasing timeout.  Or, something else may have gone wrong.");
                }
            }            
        }
        
        // Not absolutely required since we should have things coded such that a registered
        // callback for some other execution doesn't interfere with correct notification of
        // completion of this execution.   However, it might reduce noise and facilitate
        // debug to clean things up.
        callbackMgr.deregisterJobEndCallback(callback);
        
        return jobOp.getJobExecution(execID);
    }
    
	public List<JobInstance> getJobInstances(String jobName, int start, int end) throws NoSuchJobException, SecurityException {
		return jobOp.getJobInstances(jobName, start, end);
	}

	public List<StepExecution> getStepExecutions(long executionId) throws NoSuchJobExecutionException, SecurityException {
		return jobOp.getStepExecutions(executionId);
	}

}
