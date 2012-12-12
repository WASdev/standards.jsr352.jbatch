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
package com.ibm.batch.container.services.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.batch.container.IBatchConfig;
import com.ibm.batch.container.exception.BatchContainerServiceException;
import com.ibm.batch.container.services.IBatchThreadPoolService;
import com.ibm.batch.container.services.ParallelTaskResult;
import com.ibm.batch.container.util.BatchWorkUnit;

public class JSEThreadPoolServiceImpl implements IBatchThreadPoolService {
	private final static String sourceClass = JSEThreadPoolServiceImpl.class.getName();
	private final static Logger logger = Logger.getLogger(sourceClass);
	
	private int _poolSize = 1;
	private ExecutorService _cachedThreadPool;
	private ScheduledThreadPoolExecutor _scheduledThreadPool;
	
	public JSEThreadPoolServiceImpl() {
		
	}
	
	public void init(IBatchConfig pgcConfig) throws BatchContainerServiceException {
		String method = "init";
		if(logger.isLoggable(Level.FINER)) { logger.entering(sourceClass, method);	}
		
		_cachedThreadPool = Executors.newCachedThreadPool();
		_scheduledThreadPool = new ScheduledThreadPoolExecutor(_poolSize);
		
		if(logger.isLoggable(Level.FINER)) { logger.exiting(sourceClass, method);	}

	}

	public void shutdown() throws BatchContainerServiceException {
		String method = "shutdown";
		if(logger.isLoggable(Level.FINER)) { logger.entering(sourceClass, method);	}
		
		_cachedThreadPool.shutdownNow();
		_scheduledThreadPool.shutdownNow();
		_cachedThreadPool = null;
		_scheduledThreadPool = null;
		
		if(logger.isLoggable(Level.FINER)) { logger.exiting(sourceClass, method);	}
		
	}

	
	public void executeJob(BatchWorkUnit job) {
		String method = "executeJob";
		if(logger.isLoggable(Level.FINER)) { logger.entering(sourceClass, method);	}
		
		_cachedThreadPool.execute(job);
		
		if(logger.isLoggable(Level.FINER)) { logger.exiting(sourceClass, method);	}
	}

//	public void executeJob(String jobType, Work job) 
//		throws NamingException, IllegalArgumentException, WorkException, Exception {
//		String method = "executeJob";
//		if(logger.isLoggable(Level.FINER)) { logger.entering(CLASSNAME, method, jobType);	}
//		
//		
//		/*if(jobType.equalsIgnoreCase(GridContainerConstants.CI_JOB)) {
//			
//			_workManager = (WorkManager) new InitialContext().lookup("java:comp/env/wm/CIWorkManager");
//		      
//			_workManager.schedule(job);
//		} else { // for batch and mixed jobs
//*/			try {
//			_workManager = (WorkManager) new InitialContext().lookup("java:comp/env/wm/BatchWorkManager");
//		      
//			_workManager.schedule(job);
//			} catch (NamingException e) {
//				// could not find a BatchWorkManager perhaps this is an old CI app
//				_workManager = (WorkManager) new InitialContext().lookup("java:comp/env/wm/CIWorkManager");
//			      
//				_workManager.schedule(job);
//			}
//		//}
//		
//		if(logger.isLoggable(Level.FINER)) { logger.exiting(CLASSNAME, method);	}
//
//	}
	


	public void executeTask(BatchWorkUnit work, Object config) {
		String method = "executeTask";
		if(logger.isLoggable(Level.FINER)) { logger.entering(sourceClass, method);	}
		
		_cachedThreadPool.execute(work);
		
		if(logger.isLoggable(Level.FINER)) { logger.exiting(sourceClass, method);	}

	}

	public void setPoolSize(int poolSize) {
		if(poolSize > 0)
			_poolSize = poolSize;
		
	}

    public ParallelTaskResult executeParallelTask(BatchWorkUnit work, Object config) {
        String method = "executeParallelTask";
        if(logger.isLoggable(Level.FINER)) { logger.entering(sourceClass, method);  }
        
        Future result = _cachedThreadPool.submit(work);
        ParallelTaskResult taskResult = new JSEResultAdapter(result);
        
        if(logger.isLoggable(Level.FINER)) { logger.exiting(sourceClass, method);   }
        
        return taskResult;
    }



	
}
