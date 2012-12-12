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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.batch.container.IBatchConfig;
import com.ibm.batch.container.exception.BatchContainerServiceException;
import com.ibm.batch.container.impl.BatchConfigImpl;
import com.ibm.batch.container.impl.BatchKernelImpl;
import com.ibm.batch.container.services.impl.BatchTransactionServiceImpl;
import com.ibm.batch.container.services.impl.JDBCPersistenceManagerImpl;
import com.ibm.batch.container.services.impl.JSEBatchArtifactFactoryImpl;
import com.ibm.batch.container.services.impl.JSEThreadPoolServiceImpl;
import com.ibm.batch.container.services.impl.JobIdManagerImpl;
import com.ibm.batch.container.services.impl.JobStatusManagerImpl;
import com.ibm.batch.container.tck.bridge.JobEndCallbackManagerImpl;


public class ServicesManager {

	private final static String sourceClass = ServicesManager.class.getName();
	private final static Logger logger = Logger.getLogger(sourceClass);
	
	private static final String BATCH_CONTAINER_CONFIG_FILE = "batch-services.properties";

	public static final String PERSISTENCE_MANAGEMENT_SERVICE = "PERSISTENCE_MANAGEMENT_SERVICE";
	public static final String JOB_STATUS_MANAGEMENT_SERVICE = "JOB_STATUS_MANAGEMENT_SERVICE";
	public static final String BATCH_THREADPOOL_SERVICE = "BATCH_THREADPOOL_SERVICE";
	public static final String CONFIGURATION_SERVICE = "CONFIGURATION_SERVICE";
	public static final String CONTAINER_LOGGER_CONFIG_SERVICE = "CONTAINER_LOGGER_CONFIG_SERVICE";
	public static final String CONTAINER_ARTIFACT_FACTORY_SERVICE = "CONTAINER_ARTIFACT_FACTORY_SERVICE";
	public static final String BATCH_KERNEL_SERVICE = "BATCH_KERNEL_SERVICE";
	public static final String JOB_ID_MANAGEMENT_SERVICE = "JOB_ID_MANAGEMENT_SERVICE";
	public static final String JOB_OP_SERVICE = "JOB_OP_SERVICE";
	public static final String CALLBACK_SERVICE = "CALLBACK_SERVICE";
	public static final String TRANSACTION_SERVICE = "TRANSACTION_SERVICE";
	
	public static final String J2SE_MODE = "J2SE_MODE";
	private static final String JNDI_NAME = "JNDI_NAME";
	public static final String WORK_MANAGER_JNDI_NAME = "WORK_MANAGER_JNDI_NAME";
	

	// TODO need to change the default classes
	private static final String DEFAULT_PERSISTENCE_MGR_CLASS = "com.ibm.batch.container.services.impl.InMemoryPersistenceManagerImpl";
	private static final String DEFAULT_BATCH_THREADPOOL_SERVICE = "com.ibm.batch.container.services.impl.JSEThreadPoolImpl";
	private static final String DEFAULT_CONFIGURATION_SERVICE = "com.ibm.ws.gridcontainer.services.impl.DefaultConfigurationRepositoryServiceImpl";
	private static final String DEFAULT_CONTAINER_LOGGER_CONFIG_SERVICE = "com.ibm.ws.gridcontainer.services.impl.DefaultContainerLoggerConfigurationServiceImpl";
	private static final String DEFAULT_CONTAINER_ARTIFACT_FACTORY_SERVICE = "com.ibm.batch.services.impl.JSEBatchArtifactFactoryImpl";
	private static final String DEFAULT_BATCH_KERNEL_SERVICE = "com.ibm.batch.container.impl.BatchKernelImpl";
	private static final String DEFAULT_JOBSTATUS_MRG_SERVICE = "com.ibm.batch.container.services.impl.JobStatusManagerImpl";
	private static final String DEFAULT_JOBID_MRG_SERVICE = "com.ibm.batch.container.services.impl.JobIdManagerImpl";
	private static final String DEFAULT_JOB_OP_SERVICE = "com.ibm.batch.container.services.impl.JobOperatorImpl";
	private static final String DEFAULT_CALLBACK_SERVICE = "com.ibm.batch.container.tck.bridge.JobEndCallbackManagerImpl";
	private static final String DEFAULT_TRANSACTION_SERVICE = "com.ibm.batch.container.services.impl.BatchTransactionServiceImpl";
	
	private static final String JDBC_DRIVER = "JDBC_DRIVER";
	private static final String JDBC_URL = "JDBC_URL";
	private static final String DB_USER = "DB_USER";
	private static final String DB_PASSWORD = "DB_PWD";

