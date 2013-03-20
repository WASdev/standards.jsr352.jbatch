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

import java.util.logging.Level;
import java.util.logging.Logger;


import com.ibm.jbatch.container.IExecutionElementController;
import com.ibm.jbatch.container.jobinstance.RuntimeJobContextJobExecutionBridge;
import com.ibm.jbatch.container.jsl.ExecutionElement;
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

    public static IExecutionElementController getExecutionElementController(RuntimeJobContextJobExecutionBridge jobExecutionImpl, ExecutionElement executionElement) {

        String methodName = "getExecutionElementController";
        
        if(logger.isLoggable(Level.FINER)) { logger.logp (Level.FINER, CLASSNAME, methodName, "Get Execution Element Controller for", executionElement.getId());}
        
        if (executionElement instanceof Step) {
            Step step = (Step)executionElement;
            
            Partition partition = step.getPartition();
            
            if (partition != null) {
                
                if (partition.getMapper() != null ) {
                    if (logger.isLoggable(Level.FINER)) {
                        logger.logp(Level.FINER, CLASSNAME, methodName, "Found partitioned step with mapper" , step);
                    }

                    return new PartitionedStepControllerImpl(jobExecutionImpl, step);
                }
                
                if (partition.getPlan() != null) {
                    if (partition.getPlan().getPartitions() != null) {
                        if (logger.isLoggable(Level.FINER)) {
                            logger.logp(Level.FINER, CLASSNAME, methodName, "Found partitioned step with plan", step);
                        }

                        return new PartitionedStepControllerImpl(jobExecutionImpl, step);
                    }
                }
            	
            }
            
            
            Batchlet batchlet = step.getBatchlet();
            
            if (batchlet != null) {
                if(logger.isLoggable(Level.FINER)) {  logger.logp (Level.FINER, CLASSNAME, methodName, "Found batchlet", batchlet);}
                if(logger.isLoggable(Level.FINER)) {  logger.logp (Level.FINER, CLASSNAME, methodName, "Found batchlet", batchlet.getRef());}
                
                if (step.getChunk() != null) {
                    throw new IllegalArgumentException("Step contains both a batchlet and a chunk.  Aborting.");
                }       
                return new BatchletStepControllerImpl(jobExecutionImpl, step);
            } else {
                Chunk chunk = step.getChunk();
                if(logger.isLoggable(Level.FINER)) {  logger.logp (Level.FINER, CLASSNAME, methodName, "Found chunk", chunk);}
                if (chunk == null) {
                    throw new IllegalArgumentException("Step does not contain either a batchlet or a chunk.  Aborting.");
                }
                return new ChunkStepControllerImpl(jobExecutionImpl, step);
            }           
        } else if (executionElement instanceof Decision) {
            return new DecisionControllerImpl(jobExecutionImpl, (Decision)executionElement);
        } else if (executionElement instanceof Flow) {
            return new FlowControllerImpl(jobExecutionImpl, (Flow)executionElement);
        } else if (executionElement instanceof Split) {
            return new SplitControllerImpl(jobExecutionImpl, (Split)executionElement);
        }  else {
            throw new UnsupportedOperationException("Only support steps, flows, splits, and decisions so far.");
        }
        
    }
}
