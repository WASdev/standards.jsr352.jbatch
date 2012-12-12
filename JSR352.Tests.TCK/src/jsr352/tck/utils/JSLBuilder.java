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
package jsr352.tck.utils;

import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import jsr352.batch.jsl.*;

/**
 * 
 * In general, we'll use this builder to create model objects with required properties,
 * and then return the created model object for the caller to add optional properties.
 * 
 * @author skurz
 *
 */
public class JSLBuilder {

    private final static Logger logger = Logger.getLogger(JSLBuilder.class.getName());

	private static QName JSL_ROOT = new QName("http://batch.jsr352/jsl", "job");
	private static String JOBID_DFLT = "job1";
	private static String STEPID_DFLT = "step1";
	
	private JSLJob job;
	private ObjectFactory factory = new ObjectFactory();
	
	public JSLBuilder() {
		job = createDefaultJob();		
	}
	
	public JSLJob getJob() {
		return job;
	}
	
	public String getJSL() {
		return marshal();
	}
	
	public JSLJob createDefaultJob() {		
		JSLJob job = factory.createJSLJob();
		job.setId(JOBID_DFLT);
		return job;
	}	
	
	/** 
	 * Creates a Decision and appends to the end of the Job
	 * 
	 * @param deciderId
	 * @param deciderArtifactId
	 * @return Decision added to job
	 */
	public Decision addDecision(String decisionId, String deciderArtifactId) {		
		Decision decision = factory.createDecision();
		decision.setId(decisionId);
		decision.setRef(deciderArtifactId);
		job.getExecutionElements().add(decision);
		return decision;
	}

	/*
	 * Creates a batchlet Step and appends to the end of the Job
	 */

	public Step addBatchletStep(String stepId, String batchletId) {
		Step step = factory.createStep();
		Batchlet batchlet = factory.createBatchlet();
		
		step.setId(stepId);
		step.setBatchlet(batchlet);

		batchlet.setRef(batchletId);
		job.getExecutionElements().add(step);
		
		return step;
	}
		
	public Fail addFail(Decision dec, String onPattern) {
		Fail fail = factory.createFail();
		dec.getControlElements().add(fail);
		fail.setOn(onPattern);
		return fail;
	}
	
	public End addEnd(Decision dec, String onPattern) {
		End end = factory.createEnd();
		dec.getControlElements().add(end);
		end.setOn(onPattern);
		return end;
	}
	
	public Stop addStop(Decision dec, String onPattern) {
		Stop stop = factory.createStop();
		dec.getControlElements().add(stop);
		stop.setOn(onPattern);
		return stop;
	}
	
	public Next addNext(Decision dec, String onPattern, String toVal) {
		Next next = factory.createNext();
		dec.getControlElements().add(next);
		next.setOn(onPattern);
		next.setTo(toVal);
		return next;
	}

	/**
	 * @param Property name
	 * @return <property name="<name>" value="#{jobParameters['<name>']}"/>  
	 */
	public Property createUnattachedVariableProperty(String name) {
		Property prop = factory.createProperty();
		prop.setName(name);
		//only handle job level parameters for new spring style properties 
		prop.setValue("#{jobParameters['" + name + "']}");
		return prop;		
	}
	
	public Property createUnattachedProperty(String name, String value) {
		Property prop = factory.createProperty();
		prop.setName(name);
		prop.setValue(value);
		return prop;		
	}
	
	public JSLProperties createSingleProperty(String name, String value) {
		JSLProperties props = factory.createJSLProperties();
		Property prop = createUnattachedProperty(name, value);
		props.getPropertyList().add(prop);
		return props;	
	}
	
	public JSLProperties createSingleVariableProperty(String name) {
		JSLProperties props = factory.createJSLProperties();
		Property prop = createUnattachedVariableProperty(name);
		props.getPropertyList().add(prop);
		return props;	
	}
	
	public Property createNextProperty(JSLProperties props, String name, String value) {
		Property prop = createUnattachedProperty(name, value);
		props.getPropertyList().add(prop);
		return prop;
	}
	
	public Property createNextVariableProperty(JSLProperties props, String name) {
		Property prop = createUnattachedVariableProperty(name);
		props.getPropertyList().add(prop);
		return prop;
	}
	
	public Listener createSingleJobListener(String listenerId) {
		Listeners jobListeners = factory.createListeners();			
		Listener jobListener = factory.createListener();
		jobListener.setRef(listenerId);
		jobListeners.getListenerList().add(jobListener);
		job.setListeners(jobListeners);		
		return jobListener;
	}

	private String marshal() {
		StringWriter sw = new StringWriter();
		try {
			JAXBContext ctx = JAXBContext.newInstance("jsr352.batch.jsl");
			Marshaller m = ctx.createMarshaller();
		
			JAXBElement<JSLJob> elem = 
				new JAXBElement<JSLJob>(JSL_ROOT, JSLJob.class, job);
			m.marshal(elem, sw);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
		
		if (logger.isLoggable(Level.FINER)) {
			logger.finer("Dumping generated JSL:\n" + 
					    "----  begin JSL ----------\n" +
					    sw.toString() +
					    "\n----  end JSL ----------");
		}
		
		return sw.toString();
	}

}

