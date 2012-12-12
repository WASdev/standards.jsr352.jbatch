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
package com.ibm.batch.container.xjcl.impl;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import jsr352.batch.jsl.JSLJob;

import com.ibm.batch.container.xjcl.ModelSerializer;
import com.ibm.batch.xjcl.ValidatorHelper;
import com.ibm.batch.xjcl.XJCLValidationEventHandler;
import java.io.ByteArrayOutputStream;

public class JobModelSerializerImpl implements ModelSerializer<JSLJob> {

	@Override
	public String serializeModel(JSLJob model) {
		return marshalJSLJob(model);
	}

    private String marshalJSLJob(JSLJob job) {
    	String resultXML = null;
    	XJCLValidationEventHandler handler = new XJCLValidationEventHandler();
    	try {
    		JAXBContext ctx = JAXBContext.newInstance("jsr352.batch.jsl");
    		Marshaller m = ctx.createMarshaller();
    		m.setSchema(ValidatorHelper.getXJCLSchema());
    		m.setEventHandler(handler);
    		ByteArrayOutputStream baos = new ByteArrayOutputStream();
    		//m.marshal(job, baos);
    		/*
    		 * from scott: 
    		 */
    		m.marshal( new JAXBElement(
    				new QName("http://batch.jsr352/jsl","job"), JSLJob.class, job ), baos);
    		resultXML = baos.toString();
    	}
    	catch(Exception e){
    		throw new RuntimeException("Exception while marshalling JSLJob", e);
    	}
    	
    	return resultXML;
    }
    
}
