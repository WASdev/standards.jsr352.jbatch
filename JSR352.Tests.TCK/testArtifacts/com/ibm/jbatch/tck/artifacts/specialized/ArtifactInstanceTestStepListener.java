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
package com.ibm.jbatch.tck.artifacts.specialized;

import javax.batch.api.BatchProperty;
import javax.batch.api.listener.StepListener;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;

public class ArtifactInstanceTestStepListener implements StepListener {

    @Inject 
    JobContext jobCtx;
    
    @Inject    
    @BatchProperty(name="step.property")
    String stepPropertyString;
    
    static String prop1 = "stepListenerA";
    static String prop2 = "stepListenerB";
    
    static boolean sawProp1 = false;
    static boolean sawProp2 = false;
    
    int instance1Count = 0;
    int instance2Count = 0;
    
    boolean saw2Listeners = false;
    static boolean uniqueInstance1 = false;
    static boolean uniqueInstance2 = false;
    
    String passedInProp;
	
	@Override
	public void beforeStep() throws Exception {
		if (stepPropertyString.equals(prop1)){
			sawProp1 = true;
			instance1Count++;
			
		}
		else if (stepPropertyString.equals(prop2)){
			sawProp2 = true;
			instance2Count++;
		}
	}

	@Override
	public void afterStep() throws Exception {
		if (sawProp1 && sawProp2){
			saw2Listeners = true;
		}
		
		if ((stepPropertyString.equals(prop1)) && instance1Count == 1){
			uniqueInstance1 = true;
		}
		
		if ((stepPropertyString.equals(prop2)) && instance2Count == 1){
			uniqueInstance2 = true;
		}
		
		String currentStatus = jobCtx.getExitStatus();
		
		if (currentStatus != null && currentStatus.equals("BAD")){			return;
		}
		
		if ((saw2Listeners && uniqueInstance2 && !uniqueInstance1) || (saw2Listeners && !uniqueInstance2 && uniqueInstance1)){
			jobCtx.setExitStatus(jobCtx.getExitStatus()+"StepListener");
			uniqueInstance1 = false;
			uniqueInstance2 = false;
		}
		else {
			jobCtx.setExitStatus("STEP_BAD");
		}
			
	}

}
