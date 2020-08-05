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
package com.ibm.jbatch.jsl.util;

import java.net.URL;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.xml.sax.SAXException;

import com.ibm.jbatch.container.context.impl.JobContextImpl;

public class ValidatorHelper implements Constants {

    public final static String SCHEMA_LOCATION = "/xsd/jobXML_2_0.xsd";
    public final static String API_BUNDLE_SYMBOLIC_NAME = "jakarta.batch-api";
    private final static Class<ValidatorHelper> thisClass = ValidatorHelper.class;
	private final static Logger logger = Logger.getLogger(thisClass.getName());


    
    private static Schema schema = null;
    
    private static SchemaFactory sf = 
        SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);              

    /**
     * This method must be synchronized as SchemaFactory is not thread-safe
     */
    public static synchronized Schema getXJCLSchema() {
        if (schema == null) {
            try {
                URL url = thisClass.getResource(SCHEMA_LOCATION);
                logger.fine("After search via flat classpath for Job XML XSD file: " + SCHEMA_LOCATION + " url = " + url);
                if (url == null) {
                	// Try OSGI API
                	logger.fine("Job XML XSD file: " + SCHEMA_LOCATION + " not found via classpath, trying OSGI");
                	Bundle apiBundle = null;
                	for (Bundle b : FrameworkUtil.getBundle(thisClass).getBundleContext().getBundles()) {
                		if (b.getHeaders().get(BUNDLE_SYMBOLICNAME).equals(API_BUNDLE_SYMBOLIC_NAME)) {
                			if (matchesBatchAPIBundleVersion(b.getHeaders().get(BUNDLE_VERSION))) {
                				apiBundle = b;
                				logger.fine("Job XML XSD file: " + SCHEMA_LOCATION + " found in OSGi Bundle: " + apiBundle);
                				break;
                			} else {
                				// Right symbolic name, wrong version
                				logger.fine("Bundle: " + b + " found, but version doesn't match expected");
                			}
                		}
                	}
                	// Looped through all and didn't find any
                	if (apiBundle == null) {
                		String msg = "Jakarta Batch API bundle not found via OSGI bundle network.";
                		logger.severe(msg);
                		throw new IllegalStateException(msg);
                	}
                	url = apiBundle.getResource(SCHEMA_LOCATION);
                	if (url == null) {
                		String msg = "Job XML XSD file: " + SCHEMA_LOCATION + " not found in API bundle: "+ apiBundle;
                		logger.severe(msg);
                		throw new IllegalStateException(msg);
                	}
                	logger.fine("Search via OSGI bundle network for Job XML XSD file: " + SCHEMA_LOCATION + ", found url = " + url);
                }
                schema = sf.newSchema(url);
            } catch (SAXException e) {
                throw new RuntimeException(e);
            }
        }
        return schema;        
    }     

    private static boolean matchesBatchAPIBundleVersion(String bundleVersion) { 
    	return bundleVersion.startsWith("2.");
    }
}
