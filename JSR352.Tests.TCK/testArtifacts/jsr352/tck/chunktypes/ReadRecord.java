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
package jsr352.tck.chunktypes;

@javax.inject.Named("ReadRecord")
public class ReadRecord {
	private int count = 0;
	
	public ReadRecord() {
		//System.out.println("AJM: in ReadRecord ctor");
	}
	
	public ReadRecord(int in) {
		count = in;
		System.out.println("AJM: in ReadRecord ctor (int), count = " + count);

	}
	public int getCount() {
		return count;
	}
	
	public void  setRecord(int i) {
		//System.out.println("AJM: in setRecord");
		count = i;
	}
}
