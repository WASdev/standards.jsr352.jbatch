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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jsr352.batch.jsl.Decision;
import jsr352.batch.jsl.End;
import jsr352.batch.jsl.Fail;
import jsr352.batch.jsl.Flow;
import jsr352.batch.jsl.Split;
import jsr352.batch.jsl.Step;
import jsr352.batch.jsl.Stop;

import com.ibm.batch.container.AbortedBeforeStartException;
import com.ibm.batch.container.IExecutionElementController;
import com.ibm.batch.container.artifact.proxy.FlowListenerProxy;
import com.ibm.batch.container.artifact.proxy.PartitionAnalyzerProxy;
import com.ibm.batch.container.context.impl.FlowContextImpl;
import com.ibm.batch.container.context.impl.SplitContextImpl;
import com.ibm.batch.container.context.impl.StepContextImpl;
import com.ibm.batch.container.exception.BatchContainerRuntimeException;
import com.ibm.batch.container.jobinstance.RuntimeJobExecutionImpl;
import com.ibm.batch.container.util.ExecutionStatus;
import com.ibm.batch.container.util.ExecutionStatus.BatchStatus;
import com.ibm.batch.container.xjcl.ControlElement;
import com.ibm.batch.container.xjcl.ExecutionElement;
import com.ibm.batch.container.xjcl.Navigator;
import com.ibm.batch.container.xjcl.NavigatorFactory;
import com.ibm.batch.container.xjcl.Transition;

public class FlowControllerImpl implements IExecutionElementController {

	private final static String CLASSNAME = PartitionedStepControllerImpl.class.getName();
	private final static Logger logger = Logger.getLogger(CLASSNAME);
	
	private final RuntimeJobExecutionImpl jobExecutionImpl;
	
    protected SplitContextImpl splitContext;
    protected FlowContextImpl currentFlowContext;
    
    protected Flow flow;
    
    private final Navigator<Flow> flowNavigator;
	
    //
    // The currently executing controller, this will only be set to the 
    // local variable reference when we are ready to accept stop events for
    // this execution.
    private volatile IExecutionElementController currentStoppableElementController = null;
    
	private PartitionAnalyzerProxy analyzerProxy;
	List<FlowListenerProxy> flowListeners = null;
	

    public FlowControllerImpl(RuntimeJobExecutionImpl jobExecutionImpl, Flow flow) {
        this.jobExecutionImpl = jobExecutionImpl;
        this.flow = flow;
        
        flowNavigator = NavigatorFactory.createFlowNavigator(flow);
    }

   
    @Override
    public String execute() throws AbortedBeforeStartException {
        final String methodName = "execute";
        if (logger.isLoggable(Level.FINE)) {
            logger.entering(CLASSNAME, methodName);
        }

        try {
        	
        	//There is no setup for a flow so it can immediately move to STARTED state
            currentFlowContext.setBatchStatus(ExecutionStatus.getStringValue(BatchStatus.STARTED));

            
            //Set up flow listeners and call beforeFlow()
    		this.flowListeners = jobExecutionImpl.getListenerFactory().getFlowListeners(flow);

    		for (FlowListenerProxy listenerProxy : flowListeners) {
    			listenerProxy.setJobContext(jobExecutionImpl.getJobContext());
    			listenerProxy.setSplitContext(splitContext);
    			listenerProxy.setFlowContext(this.currentFlowContext);
    		}

    		// Call @BeforeFlow on all the flow listeners
    		for (FlowListenerProxy listenerProxy : flowListeners) {
    			listenerProxy.beforeFlow();
    		}
            
            
            // --------------------
            // The same as a simple Job. Loop to complete all steps and decisions in the flow.
            // --------------------
            doExecutionLoop(flowNavigator);

            String curStatusString = currentFlowContext.getBatchStatus();
            if (curStatusString == null) {
                throw new IllegalStateException("Flow BatchStatus should have been set by now");
            }

            BatchStatus curStatus = ExecutionStatus.getBatchStatusEnum(curStatusString);

            // BatchStatus may already have been set by a JSL <stop> or <fail>
            // decision directive.
            if (!(curStatus.equals(BatchStatus.FAILED) || curStatus.equals(BatchStatus.STOPPED))) {
            	currentFlowContext.setBatchStatus(ExecutionStatus.getStringValue(BatchStatus.COMPLETED));
            }

    		// Call @AfterSplit on all the split listeners
    		for (FlowListenerProxy listenerProxy : flowListeners) {
    			listenerProxy.afterFlow();
    		}
            
            if (currentFlowContext.getExitStatus() == null) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("No flow-level exitStatus set, defaulting to flow batch Status = " + currentFlowContext.getBatchStatus());
                }
                currentFlowContext.setExitStatus(currentFlowContext.getBatchStatus());
            }
            
