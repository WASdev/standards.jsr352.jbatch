/*
 * 
 * Copyright 2012,2013 International Business Machines Corp.
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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.jbatch.container.callback.IJobEndCallbackService;
import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.exception.PersistenceException;
import com.ibm.jbatch.container.impl.BatchConfigImpl;
import com.ibm.jbatch.container.services.IBatchKernelService;
import com.ibm.jbatch.container.services.IJobStatusManagerService;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.servicesmanager.ServiceTypes.Name;
import com.ibm.jbatch.container.util.BatchContainerConstants;
import com.ibm.jbatch.spi.BatchSPIManager;
import com.ibm.jbatch.spi.BatchSPIManager.PlatformMode;
import com.ibm.jbatch.spi.DatabaseConfigurationBean;
import com.ibm.jbatch.spi.ServiceRegistry;
import com.ibm.jbatch.spi.services.IBatchArtifactFactory;
import com.ibm.jbatch.spi.services.IBatchServiceBase;
import com.ibm.jbatch.spi.services.IBatchThreadPoolService;
import com.ibm.jbatch.spi.services.IJobXMLLoaderService;
import com.ibm.jbatch.spi.services.ITransactionManagementService;


/**
 * Note a call to any of the getter methods besides getInstance() will perform the initialization routine and thereby
 * 'harden' the config.
 */
public class ServicesManagerImpl implements BatchContainerConstants, ServicesManager {

	private final static String sourceClass = ServicesManagerImpl.class.getName();
	private final static Logger logger = Logger.getLogger(sourceClass);
	
	private ServicesManagerImpl() { } 
	
	// Lazily-loaded singleton.
	private static class ServicesManagerImplHolder {
		private static final ServicesManagerImpl INSTANCE = new ServicesManagerImpl();
	}

	public static ServicesManager getInstance() {
		return ServicesManagerImplHolder.INSTANCE;
	}
	
	// Declared 'volatile' to allow use in double-checked locking.  This 'isInited'
	// refers to whether the configuration has been hardened and possibly the
	// first service impl loaded, not whether the instance has merely been instantiated.
	private final byte[] isInitedLock = new byte[0];
	private volatile Boolean isInited = Boolean.FALSE;

	private DatabaseConfigurationBean databaseConfigBean = null;
	private BatchConfigImpl batchConfigImpl;
	private Properties batchContainerProps = null;

	private	Map<Name, String> serviceImplClassNames = ServiceTypes.getServiceImplClassNames();
	private Map<String, Name> propertyNameTable = ServiceTypes.getServicePropertyNames();

	// Registry of all current services
	private final ConcurrentHashMap<Name, IBatchServiceBase> serviceRegistry = new ConcurrentHashMap<Name, IBatchServiceBase>();
	private PlatformMode platformMode = null;
	
	/**
	 * Init doesn't actually load the service impls, which are still loaded lazily.   What it does is it
	 * hardens the config.  This is necessary since the batch runtime by and large is not dynamically
	 * configurable, (e.g. via MBeans).  Things like the database config used by the batch runtime's
	 * persistent store are hardened then, as are the names of the service impls to use.
	 */
	private void initIfNecessary() {
		if (logger.isLoggable(Level.FINER)) {
			logger.config("In initIfNecessary().");
		}
		// Use double-checked locking with volatile.
		if (!isInited) {
			synchronized (isInitedLock) {
				if (!isInited) {
					logger.config("--- Initializing ServicesManagerImpl ---");
					batchConfigImpl = new BatchConfigImpl();

					// Read config
					readConfigFromPropertiesFiles();
					readConfigFromSPI();
					readConfigFromSystemProperties();

					// Set config in memory
					initBatchConfigImpl();
					initServiceImplOverrides();
					initDatabaseConfig();
					initPlatformSEorEE();

					isInited = Boolean.TRUE;
					
					logger.config("--- Completed initialization of ServicesManagerImpl ---");
				}
			}
		}

		logger.config("Exiting initIfNecessary()");
	}

