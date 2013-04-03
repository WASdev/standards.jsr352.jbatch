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
package com.ibm.jbatch.container.jsl;


import com.ibm.jbatch.container.jsl.impl.FlowModelResolverImpl;
import com.ibm.jbatch.container.jsl.impl.JobModelResolverImpl;
import com.ibm.jbatch.container.jsl.impl.SplitModelResolverImpl;
import com.ibm.jbatch.container.jsl.impl.StepModelResolverImpl;
import com.ibm.jbatch.jsl.model.Flow;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.jbatch.jsl.model.Split;
import com.ibm.jbatch.jsl.model.Step;

public class ModelResolverFactory {
	
    public static ModelResolver<JSLJob> createJobResolver() {
        return new JobModelResolverImpl();
    }
    
    //FIXME: Split is no longer a valid snippet type
    public static ModelResolver<Split> createSplitResolver() {
        return new SplitModelResolverImpl();
    }
    
    //FIXME: Flow is no longer a valid snippet type
    public static ModelResolver<Flow> createFlowResolver() {
        return new FlowModelResolverImpl();
    }
    
    public static ModelResolver<Step> createStepResolver() {
        return new StepModelResolverImpl();
    }
}
