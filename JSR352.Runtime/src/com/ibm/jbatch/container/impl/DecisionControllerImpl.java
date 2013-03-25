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
import java.util.concurrent.BlockingQueue;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.StepExecution;

import com.ibm.jbatch.container.IExecutionElementController;
import com.ibm.jbatch.container.artifact.proxy.DeciderProxy;
import com.ibm.jbatch.container.artifact.proxy.InjectionReferences;
import com.ibm.jbatch.container.artifact.proxy.ProxyFactory;
import com.ibm.jbatch.container.context.impl.StepContextImpl;
import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.jobinstance.RuntimeJobContextJobExecutionBridge;
import com.ibm.jbatch.container.jsl.ExecutionElement;
import com.ibm.jbatch.container.status.InternalExecutionElementStatus;
import com.ibm.jbatch.container.util.PartitionDataWrapper;
import com.ibm.jbatch.container.validation.ArtifactValidationException;
import com.ibm.jbatch.jsl.model.Decision;
import com.ibm.jbatch.jsl.model.Flow;
import com.ibm.jbatch.jsl.model.Property;
import com.ibm.jbatch.jsl.model.Split;
import com.ibm.jbatch.jsl.model.Step;

public class DecisionControllerImpl implements IExecutionElementController {
    
    protected RuntimeJobContextJobExecutionBridge jobExecutionImpl; 
    
    protected StepContextImpl stepContext;
    
    protected Decision decision;


	
	protected List<StepExecution> stepExecutions = null;
	
	// This element is either a Flow or Split
	// it is the previous executable element before the decision
	protected ExecutionElement executionElement = null;
    
    public DecisionControllerImpl(RuntimeJobContextJobExecutionBridge jobExecutionImpl, Decision decision) {
        this.jobExecutionImpl = jobExecutionImpl;
        this.decision = decision;
    }

    
    public void setStepContext(StepContextImpl stepContext) {
    	throw new UnsupportedOperationException("Shouldn't be called on a decision.");
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
    public InternalExecutionElementStatus execute(RuntimeJobContextJobExecutionBridge rootJobExecution) {

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

        String exitStatus = deciderProxy.decide(this.stepExecutions.toArray(new StepExecution[stepExecutions.size()]));
        
        //Set the value returned from the decider as the job context exit status.
        this.jobExecutionImpl.getJobContext().setExitStatus(exitStatus);
        
        return new InternalExecutionElementStatus(exitStatus);

    }

    @Override
    public void stop() { 
    	this.stepContext.setBatchStatus(BatchStatus.STOPPING);

    }

    @Override
    public void setAnalyzerQueue(BlockingQueue<PartitionDataWrapper> analyzerQueue) {
        //no-op
    }


}
