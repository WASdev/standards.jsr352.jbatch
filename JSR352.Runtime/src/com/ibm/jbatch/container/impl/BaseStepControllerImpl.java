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

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;
import java.util.Stack;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator.BatchStatus;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.JobStartException;
import javax.batch.runtime.JobInstance;

import com.ibm.jbatch.container.AbortedBeforeStartException;
import com.ibm.jbatch.container.IExecutionElementController;
import com.ibm.jbatch.container.artifact.proxy.PartitionCollectorProxy;
import com.ibm.jbatch.container.context.impl.MetricImpl;
import com.ibm.jbatch.container.context.impl.StepContextImpl;
import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.jobinstance.RuntimeJobExecutionHelper;
import com.ibm.jbatch.container.jobinstance.StepExecutionImpl;
import com.ibm.jbatch.container.persistence.PersistentDataWrapper;
import com.ibm.jbatch.container.services.IBatchKernelService;
import com.ibm.jbatch.container.services.IJobStatusManagerService;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.servicesmanager.ServicesManagerImpl;
import com.ibm.jbatch.container.status.StepStatus;
import com.ibm.jbatch.container.util.PartitionDataWrapper;
import com.ibm.jbatch.container.util.PartitionDataWrapper.PartitionEventType;
import com.ibm.jbatch.jsl.model.JSLProperties;
import com.ibm.jbatch.jsl.model.Property;
import com.ibm.jbatch.jsl.model.Step;
import com.ibm.jbatch.spi.services.ITransactionManagementService;
import com.ibm.jbatch.spi.services.TransactionManagerAdapter;

/** Change the name of this class to something else!! Or change BaseStepControllerImpl. */
public abstract class BaseStepControllerImpl implements IExecutionElementController {

