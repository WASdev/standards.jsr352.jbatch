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
package com.ibm.jbatch.container.jsl.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.jbatch.container.jsl.ExecutionElement;
import com.ibm.jbatch.container.jsl.FlowNavigator;
import com.ibm.jbatch.container.jsl.IllegalTransitionException;
import com.ibm.jbatch.container.jsl.Transition;
import com.ibm.jbatch.jsl.model.Flow;

public class FlowNavigatorImpl extends AbstractNavigatorImpl implements FlowNavigator {

	private final static Logger logger = Logger.getLogger(FlowNavigatorImpl.class.getName());
	private Flow flow = null;

	public FlowNavigatorImpl(Flow flow) {
		this.flow = flow;
	}

	public String toString() {
		return "FlowNavigatorImpl for flow id = " + flow.getId();
	}

	@Override
	public Flow getFlow() {
		return this.flow;
	}

	@Override
	public ExecutionElement getFirstExecutionElementInFlow(String restartOn)
			throws IllegalTransitionException {
		logger.fine("Getting first execution element in flow, restartOn = " + restartOn);
		ExecutionElement firstElem = getFirstExecutionElement(flow.getExecutionElements(), restartOn);
		logger.fine("Got first execution element in flow = " + firstElem.getId());
		return firstElem;
	}

	@Override
	public Transition getNextTransitionInFlow(ExecutionElement currentExecutionElem, String currentStepExitStatus)
			throws IllegalTransitionException {
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Getting next transition in flow, currentExecutionElem = " + currentExecutionElem);
		}
		Transition nextTransition = getNextTransition(currentExecutionElem, flow.getExecutionElements(), currentStepExitStatus);
		logger.fine("Got next transition in flow = " + nextTransition);
		return nextTransition;
	}
}