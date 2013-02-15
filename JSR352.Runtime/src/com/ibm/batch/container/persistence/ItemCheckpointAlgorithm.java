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
package com.ibm.batch.container.persistence;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.api.CheckpointAlgorithm;

public final class ItemCheckpointAlgorithm implements CheckpointAlgorithm {

	private static final String className = ItemCheckpointAlgorithm.class.getName();
	private static Logger logger  = Logger.getLogger(ItemCheckpointAlgorithm.class.getPackage().getName());;
		
    CheckpointAlgorithm ichkp = null;
    boolean inCheckpoint = false;
    private static final int defaultRecordValue = 10;
    private static final int defaultTimeValue = 10;
	private static final int defaultTimeoutValue = 60;
    int threshold = defaultRecordValue;
    long timeStarted = 0;
    long requests = 0;
    int timeout = defaultTimeoutValue;
    
    java.util.Date date = null;
    long ts = 0;
    int interval = 10;  // 10 sec interval?
    long numTimes = 0;
    
    int time; 
    int item;
    
    public ItemCheckpointAlgorithm(){
    	date = new java.util.Date();
        ts = date.getTime();
        
        logger.finer("ITEMTIME: in ctor, ts = " + ts);

    }

	public void endCheckpoint() throws Exception {
    	inCheckpoint = false; 
	}

	public int getCheckpointTimeOut(int timeOut) throws Exception {
    	return timeout;
	}

	public boolean isReadyToCheckpointItem() throws Exception {
       	String method = "isReadyToCheckpoint";
    	if(logger.isLoggable(Level.FINER)) { logger.entering(className, method); }

        requests++;

        boolean itemready = (requests >= item);

        if ( itemready) {
        	logger.finer("ITEMTIMECHKPT: item checkpoint hit");
            long millis =  Long.valueOf( (new Date().getTime()) - timeStarted );
            if ( millis>0 ) { 
                String rate =  Integer.valueOf ( Long.valueOf( (requests*1000/millis) ).intValue()).toString();
                if(logger.isLoggable(Level.FINE)) { logger.fine(" - true [requests/second " + rate + "]"); }

            } else {
            	if(logger.isLoggable(Level.FINE)) { logger.fine(" - true [requests " + requests + "]"); }

            }
        }

        if ( itemready ) requests = 0;

        return itemready;
	}
	
	public boolean isReadyToCheckpointTime() throws Exception {
    	String method = "isReadyToCheckpoint";
    	if(logger.isLoggable(Level.FINER)) { logger.entering(className, method); }

        boolean timeready = false;
        numTimes++;
        java.util.Date curDate = new java.util.Date();
        long curts = curDate.getTime();
        long curdiff = curts - ts;
        int diff = (int)curdiff / 1000;
        
        if (diff >= time) {
        	logger.finer("ITEMTIMECHKPT: time checkpoint hit");
            timeready = true;
            if(logger.isLoggable(Level.FINER)) { logger.fine("Num of requests="+numTimes+" at a rate="+numTimes/diff+" req/sec");}
         
            numTimes = 0;
            
            date = new java.util.Date();
            ts = date.getTime();
            
        }

           
        if(logger.isLoggable(Level.FINER)) { logger.exiting(className, method, timeready); }

        return timeready;
	}
	
	public void setThreshold(int INthreshHold){
		threshold = INthreshHold;
	}

	public boolean isReadyToCheckpoint() throws Exception {
		
		boolean ready = false; 
		
		if (time == 0){ // no time limit, just check if item count has been reached
			if (isReadyToCheckpointItem()){
				ready = true;
			}
		} else if (isReadyToCheckpointItem() || isReadyToCheckpointTime()) {
			ready = true;
		}
		
		return ready;
	}

	public void setThresholds(int itemthreshold, int timethreshold) {

		item = itemthreshold;
        time = timethreshold;
		
	}

	@Override
	public void beginCheckpoint() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int checkpointTimeout(int timeout) throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}

	
	
	// Old Item only chkpt code
	/*
	private static final String className = ItemCheckpointAlgorithm.class.getName();
	private static Logger logger  = Logger.getLogger(ItemCheckpointAlgorithm.class.getPackage().getName());;
		
    CheckpointAlgorithm ichkp = null;
    boolean inCheckpoint = false;
    private static final int defaultRecordValue = 10;
	private static final int defaultTimeoutValue = 60;
    int itemthreshold = defaultRecordValue;
    int timethreshold = 0; // default time threshold is 0
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

        boolean ready = (requests >= itemthreshold);

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
		itemthreshold = INthreshHold;
	}

	//@Override
	public void setThresholds(int itemthreshold, int timethreshold) {
		this.itemthreshold = itemthreshold; 		
	}

	@Override
	public int checkpointTimeout(int timeout) throws Exception {
		this.timeout = timeout;
		return this.timeout;
	}
*/
}
