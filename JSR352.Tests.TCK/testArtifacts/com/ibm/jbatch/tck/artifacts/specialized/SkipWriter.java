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

import java.io.Externalizable;
import java.util.List;

import javax.batch.annotation.BatchProperty;
import javax.batch.api.ItemWriter;
import javax.inject.Inject;

import com.ibm.jbatch.tck.artifacts.chunktypes.ArrayIndexCheckpointData;
import com.ibm.jbatch.tck.artifacts.chunktypes.ReadRecord;
import com.ibm.jbatch.tck.artifacts.reusable.MyParentException;

@javax.inject.Named("skipWriter")
public class SkipWriter implements ItemWriter<ReadRecord> {

	private int[] writerDataArray = new int[30];
	//private int[] checkArray;
	private int idx = 0;
	private int chkArraySize;
	int chunkWriteIteration = 0;
	
    @Inject    
    @BatchProperty(name="app.arraysize")
    String appArraySizeString;
	
    @Inject    
    @BatchProperty(name="writerecord.fail")
    String writeRecordFailNumberString = null;
	
	int arraysize;
	int [] failnum;
	int [] writePoints;
	
	@Override
	public void open(Externalizable cpd) throws Exception {
		System.out.println("openWriter");
		
		ArrayIndexCheckpointData checkpointData = (ArrayIndexCheckpointData)cpd;
		
		arraysize = Integer.parseInt(appArraySizeString);
		
		if (!writeRecordFailNumberString.equals("null")) {
			String[] writeFailPointsStrArr = writeRecordFailNumberString.split(",");
			failnum = new int[writeFailPointsStrArr.length];
			for (int i = 0; i < writeFailPointsStrArr.length; i++) {
				failnum[i] = Integer.parseInt(writeFailPointsStrArr[i]);
			}
		}
		else {
			failnum = new int[1];
			failnum[0] = -1;
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
		
		if (!writeRecordFailNumberString.equals("null")) {
			if (chunkWriteIteration == 2) {
				chunkWriteIteration++;
				throw new MyParentException(
						"fail on purpose on write iteration = 2");
			} else if (chunkWriteIteration == 5) {
				chunkWriteIteration++;
				throw new MyParentException(
						"fail on purpose on write iteration = 5");
			} else if (chunkWriteIteration == 8) {
				chunkWriteIteration++;
				throw new MyParentException(
						"fail on purpose on write iteration = 8");
			} else {
				chunkWriteIteration++;
			}
		} else {
			chunkWriteIteration++;
		}
		
		for  (i = 0; i < myData.size(); i++) {
			
			writerDataArray[idx] = myData.get(i).getCount();
			idx++;
		}
		for (i = 0; i < arraysize; i++){
			System.out.println("WRITE: writerDataArray[" + i + "] = " + writerDataArray[i]);
		}
		System.out.println("WRITE: idx = " + idx + " and i = " + i);
		System.out.println("WRITE: chunkWriteIteration= "+ chunkWriteIteration);
		//if (checkArray[chunkWriteIteration] == (chunkWriteIteration+1)*chunksize ) {
	}
	
	@Override
	public ArrayIndexCheckpointData checkpointInfo() throws Exception {
			ArrayIndexCheckpointData _chkptData = new ArrayIndexCheckpointData();
			_chkptData.setCurrentIndex(idx);
		return _chkptData;
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

