package com.ibm.jbatch.container.services.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.spi.BatchSPIManager;
import com.ibm.jbatch.spi.services.IBatchConfig;
import com.ibm.jbatch.spi.services.IBatchThreadPoolService;
import com.ibm.jbatch.spi.services.ParallelTaskResult;

public class SPIDelegatingThreadPoolServiceImpl implements IBatchThreadPoolService {
	
	private final static String sourceClass = SPIDelegatingThreadPoolServiceImpl.class.getName();
	private final static Logger logger = Logger.getLogger(sourceClass);

	public SPIDelegatingThreadPoolServiceImpl() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void init(IBatchConfig batchConfig) {
		// Don't want to get/cache anything here since we want to delegate each time.
	}

	
	public void shutdown() throws BatchContainerServiceException {
		String method = "shutdown";
		if(logger.isLoggable(Level.FINER)) { logger.entering(sourceClass, method);	}
		
		ExecutorService delegateService = BatchSPIManager.getInstance().getExecutorServiceProvider().getExecutorService();
		delegateService.shutdownNow();
		
		if(logger.isLoggable(Level.FINER)) { logger.exiting(sourceClass, method);	}
	}

	public void executeTask(Runnable work, Object config) {
		String method = "executeTask";
		if(logger.isLoggable(Level.FINER)) { logger.entering(sourceClass, method);	}
		
		ExecutorService delegateService = BatchSPIManager.getInstance().getExecutorServiceProvider().getExecutorService();
		delegateService.execute(work);
		
		if(logger.isLoggable(Level.FINER)) { logger.exiting(sourceClass, method);	}
	}

    public ParallelTaskResult executeParallelTask(Runnable work, Object config) {
        String method = "executeParallelTask";
        if(logger.isLoggable(Level.FINER)) { logger.entering(sourceClass, method);  }
        
		ExecutorService delegateService = BatchSPIManager.getInstance().getExecutorServiceProvider().getExecutorService();
        Future result = delegateService.submit(work);
        ParallelTaskResult taskResult = new JSEResultAdapter(result);
        
        if(logger.isLoggable(Level.FINER)) { logger.exiting(sourceClass, method);   }
        
        return taskResult;
    }
	
	
}
