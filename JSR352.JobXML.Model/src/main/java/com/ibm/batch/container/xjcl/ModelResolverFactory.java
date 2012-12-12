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
package com.ibm.batch.container.xjcl;

import jsr352.batch.jsl.Flow;
import jsr352.batch.jsl.JSLJob;
import jsr352.batch.jsl.Split;
import jsr352.batch.jsl.Step;

import com.ibm.batch.container.xjcl.impl.FlowModelResolverImpl;
import com.ibm.batch.container.xjcl.impl.JobModelResolverImpl;
import com.ibm.batch.container.xjcl.impl.SplitModelResolverImpl;
import com.ibm.batch.container.xjcl.impl.StepModelResolverImpl;

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
