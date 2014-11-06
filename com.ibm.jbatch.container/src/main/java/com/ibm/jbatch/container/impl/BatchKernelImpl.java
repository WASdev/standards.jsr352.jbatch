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
package com.ibm.jbatch.container.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.batch.operations.JobExecutionNotMostRecentException;
import javax.batch.operations.JobExecutionNotRunningException;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.JobStartException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.runtime.JobInstance;

import com.ibm.jbatch.container.IThreadRootController;
import com.ibm.jbatch.container.callback.IJobEndCallbackService;
import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.jobinstance.JobExecutionHelper;
import com.ibm.jbatch.container.jobinstance.RuntimeFlowInSplitExecution;
import com.ibm.jbatch.container.jobinstance.RuntimeJobExecution;
import com.ibm.jbatch.container.services.IBatchKernelService;
import com.ibm.jbatch.container.services.IJobExecution;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.services.impl.NoOpBatchSecurityHelper;
import com.ibm.jbatch.container.services.impl.RuntimeBatchJobUtil;
import com.ibm.jbatch.container.servicesmanager.ServicesManager;
import com.ibm.jbatch.container.servicesmanager.ServicesManagerImpl;
import com.ibm.jbatch.container.util.BatchFlowInSplitWorkUnit;
import com.ibm.jbatch.container.util.BatchPartitionWorkUnit;
import com.ibm.jbatch.container.util.BatchWorkUnit;
import com.ibm.jbatch.container.util.FlowInSplitBuilderConfig;
import com.ibm.jbatch.container.util.PartitionsBuilderConfig;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.jbatch.spi.BatchJobUtil;
import com.ibm.jbatch.spi.BatchSPIManager;
import com.ibm.jbatch.spi.BatchSecurityHelper;
import com.ibm.jbatch.spi.services.IBatchConfig;
import com.ibm.jbatch.spi.services.IBatchThreadPoolService;
import com.ibm.jbatch.spi.services.ParallelTaskResult;

public class BatchKernelImpl implements IBatchKernelService {

	private final static String sourceClass = BatchKernelImpl.class.getName();
	private final static Logger logger = Logger.getLogger(sourceClass);

