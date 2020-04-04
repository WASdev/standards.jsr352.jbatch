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

import java.util.Random;
import java.util.logging.Logger;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.api.BatchProperty;
import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;
import jakarta.inject.Inject;

public class LongRunningBatchlet extends AbstractBatchlet{

	private final static Logger logger = Logger.getLogger(LongRunningBatchlet.class.getName());
	private final static String STATUS = "BATCHLET RAN TO COMPLETION";
	private volatile boolean stopped = false;
	private boolean completed = false;
	
	@Inject
	private JobContext jobCtx;
	
	@Inject 
	private StepContext stepCtx; 	
	
	private void delayPartition() throws Exception{
		int i = 0;
		int numTimesToRun = 500000000;		
		
		while(!stopped) {
			if(i % 1000 == 0) {
				logger.fine("i = " + i + "; " + Thread.currentThread());
			}
			for (int k = 0; k < 100; k++) {
				Random r = new Random(k); 
				r.nextInt();
			}
			if(i >= numTimesToRun) {
				break;
			}
			if(i == numTimesToRun) {
				completed = true;
			}
			i++;
		}
	}
	
	@Override
	public String process() throws Exception {
		delayPartition();
		if(completed) {
			return STATUS;
		} else {
			return "BATCHLET WAS STOPPED EARLY!";
		}
	}
	
	@Override
	public void stop() {
		this.stopped = true;
		stepCtx.setExitStatus("STOPPING!");
		jobCtx.setExitStatus("STOPPING!!");
	}
}