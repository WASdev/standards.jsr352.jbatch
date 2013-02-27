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
package com.ibm.jbatch.container.jsl.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.Properties;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;


import com.ibm.jbatch.container.jsl.ModelResolver;
import com.ibm.jbatch.jsl.model.Step;
import com.ibm.jbatch.jsl.util.ValidatorHelper;
import com.ibm.jbatch.jsl.util.JSLMerger;
import com.ibm.jbatch.jsl.util.JSLValidationEventHandler;

//FIXME: basically identical to JobModelResolverImpl
public class StepModelResolverImpl implements ModelResolver<Step> {

	@Override
	public Step resolveModel(String stepXML) {
        Step theStep = null;
        Step stepModel = unmarshalStepXML(stepXML);
        
        if(stepModel.getParent() != null) {
            //Resolve parent refs for inheritance --CT
        	Step parent = null;
        	try {
				parent = getStepInheritance(stepModel.getParent());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	if (parent != null) {
        		JSLMerger merger = new JSLMerger();
        		theStep = merger.mergeStep(parent, stepModel);
        	}
        }
        if (theStep == null) {
        	theStep = stepModel;
        }
        return theStep;
    }
	
	private Step getStepInheritance(String parentId) throws IOException {
		Step step = null;
		InputStream indexFileUrl = JobModelResolverImpl.class.getResourceAsStream("/META-INF/jobinheritance");
		
		if (indexFileUrl != null) {
			Properties index = new Properties();
			index.load(indexFileUrl);
			
			if (index.getProperty(parentId) != null) {
				URL parentUrl = StepModelResolverImpl.class.getResource(index.getProperty(parentId));
				String parentXml = readJobXML(parentUrl.getFile());
				
				step = resolveModel(parentXml);	
			}
		}
		
		return step;
	}
	
    private String readJobXML(String fileWithPath) throws FileNotFoundException, IOException {

        StringBuffer jobXMLBuffer = ( fileWithPath==null ? null : new StringBuffer() );
        if ( !(fileWithPath==null) ) {
            BufferedReader zin = new BufferedReader( new FileReader( new File(fileWithPath)));
            String input = zin.readLine();
            do {
                if (input != null) {
                    //jobXMLBuffer.append( input.trim() );
                	jobXMLBuffer.append(input);
                    input = zin.readLine();
                }
            } while (input!=null);
        }
        return ( jobXMLBuffer==null ? null : jobXMLBuffer.toString() );

    }

	@Override
	public Step resolveModel(Step t) {
		Step theStep = null;
        if(t.getParent() != null) {
            //Resolve parent refs for inheritance --CT
        	Step parent = null;
        	try {
				parent = getStepInheritance(t.getParent());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	if (parent != null) {
        		JSLMerger merger = new JSLMerger();
        		theStep = merger.mergeStep(parent, t);
        	}
        }
        if (theStep == null) {
        	theStep = t;
        }
		return theStep;
	}

    private Step unmarshalStepXML(String stepXML) {
        Object result = null;
        Step step = null;
        JSLValidationEventHandler handler = new JSLValidationEventHandler();
        try {
            JAXBContext ctx = JAXBContext.newInstance("com.ibm.jbatch.jsl.model");
            Unmarshaller u = ctx.createUnmarshaller();
            u.setSchema(ValidatorHelper.getXJCLSchema());

            u.setEventHandler(handler);

            // Use this for anonymous type
            //job = (Job)u.unmarshal(new StreamSource(new StringReader(jobXML)));

            // Use this for named complex type
            result = u.unmarshal(new StreamSource(new StringReader(stepXML)));

        } catch (JAXBException e) {
            throw new IllegalArgumentException("Exception unmarshalling jobXML", e);
        }

        if (handler.eventOccurred()) {
            throw new IllegalArgumentException("xJCL invalid per schema, see SysOut for now for details");
        }

        step = ((JAXBElement<Step>)result).getValue();
        
        return step;
    }

}
