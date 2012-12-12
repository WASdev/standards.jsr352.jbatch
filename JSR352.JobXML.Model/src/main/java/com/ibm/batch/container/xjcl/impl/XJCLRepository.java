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
package com.ibm.batch.container.xjcl.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jsr352.batch.jsl.Flow;
import jsr352.batch.jsl.JSLJob;
import jsr352.batch.jsl.Split;
import jsr352.batch.jsl.Step;

import com.ibm.batch.container.xjcl.ModelResolverFactory;
import com.ibm.batch.container.xjcl.XJCLLoader;


/**
 * This class is just a bunch of maps from xml id's to JAXB instances of the XJCL elements. 
 * @author Kaushik
 *
 */
public class XJCLRepository {

	//FIXME we might still need to handle concurrent access to these maps 
	
	private final static Map<String, JSLJob> jobMap = new HashMap<String, JSLJob>();
	
	private final static Map<String, Split> splitMap = new HashMap<String, Split>();
	
	private final static Map<String, Flow> flowMap = new HashMap<String, Flow>();
	
	private final static Map<String, Step> stepMap = new HashMap<String, Step>();

	
	public static Map<String, JSLJob> getJobMap() {
		return jobMap;
	}

	public static Map<String, Split> getSplitMap() {
		return splitMap;
	}

	public static Map<String, Flow> getFlowMap() {
		return flowMap;
	}

	public static Map<String, Step> getStepMap() {
		return stepMap;
	}
	
	private class DummyXJCLLoader implements XJCLLoader {

		@Override public String loadXJCL(String id) { return null; }

		@Override
		public String loadJob(String id) {
			//hardcoded link to inheritance XMLs
			
			//skurz - commented this out to see if we really needed it
			//new File("C:/workspaces/eclipse/JSR352.Tests.TCK/testJobXml/inheritance/"+id);
			File jobFile = null;
			
			//TODO: what would really happen is to drill down on a specific 
			BufferedReader br = null;
			StringBuffer sb = new StringBuffer();
			try {
				br = new BufferedReader(new FileReader(jobFile));
				String line = br.readLine();
				while(line != null) {
					sb.append(line);
					line = br.readLine();
				}
			}
			catch(FileNotFoundException fnfe){
				throw new RuntimeException(fnfe);
			}
			catch(IOException ioe) {
				throw new RuntimeException(ioe);
			}
			finally {
				try {
					if(br != null) br.close();
				} catch(IOException ioe){}
			}
			return sb.toString();
		}

		@Override public String loadSplit(String id) { return null; }

		@Override public String loadFlow(String id) { return null; }

		@Override public String loadStep(String id) { return null; }
		
	}
	
	//TODO: reentrant
	public static JSLJob getJobID(String jobID) {
		JSLJob job = jobMap.get(jobID);
		if(job == null) {
			XJCLRepository repo = new XJCLRepository();
			String jobXML = repo.new DummyXJCLLoader().loadJob(jobID);
			job = ModelResolverFactory.createJobResolver().resolveModel(jobXML);
			jobMap.put(jobID, job);
		}
		return job;
	}
	
	
}
