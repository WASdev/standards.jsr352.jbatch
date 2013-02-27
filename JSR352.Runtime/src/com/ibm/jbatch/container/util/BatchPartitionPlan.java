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
package com.ibm.jbatch.container.util;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Properties;

import javax.batch.api.PartitionPlan;

public class BatchPartitionPlan implements PartitionPlan {

	private int partitionCount;
	private int threadCount;
	private Properties[] partitionProperties;
	
	public int getPartitionCount() {
		return partitionCount;
	}
	
	public void setPartitionCount(int partitionCount) {
		this.partitionCount = partitionCount;
	}
	
	public int getThreadCount() {
		return threadCount;
	}
	
	public void setThreadCount(int threadCount) {
		this.threadCount = threadCount;
	}
	
	public Properties[] getPartitionProperties() {
		return partitionProperties;
	}
	
	public void setPartitionProperties(Properties[] partitionProperties) {
		this.partitionProperties = partitionProperties;
	}
	
	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		this.partitionCount = in.readInt();
		this.threadCount = in.readInt();
		this.partitionProperties = (Properties[])in.readObject();

	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {

		out.writeInt(this.partitionCount);
		out.writeInt(this.threadCount);
		out.writeObject(this.partitionProperties);

	}

}
