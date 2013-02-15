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
package com.ibm.batch.container.impl;

import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator.BatchStatus;
import javax.batch.operations.exception.JobRestartException;
import javax.batch.runtime.JobInstance;

import jsr352.batch.jsl.JSLProperties;
import jsr352.batch.jsl.Property;
import jsr352.batch.jsl.Step;

import com.ibm.batch.container.AbortedBeforeStartException;
import com.ibm.batch.container.IExecutionElementController;
import com.ibm.batch.container.artifact.proxy.PartitionCollectorProxy;
import com.ibm.batch.container.context.impl.MetricImpl;
import com.ibm.batch.container.context.impl.StepContextImpl;
import com.ibm.batch.container.exception.BatchContainerServiceException;
import com.ibm.batch.container.jobinstance.JobExecutionHelper;
import com.ibm.batch.container.jobinstance.RuntimeJobExecutionImpl;
import com.ibm.batch.container.jobinstance.StepExecutionImpl;
import com.ibm.batch.container.persistence.PersistentDataWrapper;
import com.ibm.batch.container.services.IJobIdManagementService;
import com.ibm.batch.container.services.IJobStatusManagerService;
import com.ibm.batch.container.services.ITransactionManagementService;
import com.ibm.batch.container.services.ServicesManager;
import com.ibm.batch.container.services.ServicesManager.ServiceType;
import com.ibm.batch.container.services.TransactionManagerAdatper;
import com.ibm.batch.container.status.StepStatus;
import com.ibm.batch.container.util.PartitionDataWrapper;
import com.ibm.batch.container.util.PartitionDataWrapper.PartitionEventType;

/** Change the name of this class to something else!! Or change BaseStepControllerImpl. */
public abstract class BaseStepControllerImpl implements IExecutionElementController {

