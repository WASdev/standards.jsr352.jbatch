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
package com.ibm.batch.container.tck.bridge;

import com.ibm.batch.container.services.ServicesManager;
import com.ibm.batch.container.services.ServicesManager.ServiceType;
import com.ibm.batch.tck.spi.BatchContainerServiceProvider;
import com.ibm.batch.tck.spi.JSLInheritanceMerger;
import com.ibm.batch.tck.spi.JobEndCallbackManager;

public class BatchContainerServiceProviderImpl implements BatchContainerServiceProvider {

    private ServicesManager servicesManager = ServicesManager.getInstance();
    
    /*
    @Override
    public JobOperator getJobOperator() {
        
        JobOperator jobOp = (JobOperator)servicesManager.getService(ServiceType.JOB_OP_SERVICE);
        
        return jobOp;
    }
    */

	@Override
	public JSLInheritanceMerger getJSLInheritanceMerger() {
		//stateless merger, new up instead of fetching from ServicesManager
		return new BatchContainerJSLMerger();
	}

	/* (non-Javadoc)
	 * @see com.ibm.batch.tck.spi.BatchContainerServiceProvider#getCallbackManager()
	 */
	@Override
	public JobEndCallbackManager getCallbackManager() {
		JobEndCallbackManager callbackMgr = 
                (JobEndCallbackManager)servicesManager.getService(ServiceType.CALLBACK_SERVICE);
            return callbackMgr;
	}
}
