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

import java.util.ArrayList;
import java.util.List;


import com.ibm.jbatch.container.jsl.ExecutionElement;
import com.ibm.jbatch.container.jsl.ModelResolver;
import com.ibm.jbatch.container.jsl.ModelResolverFactory;
import com.ibm.jbatch.container.jsl.ModelSerializer;
import com.ibm.jbatch.container.jsl.ModelSerializerFactory;
import com.ibm.jbatch.jsl.model.Chunk;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.jbatch.jsl.model.JSLProperties;
import com.ibm.jbatch.jsl.model.Listener;
import com.ibm.jbatch.jsl.model.Listeners;
import com.ibm.jbatch.jsl.model.ObjectFactory;
import com.ibm.jbatch.jsl.model.Property;
import com.ibm.jbatch.jsl.model.Step;

public class JSLMerger {
	
	public String mergeStep(String childJobXml, String parentStepXml, String childStepID) {
		JSLJob childJob = jobResolveHelper(childJobXml);
		Step parentStep = stepResolveHelper(parentStepXml);
		
		for(ExecutionElement elem : childJob.getExecutionElements()) {
			if(elem instanceof Step && parentStep.getId().equals(((Step)elem).getParent())) {
				childJob.getExecutionElements().remove(elem);
				childJob.getExecutionElements().add(mergeStep(parentStep, (Step)elem));
			}
		}
		
		ModelSerializer<JSLJob> serializer = ModelSerializerFactory.createJobModelSerializer();
		return serializer.serializeModel(childJob);		
	}
	
	public String mergeJob(String parentJobXML, String childJobXML) {
		JSLJob parentJob = jobResolveHelper(parentJobXML);
		JSLJob childJob = jobResolveHelper(childJobXML);

		//job merges from job
		if(parentJob != null && childJob != null) {
			JSLJob mergedJob = mergeJob(parentJob, childJob);
			ModelSerializer<JSLJob> serializer = ModelSerializerFactory.createJobModelSerializer();
			return serializer.serializeModel(mergedJob);
		}
		
		return childJobXML;
	}

	// Helper method to simplify path logic in main merge method
	public JSLJob jobResolveHelper(String jobXML) {
		ModelResolver<JSLJob> jobResolver = ModelResolverFactory.createJobResolver();
		JSLJob job = null;
		try {
			job = jobResolver.resolveModel(jobXML);
		} catch(ClassCastException cce) {
			//NOP, this is not a JSLJob, return null
		}
		return job;
	}

	// Helper method to simplify path logic in main merge method
	public Step stepResolveHelper(String stepXML) {
		ModelResolver<Step> stepResolver = ModelResolverFactory.createStepResolver();
		Step step = null;
		try {
			step = stepResolver.resolveModel(stepXML);
		} catch(ClassCastException cce) {
			//NOP, this is not a Step, return null
		}
		return step;
	}


	/**
	 *  
	 * @param parent the parent Step
	 * @param child the child Step
	 * @return the resulting merged Step
	 */
	public Step mergeStep(Step parent, Step child) {
		//if child doesn't link to parent, result is just the original child doc
		if(!parent.getId().equals(child.getParent())) {
			return child;			
		}

		//initialize new model object
		ObjectFactory factory = new ObjectFactory();
		Step merge = factory.createStep();
		merge.setId(child.getId());

		//merge lists
		merge.setProperties(mergeProperties(parent.getProperties(), child.getProperties()));
		merge.setListeners(mergeListeners(parent.getListeners(), child.getListeners()));

		merge.getControlElements().addAll(parent.getControlElements());
		
		merge.setChunk(mergeChunk(parent.getChunk(), child.getChunk()));
				
		//merge batchlet properties
		if(child.getBatchlet() != null && parent.getBatchlet() != null) {
			if("true".equals(child.getBatchlet().getProperties().getMerge())) {
				
			}
			merge.setBatchlet(parent.getBatchlet());
		}
		
		return merge;
	}
	
