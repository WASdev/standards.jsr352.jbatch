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
package test.junit;

import static org.junit.Assert.*;

import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;


import org.junit.Ignore;
import org.junit.Test;

import com.ibm.jbatch.jsl.model.Batchlet;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.jbatch.jsl.model.Step;
import com.ibm.jbatch.jsl.util.ValidatorHelper;
import com.ibm.jbatch.jsl.util.JSLValidationEventHandler;

public class JobModelTest {

    @Test
    public void testModelNoValidate() throws Exception {
        
        JAXBContext ctx = JAXBContext.newInstance("com.ibm.jbatch.jsl.model");
        
        Unmarshaller u = ctx.createUnmarshaller();
        URL url = this.getClass().getResource("/job1.xml");
        
        // Use this for anonymous type
        //Job job = (Job)u.unmarshal(url.openStream());
        
        // Use this for named complex type
        Object elem = u.unmarshal(url.openStream());
        JSLJob job = (JSLJob)((JAXBElement)elem).getValue();
        
        assertEquals("job1", job.getId());
        assertEquals(1, job.getExecutionElements().size());
        Step step = (Step)job.getExecutionElements().get(0);
        assertEquals("step1", step.getId());
        Batchlet b = step.getBatchlet();
        assertEquals("step1Ref", b.getRef());
    }
    
    @Test
    public void testModelValidate() throws Exception {
        
        JAXBContext ctx = JAXBContext.newInstance("com.ibm.jbatch.jsl.model");
        
        Unmarshaller u = ctx.createUnmarshaller();
        u.setSchema(ValidatorHelper.getXJCLSchema());
        JSLValidationEventHandler handler = new JSLValidationEventHandler();
        u.setEventHandler(handler);
        URL url = this.getClass().getResource("/job1.xml");
        
        // Use this for anonymous type
        //Job job = (Job)u.unmarshal(url.openStream());
        
        // Use this for named complex type
        Object elem = u.unmarshal(url.openStream());
        assertFalse("XSD invalid, see sysout", handler.eventOccurred());

        JSLJob job = (JSLJob)((JAXBElement)elem).getValue();
        
        assertEquals("job1", job.getId());
        assertEquals(1, job.getExecutionElements().size());
        Step step = (Step)job.getExecutionElements().get(0);
        assertEquals("step1", step.getId());
        Batchlet b = step.getBatchlet();
        assertEquals("step1Ref", b.getRef());
    }

    @Test
    public void testValidateInvalid() throws Exception {
        
        JAXBContext ctx = JAXBContext.newInstance("com.ibm.jbatch.jsl.model");
        
        Unmarshaller u = ctx.createUnmarshaller();
        u.setSchema(ValidatorHelper.getXJCLSchema());
        JSLValidationEventHandler handler = new JSLValidationEventHandler();
        u.setEventHandler(handler);
        URL url = this.getClass().getResource("/invalid.job1.xml");
        
        // Use this for anonymous type
        //Job job = (Job)u.unmarshal(url.openStream());
        
        // Use this for named complex type
        Object elem = u.unmarshal(url.openStream());
        assertTrue("XSD invalid, see sysout", handler.eventOccurred());
    }
    
    
    // Empty string is schema-valid so deleting previous test in this place.


}
