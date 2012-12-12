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
package jsr352.tck.specialized;

import javax.batch.annotation.*;
import javax.batch.runtime.context.JobContext;

@JobListener("MyUniversalListener")
@StepListener("MyUniversalListener")
@javax.inject.Named("MyUniversalListener")
public class MyUniversalListener {
    
    @BatchContext 
    private JobContext jobCtx = null; 
    
    @BeforeJob public void beforeJob() {
        String cur = jobCtx.getExitStatus();
        String status = (cur == null ? "BeforeJob" : cur + "BeforeJob");
        jobCtx.setExitStatus(status);
    }
    
    @AfterJob public void afterJob() {
        String cur = jobCtx.getExitStatus();
        jobCtx.setExitStatus(cur + "AfterJob");
    }
    
    @BeforeStep public void beforeStep() {
        String cur = jobCtx.getExitStatus();
        jobCtx.setExitStatus(cur + "BeforeStep");
    }
    
    @AfterStep public void afterStep() {
        String cur = jobCtx.getExitStatus();
        jobCtx.setExitStatus(cur + "AfterStep");
    }
    
}
