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
package test.junit;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.jbatch.container.servicesmanager.ServiceTypes;

public class ServiceRegistryValidatorTest {

	@Test
	public void testCorrectNumberOfEntries() {
		int enumCount = 0;
		for (ServiceTypes.Name n : ServiceTypes.Name.values()) {
			enumCount++;
		}
		
		Map<ServiceTypes.Name,String> classNames = 
				ServiceTypes.getServiceImplClassNames();
		Assert.assertEquals(enumCount, classNames.values().size());
		
		Map<String, ServiceTypes.Name> propNames = 
				ServiceTypes.getServicePropertyNames();
		Assert.assertEquals(enumCount, propNames.values().size());
	}

	@Test
	public void testNoPropertyNameDups() {
		Set<String> seenThem = new HashSet<String>();
		
		Map<String, ServiceTypes.Name> propNames = 
				ServiceTypes.getServicePropertyNames();
		for (String s : propNames.keySet()) {
			Assert.assertFalse("Found duplicate of " + s, seenThem.contains(s));
			seenThem.add(s);
		}
	}

}
