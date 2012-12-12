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
package com.ibm.batch.container.artifact.proxy;

import java.io.Externalizable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

import javax.batch.annotation.AnalyzeCollectorData;
import javax.batch.annotation.AnalyzeExitStatus;

import jsr352.batch.jsl.Property;

import com.ibm.batch.container.exception.BatchContainerRuntimeException;

public class PartitionAnalyzerProxy extends AbstractProxy {
	
	private Method analyzeCollectorDataMethod= null;
	private Method analyzeExitStatusMethod= null;

	PartitionAnalyzerProxy(Object delegate, List<Property> props) { 
        super(delegate, props);
	    
	    //find annotations
		for (Method method: delegate.getClass().getDeclaredMethods()) { 
			Annotation beforeJob= method.getAnnotation(AnalyzeCollectorData.class);
			if ( beforeJob != null ) { 
				analyzeExitStatusMethod= method;
			}
			Annotation afterJob= method.getAnnotation(AnalyzeExitStatus.class);
			if ( afterJob != null ) { 
				analyzeCollectorDataMethod= method;
			}
		}
	}
	
	public synchronized void analyzeCollectorData(Externalizable data) {
		if ( analyzeExitStatusMethod != null ) {
            try {
                analyzeExitStatusMethod.invoke(delegate, data);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
		}
	}
	
	public synchronized void analyzeExitStatus(String exitStatus) {
		if ( analyzeCollectorDataMethod != null ) {
            try {
                analyzeCollectorDataMethod.invoke(delegate, exitStatus);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
		}	
	}	
}


