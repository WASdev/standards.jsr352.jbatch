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
package com.ibm.jbatch.tck.artifacts.reusable;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.batch.api.AbstractBatchlet;


@javax.inject.Named("myParallelSubJobsExitStatusBatchlet")
public class MyParallelSubJobsExitStatusBatchlet extends AbstractBatchlet {
    
	private final static Logger logger = Logger.getLogger(MyParallelSubJobsExitStatusBatchlet.class.getName());
	
    private volatile static AtomicInteger count = new AtomicInteger(1);
    
    public static String GOOD_EXIT_STATUS = "VERY GOOD INVOCATION";       
    
	@Override
	public String process() throws Exception {	
		logger.fine("Running batchlet process(): " + count);
		
		count.incrementAndGet();
		
		//Get the last thread to start to sleep the longest so we can show its exit status is the one that is
		//picked up.
		if (count.get() == 11) {
		    Thread.sleep(2000);
		}
		
		String returnString = "VERY GOOD INVOCATION " + count;
		return returnString;
	}
	
	@Override
	public void stop() throws Exception {
				
	}
	

}