	private String persistenceMgrClass = null;
	private String threadpoolServiceClass = null;
	private String configServiceClass = null;
	private String batchArtifactFactoryClass = null;
	private String containerKernelClass = null;
	private String jobstatusMgrClass = null;
	private String jobIdMgrClass = null;
	private String jobOpClass = null;
	private String callbackClass = null;
	private String transactionClass = null;

	private boolean isInited = false;
	private Properties batchContainerProps = null;
	private static ServicesManager servicesManager;
	private static BatchConfigImpl batchConfig = null;
	private String batchConfigDir = null; 
	
	public static enum ServiceType { 
	    TRANSACTION_MANAGEMENT_SERVICE,
	    PERSISTENCE_MANAGEMENT_SERVICE,
	    JOB_STATUS_MANAGEMENT_SERVICE,
	    THREADPOOL_MANAGEMENT_SERVICE,
	    CONTAINER_ARTIFACT_FACTORY_SERVICE,
	    CONFIGURATION_SERVICE,
	    BATCH_KERNEL_SERVICE,
	    JOB_ID_MANAGEMENT_SERVICE,
	    JOB_OP_SERVICE,
	    CALLBACK_SERVICE}; 

	// Registry of all current services
	private  ConcurrentHashMap<String,IBatchServiceBase> _serviceRegistry = new ConcurrentHashMap<String,IBatchServiceBase>();

	
	private ServicesManager(Properties servicesList) {
		init(servicesList);		
	}

	private ServicesManager() {
		init(null);
	}

	private void init(Properties prop) {
		String sourceMethod = "init";
		if (logger.isLoggable(Level.FINE))
			logger.entering(sourceClass, sourceMethod );

		if(!isInited) {
			if(batchContainerProps != null)	
				this.batchContainerProps = prop;

			batchConfigDir = System.getProperty("batch.container.dir");
			if (batchConfigDir == null) {
				logger.info("batch.container.dir has not been set");
				//default path
				String currentDir = System.getProperty("user.dir");
				batchConfigDir = currentDir + File.separator + ".." + File.separator + "JSR352.Runtime" + File.separator + "bin" + File.separator + "META-INF" + File.separator + "services";
				logger.info("batch.container.dir is set to default location " + batchConfigDir );
			}else {
				logger.info("batch.container.dir set to " + batchConfigDir);
			}
			
			batchConfig = new BatchConfigImpl();
			batchConfig.setBatchContainerHome(batchConfigDir);
			loadServicesList();

			isInited = true;
		}

		if (logger.isLoggable(Level.FINE))
			logger.exiting(sourceClass, sourceMethod );
	}

	// TODO fix the location of the config file
	private void loadServicesList() {

		InputStream batchServicesListInputStream = this.getClass().getResourceAsStream("/META-INF/services/" + BATCH_CONTAINER_CONFIG_FILE);
		
		if(batchContainerProps == null) {
			batchContainerProps = new Properties();
		}

		if (batchServicesListInputStream != null) {
			// Read properties file if one exists. File always override passed in properties.    		
			try {		    	
				logger.fine("Batch Services File exists! loading it..");
				batchContainerProps.load(batchServicesListInputStream);
				batchServicesListInputStream.close();
			} catch (IOException e) {		    	
				logger.info("Error loading " + "/META-INF/services/" + BATCH_CONTAINER_CONFIG_FILE + " IOException=" + e.toString());
			} catch (Exception e) {
				logger.info("Error loading " + "/META-INF/services/" + BATCH_CONTAINER_CONFIG_FILE + " Exception=" + e.toString());
			} 
		} else {
			logger.info("Could not find batch services file: " + "/META-INF/services/" + BATCH_CONTAINER_CONFIG_FILE);
		}
		
		persistenceMgrClass = batchContainerProps.getProperty(PERSISTENCE_MANAGEMENT_SERVICE,DEFAULT_PERSISTENCE_MGR_CLASS);
		jobstatusMgrClass = batchContainerProps.getProperty(JOB_STATUS_MANAGEMENT_SERVICE, DEFAULT_JOBSTATUS_MRG_SERVICE);
		threadpoolServiceClass = batchContainerProps.getProperty(BATCH_THREADPOOL_SERVICE, DEFAULT_BATCH_THREADPOOL_SERVICE);
		configServiceClass = batchContainerProps.getProperty(CONFIGURATION_SERVICE, DEFAULT_CONFIGURATION_SERVICE);
		containerKernelClass = batchContainerProps.getProperty(BATCH_KERNEL_SERVICE, DEFAULT_BATCH_KERNEL_SERVICE);	
		jobIdMgrClass = batchContainerProps.getProperty(JOB_ID_MANAGEMENT_SERVICE, DEFAULT_JOBID_MRG_SERVICE);
		jobOpClass = batchContainerProps.getProperty(JOB_OP_SERVICE, DEFAULT_JOB_OP_SERVICE);
		callbackClass = batchContainerProps.getProperty(CALLBACK_SERVICE, DEFAULT_CALLBACK_SERVICE);
		transactionClass = batchContainerProps.getProperty(TRANSACTION_SERVICE, DEFAULT_TRANSACTION_SERVICE);
		batchArtifactFactoryClass = batchContainerProps.getProperty(CONTAINER_ARTIFACT_FACTORY_SERVICE, DEFAULT_CONTAINER_ARTIFACT_FACTORY_SERVICE);
		
		if (batchContainerProps.getProperty(J2SE_MODE) != null && batchContainerProps.getProperty(J2SE_MODE).equalsIgnoreCase("true")) {
			batchConfig.setJ2seMode(true);			
		}
		
		batchConfig.setJndiName(batchContainerProps.getProperty(JNDI_NAME));
		batchConfig.setJdbcDriver(batchContainerProps.getProperty(JDBC_DRIVER));
		batchConfig.setJdbcUrl(batchContainerProps.getProperty(JDBC_URL));
		batchConfig.setDbUser(batchContainerProps.getProperty(DB_USER));
		batchConfig.setDbPassword(batchContainerProps.getProperty(DB_PASSWORD));
		batchConfig.setWorkManagerJndiName(batchContainerProps.getProperty(WORK_MANAGER_JNDI_NAME));
	}

