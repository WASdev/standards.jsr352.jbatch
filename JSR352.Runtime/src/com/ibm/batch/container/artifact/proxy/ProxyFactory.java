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
package com.ibm.batch.container.artifact.proxy;

import java.util.List;

import com.ibm.batch.container.services.IBatchArtifactFactory;
import com.ibm.batch.container.services.ServicesManager;
import com.ibm.batch.container.services.ServicesManager.ServiceType;
import com.ibm.batch.container.validation.ArtifactSignatureValidator;
import com.ibm.batch.container.validation.ArtifactValidationException;

import jsr352.batch.jsl.Property;

/*
 * Introduce a level of indirection so proxies are not instantiated directly by newing them up.
 */
public class ProxyFactory {

    protected static ServicesManager servicesManager = ServicesManager.getInstance();

    protected static IBatchArtifactFactory batchArtifactFactory = 
        (IBatchArtifactFactory) servicesManager.getService(ServiceType.CONTAINER_ARTIFACT_FACTORY_SERVICE);

    static Object loadArtifact(String id) {
        Object loadedArtifact = null;
        try {
            loadedArtifact = batchArtifactFactory.load(id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return loadedArtifact;
    }
    /*
     * Decider
     */
    public static DeciderProxy createDeciderProxy(String id, List<Property> props) throws ArtifactValidationException {
        Object loadedArtifact = loadArtifact(id);
        DeciderProxy proxy = new DeciderProxy(loadedArtifact, props);
        ArtifactSignatureValidator.validate(proxy);
        return proxy;
    }

    /*
     * Batchlet artifact
     */
    public static BatchletProxy createBatchletProxy(String id, List<Property> props) throws ArtifactValidationException {
        Object loadedArtifact = loadArtifact(id);
        BatchletProxy proxy = new BatchletProxy(loadedArtifact, props);
        ArtifactSignatureValidator.validate(proxy);
        return proxy;
    }
    
    /*
     * The four main chunk-related artifacts
     */    
    
    public static CheckpointAlgorithmProxy createCheckpointAlgorithmProxy(String id, List<Property> props) throws ArtifactValidationException {
        Object loadedArtifact = loadArtifact(id);
        CheckpointAlgorithmProxy proxy = new CheckpointAlgorithmProxy(loadedArtifact, props);
        ArtifactSignatureValidator.validate(proxy);
        return proxy;
    }
    
    public static ItemReaderProxy createItemReaderProxy(String id, List<Property> props) throws ArtifactValidationException {
        Object loadedArtifact = loadArtifact(id);
        ItemReaderProxy proxy = new ItemReaderProxy(loadedArtifact, props);
        ArtifactSignatureValidator.validate(proxy);
        return proxy;
    }
    
    public static ItemProcessorProxy createItemProcessorProxy(String id, List<Property> props) throws ArtifactValidationException {
        Object loadedArtifact = loadArtifact(id);
        ItemProcessorProxy proxy = new ItemProcessorProxy(loadedArtifact, props);
        ArtifactSignatureValidator.validate(proxy);
        return proxy;
    }
    
    public static ItemWriterProxy createItemWriterProxy(String id, List<Property> props) throws ArtifactValidationException {
        Object loadedArtifact = loadArtifact(id);
        return new ItemWriterProxy(loadedArtifact, props);
    }
        
    /*
     * The four partition-related artifacts
     */
    
    public static PartitionReducerProxy createPartitionReducerProxy(String id, List<Property> props) throws ArtifactValidationException {
        Object loadedArtifact = loadArtifact(id);
        PartitionReducerProxy proxy = new PartitionReducerProxy(loadedArtifact, props);
        ArtifactSignatureValidator.validate(proxy);
        return proxy;
    }
    
    public static PartitionMapperProxy createPartitionMapperProxy(String id, List<Property> props) throws ArtifactValidationException {
        Object loadedArtifact = loadArtifact(id);
        PartitionMapperProxy proxy = new PartitionMapperProxy(loadedArtifact, props);
        ArtifactSignatureValidator.validate(proxy);
        return proxy;
    }
    
    public static PartitionAnalyzerProxy createPartitionAnalyzerProxy(String id, List<Property> props) throws ArtifactValidationException {
        Object loadedArtifact = loadArtifact(id);
        PartitionAnalyzerProxy proxy = new PartitionAnalyzerProxy(loadedArtifact, props);
        ArtifactSignatureValidator.validate(proxy);
        return proxy;
    }
    
    public static PartitionCollectorProxy createPartitionCollectorProxy(String id, List<Property> props) throws ArtifactValidationException {
        Object loadedArtifact = loadArtifact(id);
        PartitionCollectorProxy proxy = new PartitionCollectorProxy(loadedArtifact, props);
        ArtifactSignatureValidator.validate(proxy);
        return proxy;
    }
}
