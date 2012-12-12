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

import java.util.ArrayList;

import javax.batch.annotation.BatchProperty;
import javax.batch.annotation.Close;
import javax.batch.annotation.CheckpointInfo;
import javax.batch.annotation.ItemWriter;
import javax.batch.annotation.Open;
import javax.batch.annotation.WriteItems;

import jsr352.tck.chunktypes.ArrayIndexCheckpointData;
import jsr352.tck.chunktypes.ReadRecord;

@ItemWriter("DoSomethingSimpleArrayWriter")
@javax.inject.Named("DoSomethingSimpleArrayWriter")
public class DoSomethingSimpleArrayWriter {

	private int[] writerDataArray = new int[30];
	//private int[] checkArray;
	private int idx = 0;
	private int chkArraySize;
	int chunkWriteIteration = 0;
	
	@BatchProperty(name="app.arraysize")
    String appArraySizeString;
	
	@BatchProperty(name="app.writepoints")
    String writePointsString;
	
	int arraysize;
	int [] writePoints;
	
	@Open
	public void openWriter(ArrayIndexCheckpointData checkpointData) throws Exception {
		System.out.println("openWriter");
		
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
	
	
	@Close
	public void closeWriter() throws Exception {
		//System.out.println("closeWriter - writerDataArray:\n");
		for (int i = 0; i < arraysize; i++){
			System.out.println("WRITE: writerDataArray[" + i + "] = " + writerDataArray[i]);
		}
	}
	
	@WriteItems
	public void writeMyData(ArrayList<ReadRecord> myData) throws Exception {
		
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
	
	@CheckpointInfo
	public ArrayIndexCheckpointData getCPD() throws Exception {
			ArrayIndexCheckpointData _chkptData = new ArrayIndexCheckpointData();
			_chkptData.setCurrentIndex(idx);
		return _chkptData;
	}
}