	private Map<Long, IThreadRootController> executionId2jobControllerMap = new ConcurrentHashMap<Long, IThreadRootController>();
	private Set<Long> instanceIdExecutingSet = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());

	ServicesManager servicesManager = ServicesManagerImpl.getInstance();

	private IBatchThreadPoolService executorService = null;

	private IJobEndCallbackService callbackService = null;

	private IPersistenceManagerService persistenceService = null;

	private BatchSecurityHelper batchSecurity = null;

	private BatchJobUtil batchJobUtil = null;

	public BatchKernelImpl() {
		executorService = servicesManager.getThreadPoolService();
		callbackService = servicesManager.getJobCallbackService();
		persistenceService = servicesManager.getPersistenceManagerService();

		// registering our implementation of the util class used to purge by apptag
		batchJobUtil = new RuntimeBatchJobUtil();
		BatchSPIManager.getInstance().registerBatchJobUtil(batchJobUtil);
	}

	public BatchSecurityHelper getBatchSecurityHelper() {
		batchSecurity = BatchSPIManager.getInstance().getBatchSecurityHelper();
		if (batchSecurity == null) { 
			batchSecurity = new NoOpBatchSecurityHelper();
		}
		return batchSecurity;
	}

	public void init(IBatchConfig pgcConfig) throws BatchContainerServiceException {
	}

	@Override
	public void shutdown() throws BatchContainerServiceException {
		// TODO Auto-generated method stub

	}

	@Override
	public IJobExecution startJob(String jobXML) throws JobStartException {
		return startJob(jobXML, null);
	}

	@Override
	public IJobExecution startJob(String jobXML, Properties jobParameters) throws JobStartException {
		String method = "startJob";

		if (logger.isLoggable(Level.FINER)) {
			logger.entering(sourceClass, method, new Object[] { jobXML, jobParameters != null ? jobParameters : "<null>" });
		}

		RuntimeJobExecution jobExecution = JobExecutionHelper.startJob(jobXML, jobParameters);

		// TODO - register with status manager

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("JobExecution constructed: " + jobExecution);
		}

		BatchWorkUnit batchWork = new BatchWorkUnit(this, jobExecution);
		registerCurrentInstanceAndExecution(jobExecution, batchWork.getController());

		executorService.executeTask(batchWork, null);

		if (logger.isLoggable(Level.FINER)) {
			logger.exiting(sourceClass, method, jobExecution);
		}

		return jobExecution.getJobOperatorJobExecution();
	}

	@Override
	public void stopJob(long executionId) throws NoSuchJobExecutionException, JobExecutionNotRunningException {

		IThreadRootController controller = this.executionId2jobControllerMap.get(executionId);
		if (controller == null) {
			String msg = "JobExecution with execution id of " + executionId + "is not running.";
			logger.warning("stopJob(): " + msg);
			throw new JobExecutionNotRunningException(msg);
		}
		controller.stop();
	}

	@Override
	public IJobExecution restartJob(long executionId) throws JobRestartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException, NoSuchJobExecutionException {
		String method = "restartJob";

		if (logger.isLoggable(Level.FINER)) {
			logger.entering(sourceClass, method);
		}

		Properties dummyPropObj = new Properties();
		return this.restartJob(executionId, dummyPropObj);
	}




	@Override
	public IJobExecution restartJob(long executionId, Properties jobOverrideProps) throws JobRestartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException, NoSuchJobExecutionException {
		String method = "restartJob";

		if (logger.isLoggable(Level.FINER)) {
			logger.entering(sourceClass, method);
		}

		RuntimeJobExecution jobExecution = 
				JobExecutionHelper.restartJob(executionId, jobOverrideProps);

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("JobExecution constructed: " + jobExecution);
		}

		BatchWorkUnit batchWork = new BatchWorkUnit(this, jobExecution);

		registerCurrentInstanceAndExecution(jobExecution, batchWork.getController());

		executorService.executeTask(batchWork, null);

		if (logger.isLoggable(Level.FINER)) {
			logger.exiting(sourceClass, method, jobExecution);
		}

		return jobExecution.getJobOperatorJobExecution();
	}

	@Override
	public void jobExecutionDone(RuntimeJobExecution jobExecution) {

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("JobExecution done with batchStatus: " + jobExecution.getBatchStatus() + " , getting ready to invoke callbacks for JobExecution: " + jobExecution.getExecutionId());
		}

		callbackService.done(jobExecution.getExecutionId());

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Done invoking callbacks for JobExecution: " + jobExecution.getExecutionId());
		}

		// Remove from executionId, instanceId map,set after job is done        
		this.executionId2jobControllerMap.remove(jobExecution.getExecutionId());
		this.instanceIdExecutingSet.remove(jobExecution.getInstanceId());

		// AJM: ah - purge jobExecution from map here and flush to DB?
		// edit: no long want a 2 tier for the jobexecution...do want it for step execution
		// renamed method to flushAndRemoveStepExecution

	}

	public IJobExecution getJobExecution(long executionId) throws NoSuchJobExecutionException {
		/*
		 *  Keep logging on finest for apps like TCK which do polling
		 */
		logger.finest("Entering " + sourceClass + ".getJobExecution(), executionId = " + executionId);
		IJobExecution retVal = JobExecutionHelper.getPersistedJobOperatorJobExecution(executionId);

		logger.finest("Exiting " + sourceClass + ".getJobExecution(), retVal = " + retVal);
		return retVal;
	}

	@Override
	public void startGeneratedJob(BatchWorkUnit batchWork) {
		String method = "startGeneratedJob";

		if (logger.isLoggable(Level.FINER)) {
			logger.entering(sourceClass, method, new Object[] { batchWork });
		}

		//This call is non-blocking
		ParallelTaskResult result = executorService.executeParallelTask(batchWork, null);

		if (logger.isLoggable(Level.FINER)) {
			logger.exiting(sourceClass, method, new Object[] { batchWork });
		}
	}

	@Override
	public int getJobInstanceCount(String jobName) {
		int jobInstanceCount = 0;

		jobInstanceCount = persistenceService.jobOperatorGetJobInstanceCount(jobName);

		return jobInstanceCount;
	}

	@Override
	public JobInstance getJobInstance(long executionId){
		return JobExecutionHelper.getJobInstance(executionId);
	}


	/**
	 * Build a list of batch work units and set them up in STARTING state but don't start them yet.
	 */

	@Override
	public List<BatchPartitionWorkUnit> buildNewParallelPartitions(PartitionsBuilderConfig config) 
			throws JobRestartException, JobStartException {

		List<JSLJob> jobModels = config.getJobModels();
		Properties[] partitionPropertiesArray = config.getPartitionProperties();

		List<BatchPartitionWorkUnit> batchWorkUnits = new ArrayList<BatchPartitionWorkUnit>(jobModels.size());

		int instance = 0;
		for (JSLJob parallelJob  : jobModels){
			Properties partitionProps = (partitionPropertiesArray == null) ? null : partitionPropertiesArray[instance];    			

			if (logger.isLoggable(Level.FINER)) {
				logger.finer("Starting execution for jobModel = " + parallelJob.toString());
			}
			RuntimeJobExecution jobExecution = JobExecutionHelper.startPartition(parallelJob, partitionProps);
			jobExecution.setPartitionInstance(instance);

			if (logger.isLoggable(Level.FINE)) {
				logger.fine("JobExecution constructed: " + jobExecution);
			}
			BatchPartitionWorkUnit batchWork = new BatchPartitionWorkUnit(this, jobExecution, config);

			registerCurrentInstanceAndExecution(jobExecution, batchWork.getController());
			
			batchWorkUnits.add(batchWork);
			instance++;
		}

		return batchWorkUnits;
	}

	@Override
	public List<BatchPartitionWorkUnit> buildOnRestartParallelPartitions(PartitionsBuilderConfig config) throws JobRestartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException {

		List<JSLJob> jobModels = config.getJobModels();
		Properties[] partitionProperties = config.getPartitionProperties();

		List<BatchPartitionWorkUnit> batchWorkUnits = new ArrayList<BatchPartitionWorkUnit>(jobModels.size());

		//for now let always use a Properties array. We can add some more convenience methods later for null properties and what not

		int instance = 0;
		for (JSLJob parallelJob  : jobModels){

			Properties partitionProps = (partitionProperties == null) ? null : partitionProperties[instance];    

			try {
				long execId = getMostRecentExecutionId(parallelJob);

				RuntimeJobExecution jobExecution = null;
				try {		
					jobExecution = JobExecutionHelper.restartPartition(execId, parallelJob, partitionProps);
					jobExecution.setPartitionInstance(instance);
				} catch (NoSuchJobExecutionException e) {
					String errorMsg = "Caught NoSuchJobExecutionException but this is an internal JobExecution so this shouldn't have happened: execId =" + execId;
					logger.severe(errorMsg);
					throw new IllegalStateException(errorMsg, e);
				}

				if (logger.isLoggable(Level.FINE)) {
					logger.fine("JobExecution constructed: " + jobExecution);
				}

				BatchPartitionWorkUnit batchWork = new BatchPartitionWorkUnit(this, jobExecution, config);
				registerCurrentInstanceAndExecution(jobExecution, batchWork.getController());

				batchWorkUnits.add(batchWork);
			} catch (JobExecutionAlreadyCompleteException e) {
				logger.fine("This execution already completed: " + parallelJob.getId());
			}

			instance++;
		}

		return batchWorkUnits;
	}

	@Override
	public void  restartGeneratedJob(BatchWorkUnit batchWork) throws JobRestartException {
		String method = "restartGeneratedJob";

		if (logger.isLoggable(Level.FINER)) {
			logger.entering(sourceClass, method, new Object[] { batchWork });
		}

		//This call is non-blocking
		ParallelTaskResult result = executorService.executeParallelTask(batchWork, null);

		if (logger.isLoggable(Level.FINER)) {
			logger.exiting(sourceClass, method, batchWork);
		}

	}

	@Override
	public BatchFlowInSplitWorkUnit buildNewFlowInSplitWorkUnit(FlowInSplitBuilderConfig config) {
		JSLJob parallelJob = config.getJobModel();

		RuntimeFlowInSplitExecution execution = JobExecutionHelper.startFlowInSplit(parallelJob);

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("JobExecution constructed: " + execution);
		}
		BatchFlowInSplitWorkUnit batchWork = new BatchFlowInSplitWorkUnit(this, execution, config);

		registerCurrentInstanceAndExecution(execution, batchWork.getController());
		return batchWork;
	}

	private long getMostRecentExecutionId(JSLJob jobModel) {

		//There can only be one instance associated with a subjob's id since it is generated from an unique
		//job instance id. So there should be no way to directly start a subjob with particular
		List<Long> instanceIds = persistenceService.jobOperatorGetJobInstanceIds(jobModel.getId(), 0, 2);

		// Maybe we should blow up on '0' too?
		if (instanceIds.size() > 1) {
			String errorMsg = "Found " + instanceIds.size() + " entries for instance id = " + jobModel.getId() + ", which should not have happened.  Blowing up."; 
			logger.severe(errorMsg);
			throw new IllegalStateException(errorMsg);
		}

		List<IJobExecution> subJobExecs = persistenceService.jobOperatorGetJobExecutions(instanceIds.get(0));

		Long execId = Long.MIN_VALUE;

		for (IJobExecution subJobExec : subJobExecs ) {
			if (subJobExec.getExecutionId() > execId ) {
				execId = subJobExec.getExecutionId();
			}
		}
		return execId;
	}

	@Override
	public BatchFlowInSplitWorkUnit buildOnRestartFlowInSplitWorkUnit(FlowInSplitBuilderConfig config)  
			throws JobRestartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException {

		String method = "buildOnRestartFlowInSplitWorkUnit";

		JSLJob jobModel = config.getJobModel();

		if (logger.isLoggable(Level.FINER)) {
			logger.entering(sourceClass, method, jobModel);
		}

		long execId = getMostRecentExecutionId(jobModel);

		RuntimeFlowInSplitExecution jobExecution = null;
		try {		
			jobExecution = JobExecutionHelper.restartFlowInSplit(execId, jobModel);
		} catch (NoSuchJobExecutionException e) {
			String errorMsg = "Caught NoSuchJobExecutionException but this is an internal JobExecution so this shouldn't have happened: execId =" + execId;
			logger.severe(errorMsg);
			throw new IllegalStateException(errorMsg, e);
		}

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("JobExecution constructed: " + jobExecution);
		}

		BatchFlowInSplitWorkUnit batchWork = new BatchFlowInSplitWorkUnit(this, jobExecution, config);
		
		registerCurrentInstanceAndExecution(jobExecution, batchWork.getController());
		return batchWork;
	}

	private void registerCurrentInstanceAndExecution(RuntimeJobExecution jobExecution, IThreadRootController controller) {
		long execId = jobExecution.getExecutionId();
		long instanceId = jobExecution.getInstanceId();
		String errorPrefix = "Tried to execute with Job executionId = " + execId + " and instanceId = " + instanceId + " ";
		if (executionId2jobControllerMap.get(execId) != null) {
			String errorMsg = errorPrefix + "but executionId is already currently executing.";
			logger.warning(errorMsg);
			throw new IllegalStateException(errorMsg);
		} else if (instanceIdExecutingSet.contains(instanceId)) {
			String errorMsg = errorPrefix + "but another execution with this instanceId is already currently executing.";
			logger.warning(errorMsg);
			throw new IllegalStateException(errorMsg);
		} else {
			instanceIdExecutingSet.add(instanceId);
			executionId2jobControllerMap.put(jobExecution.getExecutionId(), controller);
		}
	}
	
	@Override
	public boolean isExecutionRunning(long executionId) {
		return executionId2jobControllerMap.containsKey(executionId);

	}
}
