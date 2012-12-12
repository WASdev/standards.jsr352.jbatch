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
package com.ibm.batch.container.context.impl;

import javax.batch.runtime.Metric;

public class MetricImpl implements Metric {
	
	public enum Counter {
		READ_COUNT("readCount"),
		WRITE_COUNT("writeCount"),
		COMMIT_COUNT("commitCount"),
		ROLLBACK_COUNT("rollbackCount"),
		READ_SKIP_COUNT("readSkipCount"),
		PROCESS_SKIP_COUNT("processSkipCount"),
		FILTER_COUNT("filterCount"),
		WRITE_SKIP_COUNT("writeSkipCount");
		
		private String name;
		private Counter(String name) {
			this.name = name;
		}
		@Override
		public String toString() {
			return this.name;
		}
	}
	
	private String name;
	
	private Counter counter;
	
	private long value;
	
	public MetricImpl(Counter counter, long value) {
		//this.name = counter.toString();
		this.counter = counter;
		this.value = value;
	}
	
	@Override
	public String getName() {
		return this.counter.toString();
	}

	@Override
	public long getValue() {
		return this.value;
	}
	
	public void incValue() {
		++this.value;
	}
	
	public void incValueBy(long incValue) {
		this.value = this.value + incValue;
	}
	
	public void decValue() {
		this.value = --this.value;
	}

}
