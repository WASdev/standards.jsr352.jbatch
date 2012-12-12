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

import javax.batch.runtime.context.SplitContext;

public class SplitContextImpl<T> implements SplitContext<T> {

    private final static String sourceClass = SplitContextImpl.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);

    private String exitStatus;
    private String batchStatus;
	private String splitId;


    public SplitContextImpl(String splitId) {
    	this.splitId = splitId;
    }

    @Override
    public String getBatchStatus() {
        return batchStatus;
    }

    public void setBatchStatus(String batchStatus) {
		this.batchStatus = batchStatus;
	}
    
    @Override
    public String getExitStatus() {
        return exitStatus;
    }

    @Override
    public String getId() {
        // TODO Auto-generated method stub
        return null;
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setTransientUserData(Object data) {
		// TODO Auto-generated method stub
		
	}

	public String getSplitId() {
		return splitId;
	}

}
