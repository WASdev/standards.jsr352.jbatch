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
package com.ibm.batch.container.context.impl;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.runtime.context.FlowContext;

public class FlowContextImpl<T> implements FlowContext<T> {

    private final static String sourceClass = FlowContextImpl.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);

    private String flowId = null;
    
    private String exitStatus = null;
    private String batchStatus;
    
    private T transientUserData = null;

	public FlowContextImpl(String flowId) {
		this.flowId = flowId;
    }

    public void setBatchStatus(String batchStatus) {
		this.batchStatus = batchStatus;
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Batch status set to: " + batchStatus + " for flow id:" + getId());
        }
	}
    
    @Override
    public String getExitStatus() {
        return exitStatus;
    }

    @Override
    public String getId() {
        
        return this.flowId;
    }

    @Override
    public void setExitStatus(String status) {
        this.exitStatus = status;
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Exit status set to: " + status + " for step id:" + getId());
        }
    }

	@Override
	public List getBatchContexts() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public T getTransientUserData() {
		return this.transientUserData;
	}

	@Override
	public void setTransientUserData(T data) {
		this.transientUserData = data;
		
	}

	@Override
	public String getBatchStatus() {
		return this.batchStatus;
	}


}
