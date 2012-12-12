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



@CheckpointListener("MyTimeCheckpointListener")
@javax.inject.Named("MyTimeCheckpointListener")
public class MyTimeCheckpointListener {
    
    private final static String sourceClass = MyCustomCheckpointListener.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);
    
    java.util.Date date;
    long ts;
    int timeinterval;
    
    @BatchProperty(name="timeinterval")
    String timeintervalString;
    
    @BatchContext
    StepContext<Integer, Externalizable> stepCtx;

    public MyTimeCheckpointListener(){
    	date = new java.util.Date();
        ts = date.getTime();
        
        System.out.println("TIMECHKPTLISTENER: in ctor, ts = " + ts);

    }
    
    @BeforeCheckpoint
    public void before() {
    	System.out.println("TIMECHKPTLISTENER: beforeCheckpoint");
    	
    	timeinterval = Integer.parseInt(timeintervalString);
    	
    	//if (timeinterval == null){
    		//timeinterval = stepCtx.getTransientUserData();
    		System.out.println("TIMECHKPTLISTENER: got the timeinterval: " + timeinterval);
    	//}
    	
        java.util.Date curDate = new java.util.Date();
        long curts = curDate.getTime();
        long curdiff = curts - ts;
        int diff = (int)curdiff / 1000;
        
		if ((diff <= timeinterval+1) || (diff >= timeinterval-1) ) {
			System.out.println("TIMECHKPTLISTENER: the chunk write is occuring at the correct time -> " + diff + " which is: " + timeinterval + " +/- 1 second");
		}
		else {
			System.out.println("TIMECHKPTLISTENER: we have an issue! throw exception here");
			//throw new Exception("WRITE: the chunk write did not occur at the correct time boundry -> "+ diff + " which is: " + timeinterval + "+/- 1 second");
		}
		
		//stepCtx.setTransientUserData(diff);
    	
    }
    
    @AfterCheckpoint
    public void after() {
    	System.out.println("CHKPTLISTENER: afterCheckpoint");
    	
    	date = new java.util.Date();
        ts = date.getTime();
    }
    

}