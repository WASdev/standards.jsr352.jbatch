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


import com.ibm.jbatch.container.jsl.IllegalTransitionException;
import com.ibm.jbatch.container.jsl.ExecutionElement;
import com.ibm.jbatch.container.jsl.ModelNavigator;
import com.ibm.jbatch.container.jsl.Transition;
import com.ibm.jbatch.jsl.model.JSLJob;

public class JobNavigatorImpl extends AbstractNavigatorImpl<JSLJob> implements ModelNavigator<JSLJob> {

	private final static Logger logger = Logger.getLogger(JobNavigatorImpl.class.getName());
	private JSLJob job = null;

	public JobNavigatorImpl(JSLJob job) {
		this.job = job;
	}
	
	public String toString() {
		return "JobNavigatorImpl for job id = " + job.getId();
	}

	@Override
	public ExecutionElement getFirstExecutionElement(String restartOn)
			throws IllegalTransitionException {
		logger.fine("Getting first execution element in job, restartOn = " + restartOn);
		ExecutionElement firstElem = getFirstExecutionElement(job.getExecutionElements(), restartOn);
		logger.fine("Got first execution element in job = " + firstElem.getId());
		return firstElem;
	}
	
	@Override
	public ExecutionElement getFirstExecutionElement()
			throws IllegalTransitionException {
		return getFirstExecutionElement(null);
	}

	@Override
	public Transition getNextTransition(ExecutionElement currentExecutionElem, String currentStepExitStatus)
			throws IllegalTransitionException {
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Getting next transition in job, currentExecutionElem = " + currentExecutionElem);
		}
		Transition nextTransition = getNextTransition(currentExecutionElem, job.getExecutionElements(), currentStepExitStatus);
		logger.fine("Got next transition in job = " + nextTransition);
		return nextTransition;
	}

	@Override
	public JSLJob getRootModelElement() {
		return job;
	}
}
	
	
