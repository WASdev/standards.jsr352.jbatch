package com.ibm.jbatch.container.config;

import com.ibm.jbatch.spi.services.IBatchServiceBase;
import com.ibm.jbatch.spi.services.ServiceType;

public interface ServicesManager {
	public abstract IBatchServiceBase getService(ServiceType serviceType);
}