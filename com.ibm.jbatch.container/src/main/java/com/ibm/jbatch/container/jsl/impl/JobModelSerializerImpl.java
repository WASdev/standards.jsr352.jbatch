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
package com.ibm.jbatch.container.jsl.impl;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;


import com.ibm.jbatch.container.jsl.ModelSerializer;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.jbatch.jsl.util.ValidatorHelper;
import com.ibm.jbatch.jsl.util.JSLValidationEventHandler;

import java.io.ByteArrayOutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class JobModelSerializerImpl implements ModelSerializer<JSLJob> {

	@Override
	public String serializeModel(JSLJob model) {
		
		final JSLJob finalModel = model;
		String serializedModel = AccessController.doPrivileged(
		    	 new PrivilegedAction<String>() {
		              public String run() {
		            	  return marshalJSLJob(finalModel);
		              }
		          });
		
		return serializedModel;
	}

    private String marshalJSLJob(JSLJob job) {
    	String resultXML = null;
    	JSLValidationEventHandler handler = new JSLValidationEventHandler();
    	try {
    		JAXBContext ctx = JAXBContext.newInstance("com.ibm.jbatch.jsl.model");
    		Marshaller m = ctx.createMarshaller();
    		m.setSchema(ValidatorHelper.getXJCLSchema());
    		m.setEventHandler(handler);
    		ByteArrayOutputStream baos = new ByteArrayOutputStream();
    		//m.marshal(job, baos);
    		/*
    		 * from scott: 
    		 */
    		m.marshal( new JAXBElement(
    				new QName("http://xmlns.jcp.org/xml/ns/javaee","job"), JSLJob.class, job ), baos);
    		resultXML = baos.toString();
    	} catch(Exception e){
    		throw new RuntimeException("Exception while marshalling JSLJob", e);
    	}
    	
    	return resultXML;
    }
    
}
