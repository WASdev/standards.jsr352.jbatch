/**
 * Copyright 2015 International Business Machines Corp.
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

import static org.junit.Assert.*;

import org.junit.Test;

import com.ibm.jbatch.container.servicesmanager.ServicesManager;
import com.ibm.jbatch.container.servicesmanager.ServicesManagerImpl;
import com.ibm.jbatch.spi.BatchSPIManager;
import com.ibm.jbatch.spi.BatchSPIManager.PlatformMode;

public class SPIOverrideExplicitEEModeIT {

	/*
	 * Not that interesting except we just had a bug where this was failing.
	 */
	@Test
	public void testSPIOverrideExplicitEEMode() {
		BatchSPIManager spiMgr = BatchSPIManager.getInstance();
		spiMgr.registerPlatformMode(PlatformMode.EE);

		ServicesManager srvcMgr = ServicesManagerImpl.getInstance();
		assertEquals(PlatformMode.EE,srvcMgr.getPlatformMode());
	}

}
