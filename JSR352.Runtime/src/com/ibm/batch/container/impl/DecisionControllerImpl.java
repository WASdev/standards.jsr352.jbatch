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
import java.util.List;

import jsr352.batch.jsl.Decision;
import jsr352.batch.jsl.Property;

import com.ibm.batch.container.AbortedBeforeStartException;
import com.ibm.batch.container.IExecutionElementController;
import com.ibm.batch.container.artifact.proxy.DeciderProxy;
import com.ibm.batch.container.artifact.proxy.PartitionAnalyzerProxy;
import com.ibm.batch.container.artifact.proxy.ProxyFactory;
import com.ibm.batch.container.context.impl.FlowContextImpl;
import com.ibm.batch.container.context.impl.SplitContextImpl;
import com.ibm.batch.container.context.impl.StepContextImpl;
import com.ibm.batch.container.exception.BatchContainerServiceException;
import com.ibm.batch.container.jobinstance.RuntimeJobExecutionImpl;
import com.ibm.batch.container.util.ExecutionStatus;
import com.ibm.batch.container.util.ExecutionStatus.BatchStatus;
import com.ibm.batch.container.validation.ArtifactValidationException;

public class DecisionControllerImpl implements IExecutionElementController {
    
    protected RuntimeJobExecutionImpl jobExecutionImpl; 
    
    protected StepContextImpl<?, ? extends Externalizable> stepContext;
    protected SplitContextImpl splitContext;
    protected FlowContextImpl flowContext;
    
    protected Decision decision;

	private PartitionAnalyzerProxy analyzerProxy;
    
    public DecisionControllerImpl(RuntimeJobExecutionImpl jobExecutionImpl, Decision decision) {
        this.jobExecutionImpl = jobExecutionImpl;
        this.decision = decision;
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
    public String execute() throws AbortedBeforeStartException {

        ExecutionStatus status = new ExecutionStatus();

        String deciderId = decision.getRef();
        List<Property> propList = (decision.getProperties() == null) ? null : decision.getProperties().getPropertyList();

        DeciderProxy deciderProxy;

        //Create a decider proxy and inject the associated properties
        try {
            deciderProxy = ProxyFactory.createDeciderProxy(deciderId, propList);
        } catch (ArtifactValidationException e) {
            throw new BatchContainerServiceException("Cannot create the decider [" + deciderId + "]", e);
        }

        /* Set the contexts associated with this scope */

        //job is always in scope
        deciderProxy.setJobContext(jobExecutionImpl.getJobContext());

        //the parent controller will only pass one valid context to a decision controller
        //so two of these contexts will always be null
        deciderProxy.setStepContext(stepContext);
        deciderProxy.setFlowContext(flowContext);
        deciderProxy.setSplitContext(splitContext);

        //make the decision
        String exitStatus = deciderProxy.decide();
        
        return exitStatus;

    }

    @Override
    public void stop() { 
    	this.stepContext.setBatchStatus(ExecutionStatus.getStringValue(BatchStatus.STOPPING));

    }

	@Override
	public void setAnalyzerProxy(PartitionAnalyzerProxy analyzerProxy) {
		this.analyzerProxy = analyzerProxy;
		
	}


}