	// clear registry
	public void shutdown() {
		String sourceMethod = "shutdown";
		if (logger.isLoggable(Level.FINE))
			logger.entering(sourceClass, sourceMethod  );

		if (logger.isLoggable(Level.FINE))
			logger.exiting(sourceClass, sourceMethod );

	}

	// Look up registry and return requested service if exist
	// If not exist, create a new one, add to registry and return that one
	public IBatchServiceBase getService(ServiceType serviceType) throws BatchContainerServiceException {
		String sourceMethod = "getService";
		if (logger.isLoggable(Level.FINE))
			logger.entering(sourceClass, sourceMethod + ", serviceType=" + serviceType );

		IBatchServiceBase service = null;

		if(isInited == false) {
			Exception e = new Exception("ServicesManager not inited!");
			throw new BatchContainerServiceException("ServicesManager not inited!", e);
		}else {
			String key = "";
			switch (serviceType) {

			case PERSISTENCE_MANAGEMENT_SERVICE:
				service = _getServiceHelper(PERSISTENCE_MANAGEMENT_SERVICE,
						this.persistenceMgrClass, 
						JDBCPersistenceManagerImpl.class);
				break;
			case CONTAINER_ARTIFACT_FACTORY_SERVICE:
				service = _getServiceHelper(CONTAINER_ARTIFACT_FACTORY_SERVICE,
						this.batchArtifactFactoryClass, 
						JSEBatchArtifactFactoryImpl.class);
				break;
            case BATCH_KERNEL_SERVICE:
                service = _getServiceHelper(BATCH_KERNEL_SERVICE,
                        this.containerKernelClass, 
                        BatchKernelImpl.class);
                break;
            case JOB_STATUS_MANAGEMENT_SERVICE:
                service = _getServiceHelper(JOB_STATUS_MANAGEMENT_SERVICE,
                        this.jobstatusMgrClass, 
                        JobStatusManagerImpl.class);
                break;    
            case JOB_ID_MANAGEMENT_SERVICE:
                service = _getServiceHelper(JOB_ID_MANAGEMENT_SERVICE,
                        this.jobIdMgrClass, 
                        JobIdManagerImpl.class);
                break;
            case JOB_OP_SERVICE:
                service = _getServiceHelper(JOB_OP_SERVICE,
                        this.jobOpClass, 
                        JobIdManagerImpl.class);
                break;
            case CALLBACK_SERVICE:
            	service = _getServiceHelper(CALLBACK_SERVICE,
            			this.callbackClass,
            			JobEndCallbackManagerImpl.class);
            	break;
            case TRANSACTION_MANAGEMENT_SERVICE:
            	service = _getServiceHelper(TRANSACTION_SERVICE,
            			this.transactionClass,
            			BatchTransactionServiceImpl.class);
            	break;
//			case BatchContainerConstants.CONFIGURATION_SERVICE:
//				service = _getServiceHelper(CONFIGURATION_SERVICE,
//						this.configServiceClass, 
//						DefaultConfigurationRepositoryServiceImpl.class);
//				break;		

			default:
				logger.finer("Invalid service type" + serviceType);
				Exception e = new Exception("Invalid service type");
				throw new BatchContainerServiceException("Invalid service type=" + serviceType,e);			
			}
		}

		if (logger.isLoggable(Level.FINE))
			logger.exiting(sourceClass, sourceMethod );

		return service;		
	}

