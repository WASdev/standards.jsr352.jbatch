/**
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
package com.ibm.batch.container.xjcl;

import jsr352.batch.jsl.Batchlet;
import jsr352.batch.jsl.Chunk;
import jsr352.batch.jsl.JSLProperties;
import jsr352.batch.jsl.Listener;
import jsr352.batch.jsl.Listeners;
import jsr352.batch.jsl.ObjectFactory;
import jsr352.batch.jsl.Property;

public class CloneUtility {

    public static Batchlet cloneBatchlet(Batchlet batchlet){
    	ObjectFactory jslFactory = new ObjectFactory();
    	Batchlet newBatchlet = jslFactory.createBatchlet();
    	
    	newBatchlet.setRef(batchlet.getRef());
    	newBatchlet.setProperties(cloneJSLProperties(batchlet.getProperties()));
    	
    	return newBatchlet;
    }
    
    public static JSLProperties cloneJSLProperties(JSLProperties jslProps) {
    	if (jslProps == null) {
    		return null;
    	}
    	ObjectFactory jslFactory = new ObjectFactory();
    	
    	JSLProperties newJSLProps = jslFactory.createJSLProperties();
    	
    	newJSLProps.setPartition(jslProps.getPartition());;
    	
    	for(Property jslProp : jslProps.getPropertyList()) {
    		Property newProperty = jslFactory.createProperty();
    		
    		newProperty.setName(jslProp.getName());
    		newProperty.setValue(jslProp.getValue());
    		newProperty.setTarget(jslProp.getTarget());
    		
    		newJSLProps.getPropertyList().add(newProperty);
    	}

    	return newJSLProps;
    }

    public static Listeners cloneListeners(Listeners listeners) {
    	if (listeners == null) {
    		return null;
    	}
    	ObjectFactory jslFactory = new ObjectFactory();
    	
    	Listeners newListeners = jslFactory.createListeners();
    	
    	for(Listener listener : listeners.getListenerList()) {
    		Listener newListener = jslFactory.createListener();
    		
    		newListener.setRef(listener.getRef());
    		newListener.setProperties(cloneJSLProperties(listener.getProperties()));
    	}

    	return newListeners;
    }
    
    public static Chunk cloneChunk(Chunk chunk) {
    	ObjectFactory jslFactory = new ObjectFactory();
    	Chunk newChunk = jslFactory.createChunk();
    	
    	newChunk.setCheckpointPolicy(chunk.getCheckpointPolicy());
    	newChunk.setBufferSize(chunk.getBufferSize());
    	newChunk.setCommitInterval(chunk.getCommitInterval());
    	newChunk.setProcessor(chunk.getProcessor());
    	newChunk.setReader(chunk.getReader());
    	newChunk.setRetryLimit(chunk.getRetryLimit());
    	newChunk.setSkipLimit(chunk.getSkipLimit());
    	newChunk.setWriter(chunk.getWriter());
    	
    	newChunk.setProperties(cloneJSLProperties(chunk.getProperties()));
    	
    	return newChunk;
    }
	
}
