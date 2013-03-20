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
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.batch.operations.JobExecutionNotMostRecentException;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.JobStartException;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobInstance;

import com.ibm.jbatch.container.context.impl.JobContextImpl;
import com.ibm.jbatch.container.jsl.ModelResolverFactory;
import com.ibm.jbatch.container.jsl.ModelSerializer;
import com.ibm.jbatch.container.jsl.ModelSerializerFactory;
import com.ibm.jbatch.container.jsl.Navigator;
import com.ibm.jbatch.container.jsl.NavigatorFactory;
import com.ibm.jbatch.container.modelresolver.PropertyResolver;
import com.ibm.jbatch.container.modelresolver.PropertyResolverFactory;
import com.ibm.jbatch.container.services.IBatchKernelService;
import com.ibm.jbatch.container.services.IJobExecution;
import com.ibm.jbatch.container.services.IJobStatusManagerService;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.services.impl.JDBCPersistenceManagerImpl;
import com.ibm.jbatch.container.servicesmanager.ServicesManager;
import com.ibm.jbatch.container.servicesmanager.ServicesManagerImpl;
import com.ibm.jbatch.container.status.JobStatus;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.jbatch.jsl.model.JSLProperties;

public class JobExecutionHelper {

    private final static String CLASSNAME = JobExecutionHelper.class.getName();
    private final static Logger logger = Logger.getLogger(CLASSNAME);
    
    private static ServicesManager servicesManager = ServicesManagerImpl.getInstance();

    private static IJobStatusManagerService _jobStatusManagerService = 
    		servicesManager.getJobStatusManagerService();
    
    private static IPersistenceManagerService _persistenceManagementService = 
    		servicesManager.getPersistenceManagerService();
    private static IBatchKernelService _batchKernelService = servicesManager.getBatchKernelService();


    private static Navigator<JSLJob> getResolvedJobNavigator(String jobXml, Properties jobParameters, boolean isPartitionedStep) {

        JSLJob jobModel = ModelResolverFactory.createJobResolver().resolveModel(jobXml); 
        PropertyResolver<JSLJob> propResolver = PropertyResolverFactory.createJobPropertyResolver(isPartitionedStep);
        propResolver.substituteProperties(jobModel, jobParameters);

        return NavigatorFactory.createJobNavigator(jobModel);
    }
    
    private static Navigator<JSLJob> getResolvedJobNavigator(JSLJob jobModel, Properties jobParameters, boolean isPartitionedStep) {

        PropertyResolver<JSLJob> propResolver = PropertyResolverFactory.createJobPropertyResolver(isPartitionedStep);
        propResolver.substituteProperties(jobModel, jobParameters);

        return NavigatorFactory.createJobNavigator(jobModel);
    }

    private static String getJobXml(JSLJob jobModel) {
        ModelSerializer<JSLJob> serializer = ModelSerializerFactory.createJobModelSerializer();
        return serializer.serializeModel(jobModel);
    }

    private static JobContextImpl getJobContext(Navigator<JSLJob> jobNavigator) {
        JSLProperties jslProperties = new JSLProperties();
        if(jobNavigator.getJSL() != null) {
            jslProperties = jobNavigator.getJSL().getProperties();
        }
        return new JobContextImpl(jobNavigator, jslProperties); 
    }

    private static JobInstance getNewJobInstance(String name, String jobXml, Properties jobParameters) {
        String apptag = _batchKernelService.getBatchSecurityHelper().getCurrentTag();
        return _persistenceManagementService.createJobInstance(name, apptag, jobXml, jobParameters);
    }


    private static JobStatus createNewJobStatus(JobInstance jobInstance) {
    	long instanceId = jobInstance.getInstanceId();
        JobStatus jobStatus = _jobStatusManagerService.createJobStatus(instanceId);
        jobStatus.setJobInstance(jobInstance);
        return jobStatus;
    }

    private static void validateRestartableFalseJobsDoNotRestart(JSLJob jobModel)
            throws JobRestartException {
        if (jobModel.getRestartable() != null && jobModel.getRestartable().equalsIgnoreCase("false")) {
            throw new JobRestartException("Job Restartable attribute is false, Job cannot be restarted.");
        }
    }
    
    public static RuntimeJobContextJobExecutionBridge startJob(String jobXML, Properties jobParameters) throws JobStartException {
        logger.entering(CLASSNAME, "startJob", new Object[]{jobXML, jobParameters});

        JSLJob jobModel = ModelResolverFactory.createJobResolver().resolveModel(jobXML); 

        RuntimeJobContextJobExecutionBridge jobExecution = startJob(jobModel, jobParameters, false);

        logger.exiting(CLASSNAME, "startJob", jobExecution);

        return jobExecution;
    }

