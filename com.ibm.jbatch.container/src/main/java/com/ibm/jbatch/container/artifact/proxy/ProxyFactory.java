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
package com.ibm.jbatch.container.artifact.proxy;

import jakarta.batch.api.Batchlet;
import jakarta.batch.api.Decider;
import jakarta.batch.api.chunk.CheckpointAlgorithm;
import jakarta.batch.api.chunk.ItemProcessor;
import jakarta.batch.api.chunk.ItemReader;
import jakarta.batch.api.chunk.ItemWriter;
import jakarta.batch.api.partition.PartitionAnalyzer;
import jakarta.batch.api.partition.PartitionCollector;
import jakarta.batch.api.partition.PartitionMapper;
import jakarta.batch.api.partition.PartitionReducer;

import com.ibm.jbatch.container.context.impl.StepContextImpl;
import com.ibm.jbatch.container.servicesmanager.ServicesManager;
import com.ibm.jbatch.container.servicesmanager.ServicesManagerImpl;
import com.ibm.jbatch.container.validation.ArtifactValidationException;
import com.ibm.jbatch.spi.services.IBatchArtifactFactory;

/*
 * Introduce a level of indirection so proxies are not instantiated directly by newing them up.
 */
public class ProxyFactory {

    protected static ServicesManager servicesManager = ServicesManagerImpl.getInstance();

    private static ThreadLocal<InjectionReferences> injectionContext = new ThreadLocal<InjectionReferences>();
    
    protected static IBatchArtifactFactory batchArtifactFactory = servicesManager.getDelegatingArtifactFactory();

    protected static Object loadArtifact(String id, InjectionReferences injectionReferences) {
        injectionContext.set(injectionReferences);
        
        Object loadedArtifact = null;
        try {
            loadedArtifact = batchArtifactFactory.load(id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return loadedArtifact;
    }
    
    public static InjectionReferences getInjectionReferences() {
        return injectionContext.get();
    }
    
    /*
     * Decider
     */
    public static DeciderProxy createDeciderProxy(String id, InjectionReferences injectionRefs) throws ArtifactValidationException {
        Decider loadedArtifact = (Decider)loadArtifact(id, injectionRefs);
        DeciderProxy proxy = new DeciderProxy(loadedArtifact);

        return proxy;
    }

    /*
     * Batchlet artifact
     */
    public static BatchletProxy createBatchletProxy(String id, InjectionReferences injectionRefs, StepContextImpl stepContext) throws ArtifactValidationException {
        Batchlet loadedArtifact = (Batchlet)loadArtifact(id, injectionRefs);
        BatchletProxy proxy = new BatchletProxy(loadedArtifact);
        proxy.setStepContext(stepContext);
        
        return proxy;
    }
    
    /*
     * The four main chunk-related artifacts
     */    
    
    public static CheckpointAlgorithmProxy createCheckpointAlgorithmProxy(String id, InjectionReferences injectionRefs, StepContextImpl stepContext) throws ArtifactValidationException {
        CheckpointAlgorithm loadedArtifact = (CheckpointAlgorithm)loadArtifact(id, injectionRefs);
        CheckpointAlgorithmProxy proxy = new CheckpointAlgorithmProxy(loadedArtifact);
        proxy.setStepContext(stepContext);
        
        return proxy;
    }
    
    public static ItemReaderProxy createItemReaderProxy(String id, InjectionReferences injectionRefs, StepContextImpl stepContext) throws ArtifactValidationException {
        ItemReader loadedArtifact = (ItemReader)loadArtifact(id, injectionRefs);
        ItemReaderProxy proxy = new ItemReaderProxy(loadedArtifact);
        proxy.setStepContext(stepContext);
        
        return proxy;
    }
    
    public static ItemProcessorProxy createItemProcessorProxy(String id, InjectionReferences injectionRefs, StepContextImpl stepContext) throws ArtifactValidationException {
        ItemProcessor loadedArtifact = (ItemProcessor)loadArtifact(id, injectionRefs);
        ItemProcessorProxy proxy = new ItemProcessorProxy(loadedArtifact);
        proxy.setStepContext(stepContext);
        
        return proxy;
    }
    
    public static ItemWriterProxy createItemWriterProxy(String id, InjectionReferences injectionRefs, StepContextImpl stepContext) throws ArtifactValidationException {
        ItemWriter loadedArtifact = (ItemWriter)loadArtifact(id, injectionRefs);
        ItemWriterProxy proxy = new ItemWriterProxy(loadedArtifact);
        proxy.setStepContext(stepContext);
        
        return proxy;
    }
        
    /*
     * The four partition-related artifacts
     */
    
    public static PartitionReducerProxy createPartitionReducerProxy(String id, InjectionReferences injectionRefs, StepContextImpl stepContext) throws ArtifactValidationException {
        PartitionReducer loadedArtifact = (PartitionReducer)loadArtifact(id, injectionRefs);
        PartitionReducerProxy proxy = new PartitionReducerProxy(loadedArtifact);
        proxy.setStepContext(stepContext);

        return proxy;
    }
    
    public static PartitionMapperProxy createPartitionMapperProxy(String id, InjectionReferences injectionRefs, StepContextImpl stepContext) throws ArtifactValidationException {
        PartitionMapper loadedArtifact = (PartitionMapper)loadArtifact(id, injectionRefs);
        PartitionMapperProxy proxy = new PartitionMapperProxy(loadedArtifact);
        proxy.setStepContext(stepContext);

        return proxy;
    }
    
    public static PartitionAnalyzerProxy createPartitionAnalyzerProxy(String id, InjectionReferences injectionRefs, StepContextImpl stepContext) throws ArtifactValidationException {
        PartitionAnalyzer loadedArtifact = (PartitionAnalyzer)loadArtifact(id, injectionRefs);
        PartitionAnalyzerProxy proxy = new PartitionAnalyzerProxy(loadedArtifact);
        proxy.setStepContext(stepContext);

        return proxy;
    }
    
    public static PartitionCollectorProxy createPartitionCollectorProxy(String id, InjectionReferences injectionRefs, StepContextImpl stepContext) throws ArtifactValidationException {
        PartitionCollector loadedArtifact = (PartitionCollector)loadArtifact(id, injectionRefs);
        PartitionCollectorProxy proxy = new PartitionCollectorProxy(loadedArtifact);
        proxy.setStepContext(stepContext);

        return proxy;
    }
}
