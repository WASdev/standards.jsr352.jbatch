package jsr352.tck.specialized;

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

import java.io.Externalizable;
import java.util.logging.Logger;

import javax.batch.annotation.*;
import javax.batch.runtime.context.StepContext;



@CheckpointListener("MyCustomCheckpointListener")
@javax.inject.Named("MyCustomCheckpointListener")
public class MyCustomCheckpointListener {
    
    private final static String sourceClass = MyCustomCheckpointListener.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);
    
    @BatchContext
    StepContext<Integer, Externalizable> stepCtx;

    @BeforeCheckpoint
    public void before() {
    	System.out.println("CUSTOMCHKPTLISTENER: beforeCheckpoint");
    }
    
    @AfterCheckpoint
    public void after() {
    	System.out.println("CUSTOMCHKPTLISTENER: afterCheckpoint");
    }
    

}

