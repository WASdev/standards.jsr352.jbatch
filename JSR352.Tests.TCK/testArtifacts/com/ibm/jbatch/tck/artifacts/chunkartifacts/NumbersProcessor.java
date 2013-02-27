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
package com.ibm.jbatch.tck.artifacts.chunkartifacts;

import javax.batch.annotation.BatchProperty;
import javax.batch.api.ItemProcessor;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

import org.testng.Reporter;

import com.ibm.jbatch.tck.artifacts.chunktypes.NumbersRecord;
import com.ibm.jbatch.tck.artifacts.reusable.MyParentException;


@javax.inject.Named("numbersProcessor")
public class NumbersProcessor implements ItemProcessor<NumbersRecord, NumbersRecord> {
	
	
    @Inject
    StepContext<?,?> stepCtx;
	
    @Inject    
    @BatchProperty(name="forced.fail.count.process")
	String forcedFailCountProp;
	
	private static final int STATE_NORMAL = 0;
	private static final int STATE_RETRY = 1;
	private static final int STATE_AFTER_RETRY = 2;
	private int testState = STATE_NORMAL;
	
	int failitem = 0;
	int count = 1;
	int forcedFailCount;
	
	boolean isInited = false;
	
	public NumbersRecord processItem(NumbersRecord record) throws Exception {
		int item = record.getItem();
		int quantity = record.getQuantity();
		
		if(!isInited) {
			forcedFailCount = Integer.parseInt(forcedFailCountProp);
			isInited = true;
		}
		
		// Throw an exception when forcedFailCount is reached
		if (forcedFailCount != 0 && count >= forcedFailCount) {
				   forcedFailCount = 0;
					testState = STATE_RETRY;
					Reporter.log("Fail on purpose in NumbersRecord.processItem<p>");
					throw new MyParentException("Fail on purpose in NumbersRecord.processItem()");	
		} 
		
		if (testState == STATE_RETRY)
		{
			if(stepCtx.getProperties().getProperty("retry.process.exception.invoked") != "true") {
				Reporter.log("onRetryProcessException not invoked<p>");
				throw new Exception("onRetryProcessException not invoked");
			}
			
			if(stepCtx.getProperties().getProperty("retry.process.exception.match") != "true") {
				Reporter.log("retryable exception does not match<p>");
				throw new Exception("retryable exception does not match");
			}
			
			testState = STATE_AFTER_RETRY;
		}
		
		
		quantity = quantity + 1;
		count++;
		
		return new NumbersRecord(item, quantity);
	}
	
}
