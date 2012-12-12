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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

import javax.batch.annotation.BeginCheckpoint;
import javax.batch.annotation.EndCheckpoint;
import javax.batch.annotation.CheckpointTimeout;
import javax.batch.annotation.IsReadyToCheckpoint;

import jsr352.batch.jsl.Property;

import com.ibm.batch.container.exception.BatchContainerRuntimeException;
import com.ibm.ws.batch.container.checkpoint.CheckpointAlgorithm;
import com.ibm.ws.batch.container.checkpoint.ItemCheckpointAlgorithm;
import com.ibm.ws.batch.container.checkpoint.ItemTimeCheckpointAlgorithm;
import com.ibm.ws.batch.container.checkpoint.TimeCheckpointAlgorithm;

public class CheckpointAlgorithmProxy extends AbstractProxy implements CheckpointAlgorithm {

    private Method getCheckpointTimeOutMethod = null;
    private Method beginCheckpointMethod = null;
    private Method isReadyToCheckpointMethod = null;
    private Method endCheckpointMethod = null;
    private String checkpointType = null;
    private String checkpointName = null;

    /*
     * Allow this to be public as a special case so we can easily treat the built-in algorithms
     * as identical to custom ones.
     */
    public CheckpointAlgorithmProxy(final Object delegate, final List<Property> props) {
        super(delegate, props);

        // find annotations
        for (final Method method : this.delegate.getClass().getDeclaredMethods()) {
            final Annotation getCheckpointTimeOut = method.getAnnotation(CheckpointTimeout.class);
            if (getCheckpointTimeOut != null) {
                getCheckpointTimeOutMethod = method;
            }

            final Annotation beginCheckpoint = method.getAnnotation(BeginCheckpoint.class);
            if (beginCheckpoint != null) {
                beginCheckpointMethod = method;
            }

            final Annotation isReadyToCheckpoint = method.getAnnotation(IsReadyToCheckpoint.class);
            if (isReadyToCheckpoint != null) {
                isReadyToCheckpointMethod = method;
            }

            final Annotation endCheckpoint = method.getAnnotation(EndCheckpoint.class);
            if (endCheckpoint != null) {
                endCheckpointMethod = method;
            }
            if (delegate instanceof ItemCheckpointAlgorithm) {
                checkpointType = "item";
                checkpointName = ItemCheckpointAlgorithm.class.getName();
            } else if (delegate instanceof TimeCheckpointAlgorithm) {
                checkpointType = "time";
                checkpointName = TimeCheckpointAlgorithm.class.getName();
            } else {
            	checkpointType = "custom";
            	
            	if (delegate instanceof ItemTimeCheckpointAlgorithm){
            		checkpointType = "item-time";
            		checkpointName = ItemTimeCheckpointAlgorithm.class.getName();
            	}
            	else {
            		checkpointType = "custom";
            		checkpointName = delegate.getClass().getName();
            	}
            }
        }
    }

    public int getCheckpointTimeOut(final int timeOut) {
        int retTimeOut = 0;
        if (getCheckpointTimeOutMethod != null) {
            try {
                retTimeOut = (Integer) getCheckpointTimeOutMethod.invoke(delegate, timeOut);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
        }
        return retTimeOut;
    }

    public void beginCheckpoint() {
        if (beginCheckpointMethod != null) {
            try {
                beginCheckpointMethod.invoke(delegate, (Object[]) null);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
        }
    }

    public void endCheckpoint() {
        if (endCheckpointMethod != null) {
            try {
                endCheckpointMethod.invoke(delegate, (Object[]) null);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
        }
    }

    public boolean isReadyToCheckpointMethod() {
        Boolean ret = false;
        if (isReadyToCheckpointMethod != null) {
            try {
                ret = (Boolean) isReadyToCheckpointMethod.invoke(delegate, (Object[]) null);
            } catch (Exception e) {
                throw new BatchContainerRuntimeException(e);
            }
        }
        return ret;
    }

    public String getCheckpointType() {
        return checkpointType;
    }

    public String getCheckpointAlgorithmClassName() {
        return checkpointName;
    }

	@Override
	public boolean isReadyToCheckpoint() throws Exception {
		// TODO Auto-generated method stub
		return isReadyToCheckpointMethod();
	}

	@Override
	public void setThreshold(int INthreshHold) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setThresholds(int itemthreshold, int timethreshold) {
		// TODO Auto-generated method stub
		
	}
    
}
