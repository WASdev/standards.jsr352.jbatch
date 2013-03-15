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

import java.io.Serializable;
import java.util.List;
import java.util.logging.Logger;

import javax.batch.annotation.BatchProperty;
import javax.batch.api.chunk.AbstractItemWriter;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

import com.ibm.jbatch.tck.artifacts.chunktypes.ArrayIndexCheckpointData;
import com.ibm.jbatch.tck.artifacts.chunktypes.ReadRecord;
import com.ibm.jbatch.tck.artifacts.reusable.MyPersistentRestartUserData;

@javax.inject.Named("doSomethingArrayItemWriterImpl")
public class DoSomethingArrayItemWriterImpl extends AbstractItemWriter<ReadRecord> {

	private final static Logger logger = Logger.getLogger(DoSomethingArrayItemWriterImpl.class.getName());
	
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
    public void open(Serializable cpd) {
                    
        ArrayIndexCheckpointData checkpointData = (ArrayIndexCheckpointData)cpd;
		logger.fine("openWriter");
		
		arraysize = Integer.parseInt(appArraySizeString);
		chunksize = Integer.parseInt(chunkSizeString);
		commitinterval = Integer.parseInt(commitintervalString);
		
	    MyPersistentRestartUserData myData = null;
        if ((myData = stepCtx.getPersistentUserData()) != null) {        	
        	stepCtx.setPersistentUserData(new MyPersistentRestartUserData(myData.getExecutionNumber() + 1, null));
        	logger.fine("AJM: iteration = " + stepCtx.getPersistentUserData().getExecutionNumber());
        } else {        
        	stepCtx.setPersistentUserData(new MyPersistentRestartUserData(1, null));
        }
		
		if (checkpointData == null){
			//position at the beginning
			idx = 0;
			logger.fine("WRITE: chkpt data = null, so idx = " + idx);
		}
		else {
			// position at index held in the cpd
			idx = checkpointData.getCurrentIndex();
			logger.fine("WRITE: chkpt data was valid, so idx = " + idx);
			
			logger.fine("WRITE: idx % chunksize =" + idx % chunksize);
			
			if (idx % chunksize == 0){
				logger.fine("WRITE: the previous checkpoint was correct"); 
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
				logger.fine("AJM: figuring out nthe array size, idx = " + idx);
				chkArraySize = round((arraysize - idx), chunksize) + round((arraysize - idx), commitinterval);
				//((arraysize - idx) / chunksize) + ((arraysize - idx) / commitinterval);
			}
		}
		else {
			chkArraySize= round(arraysize, (commitinterval % chunksize)) + 1; //arraysize / (commitinterval % chunksize);
		}
		
		checkArray = new int[chkArraySize];
		logger.fine("WRITE: check array size = " + chkArraySize);
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
			logger.fine("WRITE: chunk write point[" + n + " ]: " + checkArray[n]);
		}
		
		
		for (int i = 0; i<arraysize; i++) {
			writerDataArray[i] = 0;
		}
		//idx = checkpointData.getCurrentIndex();
		//logger.fine("WRITE: chkpt data was valid, so idx = " + idx);
	}
	
	private int round(int i1, int i2){
		return (i1 + i2 - 1) / i2;
	}
	
	@Override
	public void close() throws Exception {
		//logger.fine("closeWriter - writerDataArray:\n");
		for (int i = 0; i < arraysize; i++){
			logger.fine("WRITE: writerDataArray[" + i + "] = " + writerDataArray[i]);
		}
	}
	
    @Override
	public void writeItems(List<ReadRecord> myData) throws Exception {
		
		logger.fine("writeMyData receives chunk size=" + myData.size());
		int i;
		logger.fine("WRITE: before writing, idx = " + idx);
		
		if ((checkArray[chunkWriteIteration] == idx) ) {
			logger.fine("WRITE: the chunk write is occuring at the correct boundary (idx) ->" + idx);
		}
		else if (checkArray[chunkWriteIteration] == (chunkWriteIteration+1)*(commitinterval % chunksize)  ) {
			logger.fine("WRITE: the chunk write is occuring at the correct boundary ->" + checkArray[chunkWriteIteration]);
		}
		else {
			logger.fine("WRITE: we have an issue! throw exception here");
			throw new Exception("WRITE: the chunk write did not at the correct boundary (idx) ->" + idx);
		}
		chunkWriteIteration++;
		
		for  (i = 0; i < myData.size(); i++) {
			writerDataArray[idx] = myData.get(i).getCount();
			idx++;
		}
		for (i = 0; i < arraysize; i++){
			logger.fine("WRITE: writerDataArray[" + i + "] = " + writerDataArray[i]);
		}
		logger.fine("WRITE: idx = " + idx + " and i = " + i);
		logger.fine("WRITE: chunkWriteIteration= "+ chunkWriteIteration);
		logger.fine("WRITE: size of checkArray->" + checkArray.length);
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
