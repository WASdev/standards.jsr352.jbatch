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

import java.io.Externalizable;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Stack;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.operations.JobOperator.BatchStatus;
import javax.batch.runtime.StepExecution;

import com.ibm.jbatch.container.AbortedBeforeStartException;
import com.ibm.jbatch.container.IController;
import com.ibm.jbatch.container.IExecutionElementController;
import com.ibm.jbatch.container.artifact.proxy.InjectionReferences;
import com.ibm.jbatch.container.artifact.proxy.JobListenerProxy;
import com.ibm.jbatch.container.artifact.proxy.ListenerFactory;
import com.ibm.jbatch.container.context.impl.JobContextImpl;
import com.ibm.jbatch.container.context.impl.StepContextImpl;
import com.ibm.jbatch.container.exception.BatchContainerRuntimeException;
import com.ibm.jbatch.container.jobinstance.JobExecutionHelper;
import com.ibm.jbatch.container.jobinstance.RuntimeJobExecutionHelper;
import com.ibm.jbatch.container.jsl.TransitionElement;
import com.ibm.jbatch.container.jsl.ExecutionElement;
import com.ibm.jbatch.container.jsl.Navigator;
import com.ibm.jbatch.container.jsl.Transition;
import com.ibm.jbatch.container.services.IJobStatusManagerService;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.services.impl.JDBCPersistenceManagerImpl;
import com.ibm.jbatch.container.servicesmanager.ServicesManagerImpl;
import com.ibm.jbatch.container.util.BatchWorkUnit;
import com.ibm.jbatch.container.util.PartitionDataWrapper;
import com.ibm.jbatch.jsl.model.Decision;
import com.ibm.jbatch.jsl.model.End;
import com.ibm.jbatch.jsl.model.Fail;
import com.ibm.jbatch.jsl.model.Flow;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.jbatch.jsl.model.JSLProperties;
import com.ibm.jbatch.jsl.model.Property;
import com.ibm.jbatch.jsl.model.Split;
import com.ibm.jbatch.jsl.model.Step;
import com.ibm.jbatch.jsl.model.Stop;

public class JobControllerImpl implements IController {

    private final static String CLASSNAME = JobControllerImpl.class.getName();
    private final static Logger logger = Logger.getLogger(CLASSNAME);

    private IJobStatusManagerService jobStatusService = null;
    private IPersistenceManagerService persistenceService = null;

    private RuntimeJobExecutionHelper jobExecution = null;

    private final JobContextImpl<?> jobContext;
    private final Navigator<JSLJob> jobNavigator;

    private BlockingQueue<PartitionDataWrapper> analyzerQueue;
    private Stack<String> subJobExitStatusQueue;
	private ListenerFactory listenerFactory = null;
    private final long jobInstanceId;
    private List<String> containment = null;
    private RuntimeJobExecutionHelper rootJobExecution = null;

    //
    // The currently executing controller, this will only be set to the 
    // local variable reference when we are ready to accept stop events for
    // this execution.
    private volatile IExecutionElementController currentStoppableElementController = null;


    public JobControllerImpl(RuntimeJobExecutionHelper jobExecution, List<String> containment, RuntimeJobExecutionHelper rootJobExecution) {
        this.jobExecution = jobExecution;
        this.jobContext = jobExecution.getJobContext();
        this.containment = containment;
        this.rootJobExecution = rootJobExecution;
        jobNavigator = jobExecution.getJobNavigator();
        jobInstanceId = jobExecution.getJobInstance().getInstanceId();
        jobStatusService = ServicesManagerImpl.getInstance().getJobStatusManagerService();
        persistenceService = ServicesManagerImpl.getInstance().getPersistenceManagerService();
        
        setContextProperties();
        setupListeners();
    }
    
    private void setContextProperties() {
    	JSLJob jobModel = jobExecution.getJobNavigator().getJSL();
    	JSLProperties jslProps = jobModel.getProperties();
    	
    	if (jslProps != null) {
    		Properties contextProps = jobContext.getProperties();
    		for (Property property : jslProps.getPropertyList()) {
        		contextProps.setProperty(property.getName(), property.getValue());
        	}	
    	}
    	
    }
    
