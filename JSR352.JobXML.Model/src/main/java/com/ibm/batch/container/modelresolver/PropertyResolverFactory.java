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
package com.ibm.batch.container.modelresolver;

import jsr352.batch.jsl.Analyzer;
import jsr352.batch.jsl.Batchlet;
import jsr352.batch.jsl.Chunk;
import jsr352.batch.jsl.Collector;
import jsr352.batch.jsl.Decision;
import jsr352.batch.jsl.Flow;
import jsr352.batch.jsl.JSLJob;
import jsr352.batch.jsl.Listener;
import jsr352.batch.jsl.PartitionMapper;
import jsr352.batch.jsl.PartitionPlan;
import jsr352.batch.jsl.PartitionReducer;
import jsr352.batch.jsl.Split;
import jsr352.batch.jsl.Step;

import com.ibm.batch.container.modelresolver.impl.AnalyzerPropertyResolverImpl;
import com.ibm.batch.container.modelresolver.impl.BatchletPropertyResolverImpl;
import com.ibm.batch.container.modelresolver.impl.CheckpointAlgorithmPropertyResolverImpl;
import com.ibm.batch.container.modelresolver.impl.ChunkPropertyResolverImpl;
import com.ibm.batch.container.modelresolver.impl.CollectorPropertyResolverImpl;
import com.ibm.batch.container.modelresolver.impl.ControlElementPropertyResolverImpl;
import com.ibm.batch.container.modelresolver.impl.DecisionPropertyResolverImpl;
import com.ibm.batch.container.modelresolver.impl.FlowPropertyResolverImpl;
import com.ibm.batch.container.modelresolver.impl.JobPropertyResolverImpl;
import com.ibm.batch.container.modelresolver.impl.ListenerPropertyResolverImpl;
import com.ibm.batch.container.modelresolver.impl.PartitionMapperPropertyResolverImpl;
import com.ibm.batch.container.modelresolver.impl.PartitionPlanPropertyResolverImpl;
import com.ibm.batch.container.modelresolver.impl.PartitionReducerPropertyResolverImpl;
import com.ibm.batch.container.modelresolver.impl.SplitPropertyResolverImpl;
import com.ibm.batch.container.modelresolver.impl.StepPropertyResolverImpl;
import com.ibm.batch.container.xjcl.ControlElement;

public class PropertyResolverFactory {

    
    public static PropertyResolver<JSLJob> createJobPropertyResolver() {
        return new JobPropertyResolverImpl();
    }

    public static PropertyResolver<Step> createStepPropertyResolver() {
        return new StepPropertyResolverImpl();
    }
    
    public static PropertyResolver<Batchlet> createBatchletPropertyResolver() {
        return new BatchletPropertyResolverImpl();
    }
    
    public static PropertyResolver<Split> createSplitPropertyResolver() {
        return new SplitPropertyResolverImpl();
    }
    
    public static PropertyResolver<Flow> createFlowPropertyResolver() {
        return new FlowPropertyResolverImpl();
    }

    public static PropertyResolver<Chunk> createChunkPropertyResolver() {
        return new ChunkPropertyResolverImpl();
    }
    
    public static PropertyResolver<ControlElement> createControlElementPropertyResolver() {
        return new ControlElementPropertyResolverImpl();
    }

    
    public static PropertyResolver<Decision> createDecisionPropertyResolver() {
        return new DecisionPropertyResolverImpl();
    }
    
    public static PropertyResolver<Listener> createListenerPropertyResolver() {
        return new ListenerPropertyResolverImpl();
    }

	public static PropertyResolver<PartitionMapper> createPartitionMapperPropertyResolver() {
		return new PartitionMapperPropertyResolverImpl();
	}
	
	public static PropertyResolver<PartitionPlan> createPartitionPlanPropertyResolver() {
		return new PartitionPlanPropertyResolverImpl();
	}
	
	public static PropertyResolver<PartitionReducer> createPartitionReducerPropertyResolver() {
		return new PartitionReducerPropertyResolverImpl();	
	}
	
	public static CheckpointAlgorithmPropertyResolverImpl createCheckpointAlgorithmPropertyResolver() {
		return new CheckpointAlgorithmPropertyResolverImpl();
	}

	public static PropertyResolver<Collector> createCollectorPropertyResolver() {
		return new CollectorPropertyResolverImpl();	
	}

	public static PropertyResolver<Analyzer> createAnalyzerPropertyResolver() {
		return new AnalyzerPropertyResolverImpl();	
	}


	
    /** The resolvers haven't been implemented yet!!!! **/
    
    
    

}
