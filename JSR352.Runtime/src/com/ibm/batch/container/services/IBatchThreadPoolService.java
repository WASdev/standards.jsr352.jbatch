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
package com.ibm.batch.container.services;

import com.ibm.batch.container.util.BatchWorkUnit;


/*
* The ExecutorService provides the capability to run tasks asynchronously.
* The instances of the Executor service are not managed by the ServicesManager cache
* each invocation of ServicesManager.getExecutorService() returns a new instance of this 
* service. The caller is responsible for shutting down the service when work is completed
* 
*/
public interface IBatchThreadPoolService extends IBatchServiceBase {

	/**
     * (Optional) Plugin implementors not using a thread pool can ignore this method
	 * Sets the thread pool size
	 * @param i
	 */

	public void setPoolSize(int i);
	
	
	/**
	 * (Required) Runs the given task. A task is usually short lived
	 * @param work The task to execute
     * @param config Optional configuration to customize the execution.
     *  The Container always passes a null value. 
     *  Typically used when other user plugins wish to use the ExecutorService to execute tasks.
	 */

	public void executeTask(BatchWorkUnit work, Object config);
	
	/**
	 * (Required) Runs the given Job object. A job is usually long running.
	 * @param job
	 */

	public void executeJob(BatchWorkUnit job);
	
	
	   /**
     * Runs the given task. A task is usually short lived
     * @param work The task to execute
     * @param config Optional configuration to customize the execution.
     *  The Container always passes a null value. 
     *  Typically used when other user plugins wish to use the ExecutorService to execute tasks.
     */

    public ParallelTaskResult executeParallelTask(BatchWorkUnit work, Object config);
}
