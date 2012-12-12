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
package jsr352.test;

import javax.batch.annotation.*;
import javax.batch.annotation.Process;


@Batchlet("MyIncompleteBatchlet")
public class MyIncompleteBatchletImpl {
    
	
	@Process
	public String process() throws Exception {
		System.out.println("MyBatchLetImpl.process() - @Process");
		return "SUCCESS";		
	}
	
	/* Incomplete on purpose
	@Stop
	public void cancel() throws Exception {
		System.out.println("MyBatchLetImpl.cancel() - @Stop");		
	}
	*/
	
	@EndStep
	public void end() throws Exception {
		System.out.println("MyBatchLetImpl.end() - @EndStep");
	}
}
