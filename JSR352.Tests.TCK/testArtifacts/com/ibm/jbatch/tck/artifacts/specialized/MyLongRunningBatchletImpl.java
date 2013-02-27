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

import java.util.Random;
import java.util.logging.Logger;

import javax.batch.annotation.BatchProperty;
import javax.batch.api.AbstractBatchlet;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

import com.ibm.jbatch.tck.artifacts.reusable.StopOnBullitenBoardTestData;

@javax.inject.Named("myLongRunningBatchlet")
public class MyLongRunningBatchletImpl extends AbstractBatchlet {

    @Inject
    JobContext<?> jobCtx;

    @Inject    
    @BatchProperty(name="run.indefinitely")
    private String runIndefinitelyString = null;
    private boolean runIndefinitely = false;

    @Inject    
    @BatchProperty(name="throw.exc.on.number.3")
    private String throwExcOnThreeString = null;
    private boolean throwExcOnThree = false;
    
    @Inject    
    @BatchProperty(name="run.time")
    private String runTimeString;
    
	     @Inject 
	 private StepContext<MyTransient, StopOnBullitenBoardTestData> stepCtx = null; 

    private final static String sourceClass = MyLongRunningBatchletImpl.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);

    // I think we need this to be volatile so the value doesn't get cached on the Thread.
    // Not 100% sure.
    private volatile boolean stopped = false;

	private void begin() throws Exception {
		logger.fine("MyLongRunningBatchletImpl.begin()");
		
		System.out.println("AJM: in myLongRunning?");

		StopOnBullitenBoardTestData testData = null;
		if ((testData = stepCtx.getPersistentUserData()) != null) {
			this.runIndefinitely = testData.getRunInDef();
			this.throwExcOnThree = testData.getThrowOn3();
		} else {

		    testData = new StopOnBullitenBoardTestData();
		    
			if ("true".equalsIgnoreCase(runIndefinitelyString)) {
				runIndefinitely = true;
				// set to false for next restart
				testData.setRunInDef(false);
			}

			if ("true".equalsIgnoreCase(throwExcOnThreeString)) {
				throwExcOnThree = true;
				// set to false for next restart
				testData.setThrowOn3(false);
			}

			stepCtx.setPersistentUserData(testData);
		}
	}

    @Override
    public String process() throws Exception {
        logger.fine("MyLongRunningBatchLetImpl.process(); current ExitStatus = " + jobCtx.getExitStatus());

        this.begin();

        int i = 0;
        int numTimesToRun = 500;
        boolean maxTimesReached = false;

        while (!stopped) {
            logger.fine("i=" + i++);
            if (i==3 && throwExcOnThree) {
                logger.fine("Throwing exception to confirm fail+restart handling on unchecked exceptions.");
                throw new RuntimeException("Throwing exception to confirm fail+restart handling on unchecked exceptions.");
            }

            for (int k = 0; k < 100; k++) {
                Random r = new Random(k); 
                r.nextInt();
            }
            if ((!runIndefinitely) && (i >= numTimesToRun)) {
                maxTimesReached = true;
                break;
            }
        }               

        if (maxTimesReached)   {
            String currentExitStatus = jobCtx.getExitStatus();  
            if (currentExitStatus != null) {
                jobCtx.setExitStatus("GOOD.STEP." + currentExitStatus);
            } else {
                jobCtx.setExitStatus("GOOD.STEP");
            }
            return "BATCHLET RAN TO COMPLETION";
        } else {
            jobCtx.setExitStatus("BATCHLET CANCELED BEFORE COMPLETION");
            return "BATCHLET CANCELED BEFORE COMPLETION";
        }
    }

    @Override
    public void stop() throws Exception {
        logger.fine("MyLongRunningBatchLetImpl.cancel()");
        this.stopped = true;
    }
    
	   private class MyTransient {
	        int data = 0;
	        MyTransient(int x) {
	            data = x;
	        }   
	    }

}
