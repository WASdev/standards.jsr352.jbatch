/**
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

import jsr352.batch.jsl.Flow;
import jsr352.batch.jsl.JSLJob;
import jsr352.batch.jsl.JSLProperties;
import jsr352.batch.jsl.Listeners;
import jsr352.batch.jsl.Split;
import jsr352.batch.jsl.Step;

import com.ibm.batch.container.xjcl.ExecutionElement;

public class JobMergeHelper {
	
	/**
	 * Merge <job>s through a parent attribute reference.
	 * @param parent the <job> referred to by the child
	 * @param child the <job> with a parent reference, this object will be modified
	 */
	public void mergeParentJob(JSLJob parent, JSLJob child) {
		//assert child really links to parent
		if(!parent.getId().equals(child.getParent())) return;
		
		//TODO: no merge attribute, what default behavior to have?
		//merge lists
		child.setProperties(mergeProperties(parent.getProperties(), child.getProperties()));
		child.setListeners(mergeListeners(parent.getListeners(), child.getListeners()));
		
		//add in any elements we don't already have
		//n^2 performance. how big do we expect a JSL to be?
		for(ExecutionElement pElem : parent.getExecutionElements()) {
			boolean hasCollidingId = false;
			ExecutionElement collidingChildElement = null;
			for(ExecutionElement cElem : child.getExecutionElements()) {
				if(pElem.getId().equals(cElem.getId())) { //id is required attribute, not null
					hasCollidingId = true;
					collidingChildElement = cElem;
				}
			}
			if(hasCollidingId) {
				//figure out how to merge the offending element
				//what if same id on different element types?
				if(pElem instanceof Flow ) mergeCollidingFlow( pElem, collidingChildElement);
				if(pElem instanceof Step ) mergeCollidingStep( pElem, collidingChildElement);
				if(pElem instanceof Split) mergeCollidingSplit(pElem, collidingChildElement);
			} else {
				child.getExecutionElements().add(pElem);
			}
		}
	}
	
	/**
	 * Merge two properties lists by combining all <property> elements from parent into child.
	 * If child is null it will be instantiated, possibly empty.
	 * @param parentProps
	 * @param childProps is modified
	 * @return the modified childProps, or a new merged JSLProperties object if childProps
	 * was null.
	 */
	private JSLProperties mergeProperties(JSLProperties parentProps, JSLProperties childProps) {
		if(parentProps == null) return childProps;
		if(childProps == null) childProps = new JSLProperties();
		childProps.getPropertyList().addAll(parentProps.getPropertyList());
		//TODO: if we have to return anyway, why modify childProps at all? would be clearer
		//if we were forced to use the return value
		return childProps;
	}
	
	/**
	 * Merge two listeners lists by combining all <listeners> elements from parent into child.
	 * If child is null it will be instantiated, possibly empty/
	 * @param parentListeners
	 * @param childListeners is modified
	 * @return the modified childListeners, or a new merged Listeners object if childListeners
	 * was null. 
	 */
	private Listeners mergeListeners(Listeners parentListeners, Listeners childListeners) {
		if(parentListeners == null) return childListeners;
		if(childListeners == null) childListeners = new Listeners();
		childListeners.getListenerList().addAll(parentListeners.getListenerList());
		return childListeners;
	}
	
	public void mergeCollidingFlow(ExecutionElement parent, ExecutionElement child){}
	
	/**
	 * Merge a parent <step> that has the same id value as the child <step>.
	 * Attributes - copy new ones, child takes precedence in collisions
	 * 
	 * @param parent
	 * @param child
	 */
	public void mergeCollidingStep(ExecutionElement parent, ExecutionElement child){
		
	}
	
	public void mergeCollidingSplit(ExecutionElement parent, ExecutionElement child){}
}
