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

import java.io.Externalizable;
import java.util.List;

import javax.batch.annotation.BatchProperty;
import javax.batch.api.AbstractItemWriter;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

import jsr352.tck.chunktypes.ArrayIndexCheckpointData;
import jsr352.tck.chunktypes.ReadRecord;
import jsr352.tck.reusable.MyPersistentRestartUserData;

@javax.inject.Named("doSomethingArrayItemWriterImpl")
public class DoSomethingArrayItemWriterImpl extends AbstractItemWriter<ReadRecord> {

	private int[] writerDataArray = new int[30];
	private int[] checkArray;
	private int idx = 0;
	private int chkArraySize;
	int chunkWriteIteration = 0;
	
	     @Inject 
	 private StepContext<MyTransient, MyPersistentRestartUserData> stepCtx = null; 
	
    @Inject    
    @BatchProperty(name="app.arraysize")
    String appArraySizeString;
	
    @Inject    
    @BatchProperty(name="app.chunksize")
    String chunkSizeString;
	
    @Inject    
    @BatchProperty(name="app.commitinterval")
    String commitintervalString;
		
	int arraysize;
	int chunksize;
	int commitinterval;
	
    @Override
    public void open(Externalizable cpd) {
                    
        ArrayIndexCheckpointData checkpointData = (ArrayIndexCheckpointData)cpd;
		System.out.println("openWriter");
		
		arraysize = Integer.parseInt(appArraySizeString);
		chunksize = Integer.parseInt(chunkSizeString);
		commitinterval = Integer.parseInt(commitintervalString);
		
	    MyPersistentRestartUserData myData = null;
        if ((myData = stepCtx.getPersistentUserData()) != null) {        	
        	stepCtx.setPersistentUserData(new MyPersistentRestartUserData(myData.getExecutionNumber() + 1, null));
        	System.out.println("AJM: iteration = " + stepCtx.getPersistentUserData().getExecutionNumber());
        } else {        
        	stepCtx.setPersistentUserData(new MyPersistentRestartUserData(1, null));
        }
		
		if (checkpointData == null){
			//position at the beginning
			idx = 0;
			System.out.println("WRITE: chkpt data = null, so idx = " + idx);
		}
		else {
			// position at index held in the cpd
			idx = checkpointData.getCurrentIndex();
			System.out.println("WRITE: chkpt data was valid, so idx = " + idx);
			
			System.out.println("WRITE: idx % chunksize =" + idx % chunksize);
			
			if (idx % chunksize == 0){
				System.out.println("WRITE: the previous checkpoint was correct"); 
			}

		}
		
		// determine size of the write check array
		
		if (commitinterval % chunksize == 0){
			if (idx == 0){
				chkArraySize = round(arraysize, chunksize)+1; //arraysize / chunksize;
			}
			else {
				chkArraySize = round(arraysize - idx, chunksize)+1; //(arraysize - idx) / chunksize;
			}
		}
		else if (commitinterval > chunksize) {
			if (idx == 0){
				chkArraySize = round(arraysize, chunksize) + round (arraysize, commitinterval) + 1;
				//(arraysize / chunksize) + (arraysize / commitinterval);
			}
			else {
				System.out.println("AJM: figuring out nthe array size, idx = " + idx);
				chkArraySize = round((arraysize - idx), chunksize) + round((arraysize - idx), commitinterval);
				//((arraysize - idx) / chunksize) + ((arraysize - idx) / commitinterval);
			}
		}
		else {
			chkArraySize= round(arraysize, (commitinterval % chunksize)) + 1; //arraysize / (commitinterval % chunksize);
		}
		
		checkArray = new int[chkArraySize];
		System.out.println("WRITE: check array size = " + chkArraySize);
		int checkerIdx= 0;
		
		// fill the write check array with the correct write points
		boolean init = true;
		for (int i = idx; i <= arraysize; i++){
			if ((chunksize < commitinterval) && !(commitinterval % chunksize == 0)){
				if (init){
					if (idx == 0) {
						checkArray[checkerIdx] = idx;
						checkerIdx++;
						checkArray[checkerIdx] = chunksize;
						checkerIdx++;
						init = false;
					}
					else {
						checkArray[checkerIdx] = idx;
						checkerIdx++;
						if (idx % commitinterval == 0){
							checkArray[checkerIdx] = idx + chunksize;
							checkerIdx++;
						}
						init = false;
					}
					
				}
				else {
					if (i % commitinterval == 0){
						checkArray[checkerIdx] = i;
						if (i != arraysize){
							checkerIdx++;
							checkArray[checkerIdx] = i + chunksize;
							checkerIdx++;
						}
					}
				}
			}
			else if (commitinterval < chunksize){
				if (i % commitinterval == 0){
					checkArray[checkerIdx] = i;
					checkerIdx++;
				}
			}
			else { // chunk size equal to/multiple of commit interval
				if (i % chunksize == 0) {
					checkArray[checkerIdx] = i;
					checkerIdx++;
				}	
				else if (i % commitinterval == 0){
					checkArray[checkerIdx] = i;
					checkerIdx++;
				}
			}
		}
		
		for (int n=0; n<chkArraySize;n++){
			System.out.println("WRITE: chunk write point[" + n + " ]: " + checkArray[n]);
		}
		
		
		for (int i = 0; i<arraysize; i++) {
			writerDataArray[i] = 0;
		}
		//idx = checkpointData.getCurrentIndex();
		//System.out.println("WRITE: chkpt data was valid, so idx = " + idx);
	}
	
