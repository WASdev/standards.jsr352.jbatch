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
package com.ibm.jbatch.container.modelresolver.impl;

import java.util.Properties;

import com.ibm.jbatch.jsl.model.Collector;


public class CollectorPropertyResolverImpl extends AbstractPropertyResolver<Collector> {

	public CollectorPropertyResolverImpl(boolean isPartitionStep) {
		super(isPartitionStep);
	}

	@Override
	public Collector substituteProperties(Collector b) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collector substituteProperties(Collector b, Properties submittedProps) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collector substituteProperties(Collector b,
			Properties submittedProps, Properties parentProps) {
		// TODO Auto-generated method stub
		return null;
	}

}
