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

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.batch.operations.JobExecutionNotMostRecentException;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.JobStartException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobInstance;

import com.ibm.jbatch.container.context.impl.JobContextImpl;
import com.ibm.jbatch.container.jsl.ModelNavigator;
import com.ibm.jbatch.container.jsl.ModelResolverFactory;
import com.ibm.jbatch.container.jsl.NavigatorFactory;
import com.ibm.jbatch.container.modelresolver.PropertyResolver;
import com.ibm.jbatch.container.modelresolver.PropertyResolverFactory;
import com.ibm.jbatch.container.services.IBatchKernelService;
import com.ibm.jbatch.container.services.IJobExecution;
import com.ibm.jbatch.container.services.IJobStatusManagerService;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
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


	private static ModelNavigator<JSLJob> getResolvedJobNavigator(String jobXml, Properties jobParameters, boolean parallelExecution) {

		JSLJob jobModel = ModelResolverFactory.createJobResolver().resolveModel(jobXml); 
		PropertyResolver<JSLJob> propResolver = PropertyResolverFactory.createJobPropertyResolver(parallelExecution);
		propResolver.substituteProperties(jobModel, jobParameters);

		return NavigatorFactory.createJobNavigator(jobModel);
	}

	private static ModelNavigator<JSLJob> getResolvedJobNavigator(JSLJob jobModel, Properties jobParameters, boolean parallelExecution) {

		PropertyResolver<JSLJob> propResolver = PropertyResolverFactory.createJobPropertyResolver(parallelExecution);
		propResolver.substituteProperties(jobModel, jobParameters);

		return NavigatorFactory.createJobNavigator(jobModel);
	}

	private static JobContextImpl getJobContext(ModelNavigator<JSLJob> jobNavigator) {
		JSLProperties jslProperties = new JSLProperties();
		if(jobNavigator.getRootModelElement() != null) {
			jslProperties = jobNavigator.getRootModelElement().getProperties();
		}
		return new JobContextImpl(jobNavigator, jslProperties); 
	}

	private static JobInstance getNewJobInstance(String name, String jobXml) {
		String apptag = _batchKernelService.getBatchSecurityHelper().getCurrentTag();
		return _persistenceManagementService.createJobInstance(name, apptag, jobXml);
	}

	private static JobInstance getNewSubJobInstance(String name) {
		String apptag = _batchKernelService.getBatchSecurityHelper().getCurrentTag();
		return _persistenceManagementService.createSubJobInstance(name, apptag);
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

	public static RuntimeJobExecution startJob(String jobXML, Properties jobParameters) throws JobStartException {
		logger.entering(CLASSNAME, "startJob", new Object[]{jobXML, jobParameters==null ? "<null>" : jobParameters});

		JSLJob jobModel = ModelResolverFactory.createJobResolver().resolveModel(jobXML); 

		ModelNavigator<JSLJob> jobNavigator = getResolvedJobNavigator(jobModel, jobParameters, false);

		JobContextImpl jobContext = getJobContext(jobNavigator);

		JobInstance jobInstance = getNewJobInstance(jobNavigator.getRootModelElement().getId(), jobXML);

		RuntimeJobExecution executionHelper = 
				_persistenceManagementService.createJobExecution(jobInstance, jobParameters, jobContext.getBatchStatus());

		executionHelper.prepareForExecution(jobContext);

		JobStatus jobStatus = createNewJobStatus(jobInstance);
		_jobStatusManagerService.updateJobStatus(jobStatus);

		logger.exiting(CLASSNAME, "startJob", executionHelper);

		return executionHelper;
	}

	public static RuntimeFlowInSplitExecution startFlowInSplit(JSLJob jobModel) throws JobStartException{
		logger.entering(CLASSNAME, "startFlowInSplit", jobModel);

		ModelNavigator<JSLJob> jobNavigator = getResolvedJobNavigator(jobModel, null, true);
		JobContextImpl jobContext = getJobContext(jobNavigator);
		
		JobInstance jobInstance = getNewSubJobInstance(jobNavigator.getRootModelElement().getId());

		RuntimeFlowInSplitExecution executionHelper = 
				_persistenceManagementService.createFlowInSplitExecution(jobInstance, jobContext.getBatchStatus());

		executionHelper.prepareForExecution(jobContext);

		JobStatus jobStatus = createNewJobStatus(jobInstance);
		_jobStatusManagerService.updateJobStatus(jobStatus);

		logger.exiting(CLASSNAME, "startFlowInSplit", executionHelper);
		return executionHelper;
	}
	
	public static RuntimeJobExecution startPartition(JSLJob jobModel, Properties jobParameters) throws JobStartException{
		logger.entering(CLASSNAME, "startPartition", new Object[]{jobModel, jobParameters ==null ? "<null>" :jobParameters});

		ModelNavigator<JSLJob> jobNavigator = getResolvedJobNavigator(jobModel, jobParameters, true);
		JobContextImpl jobContext = getJobContext(jobNavigator);
		
		JobInstance jobInstance = getNewSubJobInstance(jobNavigator.getRootModelElement().getId());

		RuntimeJobExecution executionHelper = 
				_persistenceManagementService.createJobExecution(jobInstance, jobParameters, jobContext.getBatchStatus());

		executionHelper.prepareForExecution(jobContext);

		JobStatus jobStatus = createNewJobStatus(jobInstance);
		_jobStatusManagerService.updateJobStatus(jobStatus);

		logger.exiting(CLASSNAME, "startPartition", executionHelper);
		return executionHelper;
	}
	
	public static RuntimeJobExecution restartJob(long executionId, JSLJob gennedJobModel) throws JobRestartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException, NoSuchJobExecutionException {
		return restartExecution(executionId, null, null, false, false);
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
	
	public static RuntimeJobExecution restartPartition(long execId, JSLJob gennedJobModel, Properties partitionProps) throws JobRestartException, 
	JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException, NoSuchJobExecutionException {
		return restartExecution(execId, gennedJobModel, partitionProps, true, false);	
	}

	public static RuntimeFlowInSplitExecution restartFlowInSplit(long execId, JSLJob gennedJobModel) throws JobRestartException, 
	JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException, NoSuchJobExecutionException {
		return (RuntimeFlowInSplitExecution)restartExecution(execId, gennedJobModel, null, true, true);	
	}
	
	public static RuntimeJobExecution restartJob(long executionId, Properties restartJobParameters) throws JobRestartException, 
	JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException, NoSuchJobExecutionException {
		return restartExecution(executionId, null, restartJobParameters, false, false);	
	}
	
	private static RuntimeJobExecution restartExecution(long executionId, JSLJob gennedJobModel, Properties restartJobParameters, boolean parallelExecution, boolean flowInSplit) throws JobRestartException, 
	JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException, NoSuchJobExecutionException {

		long jobInstanceId = _persistenceManagementService.getJobInstanceIdByExecutionId(executionId);

		JobStatus jobStatus = _jobStatusManagerService.getJobStatus(jobInstanceId);

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("On restartJob with jobInstance Id = " + jobInstanceId + " , found JobStatus: " + jobStatus + 
					", batchStatus = " + jobStatus.getBatchStatus().name() ); 
		}

		validateJobExecutionIsMostRecent(jobInstanceId, executionId);

		validateJobInstanceNotCompleteOrAbandonded(jobStatus);

		JobInstanceImpl jobInstance = jobStatus.getJobInstance();

		ModelNavigator<JSLJob> jobNavigator = null;

		// If we are in a parallel job that is genned use the regenned JSL.
		if (gennedJobModel == null) {
			jobNavigator = getResolvedJobNavigator(jobInstance.getJobXML(), restartJobParameters, parallelExecution);
		} else {
			jobNavigator = getResolvedJobNavigator(gennedJobModel, restartJobParameters, parallelExecution);
		}
		// JSLJob jobModel = ModelResolverFactory.createJobResolver().resolveModel(jobInstance.getJobXML());
		validateRestartableFalseJobsDoNotRestart(jobNavigator.getRootModelElement());

		JobContextImpl jobContext = getJobContext(jobNavigator);
		
		RuntimeJobExecution executionHelper;
		if (flowInSplit) {
			executionHelper = _persistenceManagementService.createFlowInSplitExecution(jobInstance, jobContext.getBatchStatus());
		} else {
			executionHelper = _persistenceManagementService.createJobExecution(jobInstance, restartJobParameters, jobContext.getBatchStatus());
		}
		executionHelper.prepareForExecution(jobContext, jobStatus.getRestartOn());
		_jobStatusManagerService.updateJobStatusWithNewExecution(jobInstance.getInstanceId(), executionHelper.getExecutionId());        

		return executionHelper;
	}    

	public static IJobExecution getPersistedJobOperatorJobExecution(long jobExecutionId) throws NoSuchJobExecutionException {
		return _persistenceManagementService.jobOperatorGetJobExecution(jobExecutionId);
	}

	
	public static JobInstance getJobInstance(long executionId){
		JobStatus jobStatus = _jobStatusManagerService.getJobStatusFromExecutionId(executionId);
		JobInstanceImpl jobInstance = jobStatus.getJobInstance();
		return jobInstance;
	}
}
