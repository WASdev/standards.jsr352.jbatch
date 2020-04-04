/*
 * Copyright 2014 International Business Machines Corp.
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
package test.artifacts;

import java.util.logging.Logger;

import jakarta.batch.api.AbstractBatchlet;
import jakarta.batch.api.BatchProperty;
import jakarta.batch.runtime.context.JobContext;
import jakarta.batch.runtime.context.StepContext;
import jakarta.inject.Inject;

public class PropertyResolverBatchlet extends AbstractBatchlet{

	private final static Logger logger = Logger.getLogger(PropertyResolverBatchlet.class.getName());

	private volatile static int count = 1;
	private volatile static String data = "";

	public static String GOOD_EXIT_STATUS = "VERY GOOD INVOCATION";       

	@Inject
	JobContext jobCtx;
	
	@Inject
	StepContext stepCtx;
	
	@Inject @BatchProperty(name="partitionString")
	String partitionString;
	
	@Inject @BatchProperty
	public String sleepTime;
	int sleepVal = 0;

	@Inject @BatchProperty
	public String forceFailure = "false";
	Boolean fail;

	private void init() {
		try {
			fail = Boolean.parseBoolean(forceFailure);
		} catch (Exception e) { 
			fail = false;
		}
		try {
			sleepVal = Integer.parseInt(sleepTime);
		} catch (Exception e) { 
			sleepVal = 0;
		}
	}
	@Override
	public String process() throws Exception {	
		init();
		if (fail) {
			throw new IllegalArgumentException("Forcing failure");
		}
		if (sleepTime != null) {
			Thread.sleep(sleepVal);
		}
		logger.fine("Running batchlet process(): " + count);
		setDataValue();
		count++;

		return GOOD_EXIT_STATUS;
	}

	@Override
	public void stop() throws Exception { }
	
	private void setDataValue() {
		data = "#" + partitionString;
		stepCtx.setPersistentUserData(data);
	}
}
