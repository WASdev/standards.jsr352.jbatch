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

import javax.batch.annotation.BatchProperty;
import javax.batch.api.AbstractItemReader;
import javax.inject.Inject;

import com.ibm.jbatch.tck.artifacts.chunktypes.CheckpointData;
import com.ibm.jbatch.tck.artifacts.chunktypes.ReadRecord;
import com.ibm.jbatch.tck.artifacts.reusable.MyParentException;


@javax.inject.Named("doSomethingItemReaderImpl")
public class DoSomethingItemReaderImpl extends AbstractItemReader<ReadRecord> {
	    @Inject    
	    @BatchProperty(name="fail.immediate")
	    String failImmediateString;
	 
	    boolean failThrowEx = false;
	    
		private int count = 0;
		
		@Override
		public void open(Externalizable cpd) {
			System.out.println("DoSomethingItemReaderImpl.openMe, count should be 0, actual value = " + count);
			if (failImmediateString!=null){
				failThrowEx = Boolean.parseBoolean(failImmediateString);
			}
		}
		
		@Override
		public ReadRecord readItem() throws Exception {
			if (failThrowEx){
				throw new MyParentException("Testing getException");
			}
			
			count = count + 1;
			if (count == 10) {
				return null;
			}
		    return new ReadRecord(count);
		}
		
		@Override
		public CheckpointData checkpointInfo() {
		    return new CheckpointData();
		}

}
