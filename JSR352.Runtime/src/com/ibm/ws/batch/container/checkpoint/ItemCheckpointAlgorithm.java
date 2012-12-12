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
package com.ibm.ws.batch.container.checkpoint;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ItemCheckpointAlgorithm implements CheckpointAlgorithm {

	private static final String className = ItemCheckpointAlgorithm.class.getName();
	private static Logger logger  = Logger.getLogger(ItemCheckpointAlgorithm.class.getPackage().getName());;
		
    CheckpointAlgorithm ichkp = null;
    boolean inCheckpoint = false;
    private static final int defaultRecordValue = 10;
	private static final int defaultTimeoutValue = 60;
    int threshold = defaultRecordValue;
    long timeStarted = 0;
    long requests = 0;
    int timeout = defaultTimeoutValue;
    
	//@BeginCheckpoint
	public void beginCheckpoint() throws Exception {
        inCheckpoint = true;
        timeStarted = new Date().getTime();
	}

	//@EndCheckpoint
	public void endCheckpoint() throws Exception {
    	inCheckpoint = false; 
	}

	//@GetCheckpointTimeout
	public int getCheckpointTimeOut(int timeOut) throws Exception {
    	return timeout;
	}

	//@IsReadyToCheckpoint
	public boolean isReadyToCheckpoint() throws Exception {
       	String method = "ShouldCheckpointBeExecuted";
    	if(logger.isLoggable(Level.FINER)) { logger.entering(className, method); }

        requests++;

        boolean ready = (requests >= threshold);

        if ( ready) {
            long millis =  Long.valueOf( (new Date().getTime()) - timeStarted );
            if ( millis>0 ) { 
                String rate =  Integer.valueOf ( Long.valueOf( (requests*1000/millis) ).intValue()).toString();
                if(logger.isLoggable(Level.FINE)) { logger.fine(" - true [requests/second " + rate + "]"); }

            } else {
            	if(logger.isLoggable(Level.FINE)) { logger.fine(" - true [requests " + requests + "]"); }

            }
        }

        if ( ready ) requests = 0;

        return ready;
	}
	
	public void setThreshold(int INthreshHold){
		threshold = INthreshHold;
	}

	@Override
	public void setThresholds(int itemthreshold, int timethreshold) {
		// TODO Auto-generated method stub
		
	}

}
