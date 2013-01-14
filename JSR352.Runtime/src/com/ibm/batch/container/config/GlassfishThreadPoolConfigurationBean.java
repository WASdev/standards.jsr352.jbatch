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
package com.ibm.batch.container.config;

/*

1. max-queue-size:  The maximum number of threads in the queue. A value of indicates that there is no limit to the queue size.

2. max-thread-pool-szie:  The maximum number of threads in the thread pool

3. min-thread-pool-size: The minimum number of threads in the thread pool

4. Idle-thread-timeout: The maximum amount of time that a thread can remain idle in the pool. After this time expires, the thread is removed from the pool.

 */
public class GlassfishThreadPoolConfigurationBean {

	public int getMaxQueueSize() {
		return maxQueueSize;
	}
	public void setMaxQueueSize(int maxQueueSize) {
		this.maxQueueSize = maxQueueSize;
	}
	public int getMinThreadPoolSize() {
		return minThreadPoolSize;
	}
	public void setMinThreadPoolSize(int minThreadPoolSize) {
		this.minThreadPoolSize = minThreadPoolSize;
	}
	public int getMaxThreadPoolSize() {
		return maxThreadPoolSize;
	}
	public void setMaxThreadPoolSize(int maxThreadPoolSize) {
		this.maxThreadPoolSize = maxThreadPoolSize;
	}
	public int getIdleThreadTimeout() {
		return idleThreadTimeout;
	}
	public void setIdleThreadTimeout(int idleThreadTimeout) {
		this.idleThreadTimeout = idleThreadTimeout;
	}
	private int maxQueueSize;
	private int minThreadPoolSize;
	private int maxThreadPoolSize;
	private int idleThreadTimeout;
}
