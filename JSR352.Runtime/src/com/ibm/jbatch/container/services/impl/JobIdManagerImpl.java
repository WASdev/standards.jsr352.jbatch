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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.spi.services.IBatchConfig;
import com.ibm.jbatch.spi.services.IJobIdManagementService;

public class JobIdManagerImpl implements IJobIdManagementService {
	private static final String CLASSNAME = JobIdManagerImpl.class.getName();

	protected String rootDir;
	private static final String JOBID_FILE_NAME = "jobId.dat";
	private static Logger logger = Logger.getLogger(JobIdManagerImpl.class.getPackage().getName());
	
	public void init(IBatchConfig batchConfig)
			throws BatchContainerServiceException {
		rootDir = System.getProperty("user.home");
	}
	public void shutdown() throws BatchContainerServiceException
	{
	
	}
	
    @Override
    public long getExecutionId() {
        return getId();
    }
    @Override
    public long getInstanceId() {
        return getId();
    }
    
    @Override
    public long getStepExecutionId() {
    	return getId();
    }
    
    private synchronized String getJobIdFromStorage() {
    	StringBuilder contents = new StringBuilder("0");
    	File jobIdFile = new File(rootDir + File.separator + JOBID_FILE_NAME);  
		if(jobIdFile.exists())
		{
				// Read the file
				try {
				  
				  BufferedReader input = 
					new BufferedReader(new FileReader(rootDir + File.separator + JOBID_FILE_NAME));
				  try {
					String line = null; 
					while (( line = input.readLine()) != null){
					  contents.append(line);
					  break;
					}
				  }
				  finally {
					input.close();
				  }
				}
				catch (IOException ex){
				  ex.printStackTrace();
				  throw new RuntimeException(ex);
				}
		}
		
		return contents.toString();
    }
    
    private synchronized void saveJobIdToStorage(long currentId) {
    	File jobIdFile = new File(rootDir + File.separator + JOBID_FILE_NAME); 
		try {

			BufferedWriter output = new BufferedWriter(new FileWriter(jobIdFile));
			
			try {
				output.write(String.valueOf(currentId) );	
			} finally {
				output.close();
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
    }
    
	private long getId() {
		
		String id = getJobIdFromStorage();
		
		long jobId = Long.valueOf(id);
	
		saveJobIdToStorage(++jobId);
	
		return jobId;
	}

}