    private final static String sourceClass = BatchletStepControllerImpl.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);

    protected RuntimeJobExecutionHelper jobExecutionImpl;
    protected JobInstance jobInstance;

    protected StepContextImpl<?, ? extends Serializable> stepContext;
    protected Step step;
    protected StepStatus stepStatus;
    
    private Properties properties = new Properties();

    protected BlockingQueue<PartitionDataWrapper> analyzerStatusQueue = null;
    protected Stack<String> subJobExitStatusQueue = null;

    protected List<String> containment = null;
    protected RuntimeJobExecutionHelper rootJobExecution = null;

	protected PartitionCollectorProxy collectorProxy = null;
	
	protected static IBatchKernelService batchKernel = ServicesManagerImpl.getInstance().getBatchKernelService();
    
    protected TransactionManagerAdapter	transactionManager = null;
    
    private enum RunOnRestart {
        ALREADY_COMPLETE, RUN
    };
    
    private static IPersistenceManagerService _persistenceManagementService = ServicesManagerImpl.getInstance().getPersistenceManagerService();
    
    private static IJobStatusManagerService _jobStatusService = (IJobStatusManagerService) ServicesManagerImpl.getInstance().getJobStatusManagerService();

    protected BaseStepControllerImpl(RuntimeJobExecutionHelper jobExecutionImpl, Step step) {
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
		stepContext.addMetric(MetricImpl.MetricType.READ_COUNT, 0);
		stepContext.addMetric(MetricImpl.MetricType.WRITE_COUNT, 0);
		stepContext.addMetric(MetricImpl.MetricType.READ_SKIP_COUNT, 0);
		stepContext.addMetric(MetricImpl.MetricType.PROCESS_SKIP_COUNT, 0);
		stepContext.addMetric(MetricImpl.MetricType.WRITE_SKIPCOUNT, 0);
		stepContext.addMetric(MetricImpl.MetricType.FILTER_COUNT, 0);
		stepContext.addMetric(MetricImpl.MetricType.COMMIT_COUNT, 0);
		stepContext.addMetric(MetricImpl.MetricType.ROLLBACK_COUNT, 0);
		
		ITransactionManagementService transMgr = ServicesManagerImpl.getInstance().getTransactionManagementService();
		transactionManager = transMgr.getTransactionManager(stepContext);
    	
    }
    
    public void setStepContext(StepContextImpl<?, ? extends Serializable> stepContext) {
        this.stepContext = stepContext;
    }

    @Override
    public String execute(List<String> containment, RuntimeJobExecutionHelper rootJobExecution) throws AbortedBeforeStartException  {
           
        Throwable throwable = null;
        

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Containment: " + containment);
        }
        
        this.containment = containment;
        this.rootJobExecution = rootJobExecution;
        
        try {
            RunOnRestart rc = preInvokeStep();

            if (rc.equals(RunOnRestart.ALREADY_COMPLETE)) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Not going to run this step.  Returning previous exit status of: " + stepStatus.getExitStatus());
                }
                
                //If we are in a partitioned step put a step finished message on the queue
                if (this.analyzerStatusQueue != null) {
                    PartitionDataWrapper dataWrapper = new PartitionDataWrapper();
                    dataWrapper.setEventType(PartitionEventType.STEP_ALREADY_COMPLETED);
                    analyzerStatusQueue.add(dataWrapper);
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
            
            //partitioned jobs need to register exit status
            if (this.subJobExitStatusQueue != null){
                logger.finer("Adding step exitStatus to stack.");
            	subJobExitStatusQueue.add(this.stepContext.getExitStatus());
            }
            
            return stepContext.getExitStatus();
        }
    }

    protected abstract void invokeCoreStep() throws JobRestartException, JobStartException;
    
    protected abstract void setupStepArtifacts();
    
    protected abstract void invokePreStepArtifacts();
    
    protected abstract void invokePostStepArtifacts();

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
        _jobStatusService.updateStepStatus(stepStatus.getStepId(), stepStatus);
        stepContext.setBatchStatus(BatchStatus.STARTING);
        long time = System.currentTimeMillis();
    	Timestamp startTS = new Timestamp(time);
        stepContext.setStartTime(startTS);
    }

    protected void statusStarted() {
        stepStatus.setBatchStatus(BatchStatus.STARTED);
        _jobStatusService.updateStepStatus(stepStatus.getStepId(), stepStatus);
        stepContext.setBatchStatus(BatchStatus.STARTED);
    }
    
    protected void statusStopped() {
        stepStatus.setBatchStatus(BatchStatus.STOPPED);
        _jobStatusService.updateStepStatus(stepStatus.getStepId(), stepStatus);
        stepContext.setBatchStatus(BatchStatus.STOPPED);
    }

    protected void statusCompleted() {
        stepStatus.setBatchStatus(BatchStatus.COMPLETED);
        _jobStatusService.updateStepStatus(stepStatus.getStepId(), stepStatus);
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
        _jobStatusService.updateStepStatus(stepStatus.getStepId(), stepStatus);
        
        // set the end time metric before flushing
        long time = System.currentTimeMillis();
    	Timestamp endTS = new Timestamp(time);
        stepContext.setEndTime(endTS);
        
        _persistenceManagementService.updateStepExecution(rootJobExecution.getExecutionId(), stepContext, this.containment);
    }

    private StepExecutionImpl getNewStepExecution(long rootJobExecutionId, StepContextImpl stepContext) {
        return _persistenceManagementService.createStepExecution(rootJobExecutionId, stepContext, this.containment);
    }

    protected RunOnRestart preInvokeStep() {

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("In preInvokeStep() with stepContext =  " + this.stepContext);
        }


        this.stepStatus = _jobStatusService.getStepStatus(jobInstance.getInstanceId(), step.getId());
        if (stepStatus == null) {

            // create new step execution
            StepExecutionImpl stepExecution = getNewStepExecution(rootJobExecution.getExecutionId(), stepContext);
            // create new step status for this run
            stepStatus = _jobStatusService.createStepStatus(stepExecution.getStepExecutionId());
            ((StepContextImpl) stepContext).setStepExecutionId(stepExecution.getStepExecutionId());

        } else {
            // if a step status already exists for this instance id. It means this
            // is a restart and we need to get the previously persisted data
            ((StepContextImpl) stepContext).setPersistentUserData(stepStatus.getPersistentUserData());
            if (runOnRestart()) {
                // create new step execution
                StepExecutionImpl stepExecution = getNewStepExecution(rootJobExecution.getExecutionId(), stepContext);
                
                ((StepContextImpl) stepContext).setStepExecutionId(stepExecution.getStepExecutionId());
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

    
    protected BlockingQueue<PartitionDataWrapper> getAnalyzerQueue() {
		return analyzerStatusQueue;
	}

    public void setAnalyzerQueue(BlockingQueue<PartitionDataWrapper> analyzerQueue) {
        this.analyzerStatusQueue = analyzerQueue;
    }
    
    protected Stack<String> getSubJobExitStatusQueue() {
		return subJobExitStatusQueue;
	}

    public void setSubJobExitStatusQueue(Stack<String> subJobExitStatusQueue) {
        this.subJobExitStatusQueue = subJobExitStatusQueue;
    }
    

}
