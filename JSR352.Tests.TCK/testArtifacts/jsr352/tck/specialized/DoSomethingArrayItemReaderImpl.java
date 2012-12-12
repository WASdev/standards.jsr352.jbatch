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

import javax.batch.annotation.*;

import jsr352.tck.chunktypes.ArrayIndexCheckpointData;
import jsr352.tck.chunktypes.ReadRecord;

@ItemReader("DoSomethingArrayItemReader")
@javax.inject.Named("DoSomethingArrayItemReader")
public class DoSomethingArrayItemReaderImpl {
		
	private int count = 0;
	private int[] readerDataArray;
	private int idx;
	ArrayIndexCheckpointData _cpd = new ArrayIndexCheckpointData();
	
	@BatchProperty(name="readrecord.fail")
    String readrecordfailNumberString;
	
	@BatchProperty(name="execution.number")
    String executionNumberString;
	
	@BatchProperty(name="app.arraysize")
    String appArraySizeString;
		
	int failnum;
	int execnum;
	int arraysize;
	
	public DoSomethingArrayItemReaderImpl(){
			
	}
		
		@Open
		public void openMe(ArrayIndexCheckpointData cpd) {
						
			failnum = Integer.parseInt(readrecordfailNumberString);
            execnum = Integer.parseInt(executionNumberString);
            
    		arraysize = Integer.parseInt(appArraySizeString);
    		readerDataArray =  new int[arraysize];
    		
    		for (int i = 0; i<arraysize; i++){
    			// init the data array
    			readerDataArray[i] = i;
    		}
    	
			if (cpd == null){
				//position at the beginning
				idx = 0;
			}
			else {
				// position at index held in the cpd
				idx = cpd.getCurrentIndex() + 1; 
			}
			System.out.println("READ: starting at index: " + idx);
		}
		
		@ReadItem
		public ReadRecord readIt() throws Exception {
		
			int i = idx;
			
			if (i == arraysize) {
				return null;
			}
			if (execnum == 2){
				failnum = -1;
			}
						
			if (idx == failnum-1){
				System.out.println("READ: got the fail num..." + failnum);
				throw new Exception("fail on purpose on idx = " + failnum);
			}
			count = count + 1;
			idx = idx+1;
			_cpd.setCurrentIndex(i);
		    return new ReadRecord(readerDataArray[i]);
		}
		
		@CheckpointInfo
		public ArrayIndexCheckpointData getCPD() {
			
			System.out.println("READ: in getCPD cpd index from store: " + _cpd.getCurrentIndex());
			System.out.println("READ: in getCPD idx : " + idx);
			
		    return _cpd;   
		}

}
