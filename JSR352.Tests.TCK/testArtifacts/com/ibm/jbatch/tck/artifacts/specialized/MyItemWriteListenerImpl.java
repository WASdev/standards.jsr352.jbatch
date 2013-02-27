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

import java.util.List;
import java.util.logging.Logger;

import javax.batch.annotation.BatchProperty;
import javax.batch.api.AbstractItemWriteListener;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;

import com.ibm.jbatch.tck.artifacts.chunktypes.WriteRecord;

@javax.inject.Named("myItemWriteListenerImpl")
public class MyItemWriteListenerImpl extends AbstractItemWriteListener<WriteRecord> {
	private final static String sourceClass = MyItemWriteListenerImpl.class.getName();
	private final static Logger logger = Logger.getLogger(sourceClass);

	int beforecounter = 1;
	int aftercounter = 1;
	
	public static final String GOOD_EXIT_STATUS = "GOOD STATUS";
	public static final String BAD_EXIT_STATUS = "BAD STATUS";
	
    @Inject 
    JobContext jobCtx; 
	
    @Inject    
    @BatchProperty(name="app.listenertest")
    String applistenerTest;
	
	@Override
	public void beforeWrite(List<WriteRecord> items) throws Exception {
		if (items != null && applistenerTest.equals("WRITE")){
			logger.finer("In beforeWrite()");
			beforecounter++;
			System.out.println("AJM: beforecounter = " + beforecounter);

		}
	}
	
	@Override
	public void afterWrite(List<WriteRecord> items) throws Exception {
		
		System.out.println("AJM: applistenerTest = " + applistenerTest);
		
		if (items != null && applistenerTest.equals("WRITE")){
			logger.finer("In afterWrite()");
			
			aftercounter++;
			System.out.println("AJM: aftercounter = " + aftercounter);

			if (beforecounter == aftercounter) {
				jobCtx.setExitStatus(GOOD_EXIT_STATUS);
			} else
				jobCtx.setExitStatus(BAD_EXIT_STATUS);
		}
	}
	
    @Override
    public void onWriteError(List<WriteRecord> items, Exception e) throws Exception {
        logger.finer("In onWriteError()" + e);
    }
	
}
