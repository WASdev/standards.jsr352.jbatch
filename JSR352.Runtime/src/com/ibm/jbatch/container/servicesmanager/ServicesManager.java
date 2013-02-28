package com.ibm.jbatch.container.servicesmanager;

import com.ibm.jbatch.container.services.IBatchKernelService;
import com.ibm.jbatch.container.services.IJobStatusManagerService;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.tck.bridge.IJobEndCallbackService;
import com.ibm.jbatch.spi.services.IBatchArtifactFactory;
import com.ibm.jbatch.spi.services.IBatchThreadPoolService;
import com.ibm.jbatch.spi.services.IJobIdManagementService;
import com.ibm.jbatch.spi.services.IJobXMLLoaderService;
import com.ibm.jbatch.spi.services.ITransactionManagementService;

public interface ServicesManager {
	public IPersistenceManagerService getPersistenceManagerService();
	public IJobStatusManagerService getJobStatusManagerService();
	public IJobIdManagementService getJobIdManagementService();
	public ITransactionManagementService getTransactionManagementService();
	public IJobEndCallbackService getJobCallbackService();
	public IBatchKernelService getBatchKernelService();
	public IJobXMLLoaderService getDelegatingJobXMLLoaderService();
	public IJobXMLLoaderService getPreferredJobXMLLoaderService();
	public IBatchThreadPoolService getThreadPoolService();
	public IBatchArtifactFactory getDelegatingArtifactFactory();
	public IBatchArtifactFactory getPreferredArtifactFactory();
}