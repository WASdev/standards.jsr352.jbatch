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

@javax.inject.Named("doSomethingSimpleArrayWriter")
public class DoSomethingSimpleArrayWriter extends AbstractItemWriter<ReadRecord> {

	private int[] writerDataArray = new int[30];
	//private int[] checkArray;
	private int idx = 0;
	private int chkArraySize;
	int chunkWriteIteration = 0;
	
    @Inject    
    @BatchProperty(name="app.arraysize")
    String appArraySizeString;
	
    @Inject    
    @BatchProperty(name="app.writepoints")
    String writePointsString;
	
    @Inject    
    @BatchProperty(name="app.next.writepoints")
    String nextWritePointsString;
	
	     @Inject 
	 private StepContext<MyTransient, MyPersistentRestartUserData> stepCtx = null; 
	 
	int arraysize;
	int [] writePoints;
	
	@Override
	public void open(Externalizable cpd) throws Exception {
		System.out.println("openWriter");
		
	       MyPersistentRestartUserData myData = null;
	        if ((myData = stepCtx.getPersistentUserData()) != null) {        	
	        	stepCtx.setPersistentUserData(new MyPersistentRestartUserData(myData.getExecutionNumber()+1, nextWritePointsString));
	        	System.out.println("AJM: iteration = " + stepCtx.getPersistentUserData().getExecutionNumber());
	        	writePointsString = stepCtx.getPersistentUserData().getNextWritePoints();
	        } else {        
	        	stepCtx.setPersistentUserData(new MyPersistentRestartUserData(1, nextWritePointsString));
	        }
		
        ArrayIndexCheckpointData checkpointData = (ArrayIndexCheckpointData)cpd;
		
		arraysize = Integer.parseInt(appArraySizeString);
		
		String[] writePointsStrArr = writePointsString.split(",");
		writePoints = new int[writePointsString.length()];
		
		System.out.println("AJM: writePointsStrArr.length() = " + writePointsStrArr.length);
		
		for (int i = 0; i<writePointsStrArr.length; i++){
			System.out.println("AJM: writePointsStrArr[" + i + "] = " + writePointsStrArr[i]);
			writePoints[i] = Integer.parseInt(writePointsStrArr[i]);
			System.out.println("AJM: writePoints[" + i + "] = " + writePoints[i]);
		}

		if (checkpointData == null){
			//position at the beginning
			idx = 0;
			System.out.println("WRITE: chkpt data = null, so idx = " + idx);
		}
		else {
			// position at index held in the cpd
			idx = checkpointData.getCurrentIndex();
			//for (int i = 0; i<writePoints.length; i++){
			//	if (idx <= writePoints[i]){
			//		chunkWriteIteration++;
			//	}
			//}
			
			System.out.println("WRITE: chkpt data was valid, so idx = " + idx);
			System.out.println("WRITE: chunkWriteIteration = " + chunkWriteIteration);
		}
		//for (int n=0; n<chkArraySize;n++){
		//	System.out.println("WRITE: chunk write point[" + n + " ]: " + checkArray[n]);
		//}
		
		
		for (int i = 0; i<arraysize; i++) {
			writerDataArray[i] = 0;
		}
		//idx = checkpointData.getCurrentIndex();
		//System.out.println("WRITE: chkpt data was valid, so idx = " + idx);
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
		System.out.println("WRITE: before writing, chunkWriteIteration = " + chunkWriteIteration);
		
		if ((writePoints[chunkWriteIteration] == idx) ) {
			System.out.println("WRITE: the chunk write is occuring at the correct boundry (idx) ->" + idx);
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
		System.out.println("WRITE: size of writePoints->" + writePoints.length);
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