    private final static String sourceClass = BatchletStepControllerImpl.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);

    protected RuntimeJobExecutionImpl jobExecutionImpl;
    protected JobInstance jobInstance;

    protected StepContextImpl<?, ? extends Externalizable> stepContext;
    protected Step step;
    protected StepStatus stepStatus;
    
    private Properties properties = new Properties();

    protected LinkedBlockingQueue<PartitionDataWrapper> analyzerQueue = null;

	protected PartitionCollectorProxy collectorProxy = null;
	
	protected static BatchKernelImpl batchKernel = (BatchKernelImpl) ServicesManager.getInstance().getService(ServiceType.BATCH_KERNEL_SERVICE);
	
    protected static IJobIdManagementService _jobIdManagementService = (IJobIdManagementService)ServicesManager.getInstance().getService(ServiceType.JOB_ID_MANAGEMENT_SERVICE);
    
    protected TransactionManagerAdatper	transactionManager = null;
    
    private enum RunOnRestart {
        ALREADY_COMPLETE, RUN
    };
    
    
    
    private static IJobStatusManagerService _jobStatusService = (IJobStatusManagerService) ServicesManager.getInstance().getService(
            ServiceType.JOB_STATUS_MANAGEMENT_SERVICE);

    protected BaseStepControllerImpl(RuntimeJobExecutionImpl jobExecutionImpl, Step step) {
        this.jobExecutionImpl = jobExecutionImpl;
        this.jobInstance = jobExecutionImpl.getJobInstance();
        if (step == null) {
            throw new IllegalArgumentException("Step parameter to ctor cannot be null.");
        }
        this.step = step;
    }

    
    private void setContextProperties() {
    	JSLProperties jslProps = step.getProperties();
    	
    	if (jslProps != null) {
    		for (Property property : jslProps.getPropertyList()) {
        		Properties contextProps = stepContext.getProperties();
        		contextProps.setProperty(property.getName(), property.getValue());
        	}	
    	}
    	
		// set up metrics
		stepContext.addMetric(MetricImpl.MetricName.READCOUNT, 0);
		stepContext.addMetric(MetricImpl.MetricName.WRITECOUNT, 0);
		stepContext.addMetric(MetricImpl.MetricName.READSKIPCOUNT, 0);
		stepContext.addMetric(MetricImpl.MetricName.PROCESSSKIPCOUNT, 0);
		stepContext.addMetric(MetricImpl.MetricName.WRITESKIPCOUNT, 0);
		stepContext.addMetric(MetricImpl.MetricName.FILTERCOUNT, 0);
		stepContext.addMetric(MetricImpl.MetricName.COMMITCOUNT, 0);
		stepContext.addMetric(MetricImpl.MetricName.ROLLBACKCOUNT, 0);
		
		ITransactionManagementService transMgr = (ITransactionManagementService) ServicesManager.getInstance().getService(ServiceType.TRANSACTION_SERVICE);
		transactionManager = transMgr.getTransactionManager(stepContext);
    	
    }
    
    public void setStepContext(StepContextImpl<?, ? extends Externalizable> stepContext) {
        this.stepContext = stepContext;
    }

    @Override
    public String execute() throws AbortedBeforeStartException  {
           
        Throwable throwable = null;
        
        this.stepStatus = _jobStatusService.getStepStatus(jobInstance.getInstanceId(), step.getId());

        try {
            RunOnRestart rc = preInvokeStep();

            if (rc.equals(RunOnRestart.ALREADY_COMPLETE)) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Not going to run this step.  Returning previous exit status of: " + stepStatus.getExitStatus());
                }
                
                //If we are in a partitioned step put a step finished message on the queue
                if (this.analyzerQueue != null) {
                    PartitionDataWrapper dataWrapper = new PartitionDataWrapper();
                    dataWrapper.setEventType(PartitionEventType.STEP_FINISHED);
                    analyzerQueue.add(dataWrapper);
                }
                
                return stepStatus.getExitStatus();
                
            } else {
                invokeCoreStep();
             
                /**
                 * This order has been reversed to keep it consistent with when we invoke job, split, and flow listeners
                 */
                transitionToFinalStatus();

            
            }
        } catch (Throwable t) {
            
            throwable = t;
            
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(sourceClass + ": caught exception/error: " + t.getMessage() + " : Stack trace: " + sw.toString());
            }
            
            // If null, this says that the preInvoke failed before we even got
            // into the 'starting' state,
            // so we won't count it as an attempt. There's no record of this
            // step having executed.
            if (stepContext.getBatchStatus() != null) {
                stepContext.setBatchStatus(BatchStatus.FAILED);
            }
        } finally {
            //CALL ANALYZER AND LOGICALTX and listeners
            invokePostStepArtifacts();
        	
            if (stepContext.getBatchStatus() != null) {
                defaultExitStatusIfNecessary();
                persistStepExitStatusAndUserData();
            }
        }

        if (stepContext.getBatchStatus() == null) {
            throw new AbortedBeforeStartException("Thrown for stepId=" + step.getId());
        } else if (throwable != null) {
            throw new RuntimeException("Wrappering earlier uncaught exception: ", throwable);
        } else {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Returning step exitStatus: " + stepContext.getExitStatus()); 
            }
            return stepContext.getExitStatus();
        }
    }

    protected abstract void invokeCoreStep() throws JobRestartException;
    
    protected abstract void setupStepArtifacts();
    
    protected abstract void invokePreStepArtifacts();
    
    protected abstract void invokePostStepArtifacts();

    protected void registerStepExecution() {
    	long jobExecutionId = jobExecutionImpl.getExecutionId();
    	long stepExecutionId = _jobIdManagementService.getStepExecutionId();
    	
    	((StepContextImpl)stepContext).setStepExecutionId(stepExecutionId);
    	if (stepStatus != null) {
    		((StepContextImpl)stepContext).setPersistentUserData(stepStatus.getPersistentUserData());
    	}
    	
    	StepExecutionImpl stepExecution = new StepExecutionImpl(jobExecutionId, stepExecutionId);
        stepExecution.setStepName(step.getId());
        stepExecution.setStepContext(stepContext);
        
    }

    private void defaultExitStatusIfNecessary() {
        String stepExitStatus = stepContext.getExitStatus();
        if (stepExitStatus != null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Returning with user-set exit status: " + stepExitStatus);
            }
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Returning with default exit status");
            }
            stepContext.setExitStatus(stepContext.getBatchStatus().name());
        }
    }

    protected void statusStarting() {
        stepStatus.setBatchStatus(BatchStatus.STARTING);
        _jobStatusService.updateJobCurrentStep(jobInstance.getInstanceId(), step.getId());
        _jobStatusService.updateEntireStepStatus(jobInstance.getInstanceId(), step.getId(), stepStatus);
        stepContext.setBatchStatus(BatchStatus.STARTING);
        long time = System.currentTimeMillis();
    	Timestamp startTS = new Timestamp(time);
        stepContext.setStartTime(startTS);
    }

    protected void statusStarted() {
        stepStatus.setBatchStatus(BatchStatus.STARTED);
        _jobStatusService.updateEntireStepStatus(jobInstance.getInstanceId(), step.getId(), stepStatus);
        stepContext.setBatchStatus(BatchStatus.STARTED);
    }
    
    protected void statusStopped() {
        stepStatus.setBatchStatus(BatchStatus.STOPPED);
        _jobStatusService.updateEntireStepStatus(jobInstance.getInstanceId(), step.getId(), stepStatus);
        stepContext.setBatchStatus(BatchStatus.STOPPED);
    }

    protected void statusCompleted() {
        stepStatus.setBatchStatus(BatchStatus.COMPLETED);
        _jobStatusService.updateEntireStepStatus(jobInstance.getInstanceId(), step.getId(), stepStatus);
        stepContext.setBatchStatus(BatchStatus.COMPLETED);
    }
    
    private void transitionToFinalStatus() {
        BatchStatus currentBatchStatus = stepContext.getBatchStatus();

        if (currentBatchStatus.equals(BatchStatus.STARTING)) {
            throw new IllegalStateException("Step batch status should not be in a STARTING state");
        }

        // Transition to "COMPLETED"
        if (currentBatchStatus.equals(BatchStatus.STARTED)) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Transitioning step status to COMPLETED for step: " + step.getId());
            }
            statusCompleted();
        // Transition to "STOPPED"            
        } else if (currentBatchStatus.equals(BatchStatus.STOPPING)) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Transitioning step status to STOPPED for step: " + step.getId());
            }
            statusStopped();
        }        
    }

    private void persistStepExitStatusAndUserData() {
                
        ByteArrayOutputStream persistentBAOS = new ByteArrayOutputStream();
        ObjectOutputStream persistentDataOOS = null;
        
        try {
            persistentDataOOS = new ObjectOutputStream(persistentBAOS);
            persistentDataOOS.writeObject(stepContext.getPersistentUserData());
            persistentDataOOS.close();
        } catch (Exception e) {
            throw new BatchContainerServiceException("Cannot persist the persistent user data for the step.", e);
        }

        stepStatus.setPersistentUserData(new PersistentDataWrapper(persistentBAOS.toByteArray()));
        stepStatus.setExitStatus(stepContext.getExitStatus());
        _jobStatusService.updateEntireStepStatus(jobInstance.getInstanceId(), step.getId(), stepStatus);
        
        // set the end time metric before flushing
        long time = System.currentTimeMillis();
    	Timestamp endTS = new Timestamp(time);
        stepContext.setEndTime(endTS);
        
        //flush StepExecution to the backing store and deregister in memory instance
        JobExecutionHelper.persistStepExecution(jobExecutionImpl.getExecutionId(), stepContext);
        //batchKernel.deregisterStepExecution(jobExecutionImpl.getExecutionId(), stepContext.getStepExecutionId());

        // flush the StepExecution out into the db here - probably need cols for each metric value vs a metric obj itself
    }

    protected RunOnRestart preInvokeStep() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("In preInvokeStep() with stepContext =  " + this.stepContext);
        }

        registerStepExecution();
        
        boolean runStep = true;
        if (stepStatus == null) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Exist StepStatus not found.  Creating StepStatus for (" + jobInstance.getInstanceId() + "," + step.getId()
                        + ")");
            }
            stepStatus = new StepStatus(step.getId());
            _jobStatusService.createStepStatus(jobInstance.getInstanceId(), step.getId(), stepStatus);

        } else {
            if (runOnRestart()) {
                stepStatus.incrementStartCount();
            } else {
                return RunOnRestart.ALREADY_COMPLETE;
            }
        }

        // Update status
        statusStarting();
        
        //Set Step context properties
        setContextProperties();
        
        //SET UP STEP ARTIFACTS LIKE LISTENERS OR LOGICALTX
        setupStepArtifacts();

        // Update status
        statusStarted();

        //INVOKE PRE STEP LISTENERS OR TX's
        invokePreStepArtifacts();

        return RunOnRestart.RUN;
    }

    /*
     * Currently blows up if we're over the start limit rather than failing and
     * allowing more orderly processing within this class.
     */
    private boolean runOnRestart() {
        // TODO - maybe some more validation is required?

        BatchStatus stepBatchStatus = stepStatus.getBatchStatus();
        if (stepBatchStatus.equals(BatchStatus.COMPLETED)) {
            // A bit of parsing involved since the model gives us a String not a
            // boolean, but it
            // should default to 'false', which is the spec'd default.
            if (!Boolean.parseBoolean(step.getAllowStartIfComplete())) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Step: " + step.getId() + " already has batch status of COMPLETED, so won't be run again since it does not allow start if complete.");
                }
                return false;
            } else {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Step: " + step.getId() + " already has batch status of COMPLETED, and allow-start-if-complete is set to 'true'");
                }
            }
        }

        // Check restart limit, the spec default is '0'.
        int startLimit = 0;
        String startLimitString = step.getStartLimit();
        if (startLimitString != null) {
            try {
                startLimit = Integer.parseInt(startLimitString);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Could not parse start limit value for stepId: " + step.getId() + ", with start-limit="
                        + step.getStartLimit(), e);
            }
        }

        if (startLimit < 0) {
            throw new IllegalArgumentException("Found negative start-limit of " + startLimit + "for stepId: " + step.getId());
        }

        if (startLimit > 0) {
            int newStepStartCount = stepStatus.getStartCount() + 1;
            if (newStepStartCount > startLimit) {
                // TODO - should I fail the job or do something more specific
                // here than blowing up?
                throw new IllegalArgumentException("For stepId: " + step.getId() + ", tried to start step for the " + newStepStartCount
                        + " time, but startLimit = " + startLimit);
            } else {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Starting (possibly restarting) step: " + step.getId() + ", since newStepStartCount = " + newStepStartCount
                            + "and startLimit=" + startLimit);
                }
            }
        }

        return true;
    }

    
    protected LinkedBlockingQueue<PartitionDataWrapper> getAnalyzerQueue() {
		return analyzerQueue;
	}

    public void setAnalyzerQueue(LinkedBlockingQueue<PartitionDataWrapper> analyzerQueue) {
        this.analyzerQueue = analyzerQueue;
    }
}
