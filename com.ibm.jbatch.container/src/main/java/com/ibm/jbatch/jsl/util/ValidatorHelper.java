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

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

public class ValidatorHelper {

    // Unofficially packaged in this RI
    public final static String SCHEMA_LOCATION = "xsd/jobXML_2_0.xsd";
    
    private static Schema schema = null;
    
    private static SchemaFactory sf = 
        SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);              

    /**
     * This method must be synchronized as SchemaFactory is not thread-safe
     */
    public static synchronized Schema getXJCLSchema() {
        if (schema == null) {
            try {
                URL url = ValidatorHelper.class.getResource("/" + SCHEMA_LOCATION);
                schema = sf.newSchema(url);
            } catch (SAXException e) {
                throw new RuntimeException(e);
            }
        }
        return schema;        
    }     
}
