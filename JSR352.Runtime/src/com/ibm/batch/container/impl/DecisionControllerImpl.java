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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import javax.batch.operations.JobOperator.BatchStatus;
import javax.batch.runtime.StepExecution;

import jsr352.batch.jsl.Decision;
import jsr352.batch.jsl.Flow;
import jsr352.batch.jsl.Property;
import jsr352.batch.jsl.Split;
import jsr352.batch.jsl.Step;

import com.ibm.batch.container.IExecutionElementController;
import com.ibm.batch.container.artifact.proxy.DeciderProxy;
import com.ibm.batch.container.artifact.proxy.InjectionReferences;
import com.ibm.batch.container.artifact.proxy.PartitionAnalyzerProxy;
import com.ibm.batch.container.artifact.proxy.ProxyFactory;
import com.ibm.batch.container.context.impl.StepContextImpl;
import com.ibm.batch.container.exception.BatchContainerServiceException;
import com.ibm.batch.container.jobinstance.RuntimeJobExecutionImpl;
import com.ibm.batch.container.util.ExecutionStatus;
import com.ibm.batch.container.util.PartitionDataWrapper;
import com.ibm.batch.container.validation.ArtifactValidationException;
import com.ibm.batch.container.xjcl.ExecutionElement;

public class DecisionControllerImpl implements IExecutionElementController {
    
    protected RuntimeJobExecutionImpl jobExecutionImpl; 
    
    protected StepContextImpl<?, ? extends Externalizable> stepContext;
    
    protected Decision decision;

	private PartitionAnalyzerProxy analyzerProxy;
	
	protected List<StepExecution> stepExecutions = null;
	
	// This element is either a Flow or Split
	// it is the previous executable element before the decision
	protected ExecutionElement executionElement = null;
    
    public DecisionControllerImpl(RuntimeJobExecutionImpl jobExecutionImpl, Decision decision) {
        this.jobExecutionImpl = jobExecutionImpl;
        this.decision = decision;
    }

    
    public void setStepContext(StepContextImpl<?, ? extends Externalizable> stepContext) {
        
    	//throw new UnsupportedOperationException("Shouldn't be called on a decision.");
    	this.stepContext = stepContext;
        
    }
   
    public void setStepExecution(Flow flow, StepExecution stepExecution) {
    	this.executionElement = flow;
    	stepExecutions = new ArrayList<StepExecution>();
    	stepExecutions.add(stepExecution);
    }
    
    public void setStepExecution(Step step, StepExecution stepExecution) {
    	this.executionElement = step;
    	stepExecutions = new ArrayList<StepExecution>();
    	stepExecutions.add(stepExecution);
    }
   
    public void setStepExecutions(Split split, List<StepExecution> stepExecutions) {
    	this.executionElement = split;
    	this.stepExecutions = stepExecutions;
    }
   

    @Override
    public String execute() throws Exception {

        ExecutionStatus status = new ExecutionStatus();

        String deciderId = decision.getRef();
        List<Property> propList = (decision.getProperties() == null) ? null : decision.getProperties().getPropertyList();

        DeciderProxy deciderProxy;

        //Create a decider proxy and inject the associated properties
        
        /* Set the contexts associated with this scope */
        //job context is always in scope
        //the parent controller will only pass one valid context to a decision controller
        //so two of these contexts will always be null
        InjectionReferences injectionRef = new InjectionReferences(jobExecutionImpl.getJobContext(), stepContext, 
                propList);
        
        try {
            deciderProxy = ProxyFactory.createDeciderProxy(deciderId,injectionRef );
        } catch (ArtifactValidationException e) {
            throw new BatchContainerServiceException("Cannot create the decider [" + deciderId + "]", e);
        }

        String exitStatus = null;
        if(executionElement instanceof Split) {
        	exitStatus = deciderProxy.decide(this.stepExecutions.toArray(new StepExecution[stepExecutions.size()]));
        } else {
        	exitStatus = deciderProxy.decide(this.stepExecutions.get(0));
        }
        
        return exitStatus;

    }

    @Override
    public void stop() { 
    	this.stepContext.setBatchStatus(BatchStatus.STOPPING);

    }

    @Override
    public void setAnalyzerQueue(LinkedBlockingQueue<PartitionDataWrapper> analyzerQueue) {
        //no-op
    }


}
