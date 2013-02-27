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
package com.ibm.jbatch.container.persistence;


import com.ibm.jbatch.container.artifact.proxy.CheckpointAlgorithmProxy;
import com.ibm.jbatch.container.artifact.proxy.InjectionReferences;
import com.ibm.jbatch.container.artifact.proxy.ProxyFactory;
import com.ibm.jbatch.container.context.impl.StepContextImpl;
import com.ibm.jbatch.container.validation.ArtifactValidationException;
import com.ibm.jbatch.jsl.model.Chunk;
import com.ibm.jbatch.jsl.model.Step;

public class CheckpointAlgorithmFactory {

	public static CheckpointAlgorithmProxy getCheckpointAlgorithmProxy (Step step, InjectionReferences injectionReferences, StepContextImpl stepContext) throws ArtifactValidationException{
		Chunk chunk = step.getChunk();
		CheckpointAlgorithmProxy proxy = null;
		String checkpointType = chunk.getCheckpointPolicy();


		if (checkpointType.equals("item")) {

			proxy = new CheckpointAlgorithmProxy( new ItemCheckpointAlgorithm());

		}else if (checkpointType.equalsIgnoreCase("custom")) {

			proxy = ProxyFactory.createCheckpointAlgorithmProxy(chunk
					.getCheckpointAlgorithm().getRef(), injectionReferences, stepContext);

		}	
		return proxy;

	}

}
