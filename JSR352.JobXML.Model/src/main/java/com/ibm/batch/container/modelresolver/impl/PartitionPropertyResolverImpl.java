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
package com.ibm.batch.container.modelresolver.impl;

import java.util.Properties;

import jsr352.batch.jsl.Partition;

import com.ibm.batch.container.modelresolver.PropertyResolverFactory;

public class PartitionPropertyResolverImpl extends AbstractPropertyResolver<Partition> {



    @Override
    public Partition substituteProperties(final Partition partition, final Properties submittedProps, final Properties parentProps) {
    	/**
			<xs:complexType name="Partition">
				<xs:sequence>
				    <xs:element name="partitionMapper" type="jsl:PartitionMapper" minOccurs="0" maxOccurs="1" />
				    <xs:element name="partitionPlan" type="jsl:PartitionPlan" minOccurs="0" maxOccurs="1" />
					<xs:element name="collector" type="jsl:Collector" minOccurs="0" maxOccurs="1"/>
					<xs:element name="analyzer" type="jsl:Analyzer" minOccurs="0" maxOccurs="1"/>
					<xs:element name="partitionReducer " type="jsl:PartitionReducer" minOccurs="0" maxOccurs="1"/>
				</xs:sequence>
			</xs:complexType>
    	 */
    	
        // Resolve all the properties defined for a partition
    	//FIXME applyToPartition attribute needs to be added
        if (partition.getPartitionMapper() != null) {
        	PropertyResolverFactory.createPartitionMapperPropertyResolver().substituteProperties(partition.getPartitionMapper(), submittedProps, null);
        }
    	
        if (partition.getPartitionMapper() != null) {
        	PropertyResolverFactory.createPartitionPlanPropertyResolver().substituteProperties(partition.getPartitionPlan(), submittedProps, null);
        }
        
        if (partition.getCollector() != null) {
        	PropertyResolverFactory.createCollectorPropertyResolver().substituteProperties(partition.getCollector(), submittedProps, null);
        }
        
        if (partition.getAnalyzer() != null) {
        	PropertyResolverFactory.createAnalyzerPropertyResolver().substituteProperties(partition.getAnalyzer(), submittedProps, null);
        }
        
        if (partition.getPartitionReducer() != null) {
        	PropertyResolverFactory.createPartitionReducerPropertyResolver().substituteProperties(partition.getPartitionReducer(), submittedProps, null);
        }
        
        return partition;
    }

}