	private void readConfigFromPropertiesFiles() {

		Properties serviceIntegratorProps = new Properties();
		InputStream batchServicesListInputStream = this.getClass()
				.getResourceAsStream("/META-INF/services/" + BATCH_INTEGRATOR_CONFIG_FILE);

		if (batchServicesListInputStream != null) {
			try {
				logger.config("Batch Integrator Config File exists! loading it..");
				serviceIntegratorProps.load(batchServicesListInputStream);
				batchServicesListInputStream.close();
			} catch (IOException e) {
				logger.config("Error loading " + "/META-INF/services/" + BATCH_INTEGRATOR_CONFIG_FILE + " IOException=" + e.toString());
			} catch (Exception e) {
				logger.config("Error loading " + "/META-INF/services/" + BATCH_INTEGRATOR_CONFIG_FILE + " Exception=" + e.toString());
			}
		} else {
			logger.config("Could not find batch integrator config file: " + "/META-INF/services/" + BATCH_INTEGRATOR_CONFIG_FILE);
		}

		// See if any do not map to service impls.

		Set<String> removeThese = new HashSet<String>();
		for (Object key : serviceIntegratorProps.keySet()) {
			String keyStr = (String) key;
			if (!propertyNameTable.containsKey(keyStr)) {
				logger.warning("Found property named: " + keyStr
						+ " with value: " + serviceIntegratorProps.get(keyStr)
						+ " in " + BATCH_INTEGRATOR_CONFIG_FILE + " , but did not find a corresponding service type "
						+ "in the internal table of service types.\n Ignoring this property then.   Maybe this should have been set in batch-config.properties instead.");
				removeThese.add(keyStr);
			}
		}
		for (String s : removeThese) {
			serviceIntegratorProps.remove(s);
		}

		Properties adminProps = new Properties();
		InputStream batchAdminConfigListInputStream = this.getClass().getResourceAsStream("/META-INF/services/" + BATCH_ADMIN_CONFIG_FILE);

		if (batchServicesListInputStream != null) {
			try {
				logger.config("Batch Admin Config File exists! loading it..");
				adminProps.load(batchAdminConfigListInputStream);
				batchAdminConfigListInputStream.close();
			} catch (IOException e) {
				logger.config("Error loading " + "/META-INF/services/" + BATCH_ADMIN_CONFIG_FILE + " IOException=" + e.toString());
			} catch (Exception e) {
				logger.config("Error loading " + "/META-INF/services/" + BATCH_ADMIN_CONFIG_FILE + " Exception=" + e.toString());
			}
		} else {
			logger.config("Could not find batch admin config file: " + "/META-INF/services/" + BATCH_ADMIN_CONFIG_FILE);
		}

		// See if any DO map to service impls, which would be a mistake
		Set<String> removeTheseToo = new HashSet<String>();
		for (Object key : adminProps.keySet()) {
			String keyStr = (String) key;
			if (propertyNameTable.containsKey(keyStr)) {
				logger.warning("Found property named: " + keyStr + " with value: " + adminProps.get(keyStr) + " in "
						+ BATCH_ADMIN_CONFIG_FILE + " , but this is a batch runtime service configuration.\n"
						+ "Ignoring this property then, since this should have been set in batch-services.properties instead.");
				removeThese.add(keyStr);
			}
		}
		for (String s : removeTheseToo) {
			adminProps.remove(s);
		}

		// Merge the two into 'batchContainerProps'
		batchContainerProps = new Properties();
		batchContainerProps.putAll(adminProps);
		batchContainerProps.putAll(serviceIntegratorProps);

	}
	
	private void readConfigFromSPI() {
		//Merge in overrides from the SPI so the container can change properties
		batchContainerProps.putAll(BatchSPIManager.getInstance().getBatchContainerOverrideProperties());

		// Don't cache in the 'platformMode' field variable just yet, wait until config is complete for consistency.
		PlatformMode mode = BatchSPIManager.getInstance().getPlatformMode();
		if (mode != null) {
			if (mode.equals(PlatformMode.EE)) {
				logger.config("SPI configured platform selection of EE");
				batchContainerProps.setProperty(ServiceTypes.J2SE_MODE, "false");
			} else if (mode.equals(PlatformMode.SE)) {
				logger.config("SPI configured platform selection of SE");
				batchContainerProps.setProperty(ServiceTypes.J2SE_MODE, "true");
			}
		}
	}
	
	private void readConfigFromSystemProperties() {
		batchContainerProps.putAll(ServiceRegistry.getSystemPropertyOverrides());
	}

	private void initBatchConfigImpl() {
		logger.fine("Dumping contents of batchContainerProps after reading properties files and calling SPI.");
		for (Object key : batchContainerProps.keySet()) {
			logger.config("key = " + key);
			logger.config("value = " + batchContainerProps.get(key));
		}
		
		// Set this on the config. 
		// 
		// WARNING:  This sets us up for collisions since this is just a single holder of properties
		// potentially used by any service impl.
		batchConfigImpl.setConfigProperties(batchContainerProps);
	}

