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
package com.ibm.jbatch.spi;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

public class ServiceRegistry {

	private final static String sourceClass = ServiceRegistry.class.getName();
	private final static Logger logger = Logger.getLogger(sourceClass);
	
	public interface ServiceImplClassNames {

		public static final String CONTAINER_ARTIFACT_FACTORY_CDI = "com.ibm.jbatch.container.services.impl.CDIBatchArtifactFactoryImpl";
		public static final String CONTAINER_ARTIFACT_FACTORY_WELD_SE = "com.ibm.jbatch.container.services.impl.WeldSEBatchArtifactFactoryImpl";		

		public static final String DELEGATING_JOBXML_LOADER_DEFAULT = "com.ibm.jbatch.container.services.impl.DelegatingJobXMLLoaderServiceImpl";
		public static final String DELEGATING_ARTIFACT_FACTORY_DEFAULT = "com.ibm.jbatch.container.services.impl.DelegatingBatchArtifactFactoryImpl";

		public static final String JOBXML_LOADER_DIRECTORY = "com.ibm.jbatch.container.services.impl.DirectoryJobXMLLoaderServiceImpl";

		public static final String BATCH_THREADPOOL_BOUNDED = "com.ibm.jbatch.container.services.impl.BoundedThreadPoolServiceImpl";
		public static final String BATCH_THREADPOOL_DEFAULT = "com.ibm.jbatch.container.services.impl.GrowableThreadPoolServiceImpl";
		public static final String BATCH_THREADPOOL_GROWABLE = "com.ibm.jbatch.container.services.impl.GrowableThreadPoolServiceImpl";
		public static final String BATCH_THREADPOOL_JNDI_DELEGATING = "com.ibm.jbatch.container.services.impl.JNDIDelegatingThreadPoolServiceImpl";
		public static final String BATCH_THREADPOOL_SPI_DELEGATING = "com.ibm.jbatch.container.services.impl.SPIDelegatingThreadPoolServiceImpl";

		public static final String TRANSACTION_DEFAULT = "com.ibm.jbatch.container.services.impl.BatchTransactionServiceImpl";
	}

	public interface ServicePropertyNames {
		
		public static final String BATCH_THREADPOOL_SERVICE = "BATCH_THREADPOOL_SERVICE";
		//treat as internal-only still - public static final String CALLBACK_SERVICE = "CALLBACK_SERVICE";
		/**
		 * This is only a valid plug point in the default @see  {@link ServiceImplClassNames.DELEGATING_ARTIFACT_FACTORY_DEFAULT}
		 */
		public static final String CONTAINER_ARTIFACT_FACTORY_SERVICE = "CONTAINER_ARTIFACT_FACTORY_SERVICE";
		public static final String DELEGATING_ARTIFACT_FACTORY_SERVICE = "DELEGATING_ARTIFACT_FACTORY_SERVICE";
		public static final String DELEGATING_JOBXML_LOADER_SERVICE = "DELEGATING_JOBXML_LOADER_SERVICE";
		/**
		 * Doesn't fit well with the rest.  This was originally configurable as a property in the properties file, so we want
		 * to treat this alongside the rest of the properties, even though if we were creating a brand new Java config interface
		 * then getting/setting this one would look a bit different.
		 */
		public static final String J2SE_MODE = "J2SE_MODE";
		//treat as internal-only still - public static final String JOB_STATUS_MANAGEMENT_SERVICE = "JOB_STATUS_MANAGEMENT_SERVICE";
		/**
		 * This is only a valid plug point in the default @see {@link ServiceImplClassNames.DELEGATING_JOBXML_LOADER_SERVICE}
		 */
		public static final String JOBXML_LOADER_SERVICE = "JOBXML_LOADER_SERVICE";
		//treat as internal-only still - public static final String PERSISTENCE_MANAGEMENT_SERVICE = "PERSISTENCE_MANAGEMENT_SERVICE";
		public static final String TRANSACTION_SERVICE = "TRANSACTION_SERVICE";
	}
	
	public interface ServiceInterfaceNames {
		public static final String BATCH_THREADPOOL = "com.ibm.jbatch.spi.services.IBatchThreadPoolService";
		public static final String CONTAINER_ARTIFACT_FACTORY = "com.ibm.jbatch.spi.services.IBatchArtifactFactory";
		public static final String JOBXML_LOADER = "com.ibm.jbatch.spi.services.IJobXMLLoaderService";
		public static final String TRANSACTION = "com.ibm.jbatch.spi.services.ITransactionManagementService";
	}
	
