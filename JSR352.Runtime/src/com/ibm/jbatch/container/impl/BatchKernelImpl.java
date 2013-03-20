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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
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

import com.ibm.jbatch.container.callback.IJobEndCallbackService;
import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.jobinstance.JobExecutionHelper;
import com.ibm.jbatch.container.jobinstance.RuntimeJobContextJobExecutionBridge;
import com.ibm.jbatch.container.services.IBatchKernelService;
import com.ibm.jbatch.container.services.IJobExecution;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.services.impl.JSEBatchSecurityHelper;
import com.ibm.jbatch.container.services.impl.RuntimeBatchJobUtil;
import com.ibm.jbatch.container.servicesmanager.ServicesManager;
import com.ibm.jbatch.container.servicesmanager.ServicesManagerImpl;
import com.ibm.jbatch.container.util.BatchWorkUnit;
import com.ibm.jbatch.container.util.PartitionDataWrapper;
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

	private Map<Long, JobControllerImpl> instanceId2jobControllerMap = new ConcurrentHashMap<Long, JobControllerImpl>();
	private Map<Long, RuntimeJobContextJobExecutionBridge> jobExecutionInstancesMap = new ConcurrentHashMap<Long, RuntimeJobContextJobExecutionBridge>();

	ServicesManager servicesManager = ServicesManagerImpl.getInstance();

	private IBatchThreadPoolService executorService = null;

	private IJobEndCallbackService callbackService = null;

	private IPersistenceManagerService persistenceService = null;

	private BatchSecurityHelper batchSecurity = null;

	private BatchJobUtil batchJobUtil = null;

	// TODO - assuming we have a IBatchConfig, maybe we should get size from
	// there.
	public final static int THREAD_POOL_SIZE = 5;

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
		if(batchSecurity == null) { 
			batchSecurity = new JSEBatchSecurityHelper();
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
			logger.entering(sourceClass, method, new Object[] { jobXML, jobParameters });
		}

		RuntimeJobContextJobExecutionBridge jobExecution = JobExecutionHelper.startJob(jobXML, jobParameters);

		// TODO - register with status manager

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("JobExecution constructed: " + jobExecution);
		}

		BatchWorkUnit batchWork = new BatchWorkUnit(this, jobExecution);
		this.instanceId2jobControllerMap.put(jobExecution.getInstanceId(), batchWork.getController());
		//AJM: not needed anymore : jobExecutionInstancesMap.put(jobExecution.getExecutionId(), jobExecution);

		executorService.executeTask(batchWork, null);

		if (logger.isLoggable(Level.FINER)) {
			logger.exiting(sourceClass, method, jobExecution);
		}

		return jobExecution.getJobOperatorJobExecution();
	}

	@Override
	public void stopJob(long executionId) throws NoSuchJobExecutionException, JobExecutionNotRunningException {

		long jobInstanceId = persistenceService.jobOperatorQueryJobExecutionJobInstanceId(executionId);

		JobControllerImpl controller = this.instanceId2jobControllerMap.get(jobInstanceId);
		if (controller == null) {
			//TODO - should  be more discriminating w/ exception  to see if it's just not running or if it doesn't exist at all in the DB !
			String msg = "JobExecution with execution id of " + jobInstanceId + "is not running.";
			logger.warning("stopJob(): " + msg);
			throw new JobExecutionNotRunningException(msg);
		}

		controller.stop();
	}

	@Override
	public IJobExecution restartJob(long executionId) throws JobRestartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException {
		String method = "restartJob";

		if (logger.isLoggable(Level.FINER)) {
			logger.entering(sourceClass, method);
		}

		Properties dummyPropObj = new Properties();
		return this.restartJob(executionId, dummyPropObj);
	}

	@Override
	public IJobExecution restartJob(long executionId, Properties jobOverrideProps) throws JobRestartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException {
		String method = "restartJob";

		if (logger.isLoggable(Level.FINER)) {
			logger.entering(sourceClass, method);
		}

		RuntimeJobContextJobExecutionBridge jobExecution = JobExecutionHelper.restartJob(executionId, null, jobOverrideProps, false);

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("JobExecution constructed: " + jobExecution);
		}

		BatchWorkUnit batchWork = new BatchWorkUnit(this, jobExecution);
		this.instanceId2jobControllerMap.put(jobExecution.getInstanceId(), batchWork.getController());
		//AJM not needed any longer jobExecutionInstancesMap.put(jobExecution.getExecutionId(), jobExecution);

		executorService.executeTask(batchWork, null);

		if (logger.isLoggable(Level.FINER)) {
			logger.exiting(sourceClass, method, jobExecution);
		}

		return jobExecution.getJobOperatorJobExecution();
	}

	@Override
	public void jobExecutionDone(RuntimeJobContextJobExecutionBridge jobExecution) {

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("JobExecution done with batchStatus: " + jobExecution.getBatchStatus() + " , getting ready to invoke callbacks for JobExecution: " + jobExecution.getExecutionId());
		}

		callbackService.done(jobExecution.getExecutionId());

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Done invoking callbacks for JobExecution: " + jobExecution.getExecutionId());
		}

		// Remove from map after job is done        
		this.instanceId2jobControllerMap.remove(jobExecution.getInstanceId());

		// AJM: ah - purge jobExecution from map here and flush to DB?
		// edit: no long want a 2 tier for the jobexecution...do want it for step execution
		// renamed method to flushAndRemoveStepExecution

	}

	public IJobExecution getJobExecution(long executionId) {
		// this method will retrieve a 'live' JobOperatorJobExecutionImpl from the in memory map OR
		// hydrate a previously run job's JobOperatorJobExecutionImpl from the backing store ...

		logger.finest("Entering " + sourceClass + ".getJobExecution(), executionId = " + executionId);

		IJobExecution retVal = null;

		retVal = JobExecutionHelper.getPersistedJobOperatorJobExecution(executionId);

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


	/**
	 * Build a batch work unit and set it up in STARTING state but don't start it yet.
	 * 
	 * @param jobModel
	 * @param partitionProps
	 * @param analyzerQueue
	 * @param subJobExitStatusQueue
	 * @param completedQueue
	 * @return
	 */
	@Override
	public BatchWorkUnit buildNewBatchWorkUnit(JSLJob jobModel, Properties partitionProps,
			BlockingQueue<PartitionDataWrapper> analyzerQueue, BlockingQueue<BatchWorkUnit> completedQueue, RuntimeJobContextJobExecutionBridge rootJobExecution) throws JobStartException {
		String method = "buildBatchWorkUnit";

		if (logger.isLoggable(Level.FINER)) {
			logger.entering(sourceClass, method, new Object[] { jobModel, partitionProps });
		}

		RuntimeJobContextJobExecutionBridge jobExecution = JobExecutionHelper.startJob(jobModel, partitionProps, true);

		// TODO - register with status manager

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("JobExecution constructed: " + jobExecution);
		}

		BatchWorkUnit batchWork = new BatchWorkUnit(this, jobExecution, analyzerQueue, completedQueue, rootJobExecution, false);
		this.instanceId2jobControllerMap.put(jobExecution.getInstanceId(), batchWork.getController());
		// re-enabling while i work out the persistence
		jobExecutionInstancesMap.put(jobExecution.getExecutionId(), jobExecution);

		return batchWork;
	}



	@Override
	public List<BatchWorkUnit> buildNewParallelJobs(List<JSLJob> jobModels,
			Properties[] partitionProperties,
			BlockingQueue<PartitionDataWrapper> analyzerQueue, 
			BlockingQueue<BatchWorkUnit> completedQueue, RuntimeJobContextJobExecutionBridge rootJobExecution) 
					throws JobRestartException, JobStartException {
		List<BatchWorkUnit> batchWorkUnits = new ArrayList<BatchWorkUnit>(jobModels.size());

		//for now let always use a Properties array. We can add some more convenience methods later for null properties and what not

		int instance = 0;
		for (JSLJob parallelJob  : jobModels){


			Properties partitionProps = (partitionProperties == null) ? null : partitionProperties[instance];    

			BatchWorkUnit batchWork = this.buildNewBatchWorkUnit(parallelJob, partitionProps, analyzerQueue, 
					completedQueue, rootJobExecution);
			batchWorkUnits.add(batchWork);

			instance++;
		}

		return batchWorkUnits;

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


	@Override
	public List<BatchWorkUnit> buildRestartableParallelJobs(List<JSLJob> jobModels,
			Properties[] partitionProperties,
			BlockingQueue<PartitionDataWrapper> analyzerQueue, 
			BlockingQueue<BatchWorkUnit> completedQueue, 
			RuntimeJobContextJobExecutionBridge rootJobExecution) throws JobRestartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException {
		List<BatchWorkUnit> batchWorkUnits = new ArrayList<BatchWorkUnit>(jobModels.size());

		//for now let always use a Properties array. We can add some more convenience methods later for null properties and what not

		int instance = 0;
		for (JSLJob parallelJob  : jobModels){


			Properties partitionProps = (partitionProperties == null) ? null : partitionProperties[instance];    

			try {
				BatchWorkUnit batchWork = this.buildRestartableBatchWorkUnit(parallelJob, partitionProps, analyzerQueue, 
					completedQueue, rootJobExecution);
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
	public BatchWorkUnit buildRestartableBatchWorkUnit(JSLJob jobModel, Properties partitionProps,
			BlockingQueue<PartitionDataWrapper> analyzerQueue, BlockingQueue<BatchWorkUnit> completedQueue, 
			RuntimeJobContextJobExecutionBridge rootJobExecution) throws JobRestartException, JobExecutionAlreadyCompleteException, JobExecutionNotMostRecentException {
		String method = "restartGeneratedJob";

		if (logger.isLoggable(Level.FINER)) {
			logger.entering(sourceClass, method, new Object[] { jobModel, partitionProps });
		}

		//There can only be one instance associated with a subjob's id since it is generated from an unique
		//job instance id. So there should be no way to directly start a subjob with particular
		List<Long> instanceIds = persistenceService.jobOperatorGetJobInstanceIds(jobModel.getId(), 0, 2);

		assert(instanceIds.size() == 1);

		List<IJobExecution> partitionExecs = persistenceService.jobOperatorGetJobExecutions(instanceIds.get(0));

		Long execId = Long.MIN_VALUE;

		for (IJobExecution partitionExec : partitionExecs ) {
			if (partitionExec.getExecutionId() > execId ) {
				execId = partitionExec.getExecutionId();
			}
		}

		RuntimeJobContextJobExecutionBridge jobExecution = JobExecutionHelper.restartJob(execId, jobModel, partitionProps, true);
			

		// TODO - register with status manager

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("JobExecution constructed: " + jobExecution);
		}

		BatchWorkUnit batchWork = new BatchWorkUnit(this, jobExecution, analyzerQueue, completedQueue, rootJobExecution, false);
		this.instanceId2jobControllerMap.put(jobExecution.getInstanceId(), batchWork.getController());
		// re-enabling while i work out the persistence
		jobExecutionInstancesMap.put(jobExecution.getExecutionId(), jobExecution);


		return batchWork;

	}

}
