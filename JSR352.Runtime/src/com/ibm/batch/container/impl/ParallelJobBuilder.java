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

import java.util.Properties;

import jsr352.batch.jsl.Flow;
import jsr352.batch.jsl.JSLJob;
import jsr352.batch.jsl.ObjectFactory;
import jsr352.batch.jsl.Partition;
import jsr352.batch.jsl.PartitionPlan;
import jsr352.batch.jsl.Split;
import jsr352.batch.jsl.Step;

import com.ibm.batch.container.xjcl.CloneUtility;

public class ParallelJobBuilder {

    /*
     * Build a generated job with only one flow in it to submit to the
     * BatchKernel. This is used to build subjobs from splits.
     * 
     */
    public static JSLJob buildSubJob(Long parentJobExecutionId, Split split, Flow flow, Properties[] subJobParameters, int partitionInstance) {

        ObjectFactory jslFactory = new ObjectFactory();
        JSLJob subJob = jslFactory.createJSLJob();

        // Set the generated subjob id
        String subJobId = buildSubJobId(parentJobExecutionId, split.getId(), flow.getId(), partitionInstance);
        subJob.setId(subJobId);

        //We don't need to do a deep copy here since each flow is already independent of all others, unlike in a partition
        //where one step instance can be executed with different properties on multiple threads.

        subJob.getExecutionElements().add(flow);
        


        if (subJobParameters != null) {
        	//FIXME we probably need to pass along subJob params here somehow. 
        }

        return subJob;
    }
	
    /*
     * Build a generated job with only one step in it to submit to the
     * BatchKernel.
     * 
     */
    public static JSLJob buildSubJob(Long parentJobExecutionId, Step step, Properties[] subJobParameters, int partitionInstance) {

        ObjectFactory jslFactory = new ObjectFactory();
        JSLJob subJob = jslFactory.createJSLJob();

        // Set the generated subjob id
        String subJobId = buildSubJobId(parentJobExecutionId, step.getId(), partitionInstance);
        subJob.setId(subJobId);

        // Add one step to job
        Step newStep = jslFactory.createStep();
        
        //set id
        newStep.setId(step.getId());

        
        /***
         * deep copy all fields
         */
        newStep.setAbstract(step.getAbstract());
        newStep.setAllowStartIfComplete(step.getAllowStartIfComplete());
        
        if (step.getBatchlet() != null){
        	newStep.setBatchlet(CloneUtility.cloneBatchlet(step.getBatchlet()));
        }
        
        if (step.getChunk() != null) {
        	newStep.setChunk(CloneUtility.cloneChunk(step.getChunk()));
        }
        
        newStep.setListeners(CloneUtility.cloneListeners(step.getListeners()));
        newStep.setNextFromAttribute(step.getNextFromAttribute());
        newStep.setParent(step.getParent());

        //Add partition artifacts and set instances to 1 as the base case 
        Partition partition = step.getPartition();
        if (partition != null) {
        	if (partition.getCollector() != null) {
        		
        		Partition basePartition = jslFactory.createPartition();
        		
        		PartitionPlan partitionPlan = jslFactory.createPartitionPlan();
        		partitionPlan.setInstances("1");
        		basePartition.setPartitionPlan(partitionPlan);
        		
        		basePartition.setCollector(partition.getCollector());
        		newStep.setPartition(basePartition);
                	
        	}
        }
        
        newStep.setStartLimit(step.getStartLimit());
        newStep.setProperties(CloneUtility.cloneJSLProperties(step.getProperties()));
        
        //Add listeners
        if (step.getListeners() != null){
            newStep.setListeners(step.getListeners());
        }
        
        //Add Step properties, need to be careful here to remember the right precedence
        
        subJob.getExecutionElements().add(newStep);
        



        return subJob;
    }

    /**
     * @param parentJobExecutionId
     *            the execution id of the parent job
     * @param splitId this is the split id where the flows are nested    
     * @param flowId
     *            this is the id of the partitioned control element, it can be a
     *            step id or flow id
     * @param partitionInstance
     *            the instance number of the partitioned element
     * @return a String of the form
     *         <parentJobExecutionId>:<parentId>:<partitionInstance>
     */
    private static String buildSubJobId(Long parentJobExecutionId, String splitId, String flowId, int partitionInstance) {

        StringBuilder strBuilder = new StringBuilder(parentJobExecutionId.toString());
        strBuilder.append(':');
        strBuilder.append(splitId);
        strBuilder.append(':');
        strBuilder.append(flowId);
        strBuilder.append(':');
        strBuilder.append(partitionInstance);

        return strBuilder.toString();
    }
    
    /**
     * @param parentJobExecutionId
     *            the execution id of the parent job
     * @param stepId
     *            this is the id of the partitioned control element, it can be a
     *            step id or flow id
     * @param partitionInstance
     *            the instance number of the partitioned element
     * @return a String of the form
     *         <parentJobExecutionId>:<parentId>:<partitionInstance>
     */
    private static String buildSubJobId(Long parentJobExecutionId, String stepId, int partitionInstance) {

        StringBuilder strBuilder = new StringBuilder(parentJobExecutionId.toString());
        strBuilder.append(':');
        strBuilder.append(stepId);
        strBuilder.append(':');
        strBuilder.append(partitionInstance);

        return strBuilder.toString();
    }
    
    

    
    
}