    public static RuntimeJobContextJobExecutionBridge startJob(JSLJob jobModel, Properties jobParameters, boolean isPartitionedStep) throws JobStartException{

        logger.entering(CLASSNAME, "startJob", new Object[]{jobModel, jobParameters, isPartitionedStep});

        String jobXML = getJobXml(jobModel);

        Navigator<JSLJob> jobNavigator = getResolvedJobNavigator(jobXML, jobParameters, isPartitionedStep);

        JobContextImpl jobContext = getJobContext(jobNavigator);

        JobInstance jobInstance = getNewJobInstance(jobNavigator.getId(), jobXML, jobParameters);

        RuntimeJobContextJobExecutionBridge executionHelper = 
        		_persistenceManagementService.createJobExecution(jobInstance, jobParameters, jobContext.getBatchStatus());
        
        executionHelper.prepareForExecution(jobContext);
        
        JobStatus jobStatus = createNewJobStatus(jobInstance);
        _jobStatusManagerService.updateJobStatus(jobStatus);
        
        logger.exiting(CLASSNAME, "startJob", executionHelper);
        return executionHelper;
    }


    public static RuntimeJobContextJobExecutionBridge restartJob(long executionId, JSLJob gennedJobModel) throws JobRestartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException {
        return restartJob(executionId, null, null, false);
    }
    
    private static void validateJobInstanceNotCompleteOrAbandonded(JobStatus jobStatus) throws JobRestartException, JobExecutionAlreadyCompleteException {
        if (jobStatus.getBatchStatus() == null) {
        	String msg = "On restart, we didn't find an earlier batch status.";
        	logger.warning(msg);
        	throw new IllegalStateException(msg);
        }
        
        if (jobStatus.getBatchStatus().equals(BatchStatus.COMPLETED)) {
        	String msg = "Already completed job instance = " + jobStatus.getJobInstanceId();
        	logger.fine(msg);
        	throw new JobExecutionAlreadyCompleteException(msg);
        } else if (jobStatus.getBatchStatus().equals(BatchStatus.ABANDONED)) {
        	String msg = "Abandoned job instance = " + jobStatus.getJobInstanceId();
        	logger.warning(msg);
        	throw new JobRestartException(msg);
        } 
    }

    private static void validateJobExecutionIsMostRecent(long jobInstanceId, long executionId) throws JobExecutionNotMostRecentException {

        long mostRecentExecutionId = _persistenceManagementService.getMostRecentExecutionId(jobInstanceId);
        
        if ( mostRecentExecutionId != executionId ) {
            String message = "ExecutionId: " + executionId + " is not the most recent execution.";
            logger.warning(message);
            throw new JobExecutionNotMostRecentException(message);
        }
    }
    
    public static RuntimeJobContextJobExecutionBridge restartJob(long executionId, JSLJob gennedJobModel, Properties restartJobParameters, boolean isPartitionedStep) throws JobRestartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException {

        long jobInstanceId = _persistenceManagementService.getJobInstanceIdByExecutionId(executionId);
    	
        JobStatus jobStatus = _jobStatusManagerService.getJobStatus(jobInstanceId);
        
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("On restartJob with jobInstance Id = " + jobInstanceId + " , found JobStatus: " + jobStatus + 
            		", batchStatus = " + jobStatus.getBatchStatus().name() ); 
        }

        validateJobExecutionIsMostRecent(jobInstanceId, executionId);
        
        validateJobInstanceNotCompleteOrAbandonded(jobStatus);

        JobInstanceImpl jobInstance = jobStatus.getJobInstance();
        
        Navigator<JSLJob> jobNavigator = null;
               
        // If we are in a parallel job that is genned use the regenned JSL.
        if (gennedJobModel == null) {
            jobNavigator = getResolvedJobNavigator(jobInstance.getJobXML(), restartJobParameters, isPartitionedStep);
        } else {
            jobNavigator = getResolvedJobNavigator(gennedJobModel, restartJobParameters, isPartitionedStep);
        }
        // JSLJob jobModel = ModelResolverFactory.createJobResolver().resolveModel(jobInstance.getJobXML());
        validateRestartableFalseJobsDoNotRestart(jobNavigator.getJSL());

        JobContextImpl jobContext = getJobContext(jobNavigator);
        RuntimeJobContextJobExecutionBridge executionHelper = 
        		_persistenceManagementService.createJobExecution(jobInstance, restartJobParameters, jobContext.getBatchStatus());
    	executionHelper.prepareForExecution(jobContext, jobStatus.getRestartOn());
        _jobStatusManagerService.updateJobStatusWithNewExecution(jobInstance.getInstanceId(), executionHelper.getExecutionId());        
        
        return executionHelper;
    }    
    
    public static IJobExecution getPersistedJobOperatorJobExecution(long jobExecutionId) {
    	
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
    

    
//    public static StepExecution getStepExecution(String key){
//    	return _persistenceManagementService.getStepExecutionQueryID(key);
//    }
    
    public static JobInstance getJobInstance(long executionId){
    	JobStatus jobStatus = _jobStatusManagerService.getJobStatusFromExecutionId(executionId);
    	JobInstanceImpl jobInstance = jobStatus.getJobInstance();
    	return jobInstance;
    }
}
    