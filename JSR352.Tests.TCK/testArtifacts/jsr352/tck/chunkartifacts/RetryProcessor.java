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
package jsr352.tck.chunkartifacts;

import javax.batch.annotation.BatchProperty;
import javax.batch.api.ItemProcessor;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

import org.testng.Reporter;

import jsr352.tck.chunktypes.NumbersRecord;
import jsr352.tck.reusable.MyParentException;


@javax.inject.Named("retryProcessor")
public class RetryProcessor implements ItemProcessor<NumbersRecord, NumbersRecord> {
	
	
    @Inject
    StepContext<?,?> stepCtx;
	
    @Inject    
    @BatchProperty(name="forced.fail.count.process")
	String forcedFailCountProp;
    
    @Inject
    @BatchProperty(name="rollback")
	String rollbackProp;
	
    private static final int STATE_NORMAL = 0;
	private static final int STATE_RETRY = 1;
	private static final int STATE_SKIP = 2;
	private static final int STATE_EXCEPTION = 3;
	
	private int testState = STATE_NORMAL;
	
	int failitem = 0;
	int count = 1;
	int forcedFailCount;
	
	boolean isInited = false;
	boolean rollback;
	
	int failindex = 0;
	
	public NumbersRecord processItem(NumbersRecord record) throws Exception {
		int item = record.getItem();
		int quantity = record.getQuantity();
		Reporter.log("Processing item: " + item + "...<br>");
		Reporter.log("Processing quantity: " + quantity + "...<p>");
		
		if(!isInited) {
			forcedFailCount = Integer.parseInt(forcedFailCountProp);
			rollback = Boolean.parseBoolean(rollbackProp);
			isInited = true;
		}
		
		// Throw an exception when forcedFailCount is reached
		if (forcedFailCount != 0 && count >= forcedFailCount && (testState == STATE_NORMAL)) {
				   //forcedFailCount = 0;
					failindex = count;
					testState = STATE_RETRY;
					Reporter.log("Fail on purpose in NumbersRecord.processItem<p>");
					throw new MyParentException("Fail on purpose in NumbersRecord.processItem()");	
		} else if (forcedFailCount != 0 && (count >= forcedFailCount) && (testState == STATE_EXCEPTION)) {
			failindex = count;
			testState = STATE_SKIP;
			forcedFailCount = 0;
			Reporter.log("Test skip -- Fail on purpose NumbersRecord.readItem<p>");
			throw new MyParentException("Test skip -- Fail on purpose in NumbersRecord.readItem()");	
		}
		
		if (testState == STATE_RETRY)
		{
			if(stepCtx.getProperties().getProperty("retry.process.exception.invoked") != "true") {
				Reporter.log("onRetryProcessException not invoked<p>");
				throw new Exception("onRetryProcessException not invoked");
			} else {
				Reporter.log("onRetryProcessException was invoked<p>");
			}
			
			if(stepCtx.getProperties().getProperty("retry.process.exception.match") != "true") {
				Reporter.log("retryable exception does not match<p>");
				throw new Exception("retryable exception does not match");
			} else {
				Reporter.log("retryable exception matches<p>");
			}
			
			testState = STATE_EXCEPTION;
		} else if(testState == STATE_SKIP) {
			if(stepCtx.getProperties().getProperty("skip.process.item.invoked") != "true") {
				Reporter.log("onSkipProcessItem not invoked<p>");
				throw new Exception("onSkipProcessItem not invoked");
			} else {
				Reporter.log("onSkipProcessItem was invoked<p>");
			}
			
			if(stepCtx.getProperties().getProperty("skip.process.item.match") != "true") {
				Reporter.log("skippable exception does not match<p>");
				throw new Exception("skippable exception does not match");
			} else {
				Reporter.log("skippable exception matches<p>");
			}
			testState = STATE_NORMAL;
		}
		
		
		quantity = quantity + 1;
		Reporter.log("Process [item: " + item + " -- new quantity: " + quantity + "]<p>");
		count++;
		
		return new NumbersRecord(item, quantity);
	}
	
}

