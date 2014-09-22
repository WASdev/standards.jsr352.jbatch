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
package com.ibm.jbatch.container.api.impl;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.batch.operations.JobExecutionIsRunningException;
import javax.batch.operations.JobExecutionNotMostRecentException;
import javax.batch.operations.JobExecutionNotRunningException;
import javax.batch.operations.JobOperator;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.JobSecurityException;
import javax.batch.operations.JobStartException;
import javax.batch.operations.NoSuchJobException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.operations.NoSuchJobInstanceException;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;

import com.ibm.jbatch.container.services.IBatchKernelService;
import com.ibm.jbatch.container.services.IJobExecution;
import com.ibm.jbatch.container.services.IJobStatusManagerService;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.servicesmanager.ServicesManager;
import com.ibm.jbatch.container.servicesmanager.ServicesManagerImpl;
import com.ibm.jbatch.container.status.JobStatus;
import com.ibm.jbatch.spi.BatchSecurityHelper;
import com.ibm.jbatch.spi.services.IJobXMLLoaderService;


public class JobOperatorImpl implements JobOperator {

	private final static String sourceClass = JobOperatorImpl.class.getName();
	private final static Logger logger = Logger.getLogger(sourceClass);

	private ServicesManager servicesManager = null; 
	private IBatchKernelService batchKernel = null;
	private IPersistenceManagerService persistenceService = null;
	private IJobXMLLoaderService jobXMLLoaderService = null;
	private IJobStatusManagerService _jobStatusManagerService = null;

	public JobOperatorImpl() {
		servicesManager = ServicesManagerImpl.getInstance();
		batchKernel = servicesManager.getBatchKernelService();
		persistenceService = servicesManager.getPersistenceManagerService();
		jobXMLLoaderService =  servicesManager.getDelegatingJobXMLLoaderService();
		_jobStatusManagerService = servicesManager.getJobStatusManagerService();   
	}

	@Override
	public long start(String jobXMLName, Properties jobParameters)	throws JobStartException, JobSecurityException {
		long retVal = 0L;
		/*
		 * The whole point of this method is to have JobStartException serve as a blanket exception for anything other 
		 * than the rest of the more specific exceptions declared on the throws clause.  So we won't log but just rethrow.
		 */
		try {
			retVal = startInternal(jobXMLName, jobParameters);
		} catch (JobSecurityException e) {
			throw e;
		} catch (Exception e) {
			throw new JobStartException(e);
		}
		return retVal;
	}
	
	private long startInternal(String jobXMLName, Properties jobParameters)	throws JobStartException, JobSecurityException {

		StringWriter jobParameterWriter = new StringWriter();
		if (jobParameters != null) {
			try {
				jobParameters.store(jobParameterWriter, "Job parameters on start: ");
			} catch (IOException e) {
				jobParameterWriter.write("Job parameters on start: not printable");
			}
		} else {
			jobParameterWriter.write("Job parameters on start = null");
		}

		if (logger.isLoggable(Level.FINE)) {            
			logger.fine("JobOperator start, with jobXMLName = " + jobXMLName + "\n" + jobParameterWriter.toString());
		}

		String jobXML = jobXMLLoaderService.loadJSL(jobXMLName);

		long executionId = 0;

		if (logger.isLoggable(Level.FINE)) {            
			int concatLen = jobXML.length() > 200 ? 200 : jobXML.length();
			logger.fine("Starting job: " + jobXML.substring(0, concatLen) + "... truncated ...");
		}

		IJobExecution execution = batchKernel.startJob(jobXML, jobParameters);
		executionId = execution.getExecutionId();

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Started job with instanceId: " + execution.getInstanceId() + ", executionId: " + executionId);
		}