	/**
	 * Executor Service is not managed in the repository each invocation
	 * to get Exec service returns a new ExecutorService
	 * @param pgcConfig
	 * @param size
	 * @return
	 */
	public IBatchThreadPoolService getThreadpoolService(IBatchConfig bConfig,  int size) {
		IBatchThreadPoolService threadpoolService = null;
		try {
			/*execService = (IExecutorService)Class.forName(this.executorServiceClass).
				getConstructor(Class.forName("com.ibm.ws.gridcontainer.IPGCConfig"),
									Class.forName("java.lang.Integer")).
							newInstance(_pgcConfig, size);*/
			
			threadpoolService = (IBatchThreadPoolService)_loadService(this.threadpoolServiceClass);
		} catch (Throwable e1) {
			logger.warning("Could not instantiate: " + threadpoolServiceClass + " due to: " + e1.getMessage() + " reverting to defaults");
			threadpoolService = new JSEThreadPoolServiceImpl();
		}
		threadpoolService.setPoolSize(size);
		
		if (bConfig == null) {
			bConfig = batchConfig;
		}
		threadpoolService.init(bConfig);
		if(logger.isLoggable(Level.FINEST)) { logger.finest("got executor service");}
		return threadpoolService;
	}
	/**
	 * d706734 - 
	 * This helper method ensures that only a single instance of a service gets 
	 * new'ed up and runs the service.init() method. 
	 *
	 * The method is designed to perform optimally. Before entering the sync block, 
	 * we check if the service already exists.  If it does, we return the cached
	 * ref, without ever entering the sync block (or incurring all the overhead
	 * associated with it).  The sync block is not entered unless the service has 
	 * not yet been created. So the sync block will likely only be entered once - 
	 * upon first creation.  Subsequent callers will retrieve the service from the 
	 * hashmap without ever incurring the sync block overhead.
	 */
	private IBatchServiceBase _getServiceHelper(String key, 
			String className, 
			Class defaultClass)
	{
		IBatchServiceBase service = _serviceRegistry.get(key);

		if (service == null)
		{
			synchronized(this)
			{
				// check again, within sync, to make sure somebody hasn't beaten us to it.
				service = _serviceRegistry.get(key);

				if(service == null) 
				{
					service = _loadServiceHelper(className, defaultClass);

					service.init(batchConfig);

					_serviceRegistry.putIfAbsent(key, service);
				}	
			}	
		}
		return service;
	}

	/**
	 * Try to load the IGridContainerService given by the className.
	 * If it fails to load, default to the defaultClass.
	 * If the default fails to load, then blow out of here with a RuntimeException.
	 */
	public IBatchServiceBase _loadServiceHelper(String className, Class defaultClass)
	{
		IBatchServiceBase service = null;
		Throwable e = null;

		try 
		{
			if (className != null)
				service = _loadService(className);
		} 
		catch (Throwable e1) 
		{
			e = e1;
			if(logger != null) 
				logger.log(Level.WARNING, "Could not instantiate: " + className + " due to exception. Reverting to default: " + defaultClass.getName(), e);
		}

		if (service == null)
		{
			try
			{
				service = (IBatchServiceBase) defaultClass.newInstance();
			}
			catch (Throwable e2)
			{
				e = e2;
				if(logger != null) 
					logger.log(Level.WARNING, "Could not instantiate: " + defaultClass.getName() + " due to exception", e);
			}
		}

		if (service == null)
			throw new RuntimeException("Could not instantiate service " + className + " or default " + defaultClass.getName(), e);
		return service;
	}
	
	private IBatchServiceBase _loadService(String className) throws Exception {
		IBatchServiceBase service = null;
		Class cls = Class.forName(className);
		if(cls != null) {

			Constructor ctor = cls.getConstructor();
			if(ctor != null) {

				service = (IBatchServiceBase)ctor.newInstance();
			} else {
				throw new Exception("Service class " + className + " should  have a default constructor defined");
			}
		} else {
			throw new Exception("Exception loading Service class " + className + " make sure it exists");
		}


		return service;
	}


	public static synchronized ServicesManager getInstance() throws BatchContainerServiceException {
		if(servicesManager == null) {			
			servicesManager = new ServicesManager();
		}
		return servicesManager;
	}

	public static synchronized ServicesManager getInstance(Properties servicesList) throws BatchContainerServiceException {
		if(servicesManager == null) {			
			servicesManager = new ServicesManager(servicesList);
		}
		return servicesManager;
	}

}
