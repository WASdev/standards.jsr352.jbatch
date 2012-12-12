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
package com.ibm.batch.container.services.impl;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.ibm.batch.container.exception.BatchContainerServiceException;
import com.ibm.batch.container.services.ParallelTaskResult;

/*
 * An adapter class for a Future object so we can wait for parallel threads/steps/flows to finish before continuing 
 */
public class JSEResultAdapter implements ParallelTaskResult {

    private Future result;
    
    public JSEResultAdapter(Future result) {
        this.result = result;
    }

    @Override
    public void waitForResult() {
        try {
            result.get();
        } catch (InterruptedException e) {
            throw new BatchContainerServiceException("Parallel thread was interrupted while waiting for result.", e);
        } catch (ExecutionException e) {
            throw new BatchContainerServiceException("Parallel thread was aborted due to exception.", e);
        } catch (CancellationException e) {
            throw new BatchContainerServiceException("Parallel thread was canceled before completion.", e);
        }

    }


}
