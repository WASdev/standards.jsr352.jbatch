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

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;

public class JSLValidationEventHandler implements ValidationEventHandler {
    
    private boolean eventOccurred = false;
    
    public boolean handleEvent(ValidationEvent event) {
        System.out.println("\nMESSAGE: " + event.getMessage());
        System.out.println("\nSEVERITY: " + event.getSeverity());
        System.out.println("\nLINKED EXC: " + event.getLinkedException());
        System.out.println("\nLOCATOR INFO:\n------------");
        
        System.out.println("\n  COLUMN NUMBER:  " + event.getLocator().getColumnNumber());
        System.out.println("\n  LINE NUMBER:  " + event.getLocator().getLineNumber());
        System.out.println("\n  OFFSET:  " + event.getLocator().getOffset());
        System.out.println("\n  CLASS:  " + event.getLocator().getClass());
        System.out.println("\n  NODE:  " + event.getLocator().getNode());
        System.out.println("\n  OBJECT:  " + event.getLocator().getObject());
        System.out.println("\n  URL:  " + event.getLocator().getURL());
        
        eventOccurred = true;
        
        // Allow more parsing feedback 
        return true;
    }

    public boolean eventOccurred() {
        return eventOccurred;
    }

}
