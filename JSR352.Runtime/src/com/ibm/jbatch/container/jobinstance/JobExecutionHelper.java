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
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator.BatchStatus;
import javax.batch.operations.exception.JobRestartException;
import javax.batch.operations.exception.JobStartException;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;


import com.ibm.jbatch.container.context.impl.StepContextImpl;
import com.ibm.jbatch.container.jsl.ModelResolverFactory;
import com.ibm.jbatch.container.jsl.ModelSerializer;
import com.ibm.jbatch.container.jsl.ModelSerializerFactory;
import com.ibm.jbatch.container.jsl.Navigator;
import com.ibm.jbatch.container.jsl.NavigatorFactory;
import com.ibm.jbatch.container.modelresolver.PropertyResolver;
import com.ibm.jbatch.container.modelresolver.PropertyResolverFactory;
import com.ibm.jbatch.container.services.IJobStatusManagerService;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.services.impl.JDBCPersistenceManagerImpl;
import com.ibm.jbatch.container.servicesmanager.ServicesManager;
import com.ibm.jbatch.container.servicesmanager.ServicesManagerImpl;
import com.ibm.jbatch.container.status.JobStatus;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.jbatch.spi.services.IJobIdManagementService;

public class JobExecutionHelper {

    private final static String CLASSNAME = JobExecutionHelper.class.getName();
    private final static Logger logger = Logger.getLogger(CLASSNAME);
    
    private static final String GENERATED_JOB = "GENERATED_JOB";

    private static ServicesManager servicesManager = ServicesManagerImpl.getInstance();

    private static IJobIdManagementService _jobIdManagementService = 
    		servicesManager.getJobIdManagementService();

    private static IJobStatusManagerService _jobStatusManagerService = 
    		servicesManager.getJobStatusManagerService();
    
    private static IPersistenceManagerService _persistenceManagementService = 
    		servicesManager.getPersistenceManagerService();

    public static RuntimeJobExecutionImpl startJob(String jobXML, Properties jobParameters) throws JobStartException {
        long instanceId = _jobIdManagementService.getInstanceId();
        long executionId = _jobIdManagementService.getExecutionId();

        JSLJob jobModel = ModelResolverFactory.createJobResolver().resolveModel(jobXML); 
        
        JobInstanceImpl jobInstanceImpl = new JobInstanceImpl(jobXML, jobParameters, instanceId);                

        //Resolve the properties for this job
        PropertyResolver<JSLJob> propResolver = PropertyResolverFactory.createJobPropertyResolver(false);
        propResolver.substituteProperties(jobModel, jobParameters);
        
        Navigator jobNavigator = NavigatorFactory.createJobNavigator(jobModel);       

        validateAbstractJobDoNotStart(jobModel);
        
        jobInstanceImpl.setJobName(jobNavigator.getId());                                

        _jobStatusManagerService.createJobStatus(jobInstanceImpl, executionId);
                
        long time = System.currentTimeMillis();
        // register jobName in the jobop job information table
        Timestamp starttime = null; // what is the arg here?
        Timestamp updatetime = new Timestamp(time); // what is the arg here?
        Timestamp endtime = null;
        Timestamp createtime = new Timestamp(time);
        _persistenceManagementService.jobOperatorCreateJobInstanceData(instanceId, jobNavigator.getId());
        
        RuntimeJobExecutionImpl rtJobExec = new RuntimeJobExecutionImpl(jobNavigator, jobInstanceImpl, executionId);
        
        //perhaps this start time timestamping should be defered to the jobcontroller
        rtJobExec.setStartTime(starttime);
        rtJobExec.setCreateTime(createtime);
        rtJobExec.setLastUpdateTime(updatetime);
        rtJobExec.setEndTime(endtime);
        rtJobExec.setJobProperties(jobParameters);
        
        createJobExecutionEntry(rtJobExec.getJobOperatorJobExecution());
        
        return rtJobExec;
    }