		return executionId;
	}

	@Override
	public void abandon(long executionId)
			throws NoSuchJobExecutionException, JobExecutionIsRunningException, JobSecurityException {

		if (isAuthorized(persistenceService.getJobInstanceIdByExecutionId(executionId))) {
			IJobExecution jobEx = persistenceService.jobOperatorGetJobExecution(executionId);

			// if it is not in STARTED or STARTING state, mark it as ABANDONED
			if (!(jobEx.getBatchStatus().equals(BatchStatus.STARTED) || jobEx.getBatchStatus().equals(BatchStatus.STARTING))){
				// update table to reflect ABANDONED state
				long time = System.currentTimeMillis();
				Timestamp timestamp = new Timestamp(time);
				persistenceService.updateBatchStatusOnly(jobEx.getExecutionId(), BatchStatus.ABANDONED, timestamp);
				logger.fine("Job Execution: " + executionId + " was abandoned");

				// Don't forget to update JOBSTATUS table
				_jobStatusManagerService.updateJobBatchStatus(jobEx.getInstanceId(), BatchStatus.ABANDONED);
			}
			else {
				logger.warning("Job Execution: " + executionId + " is still running");
				throw new JobExecutionIsRunningException("Job Execution: " + executionId + " is still running");
			}
		} else {
			throw new JobSecurityException("The current user is not authorized to perform this operation");
		}

	}

	@Override
	public IJobExecution getJobExecution(long executionId)
			throws NoSuchJobExecutionException, JobSecurityException {
		if (isAuthorized(persistenceService.getJobInstanceIdByExecutionId(executionId))) {
			return batchKernel.getJobExecution(executionId);
		} else {
			throw new JobSecurityException("The current user is not authorized to perform this operation");
		}
	}

	@Override
	public List<JobExecution> getJobExecutions(JobInstance instance)
			throws NoSuchJobInstanceException, JobSecurityException {
		List<JobExecution> executions = new ArrayList<JobExecution>();

		if (isAuthorized(instance.getInstanceId())) {
			// Mediate between one 
			List<IJobExecution> executionImpls = persistenceService.jobOperatorGetJobExecutions(instance.getInstanceId());
			if (executionImpls.size() == 0 ){
				logger.warning("The current user is not authorized to perform this operation");
				throw new NoSuchJobInstanceException( "Job: " + instance.getJobName() + " does not exist");
			}
			for (IJobExecution e : executionImpls) {
				executions.add(e);
			}
		} else {
			logger.warning("The current user is not authorized to perform this operation");
			throw new JobSecurityException("The current user is not authorized to perform this operation");
		}

		return executions;
	}

	@Override
	public JobInstance getJobInstance(long executionId)
			throws NoSuchJobExecutionException, JobSecurityException {
		if (isAuthorized(persistenceService.getJobInstanceIdByExecutionId(executionId))) {
			return this.batchKernel.getJobInstance(executionId);
		} else {
			throw new JobSecurityException("The current user is not authorized to perform this operation");
		}
	}

	@Override
	public int getJobInstanceCount(String jobName) throws NoSuchJobException, JobSecurityException {

		int jobInstanceCount = 0;

		BatchSecurityHelper helper = getBatchSecurityHelper();
		
		if (isCurrentTagAdmin(helper)) {
			// Do an unfiltered query
			jobInstanceCount = persistenceService.jobOperatorGetJobInstanceCount(jobName);
		} else {
			jobInstanceCount = persistenceService.jobOperatorGetJobInstanceCount(jobName, helper.getCurrentTag());
		}

		if (jobInstanceCount > 0) {
			return jobInstanceCount;
		}
		else { 
			logger.fine("getJobInstanceCount: Job Name " + jobName + " not found");
			throw new NoSuchJobException( "Job " + jobName + " not found");
		}
	}

	@Override
	public List<JobInstance> getJobInstances(String jobName, int start,
			int count) throws NoSuchJobException, JobSecurityException {

		logger.entering(sourceClass, "getJobInstances", new Object[]{jobName, start, count});
		List<JobInstance> jobInstances = new ArrayList<JobInstance>();

		if (count == 0) {
			return new ArrayList<JobInstance>();
		} else if (count < 0) {
			throw new IllegalArgumentException("Count should be a positive integer (or 0, which will return an empty list)");
		}
		
		List<Long> instanceIds; 
		BatchSecurityHelper helper = getBatchSecurityHelper();
		if (isCurrentTagAdmin(helper)) {
			// Do an unfiltered query
			instanceIds	= persistenceService.jobOperatorGetJobInstanceIds(jobName, start, count);
		} else {
			instanceIds	= persistenceService.jobOperatorGetJobInstanceIds(jobName, helper.getCurrentTag(), start, count);
		}

		// get the jobinstance ids associated with this job name

		if (instanceIds.size() > 0){
			// for every job instance id
			for (long id : instanceIds){
				// get the job instance obj, add it to the list
				JobStatus jobStatus = this._jobStatusManagerService.getJobStatus(id);
				JobInstance jobInstance = jobStatus.getJobInstance();
				logger.finest("Matched jobInstance = " + jobInstance.getInstanceId());
				if(isAuthorized(jobInstance.getInstanceId())) {
					logger.finest("Authorized so adding to list jobInstance =  " + jobInstance.getInstanceId());
					jobInstances.add(jobInstance);	
				}
			}
			// send the list of objs back to caller
			logger.exiting(sourceClass, "getJobInstances", jobInstances);
			return jobInstances;
		} else {
			logger.fine("getJobInstances: Job Name " + jobName + " not found");
			throw new NoSuchJobException( "Job Name " + jobName + " not found");
		}
	}

	/*
	 * This should only be called by the "external" JobOperator API, since it filters
	 * out the "subjob" parallel execution entries.
	 */
	@Override
	public Set<String> getJobNames() throws JobSecurityException {

		Set<String> jobNames = new HashSet<String>();
		Map<Long, String> data = persistenceService.jobOperatorGetExternalJobInstanceData();
		Iterator<Map.Entry<Long,String>> it = data.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Long,String> entry = it.next();
			long instanceId = entry.getKey();
			if(isAuthorized(instanceId)) {
				String name = entry.getValue();
				jobNames.add(name);
			}
		}
		return jobNames;
	}

	@Override
	public Properties getParameters(long executionId)
			throws NoSuchJobExecutionException, JobSecurityException{

		Properties props = null;
		JobInstance requestedJobInstance = batchKernel.getJobInstance(executionId);

		if (isAuthorized(requestedJobInstance.getInstanceId())) {
			props = persistenceService.getParameters(executionId);
		} else {
			logger.warning("getParameters: The current user is not authorized to perform this operation");
			throw new JobSecurityException("The current user is not authorized to perform this operation");
		}

		return props;
	}


	@Override
	public List<Long> getRunningExecutions(String jobName)
			throws NoSuchJobException, JobSecurityException {

		logger.entering(sourceClass, "getRunningExecutions", jobName);
		List<Long> jobExecutions = new ArrayList<Long>();

		// get the jobexecution ids associated with this job name
		Set<Long> executionIds = persistenceService.jobOperatorGetRunningExecutions(jobName);

		if (executionIds.size() > 0){
			// for every job instance id
			for (long id : executionIds){
				try {
					logger.finer("Examining executionId: " + id);
					if(isAuthorized(persistenceService.getJobInstanceIdByExecutionId(id))) {
						if (batchKernel.isExecutionRunning(id)) {
							IJobExecution jobEx = batchKernel.getJobExecution(id);
							jobExecutions.add(jobEx.getExecutionId());
						} else {
							logger.finer("Found executionId: " + id + " with a BatchStatus indicating running, but kernel doesn't currently have an entry for this execution in the kernel's in-memory map.");
						}
					} else {
						logger.finer("Don't have authorization for executionId: " + id);
					}
				} catch (NoSuchJobExecutionException e) {
					String errorMsg = "Just found execution with id = " + id + " in table, but now seeing it as gone";
					logger.severe(errorMsg);
					throw new IllegalStateException(errorMsg, e);
				}
			}
			// send the list of objs back to caller
			logger.exiting(sourceClass, "getRunningExecutions", jobExecutions);
			return jobExecutions;
		}
		else { 
			logger.fine("getRunningExecutions: Job Name " + jobName + " not found");
			throw new NoSuchJobException( "Job Name " + jobName + " not found");
		}
	}

	@Override
	public List<StepExecution> getStepExecutions(long executionId)
			throws NoSuchJobExecutionException, JobSecurityException {

		logger.entering(sourceClass, "getStepExecutions", executionId);

		List<StepExecution> stepExecutions = new ArrayList<StepExecution>();

		IJobExecution jobEx = batchKernel.getJobExecution(executionId);
		if (jobEx == null){
			logger.fine("Job Execution: " + executionId + " not found");
			throw new NoSuchJobExecutionException("Job Execution: " + executionId + " not found");
		}
		if (isAuthorized(persistenceService.getJobInstanceIdByExecutionId(executionId))) {
			stepExecutions = persistenceService.getStepExecutionsForJobExecution(executionId);
		} else {
			logger.warning("getStepExecutions: The current user is not authorized to perform this operation");
			throw new JobSecurityException("The current user is not authorized to perform this operation");
		}

		logger.exiting(sourceClass, "getStepExecutions", stepExecutions);
		return stepExecutions;

	}

	@Override
	public long restart(long oldExecutionId, Properties restartParameters) throws JobExecutionAlreadyCompleteException,
	NoSuchJobExecutionException, JobExecutionNotMostRecentException, JobRestartException, JobSecurityException {
		/*
		 * The whole point of this method is to have JobRestartException serve as a blanket exception for anything other 
		 * than the rest of the more specific exceptions declared on the throws clause.  So we won't log but just rethrow.
		 */
		long retVal = 0L;
		try {
			retVal = restartInternal(oldExecutionId, restartParameters);
		} catch (JobExecutionAlreadyCompleteException e) {
			throw e;
		} catch (NoSuchJobExecutionException e) {
			throw e;
		} catch (JobExecutionNotMostRecentException e) {
			throw e; 
		} catch (JobSecurityException e) {
			throw e; 
		} catch (Exception e) {
			throw new JobRestartException(e);
		}
		
		return retVal;
	}
	
	private long restartInternal(long oldExecutionId, Properties restartParameters) throws JobExecutionAlreadyCompleteException,
	NoSuchJobExecutionException, JobExecutionNotMostRecentException, JobRestartException, JobSecurityException {
		long newExecutionId = -1;

		if (isAuthorized(persistenceService.getJobInstanceIdByExecutionId(oldExecutionId))) {
			StringWriter jobParameterWriter = new StringWriter();
			if (restartParameters != null) {
				try {
					restartParameters.store(jobParameterWriter, "Job parameters on restart: ");
				} catch (IOException e) {
					jobParameterWriter.write("Job parameters on restart: not printable");
				}
			} else {
				jobParameterWriter.write("Job parameters on restart = null");
			}

			if (logger.isLoggable(Level.FINE)) {            
				logger.fine("JobOperator restart, with old executionId = " + oldExecutionId + "\n" + jobParameterWriter.toString());
			}

			IJobExecution execution = batchKernel.restartJob(oldExecutionId, restartParameters);

			newExecutionId = execution.getExecutionId();

			if (logger.isLoggable(Level.FINE)) {            
				logger.fine("Restarted job with instanceID: " + execution.getInstanceId() + ", new executionId: " + newExecutionId + ", and old executionID: " + oldExecutionId);
			}
		} else {
			throw new JobSecurityException("The current user is not authorized to perform this operation");
		}

		return newExecutionId;
	}

	@Override
	public void stop(long executionId) throws NoSuchJobExecutionException,
	JobExecutionNotRunningException, JobSecurityException {

		logger.entering(sourceClass, "stop", executionId);

		if (isAuthorized(persistenceService.getJobInstanceIdByExecutionId(executionId))) {
			batchKernel.stopJob(executionId);
		} else {
			throw new JobSecurityException("The current user is not authorized to perform this operation");
		}

		logger.exiting(sourceClass, "stop");
	}

	public void purge(String apptag) {
		BatchSecurityHelper bsh = getBatchSecurityHelper();
		if (isCurrentTagAdmin(bsh)) {
			logger.finer("Current tag is admin, so authorized to purge.");
			persistenceService.purge(apptag);
		} else if (bsh.getCurrentTag().equals(apptag)) {
			logger.finer("Current tag is the tag of record so authorized to purge.");
			persistenceService.purge(apptag);
		} else {
			logger.finer("Current tag does not match the tag of record so will not purge.");
		}
	}

	private boolean isAuthorized(long instanceId) {
		logger.entering(sourceClass, "isAuthorized", instanceId);
		boolean retVal = false;
		
		String apptag = persistenceService.getJobCurrentTag(instanceId);
		
		BatchSecurityHelper bsh = getBatchSecurityHelper();
		if (isCurrentTagAdmin(bsh)) {
			logger.finer("Current tag is admin, so always authorized");
			retVal = true;
		} else if (bsh.getCurrentTag().equals(apptag)) { 
			logger.finer("Current tag is the tag of record");
			retVal = true;
		} else {
			logger.finer("Current tag does not match the tag of record");
			retVal = false;
		}
		
		logger.exiting(sourceClass, "isAuthorized", retVal);
		return retVal;
	}

	private boolean isCurrentTagAdmin(BatchSecurityHelper helper) {
		return helper.isAdmin(helper.getCurrentTag());
	}
	
	private BatchSecurityHelper getBatchSecurityHelper() {
		BatchSecurityHelper bsh = batchKernel.getBatchSecurityHelper();
		if (bsh == null) {
			throw new IllegalStateException("Expect to have a security helper, at least the NoOp security helper.");
		}
		return bsh;
	}
}