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
package jsr352.tck.specialized;

import java.util.logging.Logger;

import javax.batch.annotation.*;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;

import jsr352.tck.chunktypes.ReadRecord;
import jsr352.tck.reusable.MyParentException;


@SkipListener("MySkipListener")
@javax.inject.Named("MySkipListener")
public class MySkipListener {
	
	@BatchContext 
    JobContext jobCtx; 
	
	@BatchContext 
    StepContext stepCtx;
    
    private final static String sourceClass = MySkipAndStepListener.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);

	public static final String GOOD_EXIT_STATUS = "GOOD STATUS";
	public static final String BAD_EXIT_STATUS = "BAD STATUS";


    @OnSkipReadItem
    public void onSkipRead(Exception e) {
        logger.fine("In onSkipRead()" + e);
    
        if (e instanceof MyParentException){
        	logger.finer("SKIPLISTENER: onSkipRead, exception is an instance of: MyParentException");
        	jobCtx.setExitStatus(GOOD_EXIT_STATUS);
        	logger.fine("SKIPLISTENER: onSkipRead, exception is an instance of: MyParentException");
        }
        else {
        	System.out.println("SKIPLISTENER: onSkipRead, exception is NOT an instance of: MyParentException");
        	//jobCtx.setExitStatus(BAD_EXIT_STATUS);
        	logger.fine("SKIPLISTENER: onSkipRead, exception is NOT an instance of: MyParentException");
        }
    }
    
    @OnSkipProcessItem
    public void onSkipProcess(Exception e, ReadRecord rec) {
    	logger.fine("In onSkipProcess()" + e + "input=" + rec.getCount());
        if (e instanceof MyParentException){
        	logger.fine("SKIPLISTENER: onSkipProcess, exception is an instance of: MyParentException");
        }
        else {
        	logger.fine("SKIPLISTENER: onSkipProcess, exception is NOT an instance of: MyParentException");
        }
    }
    
    @OnSkipWriteItem
    public void onSkipWrite(Exception e, ReadRecord rec) {
    	logger.fine("In onSkipWrite()" + e + "input=" + rec.getCount());
        if (e instanceof MyParentException){
        	logger.fine("SKIPLISTENER: onSkipWrite, exception is an instance of: MyParentException");
        }
        else {
        	logger.fine("SKIPLISTENER: onSkipWrite, exception is NOT an instance of: MyParentException");
        }
    }
    
}
