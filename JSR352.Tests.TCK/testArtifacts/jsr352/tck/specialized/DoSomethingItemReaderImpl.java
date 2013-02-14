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

import javax.batch.api.AbstractItemReader;

import jsr352.tck.chunktypes.CheckpointData;
import jsr352.tck.chunktypes.ReadRecord;


@javax.inject.Named("doSomethingItemReaderImpl")
public class DoSomethingItemReaderImpl extends AbstractItemReader<ReadRecord> {
		private int count = 0;
		
		@Override
		public void open(Externalizable cpd) {
			System.out.println("DoSomethingItemReaderImpl.openMe, count should be 0, actual value = " + count);
		}
		
		@Override
		public ReadRecord readItem() {
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