	/**
	 * Returns a Set of all ServiceInfo objects
	 * @return
	 */
	/* Not clear it adds value to expose this, especially with the odd
	 * J2SE_MODE in here.
	 */
	protected static Set<String> getAllServicePropertyNames() {
		HashSet<String> retVal = new HashSet<String>();
		retVal.add(ServicePropertyNames.BATCH_THREADPOOL_SERVICE);
		retVal.add(ServicePropertyNames.CONTAINER_ARTIFACT_FACTORY_SERVICE);
		retVal.add(ServicePropertyNames.DELEGATING_ARTIFACT_FACTORY_SERVICE);
		retVal.add(ServicePropertyNames.DELEGATING_JOBXML_LOADER_SERVICE);
		retVal.add(ServicePropertyNames.J2SE_MODE);
		retVal.add(ServicePropertyNames.JOBXML_LOADER_SERVICE);
		retVal.add(ServicePropertyNames.TRANSACTION_SERVICE);
		return retVal;
	}

	/**
	 * 
	 * @param servicePropertyName Should be a constant defined in @see {@link ServicePropertyNames}
	 * @return
	 */
	public static ServiceInfo getServiceInfo(String servicePropertyName) {
		final String s = servicePropertyName;
		if (s == null) {
			throw new IllegalArgumentException("getServiceInfo() called with <null> property name");
		} else if (s.equals(ServicePropertyNames.BATCH_THREADPOOL_SERVICE)) {
			return new ServiceInfo(
					ServicePropertyNames.BATCH_THREADPOOL_SERVICE, 
					ServiceImplClassNames.BATCH_THREADPOOL_DEFAULT, 
					ServiceInterfaceNames.BATCH_THREADPOOL);
		} else if (s.equals(ServicePropertyNames.CONTAINER_ARTIFACT_FACTORY_SERVICE)) {
			return new ServiceInfo(
					ServicePropertyNames.CONTAINER_ARTIFACT_FACTORY_SERVICE,
					ServiceImplClassNames.DELEGATING_ARTIFACT_FACTORY_DEFAULT,
					ServiceInterfaceNames.CONTAINER_ARTIFACT_FACTORY);
		} else if (s.equals(ServicePropertyNames.DELEGATING_ARTIFACT_FACTORY_SERVICE)) {
			return new ServiceInfo(
					ServicePropertyNames.DELEGATING_ARTIFACT_FACTORY_SERVICE,
					ServiceImplClassNames.DELEGATING_ARTIFACT_FACTORY_DEFAULT,
					ServiceInterfaceNames.CONTAINER_ARTIFACT_FACTORY);
		} else if (s.equals(ServicePropertyNames.DELEGATING_JOBXML_LOADER_SERVICE)) {
			return new ServiceInfo(
					ServicePropertyNames.DELEGATING_JOBXML_LOADER_SERVICE,
					ServiceImplClassNames.DELEGATING_JOBXML_LOADER_DEFAULT,
					ServiceInterfaceNames.JOBXML_LOADER);
		} else if (s.equals(ServicePropertyNames.J2SE_MODE)) {
			return new ServiceInfo(
					ServicePropertyNames.J2SE_MODE,
					"<not.a.true.java.implementation>",
					"<not.a.true.java.interface>");
		} else if (s.equals(ServicePropertyNames.JOBXML_LOADER_SERVICE)) {
			return new ServiceInfo(
					ServicePropertyNames.JOBXML_LOADER_SERVICE,
					ServiceImplClassNames.DELEGATING_JOBXML_LOADER_DEFAULT,
					ServiceInterfaceNames.JOBXML_LOADER);
		} else if (s.equals(ServicePropertyNames.TRANSACTION_SERVICE)) {
			return new ServiceInfo(
					ServicePropertyNames.TRANSACTION_SERVICE,
					ServiceImplClassNames.TRANSACTION_DEFAULT, 
					ServiceInterfaceNames.TRANSACTION);
		} else {
			throw new IllegalArgumentException("getServiceInfo() called with unrecognized property name: " + s);
		}
	}

	/**
	 * If a system property is found with key equal to:
	 * 	 A.B
	 * where A is the current classname and B is some constant
	 * String defined in ServicePropertyNames, then the value V
	 * of this system property will be included in a new property
	 * added to the return value Properties object.  The return 
	 * value will include a property with key B and value V.
	 * 
	 * E.g. a system property of 
	 * 
	 *  com.ibm.jbatch.spi.ServiceRegistry.TRANSACTION_SERVICE=>XXXX
	 *  
	 * will result in the return value including a property of:
	 * 
	 *  TRANSACTION_SERVICE=>XXXX
	 *  
	 * @return Properties object as defined above.
	 */
	public static Properties getSystemPropertyOverrides() {
		final String PROP_PREFIX = sourceClass;

		Properties props = new Properties();
		for (String propName : getAllServicePropertyNames()) {
			final String key = PROP_PREFIX + "." + propName;
			final String val = System.getProperty(key);
			if (val != null) {
				logger.fine("Found override property from system properties (key,value) = (" + propName + "," + val + ")");
				props.setProperty(propName, val);
			}
		}
		return props;
	}
}
