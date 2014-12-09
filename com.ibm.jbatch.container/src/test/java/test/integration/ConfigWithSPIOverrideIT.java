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
import com.ibm.jbatch.spi.BatchSPIManager.PlatformMode;
import com.ibm.jbatch.spi.ServiceRegistry.ServiceImplClassNames;
import com.ibm.jbatch.spi.ServiceRegistry.ServicePropertyNames;

public class ConfigWithSPIOverrideIT {

	@Test
	public void testConfigWithSPIOverride() {

		// These choices don't really make sense, i.e. they wouldn't work necessarily enabling you 
		// to run a real job.  
		String class1 = ServiceImplClassNames.BATCH_THREADPOOL_JNDI_DELEGATING;
		String class2 = ServiceImplClassNames.DELEGATING_ARTIFACT_FACTORY_DEFAULT;
		String class3 = ServiceImplClassNames.CONTAINER_ARTIFACT_FACTORY_CDI;
		String class4 = ServiceImplClassNames.JOBXML_LOADER_DIRECTORY;
		String class5 = ServiceImplClassNames.JOBXML_LOADER_DIRECTORY;
		String class6 = ServiceImplClassNames.TRANSACTION_DEFAULT;


		// This is really more of a unit test than an integration test at present.
		Properties props = new Properties();
		props.setProperty(ServicePropertyNames.BATCH_THREADPOOL_SERVICE, class1);
		props.setProperty(ServicePropertyNames.CONTAINER_ARTIFACT_FACTORY_SERVICE, class2);
		props.setProperty(ServicePropertyNames.DELEGATING_ARTIFACT_FACTORY_SERVICE, class3);
		props.setProperty(ServicePropertyNames.DELEGATING_JOBXML_LOADER_SERVICE, class4);
		props.setProperty(ServicePropertyNames.JOBXML_LOADER_SERVICE, class5);
		props.setProperty(ServicePropertyNames.TRANSACTION_SERVICE, class6);
		BatchSPIManager spiMgr = BatchSPIManager.getInstance();
		spiMgr.registerBatchContainerOverrideProperties(props);
		spiMgr.registerPlatformMode(PlatformMode.SE);

		ServicesManager srvcMgr = ServicesManagerImpl.getInstance();
		assertEquals(class1, srvcMgr.getThreadPoolService().getClass().getName());
		assertEquals(class2, srvcMgr.getPreferredArtifactFactory().getClass().getName());
		assertEquals(class3, srvcMgr.getDelegatingArtifactFactory().getClass().getName());
		assertEquals(class4, srvcMgr.getDelegatingJobXMLLoaderService().getClass().getName());
		assertEquals(class5, srvcMgr.getPreferredJobXMLLoaderService().getClass().getName());
		assertEquals(class6, srvcMgr.getTransactionManagementService().getClass().getName());
	}
	
}
