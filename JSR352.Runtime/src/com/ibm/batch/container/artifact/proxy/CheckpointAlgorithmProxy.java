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

import javax.batch.api.CheckpointAlgorithm;

import com.ibm.batch.container.exception.BatchContainerRuntimeException;
import com.ibm.ws.batch.container.checkpoint.ItemCheckpointAlgorithm;
import com.ibm.ws.batch.container.checkpoint.TimeCheckpointAlgorithm;

public class CheckpointAlgorithmProxy extends AbstractProxy<CheckpointAlgorithm> implements CheckpointAlgorithm {

    private String checkpointType = null;
    private String checkpointName = null;

    /*
     * Allow this to be public as a special case so we can easily treat the built-in algorithms
     * as identical to custom ones.
     */
    public CheckpointAlgorithmProxy(final CheckpointAlgorithm delegate) {
        super(delegate);

        if (delegate instanceof ItemCheckpointAlgorithm) {
            checkpointType = "item";
            checkpointName = ItemCheckpointAlgorithm.class.getName();
        } else if (delegate instanceof TimeCheckpointAlgorithm) {
            checkpointType = "time";
            checkpointName = TimeCheckpointAlgorithm.class.getName();
        } else {
			checkpointType = "custom";
			checkpointName = delegate.getClass().getName();
        }

    }


    public String getCheckpointType() {
        return checkpointType;
    }

    public String getCheckpointAlgorithmClassName() {
        return checkpointName;
    }


    @Override
    public void beginCheckpoint() {
        try {
            this.delegate.beginCheckpoint();
        } catch (Exception e) {
            throw new BatchContainerRuntimeException(e);
        }
    }


    @Override
    public int checkpointTimeout(int timeout) {
        try {
            return this.delegate.checkpointTimeout(timeout);
        } catch (Exception e) {
            throw new BatchContainerRuntimeException(e);
        }
    }


    @Override
    public void endCheckpoint() {
        try {
             this.delegate.endCheckpoint();
        } catch (Exception e) {
            throw new BatchContainerRuntimeException(e);
        }
    }


    @Override
    public boolean isReadyToCheckpoint() {
        try {
            return this.delegate.isReadyToCheckpoint();
        } catch (Exception e) {
            throw new BatchContainerRuntimeException(e);
        }
    }

    
}
