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
package com.ibm.jbatch.container.services.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;

import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.spi.services.IBatchConfig;
import com.ibm.jbatch.spi.services.IJobXMLLoaderService;


public class DirectoryJobXMLLoaderServiceImpl implements IJobXMLLoaderService {

    
    private final static Logger logger = Logger.getLogger(DelegatingBatchArtifactFactoryImpl.class.getName());
    private final static String CLASSNAME = DirectoryJobXMLLoaderServiceImpl.class.getName();
    
    public static final String JOB_XML_DIR_PROP = "com.ibm.jbatch.jsl.directory";
    public static final String JOB_XML_PATH = System.getProperty(JOB_XML_DIR_PROP);
    
    
    @Override
	public String loadJSL(String id) {
	    
        
        String jobXML = loadJobFromDirectory(JOB_XML_PATH, id);
	    
	    return jobXML;
	    
	}


	private static String loadJobFromDirectory(String dir, String id) {

        File jobXMLFile = new File (JOB_XML_PATH, id + ".xml");
	    
	    String xmlString = readJobXML(jobXMLFile);
	    
	    return xmlString;
	    
    }

	
    private static String readJobXML(File fileWithPath)  {

        StringBuffer xmlBuffer = ( fileWithPath==null ? null : new StringBuffer() );
        try {
        if ( !(fileWithPath==null) ) {
            BufferedReader in = new BufferedReader(new FileReader(fileWithPath));
            String input = in.readLine();
            do {
                if (input != null) {
                    xmlBuffer.append(input);
                    input = in.readLine();
                }
            } while (input!=null);
        }
        } catch (FileNotFoundException e) {
            throw new BatchContainerServiceException("Could not find file " + fileWithPath);
        } catch (IOException e) {
            throw new BatchContainerServiceException(e);
        }
        
        return ( xmlBuffer==null ? null : xmlBuffer.toString() );

    }


    @Override
    public void init(IBatchConfig batchConfig) throws BatchContainerServiceException {
        // TODO Auto-generated method stub
        
    }


    @Override
    public void shutdown() throws BatchContainerServiceException {
        // TODO Auto-generated method stub
        
    }
	
}
