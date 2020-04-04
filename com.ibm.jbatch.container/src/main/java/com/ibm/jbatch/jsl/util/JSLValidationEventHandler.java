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

import java.util.logging.Logger;

import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.ValidationEventHandler;

public class JSLValidationEventHandler implements ValidationEventHandler {
    
	private static final String CLASSNAME = JSLValidationEventHandler.class.getName();

	private final static Logger logger = Logger.getLogger(CLASSNAME);
    
    private boolean eventOccurred = false;
    
    public boolean handleEvent(ValidationEvent event) {
    	StringBuffer buf = new StringBuffer(150);
        buf.append("\nMESSAGE: " + event.getMessage());
        buf.append("\nSEVERITY: " + event.getSeverity());
        buf.append("\nLINKED EXC: " + event.getLinkedException());
        buf.append("\nLOCATOR INFO:\n------------");
        
        buf.append("\n  COLUMN NUMBER:  " + event.getLocator().getColumnNumber());
        buf.append("\n  LINE NUMBER:  " + event.getLocator().getLineNumber());
        buf.append("\n  OFFSET:  " + event.getLocator().getOffset());
        buf.append("\n  CLASS:  " + event.getLocator().getClass());
        buf.append("\n  NODE:  " + event.getLocator().getNode());
        buf.append("\n  OBJECT:  " + event.getLocator().getObject());
        buf.append("\n  URL:  " + event.getLocator().getURL());
        
        logger.warning("JSL invalid per XSD, details: " + buf.toString());
        
        eventOccurred = true;
        
        // Allow more parsing feedback 
        return true;
    }

    public boolean eventOccurred() {
        return eventOccurred;
    }

}
