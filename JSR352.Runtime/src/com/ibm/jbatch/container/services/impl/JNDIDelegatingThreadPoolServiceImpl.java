package com.ibm.jbatch.container.services.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.util.BatchContainerConstants;
import com.ibm.jbatch.spi.services.IBatchConfig;
import com.ibm.jbatch.spi.services.IBatchThreadPoolService;
import com.ibm.jbatch.spi.services.ParallelTaskResult;

public class JNDIDelegatingThreadPoolServiceImpl implements IBatchThreadPoolService, BatchContainerConstants  {

	private final static String sourceClass = JNDIDelegatingThreadPoolServiceImpl.class.getName();
	private final static Logger logger = Logger.getLogger(sourceClass);

	public final String DEFAULT_JNDI_LOCATION = "java:comp/DefaultManagedExecutorService";
	private String jndiLocation = null;

	public JNDIDelegatingThreadPoolServiceImpl() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void init(IBatchConfig batchConfig) {
		// Don't want to get/cache the actual threadpool here since we want to do a JNDI lookup each time.
		jndiLocation = batchConfig.getConfigProperties().getProperty(THREADPOOL_JNDI_LOCATION, DEFAULT_JNDI_LOCATION);
	}

	public void shutdown() throws BatchContainerServiceException {
		String method = "shutdown";
		if(logger.isLoggable(Level.FINER)) { logger.entering(sourceClass, method);	}

		// We don't want to be responsible for cleaning up.

		if(logger.isLoggable(Level.FINER)) { logger.exiting(sourceClass, method);	}
	}

	public void executeTask(Runnable work, Object config) {
		String method = "executeTask";
		if(logger.isLoggable(Level.FINER)) { logger.entering(sourceClass, method);	}

		try {
			Context ctx = new InitialContext();
			ExecutorService delegateService = (ExecutorService)ctx.lookup(jndiLocation);
			delegateService.execute(work);
		} catch (NamingException e) {
			logger.severe("Lookup failed for JNDI name: " + jndiLocation);
			throw new BatchContainerServiceException(e);
		}

		if(logger.isLoggable(Level.FINER)) { logger.exiting(sourceClass, method);	}
	}

	public ParallelTaskResult executeParallelTask(Runnable work, Object config) {
		String method = "executeParallelTask";
		ParallelTaskResult taskResult = null;
		if(logger.isLoggable(Level.FINER)) { logger.entering(sourceClass, method);  }

		try {
			Context ctx = new InitialContext();
			ExecutorService delegateService = (ExecutorService)ctx.lookup(jndiLocation);
			Future result = delegateService.submit(work);
			taskResult = new JSEResultAdapter(result);
		} catch (NamingException e) { 
			logger.severe("Lookup failed for JNDI name: " + jndiLocation);
			throw new BatchContainerServiceException(e);
		}

		if(logger.isLoggable(Level.FINER)) { logger.exiting(sourceClass, method);   }

		return taskResult;
	}


}
