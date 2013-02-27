/**
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
package com.ibm.jbatch.tck.tests.xmlinheritance;

import org.testng.Reporter;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import java.util.Properties;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.ElementNameAndAttributeQualifier;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.BeforeClass;
import org.junit.Ignore;

import com.ibm.jbatch.tck.spi.JSLInheritanceMerger;
import com.ibm.jbatch.tck.utils.IOHelper;
import com.ibm.jbatch.tck.utils.ServiceGateway;

import static com.ibm.jbatch.tck.utils.AssertionUtils.assertWithMessage;

public class InheritanceMergeTests {
	
	private static JSLInheritanceMerger merger;
	
    
    public static void setup(String[] args, Properties props) throws Exception {
    	
    	String METHOD = "setup";
    	
    	try {
	        merger = ServiceGateway.getServices().getJSLInheritanceMerger();
	        XMLUnit.setIgnoreAttributeOrder(true);
	        XMLUnit.setIgnoreComments(true);
	        XMLUnit.setIgnoreWhitespace(true);
    	} catch (Exception e) {
    		handleException(METHOD, e);
    	}
    }
    
    @BeforeMethod
    @BeforeClass
    public static void setUp() throws Exception {
        merger = ServiceGateway.getServices().getJSLInheritanceMerger();
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setIgnoreComments(true);
        XMLUnit.setIgnoreWhitespace(true);
    }
    
    /* cleanup */
	public void  cleanup()
	{		
	
	}
       
    /*
     * See the X.Child/Parent/Merged.xml assets for the actual test cases.
     */
    /*
	 * @testName: testCase1
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
	@Test @org.junit.Test
	public void testCase1() throws Exception { testI(1, false, "testCase1"); }
	
	/*
	 * @testName: testCase2
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
	@Test @org.junit.Test
	public void testCase2() throws Exception { testI(2, false, "testCase2"); }
	
	/*
	 * @testName: testCase3
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
	@Test @org.junit.Test
	public void testCase3() throws Exception { testI(3, false, "testCase3"); }
	
	/*
	 * @testName: testCase4
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
	@Test @org.junit.Test
	public void testCase4() throws Exception { testI(4, false, "testCase4"); }
	
	/*
	 * @testName: testCase5
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
	@Test @org.junit.Test
	public void testCase5() throws Exception { testI(5, false, "testCase5"); }
	
	/*
	 * @testName: testCase6
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
	@Test(enabled = false) @org.junit.Test @Ignore
	public void testCase6() throws Exception { testI(6, true, "testCase6"); }
	
	/*
	 * @testName: testCase7
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
	@Test @org.junit.Test
	public void testCase7() throws Exception { testI(7, true, "testCase7"); }
	
	/*
	 * @testName: testCase8
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
	@Test @org.junit.Test
	public void testCase8() throws Exception { testI(8, false, "testCase8"); }
	
	/*
	 * @testName: testCase9
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
	@Test @org.junit.Test
	public void testCase9() throws Exception { testI(9, true, "testCase9"); }
	
	/*
	 * @testName:  testCase10
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
	@Test @org.junit.Test
	public void testCase10() throws Exception { testI(10, true, "testCase10"); }
	
	/*
	 * @testName: testCase11
	 * @assertion: FIXME
	 * @test_Strategy: FIXME
	 */
	@Test(enabled = false) @org.junit.Test @Ignore
	public void testCase11() throws Exception { testI(11, true, "testCase11"); }

	
	protected void testI(int i, boolean jobFromStep, String testMethod) throws Exception {
		
		String METHOD = testMethod;
		
		try {
		Class<InheritanceMergeTests> clazz = InheritanceMergeTests.class;
		String target = String.valueOf(i);

		Reporter.log("Read parent XML: " + "/inheritance/"+target+".Parent.xml<p>");
		String parentXML = IOHelper.readJobXML(clazz.getResource("/inheritance/"+target+".Parent.xml").getFile());
		
		Reporter.log("Read child XML: " + "/inheritance/"+target+".Child.xml<p>");
		String childXML = IOHelper.readJobXML(clazz.getResource("/inheritance/"+target+".Child.xml").getFile());
		
		Reporter.log("Read expectedMergedXML XML: " + "/inheritance/"+target+".Merged.xml<p>");
		String expectedMergedXML = IOHelper.readJobXML(clazz.getResource("/inheritance/"+target+".Merged.xml").getFile());

		String calculatedMergedXML;
		if(jobFromStep)
			calculatedMergedXML = merger.mergeStep(childXML, parentXML, "MyStep"); //test XMLs hardcoded to have MyStep id
		else
			calculatedMergedXML = merger.mergeJob(parentXML, childXML);
		
		Reporter.log("Calculated merged XML is: " + calculatedMergedXML + "<p>");
		
		/*System.out.println("==================================================");
		System.out.println("Test Case "+target);
		System.out.println("Expected  : "+expectedMergedXML);
		System.out.println("Calculated: "+calculatedMergedXML);*/
		Reporter.log("==================================================<p>");
		Reporter.log("Test Case "+target+"<p>");
		Reporter.log("Expected  : "+expectedMergedXML+"<p>");
		Reporter.log("Calculated: "+calculatedMergedXML+"<p>");

		Diff diff = new Diff(expectedMergedXML, calculatedMergedXML);
		//properties and listeners may be out of order
		diff.overrideElementQualifier(new ElementNameAndAttributeQualifier());
		
		//TODO: we want to continue
		assertWithMessage("merged XML case "+target+" not similar to expected: " + diff, diff.similar());
		Reporter.log(target+" PASS: XML similar<p>");
		} catch (Exception e) {
			handleException(METHOD, e);
		}
	}
	
	 private static void handleException(String methodName, Exception e) throws Exception {
			Reporter.log("Caught exception: " + e.getMessage()+"<p>");
			Reporter.log(methodName + " failed<p>");
			throw e;
		}
	
}
