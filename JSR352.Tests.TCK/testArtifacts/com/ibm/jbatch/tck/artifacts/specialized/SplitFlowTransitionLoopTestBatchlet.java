/*
 * Copyright 2013 International Business Machines Corp.
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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.batch.annotation.BatchProperty;
import javax.batch.api.AbstractBatchlet;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

@javax.inject.Named("splitFlowTransitionLoopTestBatchlet")
public class SplitFlowTransitionLoopTestBatchlet extends AbstractBatchlet {

	public static String GOOD_EXIT_STATUS = "VERY GOOD INVOCATION"; 

    @Inject
	JobContext jobCtx;
    
    @Inject
	StepContext stepCtx = null;
    
    @Inject    
    @BatchProperty(name="temp.file")
    private String tempFile;
    
	@Override
	public String process() throws Exception {
		
		// open file and save step id
		if(tempFile != null && tempFile.trim().length() > 0) {
			saveStepId(stepCtx.getId());
		}
				
		return GOOD_EXIT_STATUS;
	}
	
	private synchronized void saveStepId(String stepId) throws IOException {

		OutputStream out = new FileOutputStream(tempFile, true);
		Writer writer = new OutputStreamWriter(out);
		writer.write(stepId);
		writer.write(System.getProperty("line.separator"));
		writer.close();
	}

}
