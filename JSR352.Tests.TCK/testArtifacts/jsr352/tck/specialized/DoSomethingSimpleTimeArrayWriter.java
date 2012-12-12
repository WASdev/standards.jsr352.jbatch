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

@ItemWriter("DoSomethingSimpleTimeArrayWriter")
@javax.inject.Named("DoSomethingSimpleTimeArrayWriter")
public class DoSomethingSimpleTimeArrayWriter {

	private int[] writerDataArray;
	//private int[] checkArray;
	private int idx = 0;
	private int chkArraySize;
	int chunkWriteIteration = 0;
	
	@BatchProperty(name="app.arraysize")
    String appArraySizeString;
	
	@BatchProperty(name="app.sleeptime")
    String sleeptimeString;
	
	@BatchProperty(name="app.timeinterval")
    String timeintervalString;
	
	int arraysize;
	long currwritetime;
	
	@Open
	public void openWriter(ArrayIndexCheckpointData checkpointData) throws Exception {
		System.out.println("openWriter");
		
		arraysize = Integer.parseInt(appArraySizeString);

		writerDataArray = new int[arraysize];
		
		if (checkpointData == null){
			//position at the beginning
			idx = 0;
			System.out.println("WRITE: chkpt data = null, so idx = " + idx);
		}
		else {
			// position at index held in the cpd
			idx = checkpointData.getCurrentIndex();
			
			System.out.println("WRITE: chkpt data was valid, so idx = " + idx);
			System.out.println("WRITE: chunkWriteIteration = " + chunkWriteIteration);
		}
		
		for (int i = 0; i<arraysize; i++) {
			writerDataArray[i] = 0;
		}
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
		
		//java.util.Date curDate = new java.util.Date();
        //long curts = curDate.getTime();
        //long curdiff = curts - ts;
        //int diff = (int)curdiff / 1000;
        
        //System.out.println("WRITE: diff = " + diff);
		
		//if ((diff <= timeinterval+2) || (diff >= timeinterval-2) ) {
		//	System.out.println("WRITE: the chunk write is occuring at the correct time -> " + diff + " which is: " + timeinterval + " +/- 2 seconds");
		//}
		//else {
		//	System.out.println("WRITE: we have an issue! throw exception here");
		//	throw new Exception("WRITE: the chunk write did not occur at the correct time boundry -> "+ diff + " which is: " + timeinterval + "+/- 2 seconds");
		//}
		chunkWriteIteration++;
		//date = new java.util.Date();
        //ts = date.getTime();
		
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
	
	@CheckpointInfo
	public ArrayIndexCheckpointData getCPD() throws Exception {
			ArrayIndexCheckpointData _chkptData = new ArrayIndexCheckpointData();
			_chkptData.setCurrentIndex(idx);
		return _chkptData;
	}
}