	private static void validateAbstractJobDoNotStart(JSLJob jobModel)
			throws JobStartException {
		if (jobModel.getAbstract() != null && jobModel.getAbstract().equalsIgnoreCase("true")) {
        	throw new JobStartException("An abstract job is NOT executable.");
        }
	}

    public static RuntimeJobExecutionImpl restartJob(long executionId) throws JobRestartException {
    	
        return restartJob(executionId, null, false);
    }
    
    public static RuntimeJobExecutionImpl restartJob(long executionId, Properties overrideJobParameters, boolean isPartitionedStep) throws JobRestartException {
    	
    	// because of JobOp re-factor, restart now takes in an executionId
    	// this id must be that of the last execution for the jobinstance
    	// TODO: ensure that this fact is enforced somewhere
    	// must go from executionId to a jobInstanceId to do restart
    	JobExecution jobEx = _persistenceManagementService.jobOperatorGetJobExecution(executionId);
    	long jobInstanceId = jobEx.getInstanceId();
    	
        JobStatus jobStatus = _jobStatusManagerService.getJobStatus(jobInstanceId);

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("On restartJob with jobInstance Id = " + jobInstanceId + " , found JobStatus: " + jobStatus );            
        }

        JobInstanceImpl jobInstance = jobStatus.getJobInstance();

        //TODO - check that original job ended in restartable state and that it's restartable.
        // Don't get a new execution id until we do this check.

        long nextExecutionId = _jobIdManagementService.getExecutionId();

        Properties originalJobParameters = jobInstance.getOriginalJobParams();

        Properties mergedRestartJobParameters = new Properties(originalJobParameters);
        
        if (overrideJobParameters != null) {
            for (String key : overrideJobParameters.stringPropertyNames()) {
                mergedRestartJobParameters.setProperty(key, overrideJobParameters.getProperty(key));
            }
        }
                                                
        JSLJob jobModel = ModelResolverFactory.createJobResolver().resolveModel(jobInstance.getJobXML());
        
        //Resolve the merged restart properties for this job
        PropertyResolver<JSLJob> propResolver = PropertyResolverFactory.createJobPropertyResolver(isPartitionedStep);
        propResolver.substituteProperties(jobModel, mergedRestartJobParameters);  
        
        Navigator jobNavigator = NavigatorFactory.createJobNavigator(jobModel);
        
		validateRestartableFalseJobsDoNotRestart(jobModel);
        
        _jobStatusManagerService.updateJobStatusWithNewExecution(jobInstance.getInstanceId(), nextExecutionId);        
        long time = System.currentTimeMillis();
        Timestamp starttime = null; // what is the arg here?
        Timestamp updatetime = new Timestamp(time); // what is the arg here?
        Timestamp endtime = null;
        Timestamp createtime = new Timestamp(time);
//        _persistenceManagementService.jobOperatorCreateJobInstanceData(jobInstanceId, jobNavigator.getId());

        RuntimeJobExecutionImpl rtJobExec = new RuntimeJobExecutionImpl(jobNavigator, jobInstance, nextExecutionId, jobStatus.getRestartOn());
        rtJobExec.setStartTime(starttime);
        rtJobExec.setCreateTime(createtime);
        rtJobExec.setLastUpdateTime(updatetime);
        rtJobExec.setEndTime(endtime);
        rtJobExec.setJobProperties(mergedRestartJobParameters);        
        
        createJobExecutionEntry(rtJobExec.getJobOperatorJobExecution());
        
