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
package test.integration;

import java.util.Properties;

import static org.junit.Assert.*;

import org.junit.Test;

import com.ibm.jbatch.container.servicesmanager.ServicesManager;
import com.ibm.jbatch.container.servicesmanager.ServicesManagerImpl;
import com.ibm.jbatch.spi.BatchSPIManager;
import com.ibm.jbatch.spi.ServiceRegistry.ServiceImplClassNames;
import com.ibm.jbatch.spi.ServiceRegistry.ServicePropertyNames;

public class ConfigWithSPIandSystemPropertyOverrideIT {

    static final String PROP_PREFIX = "com.ibm.jbatch.spi.ServiceRegistry";

		
    /**
     *  System property overrides SPI 
     */
	@Test
	public void testConfigWithSPIandSystemPropertyOverride() {
		System.setProperty(PROP_PREFIX + "." + ServicePropertyNames.BATCH_THREADPOOL_SERVICE, 
				ServiceImplClassNames.BATCH_THREADPOOL_JNDI_DELEGATING);

		Properties props = new Properties();
		props.setProperty(ServicePropertyNames.BATCH_THREADPOOL_SERVICE, ServiceImplClassNames.BATCH_THREADPOOL_BOUNDED);
		BatchSPIManager spiMgr = BatchSPIManager.getInstance();
		spiMgr.registerBatchContainerOverrideProperties(props);

		ServicesManager srvcMgr = ServicesManagerImpl.getInstance();
		String threadPoolSrvcClassName = srvcMgr.getThreadPoolService().getClass().getName();
		assertEquals(ServiceImplClassNames.BATCH_THREADPOOL_JNDI_DELEGATING, threadPoolSrvcClassName);
	}
	
}