	/**
	 * The method name reflects the fact that we have default service impl classnames baked into
	 * the runtime that will be used in the absence of any config property.   The config is only
	 * necessary to 'override' one of these default impls.
	 */
	private void initServiceImplOverrides() {
		
		// For each property we care about (i.e that defines one of our service impls)
		for (String propKey : propertyNameTable.keySet()) {
			// If the property is defined
			String value = batchContainerProps.getProperty(propKey);
			if (value != null) {
				// Get the corresponding serviceType enum and store the value of
				// the key/value property pair in the table where we store the service impl classnames.
				Name serviceType = propertyNameTable.get(propKey);
				String defaultServiceImplClassName = serviceImplClassNames.get(serviceType); // For logging.
				serviceImplClassNames.put(serviceType, value.trim());
				logger.config("Overriding serviceType: " + serviceType + ", replacing default impl classname: " + 
							defaultServiceImplClassName + " with override impl class name: " + value.trim());
			}
		}
	}

	private void initDatabaseConfig() {
		if (databaseConfigBean == null) { 
			logger.config("First try to load 'suggested config' from BatchSPIManager");
			databaseConfigBean = BatchSPIManager.getInstance().getFinalDatabaseConfiguration();
			if (databaseConfigBean == null) { 
				logger.config("Loading database config from configuration properties file.");
				// Initialize database-related properties
				databaseConfigBean = new DatabaseConfigurationBean();
				databaseConfigBean.setJndiName(batchContainerProps.getProperty(JNDI_NAME, DEFAULT_JDBC_JNDI_NAME));
				databaseConfigBean.setJdbcDriver(batchContainerProps.getProperty(JDBC_DRIVER, DEFAULT_JDBC_DRIVER));
				databaseConfigBean.setJdbcUrl(batchContainerProps.getProperty(JDBC_URL, DEFAULT_JDBC_URL));
				databaseConfigBean.setDbUser(batchContainerProps.getProperty(DB_USER));
				databaseConfigBean.setDbPassword(batchContainerProps.getProperty(DB_PASSWORD));
				databaseConfigBean.setSchema(batchContainerProps.getProperty(DB_SCHEMA, DEFAULT_DB_SCHEMA));
			}
		}  else {
			// Currently we do not expected this path to be used by Glassfish
			logger.config("Database config has been set directly from SPI, do NOT load from properties file.");
		}
		// In either case, set this bean on the main config bean
		batchConfigImpl.setDatabaseConfigurationBean(databaseConfigBean);
	}


	// Push hardened config value onto batchConfigImpl and cache the value in a field.
	private void initPlatformSEorEE() {
		String seMode = serviceImplClassNames.get(Name.JAVA_EDITION_IS_SE_DUMMY_SERVICE);
		if (seMode.equalsIgnoreCase("true")) {
			platformMode = PlatformMode.SE;
			batchConfigImpl.setJ2seMode(true);
		} else {
			platformMode = PlatformMode.EE;
			batchConfigImpl.setJ2seMode(false);
		}
	}

	// Look up registry and return requested service if exist
	// If not exist, create a new one, add to registry and return that one
	private IBatchServiceBase getService(Name serviceType) throws BatchContainerServiceException {
		String sourceMethod = "getService";
		logger.entering(sourceClass, sourceMethod + ", serviceType=" + serviceType);

		initIfNecessary();

		IBatchServiceBase service = new ServiceLoader(serviceType).getService();

		logger.exiting(sourceClass, sourceMethod);
		return service;
	}

	/*
	 * 	public enum Name {
		JAVA_EDITION_IS_SE_DUMMY_SERVICE, 
		TRANSACTION_SERVICE, 
		PERSISTENCE_MANAGEMENT_SERVICE, 
		JOB_STATUS_MANAGEMENT_SERVICE, 
		BATCH_THREADPOOL_SERVICE, 
		BATCH_KERNEL_SERVICE, 
		JOB_ID_MANAGEMENT_SERVICE, 
		CALLBACK_SERVICE, 
		JOBXML_LOADER_SERVICE,                // Preferred
		DELEGATING_JOBXML_LOADER_SERVICE,      // Delegating wrapper
		CONTAINER_ARTIFACT_FACTORY_SERVICE,   // Preferred
		DELEGATING_ARTIFACT_FACTORY_SERVICE  // Delegating wrapper
	 */

