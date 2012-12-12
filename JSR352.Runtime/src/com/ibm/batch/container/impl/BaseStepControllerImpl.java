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

import java.io.Externalizable;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.runtime.JobInstance;
import javax.batch.runtime.spi.TransactionManagerSPI;

import jsr352.batch.jsl.JSLProperties;
import jsr352.batch.jsl.Property;
import jsr352.batch.jsl.Step;

import com.ibm.batch.container.AbortedBeforeStartException;
import com.ibm.batch.container.IExecutionElementController;
import com.ibm.batch.container.artifact.proxy.PartitionAnalyzerProxy;
import com.ibm.batch.container.artifact.proxy.PartitionCollectorProxy;
import com.ibm.batch.container.context.impl.FlowContextImpl;
import com.ibm.batch.container.context.impl.MetricImpl;
import com.ibm.batch.container.context.impl.SplitContextImpl;
import com.ibm.batch.container.context.impl.StepContextImpl;
import com.ibm.batch.container.jobinstance.JobExecutionHelper;
import com.ibm.batch.container.jobinstance.RuntimeJobExecutionImpl;
import com.ibm.batch.container.jobinstance.StepExecutionImpl;
import com.ibm.batch.container.services.IJobIdManagementService;
import com.ibm.batch.container.services.IJobStatusManagerService;
import com.ibm.batch.container.services.ITransactionManagementService;
import com.ibm.batch.container.services.ServicesManager;
import com.ibm.batch.container.services.ServicesManager.ServiceType;
import com.ibm.batch.container.status.StepStatus;
import com.ibm.batch.container.util.ExecutionStatus;
import com.ibm.batch.container.util.ExecutionStatus.BatchStatus;

/** Change the name of this class to something else!! Or change BaseStepControllerImpl. */
public abstract class BaseStepControllerImpl implements IExecutionElementController {

    private final static String sourceClass = BatchletStepControllerImpl.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);

    protected RuntimeJobExecutionImpl jobExecutionImpl;
    protected JobInstance jobInstance;

    protected StepContextImpl<?, ? extends Externalizable> stepContext;
    protected SplitContextImpl splitContext;
    protected FlowContextImpl flowContext;
    protected Step step;
    protected StepStatus stepStatus;
    
    private Properties properties = new Properties();

    protected PartitionAnalyzerProxy analyzerProxy = null;

	protected PartitionCollectorProxy collectorProxy = null;
	
	protected static BatchKernelImpl batchKernel = (BatchKernelImpl) ServicesManager.getInstance().getService(ServiceType.BATCH_KERNEL_SERVICE);
	
    protected static IJobIdManagementService _jobIdManagementService = (IJobIdManagementService)ServicesManager.getInstance().getService(ServiceType.JOB_ID_MANAGEMENT_SERVICE);
    
    protected TransactionManagerSPI	transactionManager = null;
    
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
		stepContext.addMetric(MetricImpl.Counter.valueOf("READ_COUNT"), 0);
		stepContext.addMetric(MetricImpl.Counter.valueOf("WRITE_COUNT"), 0);
		stepContext.addMetric(MetricImpl.Counter.valueOf("READ_SKIP_COUNT"), 0);
		stepContext.addMetric(MetricImpl.Counter.valueOf("PROCESS_SKIP_COUNT"), 0);
		stepContext.addMetric(MetricImpl.Counter.valueOf("WRITE_SKIP_COUNT"), 0);
		
		ITransactionManagementService transMgr = (ITransactionManagementService) ServicesManager.getInstance().getService(ServiceType.TRANSACTION_MANAGEMENT_SERVICE);
		transactionManager = transMgr.getTransactionManager(stepContext);
    	
    }
    
    public void setStepContext(StepContextImpl<?, ? extends Externalizable> stepContext) {
        this.stepContext = stepContext;
    }

    public void setSplitContext(SplitContextImpl splitContext) {
        this.splitContext = splitContext;
    }

    public void setFlowContext(FlowContextImpl flowContext) {
        this.flowContext = flowContext;
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
                stepContext.setBatchStatus(ExecutionStatus.getStringValue(BatchStatus.FAILED));
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

    protected abstract void invokeCoreStep();
    
    protected abstract void setupStepArtifacts();
    
    protected abstract void invokePreStepArtifacts();
    
    protected abstract void invokePostStepArtifacts();

    protected void registerStepExecution() {
    	long jobExecutionId = jobExecutionImpl.getExecutionId();
    	long stepExecutionId = _jobIdManagementService.getStepExecutionId();
    	
    	((StepContextImpl)stepContext).setStepExecutionId(stepExecutionId);
    	
    	StepExecutionImpl stepExecution = new StepExecutionImpl(jobExecutionId, stepExecutionId);
        stepExecution.setStepName(step.getId());
        stepExecution.setStepContext(stepContext);
        // set the StepExecutionID on the StepContext here?
        
    
        batchKernel.registerStepExecution(jobExecutionId,stepExecutionId, stepExecution);
        
    }

    protected boolean runAlreadyCompletedStep(StepStatus stepStatus) {

        if (!Boolean.parseBoolean(step.getAllowStartIfComplete())) {
            return false;
        } else {
            return true;
        }
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
            stepContext.setExitStatus(stepContext.getBatchStatus());
        }
    }

    private void statusStarting() {
        stepStatus.setBatchStatus(BatchStatus.STARTING);
        _jobStatusService.updateJobCurrentStep(jobInstance.getInstanceId(), step.getId());
        _jobStatusService.updateEntireStepStatus(jobInstance.getInstanceId(), step.getId(), stepStatus);
        stepContext.setBatchStatus(ExecutionStatus.getStringValue(BatchStatus.STARTING));
    }

    private void statusStarted() {
        stepStatus.setBatchStatus(BatchStatus.STARTED);
        _jobStatusService.updateEntireStepStatus(jobInstance.getInstanceId(), step.getId(), stepStatus);
        stepContext.setBatchStatus(ExecutionStatus.getStringValue(BatchStatus.STARTED));
    }
    
    private void statusStopped() {
        stepStatus.setBatchStatus(BatchStatus.STOPPED);
        _jobStatusService.updateEntireStepStatus(jobInstance.getInstanceId(), step.getId(), stepStatus);
        stepContext.setBatchStatus(ExecutionStatus.getStringValue(BatchStatus.STOPPED));
    }

    private void statusCompleted() {
        stepStatus.setBatchStatus(BatchStatus.COMPLETED);
        _jobStatusService.updateEntireStepStatus(jobInstance.getInstanceId(), step.getId(), stepStatus);
        stepContext.setBatchStatus(ExecutionStatus.getStringValue(BatchStatus.COMPLETED));
    }
    
    private void transitionToFinalStatus() {
        BatchStatus currentBatchStatus = ExecutionStatus.getBatchStatusEnum(stepContext.getBatchStatus());

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
        stepStatus.setPersistentUserData(stepContext.getPersistentUserData());
        stepStatus.setExitStatus(stepContext.getExitStatus());
        _jobStatusService.updateEntireStepStatus(jobInstance.getInstanceId(), step.getId(), stepStatus);
        
        JobExecutionHelper.persistStepExecution(jobExecutionImpl.getExecutionId(), stepContext);

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
                    logger.fine("Step: " + step.getId() + " won't be run again since it does not allow start if complete.");
                }
                return false;
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

    
    protected PartitionAnalyzerProxy getAnalyzerProxy() {
		return analyzerProxy;
	}

    public void setAnalyzerProxy(PartitionAnalyzerProxy analyzerProxy) {
        this.analyzerProxy = analyzerProxy;
    }
}
