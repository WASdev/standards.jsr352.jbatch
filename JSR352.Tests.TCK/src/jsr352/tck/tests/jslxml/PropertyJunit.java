/*
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
package jsr352.tck.tests.jslxml;

import java.net.URL;
import java.util.Properties;

import javax.batch.runtime.JobExecution;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import jsr352.tck.utils.IOHelper;
import jsr352.tck.utils.JobOperatorBridge;


public class PropertyJunit {
 
    private static JobOperatorBridge jobOp;

    
    public static void setup(String[] args, Properties props) throws Exception {        
        jobOp = new JobOperatorBridge();        
    }
    
    @BeforeClass
    public static void setUp() throws Exception {        
        jobOp = new JobOperatorBridge();        
    }

    @AfterClass
    public static void tearDown() throws Exception {
    }


    @After
    public void cleanup() throws Exception {
        //Clear this property for next test
        System.clearProperty("property.junit.result");
    }

    /*
   	 * @testName: testBatchletPropertyInjection
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testBatchletPropertyInjection() throws Exception {
        URL jobXMLURL = this.getClass().getResource("/job_properties2.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());

        System.setProperty("property.junit.propName", "myProperty1");
        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);
        
        String result = System.getProperty("property.junit.result");        
        assert("value1" == result);
    }

    /*
   	 * @testName: testInitializedPropertyIsOverwritten
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testInitializedPropertyIsOverwritten() throws Exception {
        URL jobXMLURL = this.getClass().getResource("/job_properties2.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());

        System.setProperty("property.junit.propName", "myProperty2");
        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);
        
        String result = System.getProperty("property.junit.result");
        assert("value2" == result);
    }

    /*
   	 * @testName: testPropertyWithJobParameter
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testPropertyWithJobParameter() throws Exception {
        URL jobXMLURL = this.getClass().getResource("/job_properties2.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());

        Properties jobParameters = new Properties();
        String expectedResult = "mySubmittedValue";
        jobParameters.setProperty("mySubmittedProp", expectedResult );

        System.setProperty("property.junit.propName", "mySubmittedProp");
        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML, jobParameters);
        
        String result = System.getProperty("property.junit.result");
        assert(expectedResult == result);
    }

    /*
   	 * @testName: testPropertyPrecedenceSystemProp
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testPropertyPrecedenceSystemProp() throws Exception {
        URL jobXMLURL = this.getClass().getResource("/job_properties2.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());

        System.setProperty("mySystemProp", "mySystemPropValue");

        System.setProperty("property.junit.propName", "mySystemProp");
        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);
        
        String result = System.getProperty("property.junit.result");

        //Clear this property for next test
        System.clearProperty("mySystemProp");

        assert("mySystemPropValue" == result);
    }

    /*
   	 * @testName: testPropertyInjectionNoPropertyOnElement
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testPropertyInjectionNoPropertyOnElement() throws Exception {
        URL jobXMLURL = this.getClass().getResource("/job_properties2.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());

        System.setProperty("property.junit.propName", "javaDefaultValueProp");
        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);
        
        String result = System.getProperty("property.junit.result");
        assert("null" == result);
    }

    /*
   	 * @testName: testDefaultPropertyName
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testDefaultPropertyName() throws Exception {
        URL jobXMLURL = this.getClass().getResource("/job_properties2.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());

        System.setProperty("property.junit.propName", "property4");
        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);
        
        String result = System.getProperty("property.junit.result");
        assert("value4" == result);
    }

    /*
   	 * @testName: testGivenPropertyName
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testGivenPropertyName() throws Exception {
        URL jobXMLURL = this.getClass().getResource("/job_properties2.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());

        System.setProperty("property.junit.propName", "myProperty4");
        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);
        
        String result = System.getProperty("property.junit.result");
        assert("value4" == result);
    }

    /*
   	 * @testName: testPropertyInnerScopePrecedence
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testPropertyInnerScopePrecedence() throws Exception {
        URL jobXMLURL = this.getClass().getResource("/job_properties2.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());

        System.setProperty("property.junit.propName", "batchletProp");
        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML);
        
        String result = System.getProperty("property.junit.result");
        assert("batchletPropValue" == result);
    }

    /*
   	 * @testName: testPropertyWithConcatenation
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test
    public void testPropertyWithConcatenation() throws Exception {
        URL jobXMLURL = this.getClass().getResource("/job_properties2.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());

        Properties jobParameters = new Properties();
        jobParameters.setProperty("myFilename", "testfile1" );

        System.setProperty("property.junit.propName", "myConcatProp");
        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML, jobParameters);
        
        String result = System.getProperty("property.junit.result");
        assert("testfile1.txt" == result);
    }

    //FIXME This test should work. Will be debugging this shortly
    /*
   	 * @testName: testJavaSystemProperty
   	 * @assertion: FIXME
   	 * @test_Strategy: FIXME
   	 */
    @Test 
    public void testJavaSystemProperty() throws Exception {
        URL jobXMLURL = this.getClass().getResource("/job_properties2.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());

        Properties jobParameters = new Properties();
        jobParameters.setProperty("myFilename", "testfile2" );

        System.setProperty("property.junit.propName", "myJavaSystemProp");
        JobExecution jobExec = jobOp.startJobAndWaitForResult(jobXML, jobParameters);
        String result = System.getProperty("property.junit.result");

        String pathSep = System.getProperty("file.separator");

        assert(pathSep + "test" + pathSep + "testfile2.txt" == result);

    }

}