	public Chunk mergeChunk(Chunk parentChunk, Chunk childChunk) {
		if(parentChunk == null && childChunk != null) return childChunk;
		if(parentChunk != null && childChunk == null) return parentChunk;
		if(parentChunk == null && childChunk == null) return null;
		
		Chunk merge = (new ObjectFactory()).createChunk();
		
		//merge chunks - 8 attributes
		merge.setCheckpointPolicy( (childChunk.getCheckpointPolicy() == null
				? parentChunk.getCheckpointPolicy()
				: childChunk.getCheckpointPolicy()));
		merge.setItemCount( (childChunk.getItemCount() == null
				? parentChunk.getItemCount()
				: childChunk.getItemCount()));
		merge.setRetryLimit( (childChunk.getRetryLimit() == null
				? parentChunk.getRetryLimit()
				: childChunk.getRetryLimit()));
		merge.setSkipLimit( (childChunk.getRetryLimit() == null
				? parentChunk.getSkipLimit()
				: childChunk.getSkipLimit()));

		
		
		//merge chunks -  8 elements
		merge.setCheckpointAlgorithm( (childChunk.getCheckpointAlgorithm() == null
				? parentChunk.getCheckpointAlgorithm()
				: childChunk.getCheckpointAlgorithm()));
        merge.setProcessor( (childChunk.getProcessor() == null
                ? parentChunk.getProcessor()
                : childChunk.getProcessor()));
        merge.setReader( (childChunk.getReader() == null
                ? parentChunk.getReader()
                : childChunk.getReader()));
        merge.setWriter( (childChunk.getWriter() == null
                ? parentChunk.getWriter()
                : childChunk.getWriter()));
		merge.setSkippableExceptionClasses( (childChunk.getSkippableExceptionClasses() == null
				? parentChunk.getSkippableExceptionClasses()
				: childChunk.getSkippableExceptionClasses()));
		merge.setRetryableExceptionClasses( (childChunk.getRetryableExceptionClasses() == null
				? parentChunk.getRetryableExceptionClasses()
				: childChunk.getRetryableExceptionClasses()));
		merge.setNoRollbackExceptionClasses( (childChunk.getNoRollbackExceptionClasses() == null
				? parentChunk.getNoRollbackExceptionClasses()
				: childChunk.getNoRollbackExceptionClasses()));
		
		return merge;
	}
	
	/**
	 * 
	 * @param parent the parent Job
	 * @param child the child Job
	 * @return the resulting merged job
	 */
	public JSLJob mergeJob(JSLJob parent, JSLJob child) {
		
		//if child doesn't link to parent, result is just the original child doc
		if(!parent.getId().equals(child.getParent())) {
			return child;			
		}
		
		//initialize new model object
		ObjectFactory factory = new ObjectFactory();
		JSLJob merge = factory.createJSLJob();
		merge.setId(child.getId());

		//TODO: no merge attribute, what default behavior to have?
		//merge lists
		merge.setProperties(mergeProperties(parent.getProperties(), child.getProperties()));
		merge.setListeners(mergeListeners(parent.getListeners(), child.getListeners()));

		List<ExecutionElement> mergedExecutionElements = mergeExecutionElements(parent.getExecutionElements(), child.getExecutionElements());
		merge.getExecutionElements().addAll(mergedExecutionElements);

		return merge;
	}
	
	private List<ExecutionElement> mergeExecutionElements(List<ExecutionElement> parent, List<ExecutionElement> child) {
		List<ExecutionElement> merged = new ArrayList<ExecutionElement>();
		
		// 5.8 Job XML Inheritance 2.1 the child's step element overrides the parent's
		merged.addAll(child);
		
		// loop the parent and add any element that does not exists in child list
		for(ExecutionElement parentElement : parent) {
			ExecutionElement theElement = null;
			for(ExecutionElement cElem : child) {
				if(!parentElement.getId().equals(cElem.getId())) { 
					merged.add(parentElement);
					break;
				}
			}
		}
		
		return resolveSteps(merged);
	}
	