            return currentFlowContext.getBatchStatus();

        } catch (Throwable t) {
                        
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(CLASSNAME + ": caught exception/error: " + t.getMessage() + " : Stack trace: " + sw.toString());
            }
            
            currentFlowContext.setBatchStatus(ExecutionStatus.getStringValue(BatchStatus.FAILED));

            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Flow failed with exception/error: " + t.getMessage());
            }

            if (currentFlowContext.getExitStatus() == null) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("No flow-level exitStatus set, defaulting to flow batch Status = " + currentFlowContext.getBatchStatus());
                }
                currentFlowContext.setExitStatus(currentFlowContext.getBatchStatus());
            }

            throw new BatchContainerRuntimeException(t);
        } finally {

            // Persist flow status, setting default if not set

            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Flow complete for flow id=" + flow.getId() + ", executionId="
                        + jobExecutionImpl.getExecutionId() + ", batchStatus=" + currentFlowContext.getBatchStatus() + ", exitStatus="
                        + currentFlowContext.getExitStatus());
            }

            try {
                //use the job status service here to persist the status        
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

    private void doExecutionLoop(Navigator<Flow> flowNavigator) {
        final String methodName = "doExecutionLoop";

        ExecutionElement currentExecutionElement = null;
        try {
            currentExecutionElement = flowNavigator.getFirstExecutionElement(jobExecutionImpl.getRestartOn());
        } catch (Exception e) {
            throw new IllegalArgumentException("Flow doesn't contain a step.", e);
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

            if (!(currentExecutionElement instanceof Step) && !(currentExecutionElement instanceof Decision)) {
                throw new UnsupportedOperationException("Only support step, and decision within a flow");
            }

            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Next execution element = " + currentExecutionElement.getId());
            }

            IExecutionElementController elementController = 
                ExecutionElementControllerFactory.getExecutionElementController(jobExecutionImpl, currentExecutionElement);

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
                	
                	//FIXME Not sure if this is valid. Can a split context be available to a decider within a flow?
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
                	
                	//FIXME Not sure if this is valid. Can a flow context be available to a decider within a flow?
                	
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
                throw new IllegalArgumentException("A flow cannot be nested within another flow.");
            } else if (currentExecutionElement instanceof Split) {
            	throw new IllegalArgumentException("A split cannot be nested within a flow.");
            }

            // check for stop before every executing each execution element
            if (ExecutionStatus.getStringValue(BatchStatus.STOPPING).equals(this.currentFlowContext.getBatchStatus())) {
                currentFlowContext.setBatchStatus(ExecutionStatus.getStringValue(BatchStatus.STOPPED));

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
            if (ExecutionStatus.getBatchStatusEnum(this.currentFlowContext.getBatchStatus()).equals(BatchStatus.STOPPING)) {

            	currentFlowContext.setBatchStatus(ExecutionStatus.getStringValue(BatchStatus.STOPPED));

                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(methodName + " Exiting as job has been stopped");
                }
                return;
            }

            Transition nextTransition = flowNavigator.getNextTransition(currentExecutionElement, executionElementExitStatus);

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

                    this.currentFlowContext.setBatchStatus(ExecutionStatus.getStringValue(BatchStatus.STOPPED));
                    //FIXME jobStatusService.updateJobStatusFromJSLStop(jobInstanceId, restartOn);

                    String newExitStatus = ((Stop) controlElem).getExitStatus();
                    if (newExitStatus != null && !newExitStatus.isEmpty()) { // overrides with exit status in JSL @exit-status
                        currentFlowContext.setExitStatus(newExitStatus);  
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
                    this.currentFlowContext.setBatchStatus(ExecutionStatus.getStringValue(BatchStatus.COMPLETED));
                    String newExitStatus = ((End) controlElem).getExitStatus();
                    if (newExitStatus != null && !newExitStatus.isEmpty()) { // overrides with exit status in JSL @exit-status
                    	this.currentFlowContext.setExitStatus(newExitStatus); 
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine(methodName + " , on end, setting new JSL-specified exit status to: " + newExitStatus);
                        }
                    } 
                } else if (controlElem instanceof Fail) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine(methodName + " , next control element is a <fail>: " + controlElem);
                    }
                    this.currentFlowContext.setBatchStatus(ExecutionStatus.getStringValue(BatchStatus.FAILED));
                    String newExitStatus = ((Fail) controlElem).getExitStatus();
                    if (newExitStatus != null && !newExitStatus.isEmpty()) { // overrides
                                                                             // with
                        currentFlowContext.setExitStatus(newExitStatus); // exit status
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
		currentFlowContext.setBatchStatus(ExecutionStatus.getStringValue(BatchStatus.STOPPING));
        if (this.currentStoppableElementController != null) {
            this.currentStoppableElementController.stop();
        }

    }

    public void setStepContext(StepContextImpl<?, ? extends Externalizable> stepContext) {
        throw new BatchContainerRuntimeException("Incorrect usage: step context is not in scope within a flow.");
    }

    public void setSplitContext(SplitContextImpl splitContext) {
        this.splitContext = splitContext;
    }

    public void setFlowContext(FlowContextImpl flowContext) {
    	this.currentFlowContext = flowContext;
    }
    
    public void setAnalyzerProxy(PartitionAnalyzerProxy analyzerProxy) {
        this.analyzerProxy = analyzerProxy;
    }
}
