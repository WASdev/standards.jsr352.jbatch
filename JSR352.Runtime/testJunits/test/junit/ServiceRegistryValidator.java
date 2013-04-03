package test.junit;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.jbatch.container.servicesmanager.ServiceTypes;

public class ServiceRegistryValidator {

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
