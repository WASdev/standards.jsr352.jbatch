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

import javax.batch.annotation.BatchProperty;
import javax.batch.api.ItemProcessor;
import javax.inject.Inject;

import com.ibm.jbatch.tck.artifacts.chunktypes.ReadRecord;
import com.ibm.jbatch.tck.artifacts.reusable.MyParentException;


@javax.inject.Named("skipProcessor")
public class SkipProcessor implements ItemProcessor<ReadRecord, ReadRecord> {
	
    @Inject    
    @BatchProperty(name="execution.number")
    String executionNumberString;
	
    @Inject    
    @BatchProperty(name="processrecord.fail")
    String processrecordfailNumberString = null;
	
	boolean init = true;
	boolean threwSkipException = false;
	private int update = 10;
	
	int [] failnum;
	int execnum;
	boolean inited = false;
	int processIteration = 0;
	
	@Override
	public ReadRecord processItem(ReadRecord record) throws Exception {
			
		if (!inited){
			if (!processrecordfailNumberString.equals("null")) {
				String[] processFailPointsStrArr = processrecordfailNumberString.split(",");
				failnum = new int[processFailPointsStrArr.length];
				for (int i = 0; i < processFailPointsStrArr.length; i++) {
					failnum[i] = Integer.parseInt(processFailPointsStrArr[i]);
				}
			}
			else {
				failnum = new int[1];
				failnum[0] = -1;
			}
			inited = true;
			System.out.println("AJM: PROCESSOR - failnum = " + failnum);
		}
		
		if (threwSkipException){
			update++;
			processIteration++;
			threwSkipException = false;
		}
		
		System.out.println("AJM: PROCESSOR: failnum = " + failnum);
		System.out.println("AJM: PROCESSOR: processIteration = " + processIteration);
		
		ReadRecord processedRecord = null;
		
		if (isFailnum(processIteration)){
			System.out.println("READ: got the fail num..." + failnum);
			threwSkipException = true;
			throw new MyParentException("fail on purpose on idx = " + failnum);
		}
		
		processedRecord = record;
		processedRecord.setRecord(update);
		update = update +1;
		processIteration++;
		return processedRecord;
	}
	
	private boolean isFailnum(int idxIn) {
		
		boolean ans = false;
		for (int i = 0; i < failnum.length; i++) {
			if (idxIn == failnum[i]){
				ans = true;
			}
		}
		return ans;
	}
}
