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

import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;


import com.ibm.jbatch.container.context.impl.StepContextImpl;
import com.ibm.jbatch.container.jobinstance.RuntimeJobExecution;
import com.ibm.jbatch.container.util.PartitionDataWrapper;
import com.ibm.jbatch.jsl.model.Batchlet;
import com.ibm.jbatch.jsl.model.Chunk;
import com.ibm.jbatch.jsl.model.Decision;
import com.ibm.jbatch.jsl.model.Flow;
import com.ibm.jbatch.jsl.model.Partition;
import com.ibm.jbatch.jsl.model.Split;
import com.ibm.jbatch.jsl.model.Step;

public class ExecutionElementControllerFactory {

	private final static String CLASSNAME = ExecutionElementControllerFactory.class.getName();
	private final static Logger logger = Logger.getLogger(CLASSNAME);

	public static BaseStepControllerImpl getStepController(RuntimeJobExecution jobExecutionImpl, Step step, StepContextImpl stepContext, long rootJobExecutionId,  BlockingQueue<PartitionDataWrapper> analyzerQueue) {

		String methodName = "getStepController";

		if(logger.isLoggable(Level.FINER)) { logger.logp (Level.FINER, CLASSNAME, methodName, "Get StepController for", step.getId());}

		Partition partition = step.getPartition();
		if (partition != null) {

			if (partition.getMapper() != null ) {
				if (logger.isLoggable(Level.FINER)) {
					logger.logp(Level.FINER, CLASSNAME, methodName, "Found partitioned step with mapper" , step);
				}
				return new PartitionedStepControllerImpl(jobExecutionImpl, step, stepContext, rootJobExecutionId);
			}

			if (partition.getPlan() != null) {
				if (partition.getPlan().getPartitions() != null) {
					if (logger.isLoggable(Level.FINER)) {
						logger.logp(Level.FINER, CLASSNAME, methodName, "Found partitioned step with plan", step);
					}
					return new PartitionedStepControllerImpl(jobExecutionImpl, step, stepContext, rootJobExecutionId);
				}
			}
		}

		Batchlet batchlet = step.getBatchlet();
		if (batchlet != null) {
			if(logger.isLoggable(Level.FINER)) {  
				logger.finer("Found batchlet: " + batchlet + ", with ref= " + batchlet.getRef());
			}
			if (step.getChunk() != null) {
				throw new IllegalArgumentException("Step contains both a batchlet and a chunk.  Aborting.");
			}       
			return new BatchletStepControllerImpl(jobExecutionImpl, step, stepContext, rootJobExecutionId, analyzerQueue);
		} else {
			Chunk chunk = step.getChunk();
			if(logger.isLoggable(Level.FINER)) {  
				logger.finer("Found chunk: " + chunk);
			}
			if (chunk == null) {
				throw new IllegalArgumentException("Step does not contain either a batchlet or a chunk.  Aborting.");
			}
			return new ChunkStepControllerImpl(jobExecutionImpl, step, stepContext, rootJobExecutionId, analyzerQueue);
		}           
	} 

	public static DecisionControllerImpl getDecisionController(RuntimeJobExecution jobExecutionImpl, Decision decision) {
		return new DecisionControllerImpl(jobExecutionImpl, decision);
	} 
	
	public static FlowControllerImpl getFlowController(RuntimeJobExecution jobExecutionImpl, Flow flow, long rootJobExecutionId) {
		return new FlowControllerImpl(jobExecutionImpl, flow, rootJobExecutionId);
	} 
	
	public static SplitControllerImpl getSplitController(RuntimeJobExecution jobExecutionImpl, Split split, long rootJobExecutionId) {
		return new SplitControllerImpl(jobExecutionImpl, split, rootJobExecutionId);
	}  
}
