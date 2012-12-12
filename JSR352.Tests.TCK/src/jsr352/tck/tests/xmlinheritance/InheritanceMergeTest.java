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
package jsr352.tck.tests.xmlinheritance;

import static org.junit.Assert.assertTrue;
import jsr352.tck.utils.IOHelper;
import jsr352.tck.utils.ServiceGateway;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.ElementNameAndAttributeQualifier;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.batch.tck.spi.JSLInheritanceMerger;

public class InheritanceMergeTest {
	
	private static JSLInheritanceMerger merger;
	
    @BeforeClass
    public static void setUp() throws Exception {
        merger = ServiceGateway.getServices().getJSLInheritanceMerger();
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setIgnoreComments(true);
        XMLUnit.setIgnoreWhitespace(true);
    }
       
    /*
     * See the X.Child/Parent/Merged.xml assets for the actual test cases.
     */
	@Test
	public void testCase1() throws Exception { testI(1, false); }
	@Test
	public void testCase2() throws Exception { testI(2, false); }
	@Test
	public void testCase3() throws Exception { testI(3, false); }
	@Test
	public void testCase4() throws Exception { testI(4, false); }
	@Test
	public void testCase5() throws Exception { testI(5, false); }
	@Test
	public void testCase6() throws Exception { testI(6, true); }
	@Test
	public void testCase7() throws Exception { testI(7, true); }
	@Test
	public void testCase8() throws Exception { testI(8, false); }
	@Test
	public void testCase9() throws Exception { testI(9, true); }
	@Test
	public void testCase10() throws Exception { testI(10, true); }
	@Test
	public void testCase11() throws Exception { testI(11, true); }

	public void testI(int i, boolean jobFromStep) throws Exception {
		Class clazz = InheritanceMergeTest.class;
		String target = String.valueOf(i);

		String parentXML = IOHelper.readJobXML(clazz.getResource("/inheritance/"+target+".Parent.xml").getFile());
		String childXML = IOHelper.readJobXML(clazz.getResource("/inheritance/"+target+".Child.xml").getFile());
		String expectedMergedXML = IOHelper.readJobXML(clazz.getResource("/inheritance/"+target+".Merged.xml").getFile());

		String calculatedMergedXML;
		if(jobFromStep)
			calculatedMergedXML = merger.mergeStep(childXML, parentXML, "MyStep"); //test XMLs hardcoded to have MyStep id
		else
			calculatedMergedXML = merger.mergeJob(parentXML, childXML);
		
		System.out.println("==================================================");
		System.out.println("Test Case "+target);
		System.out.println("Expected  : "+expectedMergedXML);
		System.out.println("Calculated: "+calculatedMergedXML);

		Diff diff = new Diff(expectedMergedXML, calculatedMergedXML);
		//properties and listeners may be out of order
		diff.overrideElementQualifier(new ElementNameAndAttributeQualifier());
		
		//TODO: we want to continue
		assertTrue("merged XML case "+target+" not similar to expected: " + diff, diff.similar());
		System.out.println(target+" PASS: XML similar");

	}
	
}