	private int round(int i1, int i2){
		return (i1 + i2 - 1) / i2;
	}
	
	@Override
	public void close() throws Exception {
		//System.out.println("closeWriter - writerDataArray:\n");
		for (int i = 0; i < arraysize; i++){
			System.out.println("WRITE: writerDataArray[" + i + "] = " + writerDataArray[i]);
		}
	}
	
    @Override
	public void writeItems(List<ReadRecord> myData) throws Exception {
		
		System.out.println("writeMyData receives chunk size=" + myData.size());
		int i;
		System.out.println("WRITE: before writing, idx = " + idx);
		
		if ((checkArray[chunkWriteIteration] == idx) ) {
			System.out.println("WRITE: the chunk write is occuring at the correct boundry (idx) ->" + idx);
		}
		else if (checkArray[chunkWriteIteration] == (chunkWriteIteration+1)*(commitinterval % chunksize)  ) {
			System.out.println("WRITE: the chunk write is occuring at the correct boundry ->" + checkArray[chunkWriteIteration]);
		}
		else {
			System.out.println("WRITE: we have an issue! throw exception here");
			throw new Exception("WRITE: the chunk write did not at the correct boundry (idx) ->" + idx);
		}
		chunkWriteIteration++;
		
		for  (i = 0; i < myData.size(); i++) {
			writerDataArray[idx] = myData.get(i).getCount();
			idx++;
		}
		for (i = 0; i < arraysize; i++){
			System.out.println("WRITE: writerDataArray[" + i + "] = " + writerDataArray[i]);
		}
		System.out.println("WRITE: idx = " + idx + " and i = " + i);
		System.out.println("WRITE: chunkWriteIteration= "+ chunkWriteIteration);
		System.out.println("WRITE: size of checkArray->" + checkArray.length);
		//if (checkArray[chunkWriteIteration] == (chunkWriteIteration+1)*chunksize ) {
	}
	
	@Override
	public ArrayIndexCheckpointData checkpointInfo() throws Exception {
			ArrayIndexCheckpointData _chkptData = new ArrayIndexCheckpointData();
			_chkptData.setCurrentIndex(idx);
		return _chkptData;
	}
	
	   private class MyTransient {
	        int data = 0;
	        MyTransient(int x) {
	            data = x;
	        }   
	    }
	
}
