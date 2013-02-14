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

import static org.junit.Assert.assertEquals;

import java.net.URL;

import jsr352.batch.jsl.JSLJob;
import jsr352.batch.jsl.Property;

import org.junit.Ignore;
import org.junit.Test;

import test.utils.IOHelper;

import com.ibm.batch.container.modelresolver.PropertyResolver;
import com.ibm.batch.container.modelresolver.PropertyResolverFactory;
import com.ibm.batch.container.xjcl.ModelResolver;
import com.ibm.batch.container.xjcl.ModelResolverFactory;

public class PropertyTests {
    
    @Test
    public void testXMLPropertySubstitution1() throws Exception {
        URL jobXMLURL = this.getClass().getResource("/job_properties1.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());

        ModelResolver<JSLJob> jobResolver = ModelResolverFactory.createJobResolver();
        JSLJob jobModel = jobResolver.resolveModel(jobXML);
        
        //Resolve the properties for this job
        PropertyResolver<JSLJob> propResolver = PropertyResolverFactory.createJobPropertyResolver(false);
        propResolver.substituteProperties(jobModel);  

        String propValue = null;

        for (Property prop :jobModel.getProperties().getPropertyList()){
            if(prop.getName().equals("myprop2")){
                propValue = prop.getValue();
            }
        }

        assertEquals("step2", propValue);
    }
    
    @Test
    public void testXMLPropertySubstitution2() throws Exception {
        URL jobXMLURL = this.getClass().getResource("/job_properties2.xml");
        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());

        ModelResolver<JSLJob> resolver = ModelResolverFactory.createJobResolver();        
        JSLJob jobModel = resolver.resolveModel(jobXML);
        
        //Resolve the properties for this job
        PropertyResolver<JSLJob> propResolver = PropertyResolverFactory.createJobPropertyResolver(false);
        propResolver.substituteProperties(jobModel);  
        
        String propValue = null;

        for (Property prop :jobModel.getProperties().getPropertyList()){
            if(prop.getName().equals("myprop2")){
                propValue = prop.getValue();
            }
        }

        assertEquals("step2", propValue);
    }
    

    /*
     * Left this in here @Ignore(d)... not sure if the intent was to come back to it or forget about it.
     */
    @Ignore
    public void testPropertyPrecedenceSubmitted() throws Exception {
        URL jobXMLURL = this.getClass().getResource("/job_properties2.xml");

        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());

        ModelResolver<JSLJob> resolver = ModelResolverFactory.createJobResolver();
        JSLJob jobModel = resolver.resolveModel(jobXML);
    }
    
    /*
     * Left this in here @Ignore(d)... not sure if the intent was to come back to it or forget about it.
     */
    @Ignore
    public void testNestedPropertyInXML() throws Exception {
        URL jobXMLURL = this.getClass().getResource("/job_properties2.xml");

        String jobXML = IOHelper.readJobXML(jobXMLURL.getFile());

        ModelResolver<JSLJob> resolver = ModelResolverFactory.createJobResolver();
        JSLJob jobModel = resolver.resolveModel(jobXML);
    }
}