        return rtJobExec;
    }

	private static void validateRestartableFalseJobsDoNotRestart(JSLJob jobModel)
			throws JobRestartException {
		if (jobModel.getRestartable() != null && jobModel.getRestartable().equalsIgnoreCase("false")) {
			throw new JobRestartException("Job Restartable attribute is false, Job cannot be restarted.");
		}
	}

    public static RuntimeJobExecutionImpl startJob(JSLJob jobModel, Properties jobParameters, boolean isPartitionedStep) {
        long instanceId = _jobIdManagementService.getInstanceId();
        long executionId = _jobIdManagementService.getExecutionId();

        ModelSerializer<JSLJob> serializer = ModelSerializerFactory.createJobModelSerializer();
        String jobXML = serializer.serializeModel(jobModel);
        JobInstanceImpl jobInstanceImpl = new JobInstanceImpl(jobXML, jobParameters, instanceId);                

        //Resolve the properties for this job
        PropertyResolver<JSLJob> propResolver = PropertyResolverFactory.createJobPropertyResolver(isPartitionedStep);
        propResolver.substituteProperties(jobModel, jobParameters);
        
        Navigator jobNavigator = NavigatorFactory.createJobNavigator(jobModel);       

        jobInstanceImpl.setJobName(jobNavigator.getId());                                

        _jobStatusManagerService.createJobStatus(jobInstanceImpl, executionId);
                
        long time = System.currentTimeMillis();
        // register jobName in the jobop job information table
        Timestamp starttime = null; // what is the arg here?
        Timestamp updatetime = new Timestamp(time); // what is the arg here?
        Timestamp endtime = null;
        Timestamp createtime = new Timestamp(time);
        _persistenceManagementService.jobOperatorCreateJobInstanceData(instanceId, jobNavigator.getId());
        
        RuntimeJobExecutionImpl rtJobExec = new RuntimeJobExecutionImpl(jobNavigator, jobInstanceImpl, executionId);
        
        //perhaps this start time timestamping should be defered to the jobcontroller
        rtJobExec.setStartTime(starttime);
        rtJobExec.setCreateTime(createtime);
        rtJobExec.setLastUpdateTime(updatetime);
        rtJobExec.setEndTime(endtime);
        rtJobExec.setJobProperties(jobParameters);
        
        createJobExecutionEntry(rtJobExec.getJobOperatorJobExecution());
        
        return rtJobExec;

    }
    
    public static void persistStepExecution(long jobExecutionInstanceID, StepContextImpl stepContext){
    	
    	String stepExecutionKey = getJobStepExecId(jobExecutionInstanceID, stepContext.getStepExecutionId());

		
    	_persistenceManagementService.stepExecutionCreateStepExecutionData(stepExecutionKey, jobExecutionInstanceID, stepContext);
    	
    }
    
    public static void createJobExecutionEntry(JobExecution jobEx){
    	
    	long executionId = jobEx.getExecutionId();
    	long instanceId = jobEx.getInstanceId();
    	Timestamp starttime = new Timestamp(0); // no valid value yet
        Timestamp updatetime = new Timestamp(jobEx.getLastUpdatedTime().getTime()); // what is the arg here?
        Timestamp endtime = new Timestamp(0); // no valid value yet?
        Timestamp createtime = new Timestamp(jobEx.getCreateTime().getTime());
        Properties jobParameters = jobEx.getJobParameters();
        BatchStatus batchStatus = jobEx.getBatchStatus() == null ? BatchStatus.STARTING : jobEx.getBatchStatus();
        String exitstatus = jobEx.getExitStatus();
        
		//System.out.println("AJM: props = " + jobParameters.toString());

        
    	_persistenceManagementService.jobOperatorCreateExecutionData(executionId, starttime, updatetime, endtime, createtime, jobParameters, instanceId, batchStatus.name(), exitstatus);
    }
    
    public static void updateJobExecutionEntry(JobExecution jobEx){
    	
    	long executionId = jobEx.getExecutionId();
    	long instanceId = jobEx.getInstanceId();
    	Timestamp starttime = new Timestamp(jobEx.getStartTime().getTime()); // what is the arg here?
        Timestamp updatetime = new Timestamp(jobEx.getLastUpdatedTime().getTime()); // what is the arg here?
        Timestamp endtime = new Timestamp(jobEx.getEndTime().getTime());
        Timestamp createtime = new Timestamp(jobEx.getCreateTime().getTime());
        Properties jobParameters = jobEx.getJobParameters();
        BatchStatus batchStatus = jobEx.getBatchStatus();
        String exitstatus = jobEx.getExitStatus();
        
    	_persistenceManagementService.jobOperatorCreateExecutionData(executionId, starttime, updatetime, endtime, createtime, jobParameters, instanceId, batchStatus.name(), exitstatus);
    }
    
    public static JobExecution getPersistedJobOperatorJobExecution(long jobExecutionId) {
    	
    	if (_persistenceManagementService instanceof JDBCPersistenceManagerImpl){
    		return _persistenceManagementService.jobOperatorGetJobExecution(jobExecutionId);
    	}
    	else {
    		return null;
    	}
    }
    
    public static void updateBatchStatusUPDATEonly(long executionId, String batchStatusString, Timestamp ts){
    	// update the batch status col and the updateTS col
    	_persistenceManagementService.jobOperatorUpdateBatchStatusWithUPDATETSonly(executionId, JDBCPersistenceManagerImpl.BATCH_STATUS, batchStatusString, ts);
    }
    
    public static void updateBatchStatusSTART(long executionId, String batchStatusString, Timestamp startTs){
    	// update the batch status col and the updateTS col
    	
    	_persistenceManagementService.jobOperatorUpdateBatchStatusWithSTATUSandUPDATETSonly(executionId, JDBCPersistenceManagerImpl.BATCH_STATUS, batchStatusString, startTs);
    }
    
    public static void updateBatchStatusSTOP(long executionId, String batchStatusString, Timestamp stopTs){
    	// update the batch status col and the updateTS col
    	
    	_persistenceManagementService.jobOperatorUpdateBatchStatusWithSTATUSandUPDATETSonly(executionId, JDBCPersistenceManagerImpl.BATCH_STATUS, batchStatusString, stopTs);
    }
    
    public static void updateBatchStatusCOMPLETED(long executionId, String batchStatusString, Timestamp completedTs){
    	// update the batch status col and the updateTS col
    	
    	_persistenceManagementService.jobOperatorUpdateBatchStatusWithSTATUSandUPDATETSonly(executionId, JDBCPersistenceManagerImpl.BATCH_STATUS, batchStatusString, completedTs);
    }
  
    public static void updateBatchStatusENDED(long executionId, String batchStatusString, Timestamp endedTs){
    	// update the batch status col and the updateTS col
    	
    	_persistenceManagementService.jobOperatorUpdateBatchStatusWithSTATUSandUPDATETSonly(executionId, JDBCPersistenceManagerImpl.BATCH_STATUS, batchStatusString, endedTs);
    }
    
    public static void updateBatchStatusFAILED(long executionId, String batchStatusString, Timestamp failedTs){
    	// update the batch status col and the updateTS col
    	
    	_persistenceManagementService.jobOperatorUpdateBatchStatusWithSTATUSandUPDATETSonly(executionId, JDBCPersistenceManagerImpl.BATCH_STATUS, batchStatusString, failedTs);
    }
    
    public static StepExecution getStepExecutionIDInfo(long stepexecutionId){
    	return _persistenceManagementService.getStepExecutionObjQueryByStepID(stepexecutionId);
    }
    
    public static List<StepExecution> getstepExecutionIDInfoList(long jobexecutionId){
    	return _persistenceManagementService.getStepExecutionIDListQueryByJobID(jobexecutionId);
    }
    
//    public static StepExecution getStepExecution(String key){
//    	return _persistenceManagementService.getStepExecutionQueryID(key);
//    }
    
    /*
     * creates unique key to get StepExecution
     */
    private static String getJobStepExecId(long jobExecutionId, long stepExecutionId) {
    	return String.valueOf(jobExecutionId) + ':' + String.valueOf(stepExecutionId);
    }
    
    public static JobInstance getJobInstance(long instanceId){
    	JobStatus jobStatus = _jobStatusManagerService.getJobStatus(instanceId);
    	JobInstanceImpl jobInstance = jobStatus.getJobInstance();
    	return jobInstance;
    }
}
