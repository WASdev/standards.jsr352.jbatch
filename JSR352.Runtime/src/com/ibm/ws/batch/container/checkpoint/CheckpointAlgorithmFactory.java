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
package com.ibm.ws.batch.container.checkpoint;

import java.util.List;

import jsr352.batch.jsl.Chunk;
import jsr352.batch.jsl.Property;
import jsr352.batch.jsl.Step;

import com.ibm.batch.container.artifact.proxy.CheckpointAlgorithmProxy;
import com.ibm.batch.container.artifact.proxy.ProxyFactory;
import com.ibm.batch.container.validation.ArtifactValidationException;

public class CheckpointAlgorithmFactory {

	public static CheckpointAlgorithmProxy getCheckpointAlgorithmProxy (Step step) throws ArtifactValidationException{
		Chunk chunk = step.getChunk();
		CheckpointAlgorithmProxy proxy = null;
		String checkpointType = chunk.getCheckpointPolicy();

		//TODO - is the checkpoint properties same as chunk properties ?
		List<Property> propList = (chunk.getProperties() == null) ? null : chunk.getProperties().getPropertyList();

		final String ItemCheckpointPolicyClassName = ItemCheckpointAlgorithm.class.getName();
		final String TimeCheckpointPolicyClassName = TimeCheckpointAlgorithm.class.getName();

		if (checkpointType.equals("item")) {

			proxy = new CheckpointAlgorithmProxy( new ItemCheckpointAlgorithm(), propList);

		}else if (checkpointType.equalsIgnoreCase("time")) {

			proxy = new CheckpointAlgorithmProxy(new TimeCheckpointAlgorithm(), propList);

		}else if (checkpointType.equalsIgnoreCase("custom")) {

			//TODO - chunk need checkpoint-algorithm element and properties
			//String customCheckpointRef = "customCheckpointRef-TOBECHANGED";
			List<Property> custompropList = (chunk.getCheckpointAlgorithm().getProperties() == null) ? null : chunk.getCheckpointAlgorithm().getProperties().getPropertyList();
			if (chunk.getCheckpointAlgorithm().getRef() == null){
				proxy = new CheckpointAlgorithmProxy( new ItemTimeCheckpointAlgorithm(), custompropList);
			}
			else {
				proxy = ProxyFactory.createCheckpointAlgorithmProxy(chunk.getCheckpointAlgorithm().getRef(), custompropList);
			}
		}	
		return proxy;

	}

}
