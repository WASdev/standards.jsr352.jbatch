/**
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
package com.ibm.jbatch.container.servicesmanager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.jbatch.spi.ServiceRegistry;


public class ServiceTypes implements ServiceRegistry.ServicePropertyNames, ServiceRegistry.ServiceImplClassNames {

	// Internal-only for forseeable future
	private static final String BATCH_KERNEL_SERVICE = "BATCH_KERNEL_SERVICE";
	
	// Internal-only for now, will expose when feasible and desirable
	public static final String CALLBACK_SERVICE = "CALLBACK_SERVICE";
	public static final String JOB_STATUS_MANAGEMENT_SERVICE = "JOB_STATUS_MANAGEMENT_SERVICE";
	public static final String PERSISTENCE_MANAGEMENT_SERVICE = "PERSISTENCE_MANAGEMENT_SERVICE";

	// Internal-only impls
	private static final String DEFAULT_BATCH_KERNEL_SERVICE = "com.ibm.jbatch.container.impl.BatchKernelImpl";
	private static final String DEFAULT_CALLBACK_SERVICE = "com.ibm.jbatch.container.callback.JobEndCallbackManagerImpl";
	private static final String DEFAULT_PERSISTENCE_MGR_CLASS = "com.ibm.jbatch.container.services.impl.JDBCPersistenceManagerImpl";
	private static final String DEFAULT_JOBSTATUS_MGR_SERVICE = "com.ibm.jbatch.container.services.impl.JobStatusManagerImpl";

	public enum Name {
		JAVA_EDITION_IS_SE_DUMMY_SERVICE, 
		TRANSACTION_SERVICE, 
		PERSISTENCE_MANAGEMENT_SERVICE, 
		JOB_STATUS_MANAGEMENT_SERVICE, 
		BATCH_THREADPOOL_SERVICE, 
		BATCH_KERNEL_SERVICE, 
		CALLBACK_SERVICE, 
		JOBXML_LOADER_SERVICE,                // Preferred
		DELEGATING_JOBXML_LOADER_SERVICE,      // Delegating wrapper
		CONTAINER_ARTIFACT_FACTORY_SERVICE,   // Preferred
		DELEGATING_ARTIFACT_FACTORY_SERVICE  // Delegating wrapper
	}
	
	// The purpose of the awkwardness of complexity of treating SE vs EE as a
	// "service" is to emphasize the fact that it's something an
	// integrator of the RI into a new environment would configure, rather than
	// a "batch admin".
	public static final String J2SE_MODE = "J2SE_MODE"; // Trying to preserve  this value since we already shared it.
	private static final String DEFAULT_JAVA_EDITION_IS_SE_DUMMY_SERVICE = "false";

	private static Map<String, Name> servicePropertyNames;
	static { 
		servicePropertyNames = new ConcurrentHashMap<String, Name>();
		servicePropertyNames.put(J2SE_MODE, Name.JAVA_EDITION_IS_SE_DUMMY_SERVICE);
		servicePropertyNames.put(TRANSACTION_SERVICE, Name.TRANSACTION_SERVICE);
		servicePropertyNames.put(PERSISTENCE_MANAGEMENT_SERVICE, Name.PERSISTENCE_MANAGEMENT_SERVICE);
		servicePropertyNames.put(JOB_STATUS_MANAGEMENT_SERVICE, Name.JOB_STATUS_MANAGEMENT_SERVICE);
		servicePropertyNames.put(BATCH_THREADPOOL_SERVICE, Name.BATCH_THREADPOOL_SERVICE);
		servicePropertyNames.put(BATCH_KERNEL_SERVICE, Name.BATCH_KERNEL_SERVICE);
		servicePropertyNames.put(CALLBACK_SERVICE, Name.CALLBACK_SERVICE);
		servicePropertyNames.put(JOBXML_LOADER_SERVICE, Name.JOBXML_LOADER_SERVICE);
		servicePropertyNames.put(DELEGATING_JOBXML_LOADER_SERVICE, Name.DELEGATING_JOBXML_LOADER_SERVICE);
		servicePropertyNames.put(CONTAINER_ARTIFACT_FACTORY_SERVICE, Name.CONTAINER_ARTIFACT_FACTORY_SERVICE);
		servicePropertyNames.put(DELEGATING_ARTIFACT_FACTORY_SERVICE, Name.DELEGATING_ARTIFACT_FACTORY_SERVICE);
	}
	
	public static Map<String, Name> getServicePropertyNames() {
		return servicePropertyNames;
	}
	
	// Use class names instead of Class objects to not drag in any dependencies);
	private static Map<Name, String> serviceImplClassNames;
	static { 
		serviceImplClassNames = new ConcurrentHashMap<Name, String>();
		serviceImplClassNames.put(Name.JAVA_EDITION_IS_SE_DUMMY_SERVICE, DEFAULT_JAVA_EDITION_IS_SE_DUMMY_SERVICE);
		serviceImplClassNames.put(Name.TRANSACTION_SERVICE, TRANSACTION_DEFAULT);
		serviceImplClassNames.put(Name.PERSISTENCE_MANAGEMENT_SERVICE, DEFAULT_PERSISTENCE_MGR_CLASS);
		serviceImplClassNames.put(Name.JOB_STATUS_MANAGEMENT_SERVICE, DEFAULT_JOBSTATUS_MGR_SERVICE);
		serviceImplClassNames.put(Name.BATCH_THREADPOOL_SERVICE, BATCH_THREADPOOL_GROWABLE);
		serviceImplClassNames.put(Name.BATCH_KERNEL_SERVICE, DEFAULT_BATCH_KERNEL_SERVICE);
		serviceImplClassNames.put(Name.CALLBACK_SERVICE, DEFAULT_CALLBACK_SERVICE);
		// Tricky but the implementation of the default "delegating" services depend on having the
		// delegate services configured with the same service impl class name. They are coded to avoid recursion
		// with this config.
		serviceImplClassNames.put(Name.JOBXML_LOADER_SERVICE, DELEGATING_JOBXML_LOADER_DEFAULT);
		serviceImplClassNames.put(Name.DELEGATING_JOBXML_LOADER_SERVICE, DELEGATING_JOBXML_LOADER_DEFAULT);
		serviceImplClassNames.put(Name.CONTAINER_ARTIFACT_FACTORY_SERVICE, DELEGATING_ARTIFACT_FACTORY_DEFAULT); 
		serviceImplClassNames.put(Name.DELEGATING_ARTIFACT_FACTORY_SERVICE, DELEGATING_ARTIFACT_FACTORY_DEFAULT);
	}
	
	public static Map<Name, String> getServiceImplClassNames() {
		return serviceImplClassNames;
	}
}
