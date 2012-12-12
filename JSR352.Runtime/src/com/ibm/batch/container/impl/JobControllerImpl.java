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
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import jsr352.batch.jsl.Decision;
import jsr352.batch.jsl.End;
import jsr352.batch.jsl.Fail;
import jsr352.batch.jsl.Flow;
import jsr352.batch.jsl.JSLJob;
import jsr352.batch.jsl.JSLProperties;
import jsr352.batch.jsl.Property;
import jsr352.batch.jsl.Split;
import jsr352.batch.jsl.Step;
import jsr352.batch.jsl.Stop;

import com.ibm.batch.container.AbortedBeforeStartException;
import com.ibm.batch.container.IController;
import com.ibm.batch.container.IExecutionElementController;
import com.ibm.batch.container.artifact.proxy.JobListenerProxy;
import com.ibm.batch.container.artifact.proxy.ListenerFactory;
import com.ibm.batch.container.artifact.proxy.PartitionAnalyzerProxy;
import com.ibm.batch.container.context.impl.FlowContextImpl;
import com.ibm.batch.container.context.impl.JobContextImpl;
import com.ibm.batch.container.context.impl.SplitContextImpl;
import com.ibm.batch.container.context.impl.StepContextImpl;
import com.ibm.batch.container.exception.BatchContainerRuntimeException;
import com.ibm.batch.container.jobinstance.RuntimeJobExecutionImpl;
import com.ibm.batch.container.services.IJobStatusManagerService;
import com.ibm.batch.container.services.IPersistenceManagerService;
import com.ibm.batch.container.services.ServicesManager;
import com.ibm.batch.container.services.impl.JDBCPersistenceManagerImpl;
import com.ibm.batch.container.util.ExecutionStatus;
import com.ibm.batch.container.util.ExecutionStatus.BatchStatus;
import com.ibm.batch.container.xjcl.ControlElement;
import com.ibm.batch.container.xjcl.ExecutionElement;
import com.ibm.batch.container.xjcl.Navigator;
import com.ibm.batch.container.xjcl.Transition;

public class JobControllerImpl implements IController {

    private final static String CLASSNAME = JobControllerImpl.class.getName();
    private final static Logger logger = Logger.getLogger(CLASSNAME);

    private IJobStatusManagerService jobStatusService = null;
    private IPersistenceManagerService persistenceService = null;

    private RuntimeJobExecutionImpl jobExecution = null;

    private final JobContextImpl<?> jobContext;
    private final Navigator<JSLJob> jobNavigator;
    private final String jobId;

    private PartitionAnalyzerProxy analyzerProxy = null;
    
	private ListenerFactory listenerFactory = null;
    
    private final long jobInstanceId;

    //
    // The currently executing controller, this will only be set to the 
    // local variable reference when we are ready to accept stop events for
    // this execution.
    private volatile IExecutionElementController currentStoppableElementController = null;

    public JobControllerImpl(RuntimeJobExecutionImpl jobExecution) {
        this.jobExecution = jobExecution;
        this.jobContext = jobExecution.getJobContext();
        jobNavigator = jobExecution.getJobNavigator();
        jobId = jobNavigator.getId();
        //AJM: jobContext = new JobContextImpl(jobId); // TODO - is this the right id?
        jobInstanceId = jobExecution.getJobInstance().getInstanceId();
        jobStatusService = (IJobStatusManagerService) ServicesManager.getInstance().getService(
                ServicesManager.ServiceType.JOB_STATUS_MANAGEMENT_SERVICE);
        persistenceService = (IPersistenceManagerService) ServicesManager.getInstance().getService(
                ServicesManager.ServiceType.PERSISTENCE_MANAGEMENT_SERVICE);
        
        setContextProperties();
        setupListeners();
    }
    
    private void setContextProperties() {
    	JSLJob jobModel = jobExecution.getJobNavigator().getJSL();
    	JSLProperties jslProps = jobModel.getProperties();
    	
    	if (jslProps != null) {
    		for (Property property : jslProps.getPropertyList()) {
        		Properties contextProps = jobContext.getProperties();
        		
        		contextProps.setProperty(property.getName(), property.getValue());
        	}	
    	}
    	
    }
    
    private void setupListeners() {
        JSLJob jobModel = jobExecution.getJobNavigator().getJSL();            
        listenerFactory = new ListenerFactory(jobModel);
        jobExecution.setListenerFactory(listenerFactory);
    }

