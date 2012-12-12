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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

import javax.batch.annotation.CalculatePartitions;
import javax.batch.api.parameters.PartitionPlan;

import jsr352.batch.jsl.Property;

import com.ibm.batch.container.exception.BatchContainerRuntimeException;

public class PartitionMapperProxy extends AbstractProxy {

    private Method calculatePartitions = null;

    PartitionMapperProxy(Object delegate, List<Property> props) { 
        super(delegate, props);

        for (Method method : this.delegate.getClass().getDeclaredMethods()) {
            Annotation onSkipInRead = method.getAnnotation(CalculatePartitions.class);
            if (onSkipInRead != null) {
                calculatePartitions = method;
            }

        }
    }


    public PartitionPlan calculatePartitions() {
        
        PartitionPlan partitionPlan = null;
        
        if (calculatePartitions != null) {
             try {
                partitionPlan = (PartitionPlan)calculatePartitions.invoke(delegate);
             } catch (Exception e) {
                 throw new BatchContainerRuntimeException(e);
             }
        }
    
        return partitionPlan;
    }
    
    
}