    private void setupListeners() {
        JSLJob jobModel = jobExecution.getJobNavigator().getJSL();   
        
        InjectionReferences injectionRef = new InjectionReferences(jobContext, null, null);
        
        listenerFactory = new ListenerFactory(jobModel, injectionRef);
        jobExecution.setListenerFactory(listenerFactory);
    }

    public void executeJob() {

        final String methodName = "executeJob";
        if (logger.isLoggable(Level.FINE)) {
            logger.entering(CLASSNAME, methodName);
        }

        try {
            // Periodic check for stopping job
            if (BatchStatus.STOPPING.equals(jobContext.getBatchStatus())) {
                updateJobBatchStatus(BatchStatus.STOPPED);
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(methodName + " Exiting as job has been stopped");
                }
                return;
            }

            updateJobBatchStatus(BatchStatus.STARTING);

            // Periodic check for stopping job
            if (jobContext.getBatchStatus().equals(BatchStatus.STOPPING)) {
                updateJobBatchStatus(BatchStatus.STOPPED);
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(methodName + " Exiting as job has been stopped");
                }
                return;
            }

            updateJobBatchStatus(BatchStatus.STARTED);
            
            List<JobListenerProxy> jobListeners = listenerFactory.getJobListeners();

            // Call @BeforeJob on all the job listeners
            for (JobListenerProxy listenerProxy : jobListeners) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(methodName + " Invoking @BeforeJob on jobListener: " + listenerProxy.getDelegate() + " of type: " + listenerProxy.getDelegate().getClass());
                }
                listenerProxy.beforeJob();
            }

            // Periodic check for stopping job
            if (jobContext.getBatchStatus().equals(BatchStatus.STOPPING)) {
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
            BatchStatus currentStatus = jobContext.getBatchStatus();
            if (currentStatus == null) {
                throw new IllegalStateException("Job BatchStatus should have been set by now");
            }
            BatchStatus curStatus = currentStatus;

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
                jobContext.setExitStatus(jobContext.getBatchStatus().name());
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
            
            logger.severe(CLASSNAME + ": caught exception/error: " + t.getMessage() + " : Stack trace: " + sw.toString());
            
            updateJobBatchStatus(BatchStatus.FAILED);

            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Job failed with exception/error: " + t.getMessage());
            }

            if (jobContext.getExitStatus() == null) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("No job-level exitStatus set, defaulting to job batch Status = " + jobContext.getBatchStatus());
                }
                jobContext.setExitStatus(jobContext.getBatchStatus().name());
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
                        jobContext.getBatchStatus(), 
                        jobContext.getExitStatus());
              //set update time onto the runtime JobExecution Obj - should I also update the status string here too?
                long time = System.currentTimeMillis();
            	Timestamp updateTS = new Timestamp(time);
            	jobExecution.setLastUpdateTime(updateTS);
            	jobExecution.setEndTime(updateTS);
                if (persistenceService instanceof JDBCPersistenceManagerImpl){	
                	persistenceService.jobExecutionStatusStringUpdate(jobExecution.getExecutionId(), JDBCPersistenceManagerImpl.EXIT_STATUS, jobContext.getExitStatus(), updateTS);
                	persistenceService.jobExecutionTimestampUpdate(jobExecution.getExecutionId(), JDBCPersistenceManagerImpl.END_TIME, updateTS);
                }                        
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

    private void doExecutionLoop(Navigator jobNavigator) throws Exception {
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

        ExecutionElement previousExecutionElement = null;
        
        IExecutionElementController previousElementController = null;

        while (true) {

            if (!(currentExecutionElement instanceof Step) && !(currentExecutionElement instanceof Decision) 
            		&& !(currentExecutionElement instanceof Flow) && !(currentExecutionElement instanceof Split)) {
                throw new IllegalStateException("Found unknown currentExecutionElement type = " + currentExecutionElement.getClass().getName());
            }

            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Next execution element = " + currentExecutionElement.getId());
            }

            IExecutionElementController elementController = 
                ExecutionElementControllerFactory.getExecutionElementController(jobExecution, currentExecutionElement);

            //If this is a sub job it may have a analyzer queue we need to pass along
            elementController.setAnalyzerQueue(this.analyzerQueue);
            
            //If this is a sub job, pass along exit status queue
            elementController.setSubJobExitStatusQueue(this.subJobExitStatusQueue);
            
            // Depending on the execution element new up the associated context
            // and add it to the controller
            if (currentExecutionElement instanceof Decision) {
                if (previousExecutionElement == null) {
                    // only job context is available to the decider since it is
                    // the first execution element in the job
                } else if (previousExecutionElement instanceof Decision) {
                    throw new BatchContainerRuntimeException("A decision cannot precede another decision.");
                } else if (previousExecutionElement instanceof Step) {
                    // the
                    // context
                    // from the
                    // previous
                    // execution
                    // element
                    StepExecution lastStepExecution = getLastStepExecution((Step) previousExecutionElement);
                    
                    ((DecisionControllerImpl)elementController).setStepExecution((Step)previousExecutionElement, lastStepExecution);

                } else if (previousExecutionElement instanceof Split) {
                	
                	List<StepExecution> stepExecutions = getSplitStepExecutions(previousElementController);
           		
            		((DecisionControllerImpl)elementController).setStepExecutions((Split)previousExecutionElement, stepExecutions);
            		                    
                } else if (previousExecutionElement instanceof Flow) {
                	
                    // get last step in flow
                    Step last = getLastStepInTheFlow(previousExecutionElement);
                    
                    // get last step StepExecution
                    StepExecution lastStepExecution = getLastStepExecution(last);
                    
                    ((DecisionControllerImpl)elementController).setStepExecution((Flow)previousExecutionElement, lastStepExecution);
           
                }

            } else if (currentExecutionElement instanceof Step) {
                String stepId = ((Step) currentExecutionElement).getId();
                stepContext = new StepContextImpl<Object, Externalizable>(stepId);
                elementController.setStepContext(stepContext);
            } else if (currentExecutionElement instanceof Flow) {
            	String flowId = ((Flow) currentExecutionElement).getId();
            } else if (currentExecutionElement instanceof Split) {
            	String splitId = ((Split) currentExecutionElement).getId();
            }

            // check for stop before every executing each execution element
            if (jobContext.getBatchStatus().equals(BatchStatus.STOPPING)) {
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
                //we need to create a new copy of the containment list to pass around because we
                //don't want to modify the original containment list, since it can get reused
                //multiple times
                ArrayList<String> currentContainment = null;
                if (containment != null) {
                    currentContainment = new ArrayList<String>();
                    currentContainment.addAll(containment);
                }
                executionElementExitStatus = elementController.execute(currentContainment, this.rootJobExecution);
            } catch (AbortedBeforeStartException e) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Execution failed before even getting to execute execution element = " + currentExecutionElement.getId());
                }
                logger.warning("Execution failed, InstanceId: " + this.jobInstanceId + ", executionId = " + this.jobExecution.getExecutionId());
                throw new IllegalStateException("Execution failed before even getting to execute execution element = " + 
                        currentExecutionElement.getId() + "; breaking out of execution loop.");                
            }
            
            // set the execution element controller to null so we don't try to
            // call stop
            // on it after the element has finished executing
            this.currentStoppableElementController = null; 
            previousElementController = elementController;

            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Done executing element=" + currentExecutionElement.getId() + ", exitStatus=" + executionElementExitStatus);
            }

            // If we are currently in STOPPING state, then we can now move transition to STOPPED state.
            if (jobContext.getBatchStatus().equals(BatchStatus.STOPPING)) {

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
                TransitionElement controlElem = nextTransition.getControlElement();

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

	private List<StepExecution> getSplitStepExecutions(
			IExecutionElementController previousElementController) {
		List<StepExecution> stepExecutions = new ArrayList<StepExecution>();
		if(previousElementController != null) {
			SplitControllerImpl controller = (SplitControllerImpl)previousElementController;
			for (BatchWorkUnit batchWorkUnit : controller.getParallelJobExecs()) {
				                			
				StepExecution lastStepExecution = null;
				List<StepExecution<?>> stepExecs = persistenceService.getStepExecutionIDListQueryByJobID(batchWorkUnit.getJobExecutionImpl().getExecutionId());
				for (StepExecution stepExecution : stepExecs) {
					lastStepExecution = stepExecution;
				}
				stepExecutions.add(lastStepExecution);
			}
		}
		return stepExecutions;
	}

	private StepExecution getLastStepExecution(Step last) {
		StepExecution lastStepExecution = null;
		List<StepExecution<?>> stepExecs = persistenceService.getStepExecutionIDListQueryByJobID(jobExecution.getExecutionId());
		for (StepExecution stepExecution : stepExecs) {
			if(last.getId().equals(stepExecution.getStepName())) {
				lastStepExecution = stepExecution;
			}
		}
		return lastStepExecution;
	}

	private Step getLastStepInTheFlow(ExecutionElement previousExecutionElement) {
		Flow flow = (Flow)previousExecutionElement;
		Step last = null;
		for (ExecutionElement elem : flow.getExecutionElements()) {
			if(elem instanceof Step) {
				last = (Step) elem;
			}
		}
		return last;
	}

    @Override
    public void stop() {
    	if (jobContext.getBatchStatus().equals(BatchStatus.STARTING) ||
    			jobContext.getBatchStatus().equals(BatchStatus.STARTED)) {
    	
        updateJobBatchStatus(BatchStatus.STOPPING);
        if (this.currentStoppableElementController != null) {
            this.currentStoppableElementController.stop();
        }
    	} else {
        	//TODO do we need to throw an error if the batchlet is already stopping/stopped
    		//a stop gets issued twice
    	}

    }

    private void updateJobBatchStatus(BatchStatus batchStatus) {
        String methodName = "updateJobBatchStatus";

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(methodName + " Setting job batch status to: " + batchStatus);
        }

        jobContext.setBatchStatus(batchStatus);
        jobStatusService.updateJobBatchStatus(jobInstanceId, batchStatus);
        
        //set update time onto the runtime JobExecution Obj - should I also update the status string here too?
        long time = System.currentTimeMillis();
    	Timestamp timestamp = new Timestamp(time);
    	jobExecution.setLastUpdateTime(timestamp);
    	
    	switch (batchStatus) {
    	case STARTING:
    		//perisistence call to update batch status and update time
    		JobExecutionHelper.updateBatchStatusUPDATEonly(jobExecution.getExecutionId(), batchStatus.name(), timestamp);
    		break;
    	case STARTED:
    		//perisistence call to update batch status and update time and start time
    		// Timestamp startTS = new Timestamp(time);
    		JobExecutionHelper.updateBatchStatusSTART(jobExecution.getExecutionId(), batchStatus.name(), timestamp);
    		break;
    	case STOPPING:
    		//perisistence call to update batch status and update time
    		JobExecutionHelper.updateBatchStatusUPDATEonly(jobExecution.getExecutionId(), batchStatus.name(), timestamp);
    		break;
    	case STOPPED:
    		//perisistence call to update batch status and update time and stop time
    		// Timestamp stopTS = new Timestamp(time);
    		JobExecutionHelper.updateBatchStatusSTOP(jobExecution.getExecutionId(), batchStatus.name(), timestamp);
    		break;
    	case COMPLETED:
    		//perisistence call to update batch status and update time and end time
    		// Timestamp stopTS = new Timestamp(time);
    		JobExecutionHelper.updateBatchStatusCOMPLETED(jobExecution.getExecutionId(), batchStatus.name(), timestamp);
    		break;
    	case FAILED:
    		//perisistence call to update batch status and update time and end time
    		// Timestamp endTS = new Timestamp(time);
    		JobExecutionHelper.updateBatchStatusFAILED(jobExecution.getExecutionId(), batchStatus.name(), timestamp);
    		break;
    	default:
    			//?
        }
    	
        // update job execution instance information keyed by execution id
        //if (persistenceService instanceof JDBCPersistenceManagerImpl){	
        //	persistenceService.jobExecutionStatusStringUpdate(jobExecution.getExecutionId(), JDBCPersistenceManagerImpl.BATCH_STATUS, ExecutionStatus.getStringValue(batchStatus), updateTS);
        //}
        
    }

    public void setAnalyzerQueue(BlockingQueue<PartitionDataWrapper> analyzerQueue) {
        this.analyzerQueue = analyzerQueue;
    }
    
    public void setSubJobExitStatusQueue(Stack<String> subJobExitStatusQueue) {
        this.subJobExitStatusQueue = subJobExitStatusQueue;
    }

}
