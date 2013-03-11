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

import javax.batch.operations.JobRestartException;
import javax.batch.operations.JobStartException;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;

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
    
    private static final String GENERATED_JOB = "GENERATED_JOB";

    private static ServicesManager servicesManager = ServicesManagerImpl.getInstance();

    private static IJobStatusManagerService _jobStatusManagerService = 
    		servicesManager.getJobStatusManagerService();
    
    private static IPersistenceManagerService _persistenceManagementService = 
    		servicesManager.getPersistenceManagerService();
    private static IBatchKernelService _batchKernelService = servicesManager.getBatchKernelService();


    private static Navigator getResolvedJobNavigator(String jobXml, Properties jobParameters, boolean isPartitionedStep) {

        JSLJob jobModel = ModelResolverFactory.createJobResolver().resolveModel(jobXml); 
        PropertyResolver<JSLJob> propResolver = PropertyResolverFactory.createJobPropertyResolver(isPartitionedStep);
        propResolver.substituteProperties(jobModel, jobParameters);

        return NavigatorFactory.createJobNavigator(jobModel);
    }

    private static String getJobXml(JSLJob jobModel) {
        ModelSerializer<JSLJob> serializer = ModelSerializerFactory.createJobModelSerializer();
        return serializer.serializeModel(jobModel);
    }

    private static JobContextImpl getJobContext(Navigator jobNavigator) {
        JSLProperties jslProperties = new JSLProperties();
        if(jobNavigator.getJSL() != null && jobNavigator.getJSL() instanceof JSLJob) {
            jslProperties = ((JSLJob)jobNavigator.getJSL()).getProperties();
        }
        return new JobContextImpl(jobNavigator.getId(), jslProperties); 
    }

    private static JobInstance getNewJobInstance(String name, String jobXml, Properties jobParameters) {
        String apptag = _batchKernelService.getBatchSecurityHelper().getCurrentTag();
        return _persistenceManagementService.createJobInstance(name, apptag, jobXml, jobParameters);
    }

    private static RuntimeJobExecutionHelper getNewJobExecution(Navigator jobNavigator, JobInstance jobInstance, Properties jobParameters, JobContextImpl jobContext) {
        return _persistenceManagementService.createJobExecution(jobNavigator, jobInstance, jobParameters, jobContext);
    }

    private static JobStatus createNewJobStatus(long instanceId) {
        return _jobStatusManagerService.createJobStatus(instanceId);
    }

    private static void validateAbstractJobDoNotStart(JSLJob jobModel)
            throws JobStartException {
        if (jobModel.getAbstract() != null && jobModel.getAbstract().equalsIgnoreCase("true")) {
            throw new JobStartException("An abstract job is NOT executable.");
        }
    }

    private static void validateRestartableFalseJobsDoNotRestart(JSLJob jobModel)
            throws JobRestartException {
        if (jobModel.getRestartable() != null && jobModel.getRestartable().equalsIgnoreCase("false")) {
            throw new JobRestartException("Job Restartable attribute is false, Job cannot be restarted.");
        }
    }
    
    public static RuntimeJobExecutionHelper startJob(String jobXML, Properties jobParameters) throws JobStartException {
        logger.entering(CLASSNAME, "startJob", new Object[]{jobXML, jobParameters});

        JSLJob jobModel = ModelResolverFactory.createJobResolver().resolveModel(jobXML); 

        RuntimeJobExecutionHelper jobExecution = startJob(jobModel, jobParameters, false);

        logger.exiting(CLASSNAME, "startJob", jobExecution);

        return jobExecution;
    }

    public static RuntimeJobExecutionHelper startJob(JSLJob jobModel, Properties jobParameters, boolean isPartitionedStep) throws JobStartException{

        logger.entering(CLASSNAME, "startJob", new Object[]{jobModel, jobParameters, isPartitionedStep});

        validateAbstractJobDoNotStart(jobModel);

        String jobXML = getJobXml(jobModel);

        Navigator jobNavigator = getResolvedJobNavigator(jobXML, jobParameters, isPartitionedStep);

        JobContextImpl jobContext = getJobContext(jobNavigator);

        JobInstance jobInstance = getNewJobInstance(jobNavigator.getId(), jobXML, jobParameters);

        RuntimeJobExecutionHelper jobExecution = getNewJobExecution(jobNavigator, jobInstance, jobParameters, jobContext);

        JobStatus jobStatus = createNewJobStatus(jobInstance.getInstanceId());
        jobStatus.setJobInstance(jobInstance);
        _jobStatusManagerService.updateJobStatus(jobStatus);
        
        logger.exiting(CLASSNAME, "startJob", jobExecution);
        return jobExecution;
    }


    public static RuntimeJobExecutionHelper restartJob(long executionId) throws JobRestartException {
    	
        return restartJob(executionId, null, false);
    }
    
    public static RuntimeJobExecutionHelper restartJob(long executionId, Properties restartJobParameters, boolean isPartitionedStep) throws JobRestartException {

    	long jobInstanceId = _persistenceManagementService.getJobInstanceIdByExecutionId(executionId);
    	
        JobStatus jobStatus = _jobStatusManagerService.getJobStatus(jobInstanceId);

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("On restartJob with jobInstance Id = " + jobInstanceId + " , found JobStatus: " + jobStatus ); 
        }

        JobInstanceImpl jobInstance = jobStatus.getJobInstance();

        Navigator jobNavigator = getResolvedJobNavigator(jobInstance.getJobXML(), restartJobParameters, isPartitionedStep);
        JSLJob jobModel = ModelResolverFactory.createJobResolver().resolveModel(jobInstance.getJobXML());
        validateRestartableFalseJobsDoNotRestart(jobModel);

        JobContextImpl jobContext = getJobContext(jobNavigator);
        
        RuntimeJobExecutionHelper jobExecution = getNewJobExecution(jobNavigator, jobInstance, restartJobParameters, jobContext);
        
        _jobStatusManagerService.updateJobStatusWithNewExecution(jobInstance.getInstanceId(), jobExecution.getExecutionId());        
        
        return jobExecution;
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
    
    public static StepExecution getStepExecutionIDInfo(long stepexecutionId){
    	return _persistenceManagementService.getStepExecutionObjQueryByStepID(stepexecutionId);
    }
    
    public static List<StepExecution<?>> getstepExecutionIDInfoList(long jobexecutionId){
    	return _persistenceManagementService.getStepExecutionIDListQueryByJobID(jobexecutionId);
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
    