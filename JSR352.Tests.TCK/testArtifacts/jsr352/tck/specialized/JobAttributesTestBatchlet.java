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
package jsr352.tck.specialized;

import javax.batch.api.AbstractBatchlet;

@javax.inject.Named("jobAttributesTestBatchlet")
public class JobAttributesTestBatchlet extends AbstractBatchlet {

	public static String GOOD_EXIT_STATUS = "VERY GOOD INVOCATION"; 
	
	private boolean stopped = false;

	@Override
	public String process() throws Exception {

		// do nothing, we are only testing job restartable and abstract attribute
		// loop until stopped or test time out.
		for (int i = 0; i < 10000; i++) {
			// do nothing
//			Thread.sleep(100);
			if (stopped) { break; }
		}
		
		return GOOD_EXIT_STATUS;
	}
	
	@Override
	public void stop() throws Exception {
		super.stop();
		stopped = true;
	}
}
