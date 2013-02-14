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
package com.ibm.batch.container.services;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.batch.container.config.DatabaseConfigurationBean;
import com.ibm.batch.container.config.IBatchConfig;
import com.ibm.batch.container.config.GlassfishThreadPoolConfigurationBean;
import com.ibm.batch.container.exception.BatchContainerServiceException;
import com.ibm.batch.container.impl.BatchConfigImpl;
import com.ibm.batch.container.services.impl.GlassfishThreadPoolServiceImpl;
import com.ibm.batch.container.util.BatchContainerConstants;

public class ServicesManager implements BatchContainerConstants {

    private final static String sourceClass = ServicesManager.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);

    private boolean isInited = false;
    private Properties batchContainerProps = null;
    private static ServicesManager servicesManager;
    private IBatchConfig batchRuntimeConfig = null;

    private static final String DEFAULT_PERSISTENCE_MGR_CLASS = "com.ibm.batch.container.services.impl.JDBCPersistenceManagerImpl";
    private static final String DEFAULT_BATCH_THREADPOOL_SERVICE = "com.ibm.batch.container.services.impl.JSEThreadPoolServiceImpl";
    private static final String DEFAULT_CONTAINER_ARTIFACT_FACTORY_SERVICE = "com.ibm.batch.container.services.impl.DelegatingBatchArtifactFactoryImpl";
    private static final String DEFAULT_BATCH_KERNEL_SERVICE = "com.ibm.batch.container.impl.BatchKernelImpl";
    private static final String DEFAULT_JOBSTATUS_MGR_SERVICE = "com.ibm.batch.container.services.impl.JobStatusManagerImpl";
    private static final String DEFAULT_JOBID_MGR_SERVICE = "com.ibm.batch.container.services.impl.JobIdManagerImpl";
    private static final String DEFAULT_CALLBACK_SERVICE = "com.ibm.batch.container.tck.bridge.JobEndCallbackManagerImpl";
    private static final String DEFAULT_TRANSACTION_SERVICE = "com.ibm.batch.container.services.impl.BatchTransactionServiceImpl";
    private static final String DEFAULT_JOBXML_LOADER_SERVICE = "com.ibm.batch.container.services.impl.DelegatingJobXMLLoaderServiceImpl";

    // The purpose of the awkwardness of complexity of treating SE vs EE as a
    // "service" is to emphasize the fact that it's something an
    // integrator of the RI into a new environment would configure, rather than
    // a "batch admin".
    private static final String DEFAULT_JAVA_EDITION_IS_SE_DUMMY_SERVICE = "false"; // is-SE
                                                                                    // defaulting
                                                                                    // to
                                                                                    // "false"
                                                                                    // means
                                                                                    // default
                                                                                    // is
                                                                                    // EE
                                                                                    // !!

    public static enum ServiceType {
		TRANSACTION_SERVICE,
		PERSISTENCE_MANAGEMENT_SERVICE,
		JOB_STATUS_MANAGEMENT_SERVICE,
		BATCH_THREADPOOL_SERVICE,
		CONTAINER_ARTIFACT_FACTORY_SERVICE,
		DELEGATING_ARTIFACT_FACTORY_SERVICE,
		BATCH_KERNEL_SERVICE,
		JOB_ID_MANAGEMENT_SERVICE,
		CALLBACK_SERVICE,
		JAVA_EDITION_IS_SE_DUMMY_SERVICE,
		DELEGATING_JOBXML_LOADER_SERVICE,
		JOBXML_LOADER_SERVICE}; 

    // Registry of all current services
    private ConcurrentHashMap<ServiceType, IBatchServiceBase> serviceRegistry = new ConcurrentHashMap<ServiceType, IBatchServiceBase>();

    private Map<ServiceType, String> serviceImplClassNames = new ConcurrentHashMap<ServiceType, String>();

    private Map<String, ServiceType> propertyNameTable = new ConcurrentHashMap<String, ServiceType>();

    public static final String PERSISTENCE_MANAGEMENT_SERVICE = "PERSISTENCE_MANAGEMENT_SERVICE";
    public static final String JOB_STATUS_MANAGEMENT_SERVICE = "JOB_STATUS_MANAGEMENT_SERVICE";
    public static final String BATCH_THREADPOOL_SERVICE = "BATCH_THREADPOOL_SERVICE";
    public static final String CONFIGURATION_SERVICE = "CONFIGURATION_SERVICE";
    public static final String CONTAINER_LOGGER_CONFIG_SERVICE = "CONTAINER_LOGGER_CONFIG_SERVICE";
    public static final String CONTAINER_ARTIFACT_FACTORY_SERVICE = "CONTAINER_ARTIFACT_FACTORY_SERVICE";
    public static final String DELEGATING_ARTIFACT_FACTORY_SERVICE = "DELEGATING_ARTIFACT_FACTORY_SERVICE";
    public static final String BATCH_KERNEL_SERVICE = "BATCH_KERNEL_SERVICE";
    public static final String JOB_ID_MANAGEMENT_SERVICE = "JOB_ID_MANAGEMENT_SERVICE";
    public static final String CALLBACK_SERVICE = "CALLBACK_SERVICE";
    public static final String TRANSACTION_SERVICE = "TRANSACTION_SERVICE";
    public static final String JOBXML_LOADER_SERVICE = "JOBXML_LOADER_SERVICE";
    public static final String DELEGATING_JOBXML_LOADER_SERVICE = "DELEGATING_JOBXML_LOADER_SERVICE";
    public static final String J2SE_MODE = "J2SE_MODE"; // Trying to preserve
                                                        // this value since we
                                                        // already shared it.

    private ServicesManager() {
        init();
    }

    private void populateServicePropertyNameTable() {
        for (ServiceType s : ServiceType.values()) {
            if (s.equals(ServiceType.TRANSACTION_SERVICE)) {
                propertyNameTable.put(TRANSACTION_SERVICE, s);
            } else if (s.equals(ServiceType.PERSISTENCE_MANAGEMENT_SERVICE)) {
                propertyNameTable.put(PERSISTENCE_MANAGEMENT_SERVICE, s);
            } else if (s.equals(ServiceType.JOB_STATUS_MANAGEMENT_SERVICE)) {
                propertyNameTable.put(JOB_STATUS_MANAGEMENT_SERVICE, s);
            } else if (s.equals(ServiceType.BATCH_THREADPOOL_SERVICE)) {
                propertyNameTable.put(BATCH_THREADPOOL_SERVICE, s);
            } else if (s.equals(ServiceType.CONTAINER_ARTIFACT_FACTORY_SERVICE)) {
                propertyNameTable.put(CONTAINER_ARTIFACT_FACTORY_SERVICE, s);
            } else if (s.equals(ServiceType.DELEGATING_ARTIFACT_FACTORY_SERVICE)) {
                propertyNameTable.put(DELEGATING_ARTIFACT_FACTORY_SERVICE, s);
            } else if (s.equals(ServiceType.BATCH_KERNEL_SERVICE)) {
                propertyNameTable.put(BATCH_KERNEL_SERVICE, s);
            } else if (s.equals(ServiceType.JOB_ID_MANAGEMENT_SERVICE)) {
                propertyNameTable.put(JOB_ID_MANAGEMENT_SERVICE, s);
            } else if (s.equals(ServiceType.CALLBACK_SERVICE)) {
                propertyNameTable.put(CALLBACK_SERVICE, s);
            } else if (s.equals(ServiceType.JOBXML_LOADER_SERVICE)) {
                propertyNameTable.put(JOBXML_LOADER_SERVICE, s);
            } else if (s.equals(ServiceType.DELEGATING_JOBXML_LOADER_SERVICE)) {
                propertyNameTable.put(DELEGATING_JOBXML_LOADER_SERVICE, s);
            } else if (s.equals(ServiceType.JAVA_EDITION_IS_SE_DUMMY_SERVICE)) {
                propertyNameTable.put(J2SE_MODE, s);
            } else {
                if (logger.isLoggable(Level.WARNING)) {
                    logger.warning("Configuration lacks a definition of property key for service type: " + s);
                }
            }
        }
    }

    /*
     * Doing it this way we have a check in case we forget to update one, though
     * not a check if one becomes obsolete and we no longer care about it.
     */
    private void registerDefaultServiceImpls() {
        for (ServiceType s : ServiceType.values()) {
            if (s.equals(ServiceType.TRANSACTION_SERVICE)) {
                serviceImplClassNames.put(s, DEFAULT_TRANSACTION_SERVICE);
            } else if (s.equals(ServiceType.PERSISTENCE_MANAGEMENT_SERVICE)) {
                serviceImplClassNames.put(s, DEFAULT_PERSISTENCE_MGR_CLASS);
            } else if (s.equals(ServiceType.JOB_STATUS_MANAGEMENT_SERVICE)) {
                serviceImplClassNames.put(s, DEFAULT_JOBSTATUS_MGR_SERVICE);
            } else if (s.equals(ServiceType.BATCH_THREADPOOL_SERVICE)) {
                serviceImplClassNames.put(s, DEFAULT_BATCH_THREADPOOL_SERVICE);
            } else if (s.equals(ServiceType.CONTAINER_ARTIFACT_FACTORY_SERVICE)) {
                serviceImplClassNames.put(s, DEFAULT_CONTAINER_ARTIFACT_FACTORY_SERVICE);
            } else if (s.equals(ServiceType.DELEGATING_ARTIFACT_FACTORY_SERVICE)) {
                serviceImplClassNames.put(s, DEFAULT_CONTAINER_ARTIFACT_FACTORY_SERVICE);
            } else if (s.equals(ServiceType.BATCH_KERNEL_SERVICE)) {
                serviceImplClassNames.put(s, DEFAULT_BATCH_KERNEL_SERVICE);
            } else if (s.equals(ServiceType.JOB_ID_MANAGEMENT_SERVICE)) {
                serviceImplClassNames.put(s, DEFAULT_JOBID_MGR_SERVICE);
            } else if (s.equals(ServiceType.CALLBACK_SERVICE)) {
                serviceImplClassNames.put(s, DEFAULT_CALLBACK_SERVICE);
            } else if (s.equals(ServiceType.JOBXML_LOADER_SERVICE)) {
                serviceImplClassNames.put(s, DEFAULT_JOBXML_LOADER_SERVICE);
            } else if (s.equals(ServiceType.DELEGATING_JOBXML_LOADER_SERVICE)) {
                serviceImplClassNames.put(s, DEFAULT_JOBXML_LOADER_SERVICE);                
            } else if (s.equals(ServiceType.JAVA_EDITION_IS_SE_DUMMY_SERVICE)) {
                serviceImplClassNames.put(s, DEFAULT_JAVA_EDITION_IS_SE_DUMMY_SERVICE);
            } else {
                if (logger.isLoggable(Level.WARNING)) {
                    logger.warning("Configuration lacks a default value for service type: " + s);
                }
            }
        }
    }

    private void init() {
        String sourceMethod = "init";
        if (logger.isLoggable(Level.FINE))
            logger.entering(sourceClass, sourceMethod);

        if (!isInited) {

            batchRuntimeConfig = new BatchConfigImpl();

            registerDefaultServiceImpls();

            populateServicePropertyNameTable();

            loadBatchConfigPropertiesFiles();

            loadServiceImplOverrides();

            loadDatabaseConfig();

            loadGlassfishThreadPoolConfig();

            loadOtherConfig();

            isInited = true;
        }

        if (logger.isLoggable(Level.FINE))
            logger.exiting(sourceClass, sourceMethod);
    }

    private void loadBatchConfigPropertiesFiles() {

        Properties serviceIntegratorProps = new Properties();
        InputStream batchServicesListInputStream = this.getClass()
                .getResourceAsStream("/META-INF/services/" + BATCH_INTEGRATOR_CONFIG_FILE);

        if (batchServicesListInputStream != null) {
            try {
                logger.fine("Batch Integrator Config File exists! loading it..");
                serviceIntegratorProps.load(batchServicesListInputStream);
                batchServicesListInputStream.close();
            } catch (IOException e) {
                logger.info("Error loading " + "/META-INF/services/" + BATCH_INTEGRATOR_CONFIG_FILE + " IOException=" + e.toString());
            } catch (Exception e) {
                logger.info("Error loading " + "/META-INF/services/" + BATCH_INTEGRATOR_CONFIG_FILE + " Exception=" + e.toString());
            }
        } else {
            logger.info("Could not find batch integrator config file: " + "/META-INF/services/" + BATCH_INTEGRATOR_CONFIG_FILE);
        }

        // See if any do not map to service impls.

        Set<String> removeThese = new HashSet<String>();
        for (Object key : serviceIntegratorProps.keySet()) {
            String keyStr = (String) key;
            if (!propertyNameTable.containsKey(keyStr)) {
                logger
                        .warning("Found property named: "
                                + keyStr
                                + " with value: "
                                + serviceIntegratorProps.get(keyStr)
                                + " in "
                                + BATCH_INTEGRATOR_CONFIG_FILE
                                + " , but did not find a corresponding service type "
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
                logger.fine("Batch Admin Config File exists! loading it..");
                adminProps.load(batchAdminConfigListInputStream);
                batchAdminConfigListInputStream.close();
            } catch (IOException e) {
                logger.info("Error loading " + "/META-INF/services/" + BATCH_ADMIN_CONFIG_FILE + " IOException=" + e.toString());
            } catch (Exception e) {
                logger.info("Error loading " + "/META-INF/services/" + BATCH_ADMIN_CONFIG_FILE + " Exception=" + e.toString());
            }
        } else {
            logger.info("Could not find batch admin config file: " + "/META-INF/services/" + BATCH_ADMIN_CONFIG_FILE);
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

        // Trace
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Dumping contents of batchContainerProps after reading properties files.");
            for (Object key : batchContainerProps.keySet()) {
                logger.fine("key = " + key);
                logger.fine("value = " + batchContainerProps.get(key));
            }
        }
    }

    private void loadServiceImplOverrides() {
        // For each property we care about (i.e that defines one of our service
        // impls)
        for (String propKey : propertyNameTable.keySet()) {
            // If the property is defined
            String value = batchContainerProps.getProperty(propKey);
            if (value != null) {
                // Get the corresponding serviceType enum and store the value of
                // the key/value property pair
                // in the table where we store the service impl classnames.
                ServiceType serviceType = propertyNameTable.get(propKey);
                serviceImplClassNames.put(serviceType, value.trim());

                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Overriding serviceType: " + serviceType + " with impl class name: " + value.trim());
                }
            }
        }
    }

    private void loadDatabaseConfig() {
        // Initialize database-related properties
        DatabaseConfigurationBean databaseConfigBean = new DatabaseConfigurationBean();
        batchRuntimeConfig.setDatabaseConfigurationBean(databaseConfigBean);
        databaseConfigBean.setJndiName(batchContainerProps.getProperty(JNDI_NAME, DEFAULT_JDBC_JNDI_NAME));
        databaseConfigBean.setJdbcDriver(batchContainerProps.getProperty(JDBC_DRIVER, DEFAULT_JDBC_DRIVER));
        databaseConfigBean.setJdbcUrl(batchContainerProps.getProperty(JDBC_URL, DEFAULT_JDBC_URL));
        databaseConfigBean.setDbUser(batchContainerProps.getProperty(DB_USER));
        databaseConfigBean.setDbPassword(batchContainerProps.getProperty(DB_PASSWORD));
        databaseConfigBean.setSchema(batchContainerProps.getProperty(DB_SCHEMA, DEFAULT_DB_SCHEMA));
    }

    private void loadGlassfishThreadPoolConfig() {
        // Initialize Glassfish threadpool-related properties
        GlassfishThreadPoolConfigurationBean threadPoolConfigBean = new GlassfishThreadPoolConfigurationBean();
        batchRuntimeConfig.setGlassfishThreadPoolConfigurationBean(threadPoolConfigBean);
        threadPoolConfigBean.setIdleThreadTimeout(Integer.parseInt(batchContainerProps.getProperty(
                GLASSFISH_THREADPOOL_IDLE_THREAD_TIMEOUT, "900")));
        threadPoolConfigBean
                .setMaxQueueSize(Integer.parseInt(batchContainerProps.getProperty(GLASSFISH_THREADPOOL_MAX_QUEUE_SIZE, "4096")));
        threadPoolConfigBean.setMaxThreadPoolSize(Integer
                .parseInt(batchContainerProps.getProperty(GLASSFISH_THREADPOOL_MAX_POOL_SIZE, "5")));
        threadPoolConfigBean.setMinThreadPoolSize(Integer
                .parseInt(batchContainerProps.getProperty(GLASSFISH_THREADPOOL_MIN_POOL_SIZE, "2")));
    }

    private void loadOtherConfig() {

        String seMode = serviceImplClassNames.get(ServiceType.JAVA_EDITION_IS_SE_DUMMY_SERVICE);

        if (seMode.equalsIgnoreCase("true")) {
            batchRuntimeConfig.setJ2seMode(true);
        }

        // WorkManager JNDI name
        String workManagerJNDI = batchContainerProps.getProperty(WORK_MANAGER_JNDI_NAME);
        if (workManagerJNDI != null) {
            batchRuntimeConfig.setWorkManagerJndiName(workManagerJNDI.trim());
        }
    }

    // clear registry
    public void shutdown() {
        String sourceMethod = "shutdown";
        if (logger.isLoggable(Level.FINE))
            logger.entering(sourceClass, sourceMethod);

        if (logger.isLoggable(Level.FINE))
            logger.exiting(sourceClass, sourceMethod);
    }

    // Look up registry and return requested service if exist
    // If not exist, create a new one, add to registry and return that one
    public IBatchServiceBase getService(ServiceType serviceType) throws BatchContainerServiceException {
        String sourceMethod = "getService";
        if (logger.isLoggable(Level.FINE))
            logger.entering(sourceClass, sourceMethod + ", serviceType=" + serviceType);

        IBatchServiceBase service = null;

        if (isInited == false) {
            Exception e = new Exception("ServicesManager not inited!");
            throw new BatchContainerServiceException("ServicesManager not inited!", e);
        } else {
            service = _getServiceHelper(serviceType);

        }

        if (logger.isLoggable(Level.FINE))
            logger.exiting(sourceClass, sourceMethod);

        return service;
    }

    /**
     * This helper method ensures that only a single instance of a service gets
     * new'ed up and runs the service.init() method.
     * 
     * The method is designed to perform optimally. Before entering the sync
     * block, we check if the service already exists. If it does, we return the
     * cached ref, without ever entering the sync block (or incurring all the
     * overhead associated with it). The sync block is not entered unless the
     * service has not yet been created. So the sync block will likely only be
     * entered once - upon first creation. Subsequent callers will retrieve the
     * service from the hashmap without ever incurring the sync block overhead.
     */
    private IBatchServiceBase _getServiceHelper(ServiceType serviceType) {
        IBatchServiceBase service = serviceRegistry.get(serviceType);

        if (service == null) {
            synchronized (this) {
                // check again, within sync, to make sure somebody hasn't beaten
                // us to it.
                service = serviceRegistry.get(serviceType);

                if (service == null) {
                    service = _loadServiceHelper(serviceType);
                    service.init(batchRuntimeConfig);
                    serviceRegistry.putIfAbsent(serviceType, service);
                }
            }
        }
        return service;
    }

    /**
     * Try to load the IGridContainerService given by the className. If it fails
     * to load, default to the defaultClass. If the default fails to load, then
     * blow out of here with a RuntimeException.
     */
    private IBatchServiceBase _loadServiceHelper(ServiceType serviceType) {
        IBatchServiceBase service = null;
        Throwable e = null;

        String className = serviceImplClassNames.get(serviceType);
        try {
            if (className != null)
                service = _loadService(className);
        } catch (Throwable e1) {
            e = e1;
            if (logger != null)
                logger.log(Level.WARNING, "Could not instantiate service: " + className + " due to exception:" + e);
            throw new RuntimeException("Could not instantiate service " + className + " due to exception: " + e);
        }

        if (service == null) {
            throw new RuntimeException("Instantiate of service=: " + className + " returned null. Aborting...");
        }

        return service;
    }

    private IBatchServiceBase _loadService(String className) throws Exception {

        IBatchServiceBase service = null;

        Class cls = Class.forName(className);

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

    public IBatchConfig getBatchRuntimeConfiguration() {
        return batchRuntimeConfig;
    }

    /*
     * Services are lazily initialized. If we haven't instantiated the thread
     * pool service yet, jut update the glassfish configuration bean on the main
     * config object. If we have instantiated this service, and it is the
     * glassfish thread pool impl, then go and update it on the backing object.
     * This is a one-off and doesn't fit well with other config, but no need to
     * over-architect at this point.
     */
    public void updateGlassfishThreadPoolConfiguration(GlassfishThreadPoolConfigurationBean config) {

        IBatchThreadPoolService threadPoolService = (IBatchThreadPoolService) serviceRegistry.get(ServiceType.BATCH_THREADPOOL_SERVICE);

        if (threadPoolService == null) {
            if (logger.isLoggable(Level.INFO)) {
                logger
                        .info("In method: updateGlassfishThreadPoolConfiguration. Thread pool service not initialized yet, so simply updating the config.");
                logger.info("Glassfish thread pool settings: (" + config.getMinThreadPoolSize() + "," + config.getMaxThreadPoolSize() + ","
                        + config.getIdleThreadTimeout() + "," + TimeUnit.SECONDS + "," + config.getMaxQueueSize() + ")");
            }
            batchRuntimeConfig.setGlassfishThreadPoolConfigurationBean(config);
        } else if (threadPoolService instanceof GlassfishThreadPoolServiceImpl) {
            GlassfishThreadPoolServiceImpl tps = (GlassfishThreadPoolServiceImpl) threadPoolService;
            tps.updateConfig(config);
        }
    }

    public static synchronized ServicesManager getInstance() throws BatchContainerServiceException {
        if (servicesManager == null) {
            servicesManager = new ServicesManager();
        }
        return servicesManager;
    }

}
