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
import jsr352.batch.jsl.ItemProcessor;
import jsr352.batch.jsl.ItemReader;
import jsr352.batch.jsl.ItemWriter;
import jsr352.batch.jsl.Batchlet;
import jsr352.batch.jsl.Chunk;
import jsr352.batch.jsl.Collector;
import jsr352.batch.jsl.Decision;
import jsr352.batch.jsl.Flow;
import jsr352.batch.jsl.JSLJob;
import jsr352.batch.jsl.Listener;
import jsr352.batch.jsl.Partition;
import jsr352.batch.jsl.PartitionMapper;
import jsr352.batch.jsl.PartitionPlan;
import jsr352.batch.jsl.PartitionReducer;
import jsr352.batch.jsl.Split;
import jsr352.batch.jsl.Step;

import com.ibm.batch.container.modelresolver.impl.AnalyzerPropertyResolverImpl;
import com.ibm.batch.container.modelresolver.impl.ItemProcessorPropertyResolverImpl;
import com.ibm.batch.container.modelresolver.impl.ItemReaderPropertyResolverImpl;
import com.ibm.batch.container.modelresolver.impl.ItemWriterPropertyResolverImpl;
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
import com.ibm.batch.container.modelresolver.impl.PartitionPropertyResolverImpl;
import com.ibm.batch.container.modelresolver.impl.PartitionReducerPropertyResolverImpl;
import com.ibm.batch.container.modelresolver.impl.SplitPropertyResolverImpl;
import com.ibm.batch.container.modelresolver.impl.StepPropertyResolverImpl;
import com.ibm.batch.container.xjcl.ControlElement;

public class PropertyResolverFactory {

    
    public static PropertyResolver<JSLJob> createJobPropertyResolver(boolean isPartitionedStep) {
        return new JobPropertyResolverImpl(isPartitionedStep);
    }

    public static PropertyResolver<Step> createStepPropertyResolver(boolean isPartitionedStep) {
        return new StepPropertyResolverImpl(isPartitionedStep);
    }
    
    public static PropertyResolver<Batchlet> createBatchletPropertyResolver(boolean isPartitionedStep) {
        return new BatchletPropertyResolverImpl(isPartitionedStep);
    }
    
    public static PropertyResolver<Split> createSplitPropertyResolver(boolean isPartitionedStep) {
        return new SplitPropertyResolverImpl(isPartitionedStep);
    }
    
    public static PropertyResolver<Flow> createFlowPropertyResolver(boolean isPartitionedStep) {
        return new FlowPropertyResolverImpl(isPartitionedStep);
    }

    public static PropertyResolver<Chunk> createChunkPropertyResolver(boolean isPartitionedStep) {
        return new ChunkPropertyResolverImpl(isPartitionedStep);
    }
    
    public static PropertyResolver<ControlElement> createControlElementPropertyResolver(boolean isPartitionedStep) {
        return new ControlElementPropertyResolverImpl(isPartitionedStep);
    }

    
    public static PropertyResolver<Decision> createDecisionPropertyResolver(boolean isPartitionedStep) {
        return new DecisionPropertyResolverImpl(isPartitionedStep);
    }
    
    public static PropertyResolver<Listener> createListenerPropertyResolver(boolean isPartitionedStep) {
        return new ListenerPropertyResolverImpl(isPartitionedStep);
    }
    
    public static PropertyResolver<Partition> createPartitionPropertyResolver(boolean isPartitionedStep) {
        return new PartitionPropertyResolverImpl(isPartitionedStep);
    }

	public static PropertyResolver<PartitionMapper> createPartitionMapperPropertyResolver(boolean isPartitionedStep) {
		return new PartitionMapperPropertyResolverImpl(isPartitionedStep);
	}
	
	public static PropertyResolver<PartitionPlan> createPartitionPlanPropertyResolver(boolean isPartitionedStep) {
		return new PartitionPlanPropertyResolverImpl(isPartitionedStep);
	}
	
	public static PropertyResolver<PartitionReducer> createPartitionReducerPropertyResolver(boolean isPartitionedStep) {
		return new PartitionReducerPropertyResolverImpl(isPartitionedStep);	
	}
	
	public static CheckpointAlgorithmPropertyResolverImpl createCheckpointAlgorithmPropertyResolver(boolean isPartitionedStep) {
		return new CheckpointAlgorithmPropertyResolverImpl(isPartitionedStep);
	}

	public static PropertyResolver<Collector> createCollectorPropertyResolver(boolean isPartitionedStep) {
		return new CollectorPropertyResolverImpl(isPartitionedStep);	
	}

	public static PropertyResolver<Analyzer> createAnalyzerPropertyResolver(boolean isPartitionedStep) {
		return new AnalyzerPropertyResolverImpl(isPartitionedStep);	
	}

	public static PropertyResolver<ItemReader> createReaderPropertyResolver(boolean isPartitionedStep) {
		return new ItemReaderPropertyResolverImpl(isPartitionedStep);
	}

	public static PropertyResolver<ItemProcessor> createProcessorPropertyResolver(boolean isPartitionedStep) {
		return new ItemProcessorPropertyResolverImpl(isPartitionedStep);
	}

	public static PropertyResolver<ItemWriter> createWriterPropertyResolver(boolean isPartitionedStep) {
		return new ItemWriterPropertyResolverImpl(isPartitionedStep);
	}

	
    /** The resolvers haven't been implemented yet!!!! **/
    
    
    

}
