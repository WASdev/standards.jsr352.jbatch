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

import javax.batch.annotation.Close;
import javax.batch.annotation.CheckpointInfo;
import javax.batch.annotation.ItemWriter;
import javax.batch.annotation.Open;
import javax.batch.annotation.WriteItems;

import jsr352.tck.chunktypes.CheckpointData;
import jsr352.tck.chunktypes.ReadRecord;


@ItemWriter("DoSomethingItemWriter")
@javax.inject.Named("DoSomethingItemWriter")
public class DoSomethingItemWriterImpl {

	@Open
	public void openWriter(CheckpointData checkpointData) throws Exception {
		System.out.println("openWriter");
	}
	
	@Close
	public void closeWriter() throws Exception {
		System.out.println("closeWriter");
	}
	
	@WriteItems
	public void writeMyData(ArrayList<ReadRecord> myData) throws Exception {
		System.out.println("writeMyData receives chunk size=" + myData.size());
		for  (int i = 0; i< myData.size(); i++) {
			System.out.println("myData=" + myData.get(i).getCount());
		}
	}
	
	@CheckpointInfo
	public CheckpointData getCPD() throws Exception {
		return new CheckpointData();
	}
}
