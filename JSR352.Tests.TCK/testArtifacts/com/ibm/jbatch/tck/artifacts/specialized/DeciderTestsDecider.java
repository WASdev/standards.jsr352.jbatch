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
import javax.batch.api.Decider;
import javax.batch.runtime.StepExecution;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;

import com.ibm.jbatch.tck.artifacts.common.StatusConstants;

@javax.inject.Named
public class DeciderTestsDecider implements Decider, StatusConstants {
	
	public final static String SPECIAL_EXIT_STATUS = "SpecialExitStatus";

	/*
	 * Since this is a job involving repeated uses of the same step, let's include
	 * a count to ensure we don't wrongly re-run a step multiple times.
	 */
	@Inject
    JobContext jobCtx;

    @Inject    
    @BatchProperty(name=SPECIAL_EXIT_STATUS)
    String specialExitStatus;    
    
	@Override
	public String decide(StepExecution[] stepExecutions) {	
		if (stepExecutions.length != 1) {
			throw new IllegalStateException("Expecting stepExecutions array of size 1, found one of size = " + stepExecutions.length);
		}
		StepExecution stepExec = stepExecutions[0];
		String coreExitStatus = coreExitStatus(stepExec);
		Integer count = (Integer)jobCtx.getTransientUserData();
		String retVal = count.toString() + ":" + coreExitStatus;
		return retVal;
	}
	
	private String coreExitStatus(StepExecution stepExec) {		
		String action = (String)stepExec.getUserPersistentData();
		String currentExitStatus = stepExec.getExitStatus();
		
		// "Normally" we just pass set 'normalExitStatus' as exit status.
		if (currentExitStatus.equals(GOOD_STEP_EXIT_STATUS)) {
			return action;			
		// But if it's the magic number then we return our "SpecialExitStatus".
		} else {
			return specialExitStatus;
		}		
	}
	
}