	private List<ExecutionElement> resolveSteps(List<ExecutionElement> executionElements) {
		List<ExecutionElement> resolved = new ArrayList<ExecutionElement>();
		
		for (ExecutionElement executionElement : executionElements) {
			if (executionElement instanceof Step) {
				resolved.add(ModelResolverFactory.createStepResolver().resolveModel((Step)executionElement));
			} else {
				resolved.add(executionElement);
			}
		}
		return resolved;
	}
	
	public JSLJob mergeJobSteps(JSLJob jslJob) {
		//initialize new model object
		ObjectFactory factory = new ObjectFactory();
		JSLJob merge = factory.createJSLJob();
		merge.setId(jslJob.getId());

		merge.setProperties(jslJob.getProperties());
		merge.setListeners(jslJob.getListeners());

		for(ExecutionElement elem : jslJob.getExecutionElements()) {
			
			ExecutionElement theElement = null;
			// step inheritance
			if (elem instanceof Step) {
				theElement = ModelResolverFactory.createStepResolver().resolveModel((Step)elem);
			} else { 
				theElement = elem;
			}
			
			merge.getExecutionElements().add(theElement);

		}

		return merge;
	}
	
	
	/**
	 * Merge two properties lists by combining all <property> elements from parent and child.
	 * If child is null it will be instantiated, possibly empty.
	 * @param parentProps
	 * @param childProps
	 * @return a new merged JSLProperties object containing parent and child properties
	 */
	private JSLProperties mergeProperties(JSLProperties parentProps, JSLProperties childProps) {
		if(parentProps == null) return childProps;
		if(childProps == null) childProps = new JSLProperties();

		//for merge=false, child properties list takes precedence and is unchanged
		if("false".equals(childProps.getMerge())) {
			childProps.setMerge(null);
			return childProps;
		}
		
		JSLProperties mergedProps = new JSLProperties();
		mergedProps.getPropertyList().addAll(childProps.getPropertyList());
		//for each parent property, add it only if it does not exist in the child
		//i.e.; intersecting properties are overridden by the child
		for(Property parentProp : parentProps.getPropertyList()) {
			boolean foundParentProp = false;
			for(Property childProp : childProps.getPropertyList()) {
				if(parentProp.getName() != null && parentProp.getName().equals(childProp.getName()))
					foundParentProp = true;
			}
			if(!foundParentProp)
				mergedProps.getPropertyList().add(parentProp);
		}
		
		return mergedProps;
	}
	
	/**
	 * Merge two listeners lists by combining all <listeners> elements from parent and child.
	 * If child is null it will be instantiated, possibly empty
	 * @param parentListeners
	 * @param childListeners is modified
	 * @return a new merged Listeners object containing parent and child listeners
	 */
	private Listeners mergeListeners(Listeners parentListeners, Listeners childListeners) {
		if(parentListeners == null) return childListeners;
		if(childListeners == null) childListeners = new Listeners();
		
		//for merge=false, child properties list takes precedence and is unchanged
		if("false".equals(childListeners.getMerge())) {
			childListeners.setMerge(null);
			return childListeners;
		}

		Listeners mergedListeners = new Listeners();
		mergedListeners.getListenerList().addAll(childListeners.getListenerList());
		//for each parent property, add it only if it does not exist in the child
		//i.e.; intersecting properties are overridden by the child
		for(Listener parentListener : parentListeners.getListenerList()) {
			boolean foundParentListener = false;
			for(Listener childListener : childListeners.getListenerList()) {
				if(parentListener.getRef() != null && parentListener.getRef().equals(childListener.getRef()))
					foundParentListener = true;
			}
			if(!foundParentListener)
				mergedListeners.getListenerList().add(parentListener);
		}
		return mergedListeners;
	}

}
