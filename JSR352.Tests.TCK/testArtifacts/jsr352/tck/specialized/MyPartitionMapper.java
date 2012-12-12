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
package jsr352.tck.specialized;

import java.util.Properties;

import javax.batch.annotation.BatchProperty;
import javax.batch.annotation.CalculatePartitions;
import javax.batch.annotation.PartitionMapper;
import javax.batch.api.parameters.PartitionPlan;

@PartitionMapper
@javax.inject.Named
public class MyPartitionMapper {

	
	private static final String GOOD_EXIT_STATUS = "good_partition_status";

	@BatchProperty
	private String numPartitionsProp = null;
	
	private int numPartitions;
	
	@CalculatePartitions
	public PartitionPlan calculatePartitions() throws Exception {
		
		numPartitions = Integer.parseInt(numPartitionsProp);
		
		Properties[] props = new Properties[numPartitions];
		
		
		for (int i = 0; i < numPartitions; i++) {
			props[i] = new Properties();
			props[i].setProperty(GOOD_EXIT_STATUS, Integer.toString(i));
		}
		
		PartitionPlan partitionPlan = new MyPartitionPlan(numPartitions, props);
		
		return partitionPlan;
		
	}
	

}
