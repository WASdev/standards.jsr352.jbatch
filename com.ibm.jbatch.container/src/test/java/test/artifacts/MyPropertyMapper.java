/*
 * Copyright 2014 International Business Machines Corp.
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
package test.artifacts;

import java.util.Properties;

import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.partition.PartitionMapper;
import jakarta.batch.api.partition.PartitionPlan;
import jakarta.batch.api.partition.PartitionPlanImpl;
import jakarta.batch.runtime.context.StepContext;
import jakarta.inject.Inject;


public class MyPropertyMapper implements PartitionMapper{
	
	@Inject
	StepContext stepCtx;
	
	@Inject @BatchProperty(name="stepProp2")
	String stepProp2;

	@Override
	public PartitionPlan mapPartitions() throws Exception {

		PartitionPlanImpl pp = new PartitionPlanImpl();
		pp.setPartitions(4);
		Properties p0 = new Properties();
		p0.setProperty("part", "");
		Properties p1 = new Properties();
		p1.setProperty("part", "");
		Properties p2 = new Properties();
		p2.setProperty("part", "");
		Properties p3 = new Properties();
		p3.setProperty("part", "");
		Properties[] partitionProps = new Properties[4];
		partitionProps[0] = p0;
		partitionProps[1] = p1;
		partitionProps[2] = p2;
		partitionProps[3] = p3;
		pp.setPartitionProperties(partitionProps);
		
		stepCtx.setExitStatus(stepCtx.getExitStatus() + ":" + stepProp2 + ":" + pp.getPartitions());

		return pp;
	}

}
