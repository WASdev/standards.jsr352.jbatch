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
package com.ibm.jbatch.container.tck.bridge;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.spi.IBatchConfig;
import com.ibm.jbatch.tck.spi.JobEndCallback;

public class JobEndCallbackManagerImpl implements IJobEndCallbackService {
    
    private final static String sourceClass = JobEndCallbackManagerImpl.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);
    
	private Set<JobEndCallback> callbacks = new HashSet<JobEndCallback>();
    
	@Override
	public synchronized void registerJobEndCallback(JobEndCallback callback) {
		callbacks.add(callback);
		
	}
	
	@Override
	public synchronized void deregisterJobEndCallback(JobEndCallback callback) {
		callbacks.remove(callback);
	}

	@Override
	public synchronized void done(long jobExecutionId) {
        if (logger.isLoggable(Level.FINER)) {            
            logger.finer("Firing callbacks for job execution id: " + jobExecutionId);
        }
		JobEndCallback[] arr = callbacks.toArray(new JobEndCallback[0]);
		for (JobEndCallback callback : arr) {
			if (logger.isLoggable(Level.FINE)) {            
				logger.fine("Next registered callback: " + callback);
			}
			callback.done(jobExecutionId);
		}
        if (logger.isLoggable(Level.FINER)) {            
            logger.finer("Done firing callbacks for job execution id: " + jobExecutionId);
        }
	}

	/* (non-Javadoc)
	 * @see com.ibm.jbatch.container.services.IBatchServiceBase#init(com.ibm.jbatch.container.IBatchConfig)
	 */
	@Override
	public void init(IBatchConfig batchConfig)
			throws BatchContainerServiceException {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.ibm.jbatch.container.services.IBatchServiceBase#shutdown()
	 */
	@Override
	public void shutdown() throws BatchContainerServiceException {
		// TODO Auto-generated method stub
		
	}

}