    public void executeJob() {

        final String methodName = "executeJob";
        if (logger.isLoggable(Level.FINE)) {
            logger.entering(CLASSNAME, methodName);
        }

        try {

            updateJobBatchStatus(BatchStatus.STARTING);

            // Periodic check for stopping job
            if (jobContext.getBatchStatus().equals(ExecutionStatus.getStringValue(BatchStatus.STOPPING))) {
                updateJobBatchStatus(BatchStatus.STOPPED);
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(methodName + " Exiting as job has been stopped");
                }
                return;
            }

            //AJM: jobExecution.setJobContext(jobContext);  // move this up to be the first thing we do in this method          


            // Periodic check for stopping job
            if (jobContext.getBatchStatus().equals(ExecutionStatus.getStringValue(BatchStatus.STOPPING))) {
                updateJobBatchStatus(BatchStatus.STOPPED);
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(methodName + " Exiting as job has been stopped");
                }
                return;
            }

            updateJobBatchStatus(BatchStatus.STARTED);
            
            List<JobListenerProxy> jobListeners = listenerFactory.getJobListeners();

            // Inject job context into listeners, we could move this into
            // the above
            // for loop if need. But it's more readable here for now. We
            // haven't
            // started the step yet so I assume the step context will be
            // null.
            for (JobListenerProxy listenerProxy : jobListeners) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(methodName + " Setting jobContext on jobListener: " + listenerProxy.getDelegate() + " of type: " + listenerProxy.getDelegate().getClass());
                }
                listenerProxy.setJobContext(jobContext);
            }

            // Call @BeforeJob on all the job listeners
            for (JobListenerProxy listenerProxy : jobListeners) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(methodName + " Invoking @BeforeJob on jobListener: " + listenerProxy.getDelegate() + " of type: " + listenerProxy.getDelegate().getClass());
                }
                listenerProxy.beforeJob();
            }

            // Periodic check for stopping job
            if (jobContext.getBatchStatus().equals(ExecutionStatus.getStringValue(BatchStatus.STOPPING))) {
                updateJobBatchStatus(BatchStatus.STOPPED);
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(methodName + " Exiting as job has been stopped");
                }
                return;
            }

            // --------------------
            //
            // The BIG loop!!!
            //
            // --------------------
            doExecutionLoop(jobNavigator);

            // TODO - Before or after afterJob()?
            String curStatusString = jobContext.getBatchStatus();
            if (curStatusString == null) {
                throw new IllegalStateException("Job BatchStatus should have been set by now");
            }
            BatchStatus curStatus = ExecutionStatus.getBatchStatusEnum(curStatusString);

            // BatchStatus may already have been set by a JSL <stop> or <fail>
            // decision directive.
            if (!(curStatus.equals(BatchStatus.FAILED) || curStatus.equals(BatchStatus.STOPPED))) {
                updateJobBatchStatus(BatchStatus.COMPLETED);
            }

            // TODO - this can't be the exact order... but I'm assuming
            // @AfterJob wants to react to the exit status.
            if (jobContext.getExitStatus() == null) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("No job-level exitStatus set, defaulting to job batch Status = " + jobContext.getBatchStatus());
                }
                jobContext.setExitStatus(jobContext.getBatchStatus());
            }

            // TODO - These job listener afterJob() still gets called on
            // stop/end/fail, right?

            // Call @AfterJob on all the job listeners
            for (JobListenerProxy listenerProxy : jobListeners) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(methodName + " Invoking @AfterJob on jobListener: " + listenerProxy.getDelegate() + " of type: " + listenerProxy.getDelegate().getClass());
                }
                listenerProxy.afterJob();
            }
        } catch (Throwable t) {
                        
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(CLASSNAME + ": caught exception/error: " + t.getMessage() + " : Stack trace: " + sw.toString());
            }
            
            updateJobBatchStatus(BatchStatus.FAILED);

            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Job failed with exception/error: " + t.getMessage());
            }

            if (jobContext.getExitStatus() == null) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("No job-level exitStatus set, defaulting to job batch Status = " + jobContext.getBatchStatus());
                }
                jobContext.setExitStatus(jobContext.getBatchStatus());
            }

            throw new BatchContainerRuntimeException(t);
        } finally {

            // Persist exit status, setting default if not set

            // TODO - should we override the empty string or just null?

            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Job complete for job id=" + jobExecution.getJobInstance().getJobName() + ", executionId="
                        + jobExecution.getExecutionId() + ", batchStatus=" + jobContext.getBatchStatus() + ", exitStatus="
                        + jobContext.getExitStatus());
            }

            try {
                jobStatusService.updateJobExecutionStatus(jobExecution.getInstanceId(), 
                        ExecutionStatus.getBatchStatusEnum(jobContext.getBatchStatus()), 
                        jobContext.getExitStatus());
                        
            } catch (Throwable t) {
                if (logger.isLoggable(Level.WARNING)) {
                    StringWriter sw = new StringWriter();
                    t.printStackTrace(new PrintWriter(sw));
                    logger.warning("Caught Throwable on updating execution status: " + sw.toString());
                }
            }

            if (logger.isLoggable(Level.FINE)) {
                logger.exiting(CLASSNAME, methodName);
            }
        }
    }

    private void doExecutionLoop(Navigator jobNavigator) {
        final String methodName = "doExecutionLoop";

        JobContextImpl<?> jobContext = jobExecution.getJobContext();

        ExecutionElement currentExecutionElement = null;
        try {
            currentExecutionElement = jobNavigator.getFirstExecutionElement(jobExecution.getRestartOn());
        } catch (Exception e) {
            throw new IllegalArgumentException("Job doesn't contain a step.", e);
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("First execution element = " + currentExecutionElement.getId());
        }

        // TODO can the first execution element be a decision ??? seems like
        // it's possible

        StepContextImpl<?, ?> stepContext = null;
        FlowContextImpl flowContext = null;
        SplitContextImpl splitContext = null;

        ExecutionElement previousExecutionElement = null;

        while (true) {

            if (!(currentExecutionElement instanceof Step) && !(currentExecutionElement instanceof Decision) 
            		&& !(currentExecutionElement instanceof Flow) && !(currentExecutionElement instanceof Split)) {
                throw new UnsupportedOperationException("Only support step, flow, and decision at the moment.");
            }

            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Next execution element = " + currentExecutionElement.getId());
            }

            IExecutionElementController elementController = 
                ExecutionElementControllerFactory.getExecutionElementController(jobExecution, currentExecutionElement);

            //If this is a sub job it may have a partition analyzer we need to pass along
            elementController.setAnalyzerProxy(analyzerProxy);
            
            // Depending on the execution element new up the associated context
            // and add it to the controller
            if (currentExecutionElement instanceof Decision) {

                if (previousExecutionElement == null) {
                    // only job context is available to the decider since it is
                    // the first execution element in the job

                    // we need to set to null if batch artifacts are reused
                    elementController.setStepContext(null);
                    elementController.setFlowContext(null);
                    elementController.setSplitContext(null);

                } else if (previousExecutionElement instanceof Decision) {
                    throw new BatchContainerRuntimeException("A decision cannot precede another decision...OR CAN IT???");
                } else if (previousExecutionElement instanceof Step) {
                    elementController.setStepContext(stepContext); // this is
                    // the
                    // context
                    // from the
                    // previous
                    // execution
                    // element
                    elementController.setFlowContext(null); // this is supposed to pass null
                    elementController.setSplitContext(null); // this is supposed to pass null

                } else if (previousExecutionElement instanceof Split) {
                    elementController.setStepContext(null);// this is supposed to pass null
                    elementController.setFlowContext(null); // this is supposed to pass null
                    elementController.setSplitContext(splitContext);// this is
                    // the
                    // context
                    // from the
                    // previous
                    // execution
                    // element
                } else if (previousExecutionElement instanceof Flow) {
                    elementController.setStepContext(null);// this is supposed to pass null
                    elementController.setFlowContext(flowContext); // this is
                    // the
                    // context
                    // from the
                    // previous
                    // execution
                    // element
                    elementController.setSplitContext(null); // this is supposed
                    // to pass null
                }

            } else if (currentExecutionElement instanceof Step) {
                String stepId = ((Step) currentExecutionElement).getId();
                stepContext = new StepContextImpl<Object, Externalizable>(stepId);
                elementController.setStepContext(stepContext);
            } else if (currentExecutionElement instanceof Flow) {
            	String flowId = ((Flow) currentExecutionElement).getId();
                flowContext = new FlowContextImpl(flowId);
                elementController.setFlowContext(flowContext);
            } else if (currentExecutionElement instanceof Split) {
            	String splitId = ((Split) currentExecutionElement).getId();
                splitContext = new SplitContextImpl(splitId);
                elementController.setSplitContext(splitContext);
            }

            // check for stop before every executing each execution element
            if (jobContext.getBatchStatus().equals(ExecutionStatus.getStringValue(BatchStatus.STOPPING))) {
                updateJobBatchStatus(BatchStatus.STOPPED);

                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(methodName + " Exiting as job has been stopped");
                }
                return;
            }

            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Start executing element = " + currentExecutionElement.getId());
            }
            
            /*
             * NOTE:
             * One approach would be to call:  jobStatusService.updateJobCurrentStep()
             * now.  However for something like a flow the element controller (flow controller) will
             * have a better view of what the "current step" is, so let's delegate to it instead. 
             */
            
            this.currentStoppableElementController = elementController;
            String executionElementExitStatus = null;
            try {
                executionElementExitStatus = elementController.execute();
            } catch (AbortedBeforeStartException e) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Execution failed before even getting to execute execution element = " + currentExecutionElement.getId());
                }
                throw new IllegalStateException("Execution failed before even getting to execute execution element = " + 
                        currentExecutionElement.getId() + "; breaking out of execution loop.");                
            }
            
            // set the execution element controller to null so we don't try to
            // call stop
            // on it after the element has finished executing
            this.currentStoppableElementController = null;            

            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Done executing element=" + currentExecutionElement.getId() + ", exitStatus=" + executionElementExitStatus);
            }

            // If we are currently in STOPPING state, then we can now move transition to STOPPED state.
            if (ExecutionStatus.getBatchStatusEnum(jobContext.getBatchStatus()).equals(BatchStatus.STOPPING)) {

                updateJobBatchStatus(BatchStatus.STOPPED);

                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(methodName + " Exiting as job has been stopped");
                }
                return;
            }

            Transition nextTransition = jobNavigator.getNextTransition(currentExecutionElement, executionElementExitStatus);

            // TODO
            if (nextTransition == null) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(methodName + " TODO: is this an expected state or not? ");
                }
                return;
            }

            if (nextTransition.getNextExecutionElement() != null) {
                // hold on to the previous execution element for the decider
                // we need it because we need to inject the context of the
                // previous execution element
                // into the decider
                previousExecutionElement = currentExecutionElement;
                currentExecutionElement = nextTransition.getNextExecutionElement();
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(methodName + " , Looping through to next execution element=" + currentExecutionElement.getId());
                }
            } else if (nextTransition.getControlElement() != null) {
                // TODO - update job status mgr
                ControlElement controlElem = nextTransition.getControlElement();

                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(methodName + " , Looping through to next control element=" + controlElem);
                }

                if (controlElem instanceof Stop) {
                    String restartOn = ((Stop) controlElem).getRestart();

                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine(methodName + " , next control element is a <stop> : " + controlElem + " with restartOn=" + restartOn);
                    }

                    updateJobBatchStatus(BatchStatus.STOPPED);
                    jobStatusService.updateJobStatusFromJSLStop(jobInstanceId, restartOn);

                    String newExitStatus = ((Stop) controlElem).getExitStatus();
                    if (newExitStatus != null && !newExitStatus.isEmpty()) { // overrides with exit status in JSL @exit-status
                        jobContext.setExitStatus(newExitStatus);  
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine(methodName + " , on stop, setting new JSL-specified exit status to: " + newExitStatus);
                        }
                    } 
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine(methodName + " Exiting stopped job");
                    }
                    return;

                } else if (controlElem instanceof End) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine(methodName + " , next control element is an <end>: " + controlElem);
                    }
                    updateJobBatchStatus(BatchStatus.COMPLETED);
                    String newExitStatus = ((End) controlElem).getExitStatus();
                    if (newExitStatus != null && !newExitStatus.isEmpty()) { // overrides with exit status in JSL @exit-status
                        jobContext.setExitStatus(newExitStatus); 
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine(methodName + " , on end, setting new JSL-specified exit status to: " + newExitStatus);
                        }
                    } 
                } else if (controlElem instanceof Fail) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine(methodName + " , next control element is a <fail>: " + controlElem);
                    }
                    updateJobBatchStatus(BatchStatus.FAILED);
                    String newExitStatus = ((Fail) controlElem).getExitStatus();
                    if (newExitStatus != null && !newExitStatus.isEmpty()) { // overrides
                                                                             // with
                        jobContext.setExitStatus(newExitStatus); // exit status
                                                                 // in
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine(methodName + " , on fail, setting new JSL-specified exit status to: " + newExitStatus);
                        }
                    } // <fail> @exit-status
                } else {
                    throw new IllegalStateException("Not sure how we'd get here but better than looping.");
                }
                return;
            } else {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(methodName + " Exiting as there are no more execution elements= ");
                }
                return;
            }
        }
    }

    @Override
    public void stop() {
        updateJobBatchStatus(BatchStatus.STOPPING);
        if (this.currentStoppableElementController != null) {
            this.currentStoppableElementController.stop();
        }
    }

    private void updateJobBatchStatus(BatchStatus batchStatus) {
        String methodName = "updateJobBatchStatus";

        if (logger.isLoggable(Level.INFO)) {
            logger.info(methodName + " Setting job batch status to: " + ExecutionStatus.getStringValue(batchStatus));
        }

        jobContext.setBatchStatus(ExecutionStatus.getStringValue(batchStatus));
        jobStatusService.updateJobBatchStatus(jobInstanceId, batchStatus);
        
        // update job information keyed by execution id
        if (persistenceService instanceof JDBCPersistenceManagerImpl){
        	Timestamp updateTS = new Timestamp(0);
        	persistenceService.jobExecutionStatusStringUpdate(jobExecution.getExecutionId(), JDBCPersistenceManagerImpl.BATCH_STATUS, ExecutionStatus.getStringValue(batchStatus), updateTS);
        }
    }

    public void setAnalyzerProxy(PartitionAnalyzerProxy analyzerProxy) {
        this.analyzerProxy = analyzerProxy;
    }

}
