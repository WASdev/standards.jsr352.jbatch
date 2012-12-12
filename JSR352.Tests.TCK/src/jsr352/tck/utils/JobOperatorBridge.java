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
package jsr352.tck.utils;

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.batch.operations.JobOperator;
import javax.batch.operations.exception.JobExecutionNotRunningException;
import javax.batch.operations.exception.JobInstanceAlreadyCompleteException;
import javax.batch.operations.exception.JobRestartException;
import javax.batch.operations.exception.JobStartException;
import javax.batch.operations.exception.NoSuchJobException;
import javax.batch.operations.exception.NoSuchJobExecutionException;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.StepExecution;

import com.ibm.batch.tck.spi.JobEndCallbackManager;


public class JobOperatorBridge {

    private JobOperator jobOp = ServiceGateway.getServices().getJobOperator();
    private JobEndCallbackManager callbackMgr = ServiceGateway.getServices().getCallbackManager();

    private Set<Long> completedExecutions = new HashSet<Long>();

    private int sleepTime = Integer.parseInt(System.getProperty("junit.jobOperator.sleep.time", "900000"));

    public JobOperatorBridge() {
        super();        
    }
    
    public List<StepExecution> getJobSteps(long jobExecutionId) {
    	return jobOp.getJobSteps(jobExecutionId);
    }
    
    public StepExecution getStepExecution(long jobExecutionId, long stepExecutionId) {
    	return jobOp.getStepExecution(jobExecutionId, stepExecutionId);
    }

    public JobExecution restartJobAndWaitForResult(long jobInstanceId, Properties jobParametersOverride) throws JobInstanceAlreadyCompleteException, NoSuchJobExecutionException, NoSuchJobException, JobRestartException {

        // Register callback first in case job completes before we get control back 
        JobEndCallbackImpl callback = new JobEndCallbackImpl();
        
        callbackMgr.registerJobEndCallback(callback);
        Long execID = (Long)jobOp.restart(jobInstanceId, jobParametersOverride);        
        
        return jobExecutionResult(execID, callback);
    }

    public JobExecution startJobAndWaitForResult(String xJCL) throws JobStartException {
        return startJobAndWaitForResult(xJCL, null);
    }

    public JobExecution startJobWithoutWaitingForResult(String xJCL, Properties jobParameters) throws JobStartException {
        Long execID = (Long)jobOp.start(xJCL, jobParameters);
        return jobOp.getJobExecution(execID);
    }
    
    public void stopJobWithoutWaitingForResult(long jobInstanceId) throws NoSuchJobExecutionException, JobExecutionNotRunningException {
        jobOp.stop(jobInstanceId);
    }
            
    /*
     * I haven't mentally proven it to myself but I'm assuming this can ONLY be used
     * after startJobWithoutWaitingForResult(), not after startJobAndWaitForResult().
     */
    public JobExecution stopJobAndWaitForResult(JobExecution jobExecution) throws NoSuchJobExecutionException, JobExecutionNotRunningException {
        JobEndCallbackImpl callback = new JobEndCallbackImpl();

        long execID = jobExecution.getExecutionId();
        callbackMgr.registerJobEndCallback(callback);
        jobOp.stop(jobExecution.getInstanceId());

        return jobExecutionResult(execID, callback);
    }

    public JobExecution startJobAndWaitForResult(String xJCL, Properties jobParameters) throws JobStartException {

        JobEndCallbackImpl callback = new JobEndCallbackImpl();

        callbackMgr.registerJobEndCallback(callback);
        Long execID = (Long)jobOp.start(xJCL, jobParameters);
        
        return jobExecutionResult(execID, callback);
    }
        
    protected JobExecution jobExecutionResult(long execID, JobEndCallbackImpl callback) {
        synchronized (callback) {          
            if (!completedExecutions.contains(execID)) {
                try {
                    
                    callback.wait(sleepTime);
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }            
            // Now we should have the result.
            if (!completedExecutions.contains(execID)) {
                throw new IllegalStateException("Still didn't see a result for executionId: " + execID + 
                        ".  Perhaps try increasing timeout.  Or, something else may have gone wrong.");
            }
        }
        
        // Not absolutely required, but should be useful to clean things up a bit?
        callbackMgr.deregisterJobEndCallback(callback);
        
        return jobOp.getJobExecution(execID);
    }

    //TODO - when JobOperator introduces a deregister we should call it.
    public void destroy() {

    }

    private class JobEndCallbackImpl implements com.ibm.batch.tck.spi.JobEndCallback {
        @Override
        public void done(long jobExecutionId) {
            synchronized(this) {
                completedExecutions.add(jobExecutionId);
                this.notify();
            }
        }
    }
}
