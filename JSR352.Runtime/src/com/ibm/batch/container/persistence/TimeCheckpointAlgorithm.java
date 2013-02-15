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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.api.CheckpointAlgorithm;


public final class TimeCheckpointAlgorithm implements CheckpointAlgorithm {

	private static Logger logger  = Logger.getLogger(TimeCheckpointAlgorithm.class.getPackage().getName());;
	private static final String className = TimeCheckpointAlgorithm.class.getName();
	
    boolean inCheckpoint = false;
    int interval = 10;  // 10 sec interval?
    int timeout = 60;   // 60 sec timeout?
    long ts = 0;
    java.util.Date date = null;
    long numTimes = 0;
	
    public TimeCheckpointAlgorithm(){
    	date = new java.util.Date();
        ts = date.getTime();
        
        logger.finer("TIME: in ctor, ts = " + ts);

    }
    
	public void beginCheckpoint() throws Exception {
		String method = "startCheckpoint";
    	if(logger.isLoggable(Level.FINER)) { logger.entering(className, method); }


        inCheckpoint = true;
        date = new java.util.Date();
        ts = date.getTime();
        
        logger.finer("TIME: in beginCHKPT, ts = " + ts);

       
    	if(logger.isLoggable(Level.FINER)) { logger.exiting(className, method); }
	}

	public void endCheckpoint() throws Exception {

		String method = "stopCheckpoint";
    	if(logger.isLoggable(Level.FINER)) { logger.entering(className, method); }


        inCheckpoint = false;

       
    	if(logger.isLoggable(Level.FINER)) { logger.exiting(className, method); }

	}

	//@GetCheckpointTimeout
	public int getCheckpointTimeOut(int timeOut) throws Exception {

		if(logger.isLoggable(Level.FINE)) { logger.fine("getRecommendedTimeOutValue "+timeout); }
        return timeout;	
        
    }

	//@IsReadyToCheckpoint
	public boolean isReadyToCheckpoint() throws Exception {
    	String method = "isReadyToCheckpoint";
    	if(logger.isLoggable(Level.FINER)) { logger.entering(className, method); }

        boolean ready = false;
        numTimes++;
        java.util.Date curDate = new java.util.Date();
        long curts = curDate.getTime();
        long curdiff = curts - ts;
        int diff = (int)curdiff / 1000;
        
        if (diff >= interval) {
        	
            ready = true;
            System.out.println("Ready to checkpoint. interval set to: " + interval + "seconds. Time since last checkpoint: " + diff + " seconds.");
            if(logger.isLoggable(Level.FINER)) {
            	logger.fine("Ready to checkpoint. interval set to: " + interval + "seconds. Time since last checkpoint: " + diff + " seconds.");	
            	logger.fine("Num of requests="+numTimes+" at a rate="+numTimes/diff+" req/sec");
            }
         
            numTimes = 0;
            
            date = new java.util.Date();
            ts = date.getTime();
            
        }

           
        if(logger.isLoggable(Level.FINER)) { logger.exiting(className, method, ready); }

        return ready;
	}

	//@Override
	public void setThreshold(int INthreshHold) {
		// TODO Auto-generated method stub
		interval = INthreshHold;
		
	}

	//@Override
	public void setThresholds(int itemthreshold, int timethreshold) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int checkpointTimeout(int timeout) throws Exception {
		
		this.timeout = timeout;
		return this.timeout;
		
	}

}
