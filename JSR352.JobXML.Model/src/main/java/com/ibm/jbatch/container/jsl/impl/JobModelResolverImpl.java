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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Properties;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;


import com.ibm.jbatch.container.jsl.ModelResolver;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.jbatch.jsl.model.Step;
import com.ibm.jbatch.jsl.util.ValidatorHelper;
import com.ibm.jbatch.jsl.util.JSLValidationEventHandler;

public class JobModelResolverImpl implements ModelResolver<JSLJob> {
    
    public JobModelResolverImpl() {
        super();
    }    
    
    private JSLJob unmarshalJobXML(String jobXML) {
        Object result = null;
        JSLJob job = null;
        JSLValidationEventHandler handler = new JSLValidationEventHandler();
        try {
            JAXBContext ctx = JAXBContext.newInstance("com.ibm.jbatch.jsl.model");
            Unmarshaller u = ctx.createUnmarshaller();
            u.setSchema(ValidatorHelper.getXJCLSchema());

            u.setEventHandler(handler);

            // Use this for anonymous type
            //job = (Job)u.unmarshal(new StreamSource(new StringReader(jobXML)));

            // Use this for named complex type
            result = u.unmarshal(new StreamSource(new StringReader(jobXML)));

        } catch (JAXBException e) {
            throw new IllegalArgumentException("Exception unmarshalling jobXML", e);
        }

        if (handler.eventOccurred()) {
            throw new IllegalArgumentException("xJCL invalid per schema, see SysOut for now for details");
        }

        job = ((JAXBElement<JSLJob>)result).getValue();
        
        return job;
    }
    
	private JSLJob getJslJobInheritance(String jobId) throws IOException {
		
		JSLJob jslJob = null;
		InputStream indexFileUrl = JobModelResolverImpl.class.getResourceAsStream("/META-INF/jobinheritance");
		
		if (indexFileUrl != null) {
			Properties index = new Properties();
			index.load(indexFileUrl);
			
			if (index.getProperty(jobId) != null) {
				URL parentUrl = JobModelResolverImpl.class.getResource(index.getProperty(jobId));
				String parentXml = readJobXML(parentUrl.getFile());
			
				jslJob = resolveModel(parentXml);
			}
		}
		return jslJob;
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
    public JSLJob resolveModel(String jobXML) {        
    	
    	final String finalJobXML = jobXML;
    	JSLJob jslJob = AccessController.doPrivileged(
    	
		          new PrivilegedAction<JSLJob>() {
		              public JSLJob run() {
		            	  return  unmarshalJobXML(finalJobXML);
		              }
		          });
        
        return jslJob;
    }


    // FIXME These maps need to move to the xJCL Repository
    private static HashMap<String, JSLJob> jobid2InstanceMap = new HashMap<String, JSLJob>();
    private static HashMap<String, Step> stepid2InstanceMap = new HashMap<String, Step>();
    
	@Override
	public JSLJob resolveModel(JSLJob t) {
		// TODO Auto-generated method stub
		// was this intended for inheritance?
		return null;
	}

    // FIXME HashMap<String, Split> splitid2InstanceMap = new HashMap<String,Split>();
    // FIXME HashMap<String, Flow> flowid2InstanceMap = new HashMap<String,Flow>();

    //
    // This is where we will implement job/step inheritance, though we don't at
    // the moment.
    //
    /*
    public static ResolvedJob resolveJob(Job job) {
        ArrayList<ResolvedStep> steps = new ArrayList<ResolvedStep>();
        ArrayList<ResolvedDecision> decisions = new ArrayList<ResolvedDecision>();
        ArrayList<ResolvedSplit> splits = new ArrayList<ResolvedSplit>();
        ArrayList<ResolvedFlow> flows = new ArrayList<ResolvedFlow>();
        
        ResolvedJob resolvedJob = new ResolvedJob(job.getId(), steps, decisions, splits, flows);
        
        for (Object next : job.getControlElements()) {
            if (next instanceof Step) {
                steps.add(new ResolvedStep(resolvedJob, (Step) next));
            } else if (next instanceof Decision) {
                decisions.add(new ResolvedDecision(resolvedJob, (Decision) next));
            } else if (next instanceof Split) {
                splits.add(new ResolvedSplit(resolvedJob, (Split) next));
            } else if (next instanceof Flow) {
                flows.add(new ResolvedFlow(resolvedJob, (Flow) next));
            }
        }
        
        return resolvedJob;
    }

    
    //FIXME We started implementing job inheritance here. Set to private so no one uses this yet.
    private static ResolvedJob resolveModel(Job leafJob) {
        String parentID = leafJob.getParent();

        Job resolvedJob = resolveModel(leafJob, parentID);
        // FIXME you need to create a new ResolvedJob here.
        return null;

    }

    private static Job resolveModel(Job leafJob, String parentID) {

        if (!parentID.equals("")) {
            Job parentJob = jobid2InstanceMap.get(parentID);
            if (parentJob == null) {
                throw new BatchContainerRuntimeException(new IllegalArgumentException(), "The parent job id '" + parentID + "' on Job id '"
                        + leafJob.getParent() + " cannot be found");
            }

            // add all the attributes, steps, flows, and splits from the parent
            // to child if they don't exist on child            
            leafJob.getControlElements().addAll(parentJob.getControlElements());

            return resolveModel(leafJob, parentJob.getParent());

        }

        for (Object next : leafJob.getControlElements()) {
            if (next instanceof Step) {
                resolveModel((Step)next);
            } else if (next instanceof Split) {
                //resolveModel((Split)next);
            } else if (next instanceof Flow) {
                //resolveModel((Flow)next);
            }

        }

        return leafJob;

    }

    //FIXME Set to private so no one uses this yet.
    private static ResolvedStep resolveModel(Step leafStep) {

        String parentID = leafStep.getParent();

        Step resolvedStep = resolveModel(leafStep, parentID);

        // FIXME you need to clone the step to a resolved step
        return null;

    }

    private static Step resolveModel(Step leafStep, String parentID) {
        if (!parentID.equals("")) {
            Step parentStep = stepid2InstanceMap.get(parentID);
            if (parentStep == null) {
                throw new BatchContainerRuntimeException(new IllegalArgumentException(), "The parent step id '" + parentID
                        + "' on Step id '" + leafStep.getParent() + " cannot be found");
            }

            // add all the attributes, batchlets, chunks...etc from a parent
            // step if they don't
            // exist on the child step
            // leafStep.getXXX().addAll(parentStep.getXXX());

            return resolveModel(leafStep, parentStep.getParent());

        }

        // batchlet
        //

        // resolve chunks

        // next, startlimit ...etc

        // FIXME ...

        return leafStep;
    }
    */
    
}