	@Override
	public ITransactionManagementService getTransactionManagementService() {
		return (ITransactionManagementService)getService(Name.TRANSACTION_SERVICE);
	}

	@Override
	public IPersistenceManagerService getPersistenceManagerService() {
		return (IPersistenceManagerService)getService(Name.PERSISTENCE_MANAGEMENT_SERVICE);
	}

	@Override
	public IJobStatusManagerService getJobStatusManagerService() {
		return (IJobStatusManagerService)getService(Name.JOB_STATUS_MANAGEMENT_SERVICE);
	}

	@Override
	public IBatchThreadPoolService getThreadPoolService() {
		return (IBatchThreadPoolService)getService(Name.BATCH_THREADPOOL_SERVICE);
	}

	@Override
	public IBatchKernelService getBatchKernelService() {
		return (IBatchKernelService)getService(Name.BATCH_KERNEL_SERVICE);
	}

	@Override
	public IJobEndCallbackService getJobCallbackService() {
		return (IJobEndCallbackService)getService(Name.CALLBACK_SERVICE);
	}

	@Override
	public IJobXMLLoaderService getPreferredJobXMLLoaderService() {
		return (IJobXMLLoaderService)getService(Name.JOBXML_LOADER_SERVICE);
	}

	@Override
	public IJobXMLLoaderService getDelegatingJobXMLLoaderService() {
		return (IJobXMLLoaderService)getService(Name.DELEGATING_JOBXML_LOADER_SERVICE);
	}

	@Override
	public IBatchArtifactFactory getPreferredArtifactFactory() {
		return (IBatchArtifactFactory)getService(Name.CONTAINER_ARTIFACT_FACTORY_SERVICE);
	}
	
	@Override
	public IBatchArtifactFactory getDelegatingArtifactFactory() {
		return (IBatchArtifactFactory)getService(Name.DELEGATING_ARTIFACT_FACTORY_SERVICE);
	}

	/**
	 * Note this will always return a non-null platform mode, i.e. defaulting is
	 * taken care of.
	 * 
	 * @return mode signifying whether we are executing on an SE or EE platform.  
	 */
	@Override
	public PlatformMode getPlatformMode() {
		initIfNecessary();
		return platformMode;
	}
	
	private class ServiceLoader {
		
		volatile IBatchServiceBase service = null;
		private Name serviceType = null;

		private ServiceLoader(Name name) {
			this.serviceType = name;
		}

		private IBatchServiceBase getService() {
			service = serviceRegistry.get(serviceType);
			if (service == null) {
				// Probably don't want to be loading two on two different threads so lock the whole table.
				synchronized (serviceRegistry) {
					if (service == null) {
						service = _loadServiceHelper(serviceType);
						service.init(batchConfigImpl);
						serviceRegistry.putIfAbsent(serviceType, service);
					}
				}
			}
			return service;
		}
		/**
		 * Try to load the service impl given by the className.
		 */
		private IBatchServiceBase _loadServiceHelper(Name serviceType) {
			IBatchServiceBase service = null;

			String className = serviceImplClassNames.get(serviceType);
			try {
				if (className != null) {
					service = _loadService(className);
				}
			} catch (Throwable t) {
				logger.log(Level.SEVERE, "Could not instantiate service: " + className + " due to exception:" + t);
				throw new RuntimeException("Could not instantiate service " + className, t);
			}

			if (service == null) {
				throw new RuntimeException("Instantiate of service=: " + className + " for serviceType: " + serviceType + " returned null. Aborting...");
			}

			return service;
		}

		private IBatchServiceBase _loadService(String className) throws Exception {

			IBatchServiceBase service = null;
			Class cls;
			try {
				cls = Class.forName(className);
			} catch (ClassNotFoundException cnfe) {
				cls = Thread.currentThread().getContextClassLoader().loadClass(className);
			}

			if (cls != null) {
				Constructor ctor = cls.getConstructor();
				if (ctor != null) {
					service = (IBatchServiceBase) ctor.newInstance();
				} else {
					throw new Exception("Service class " + className + " should  have a default constructor defined");
				}
			} else {
				throw new Exception("Exception loading Service class " + className + " make sure it exists");
			}

			return service;
		}
	}
}

