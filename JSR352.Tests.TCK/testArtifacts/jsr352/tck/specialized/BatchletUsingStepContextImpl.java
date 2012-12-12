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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.batch.annotation.*;
import javax.batch.annotation.Process;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;

@Batchlet("BatchletUsingStepContextImpl")
@javax.inject.Named("BatchletUsingStepContextImpl")
public class BatchletUsingStepContextImpl {

    @BatchContext 
    private StepContext<MyTransient, MyPersistent> stepCtx = null; 

    @BatchContext 
    private JobContext jobCtx = null; 

    
    private String BEGAN = "MadeItToBegin";
    private String CANCEL = "Cancelled";
    private String PROCESSED = "Processed";
    
    
    private void begin() throws Exception {
        System.out.println("BatchletUsingStepContextImpl - @BeginStep");
        assert stepCtx.getExitStatus()==null;
        stepCtx.setExitStatus(BEGAN);
    }

    public static String GOOD_STEP_EXIT_STATUS = "VERY GOOD INVOCATION";
    public static String GOOD_JOB_EXIT_STATUS = "JOB: " + GOOD_STEP_EXIT_STATUS;

    @Process
    public String process() throws Exception {
    	this.begin();   	
    	
        System.out.println("BatchletUsingStepContextImpl - @Process");		
        assert stepCtx.getExitStatus().equals(BEGAN);
        stepCtx.setPersistentUserData(new MyPersistent(4));
        stepCtx.setTransientUserData(new MyTransient(3));
        stepCtx.setExitStatus(PROCESSED);
        end();
        return "COMPLETED";
    }

    @Stop
    public void cancel() throws Exception {
        System.out.println("BatchletUsingStepContextImpl - @Cancel");		
        stepCtx.setExitStatus(CANCEL);
    }

    private void end() throws Exception {
        System.out.println("BatchletUsingStepContextImpl - formerly @EndStep");
        MyPersistent p = stepCtx.getPersistentUserData();
        MyTransient t = stepCtx.getTransientUserData();
        System.out.println("MyBatchLetImpl.end() p,t = " + p.data + "," + t.data);
        assert stepCtx.getExitStatus().equals(PROCESSED);
        stepCtx.setExitStatus(GOOD_STEP_EXIT_STATUS);
        jobCtx.setExitStatus(GOOD_JOB_EXIT_STATUS);
    }
    
    private class MyTransient {
        int data = 0;
        MyTransient(int x) {
            data = x;
        }   
    }
    private class MyPersistent implements Externalizable {
        int data = 0;
        
        MyPersistent(int x) {
            data = x;
        }
        
        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            // TODO Auto-generated method stub
            
        }
        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            // TODO Auto-generated method stub
            
        }   
    }

}

