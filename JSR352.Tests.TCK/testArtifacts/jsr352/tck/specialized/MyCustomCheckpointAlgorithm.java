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
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.annotation.BatchProperty;
import javax.batch.annotation.CheckpointAlgorithm;
import javax.batch.annotation.IsReadyToCheckpoint;

@CheckpointAlgorithm("MyCustomCheckpointAlgorithm")
@javax.inject.Named("MyCustomCheckpointAlgorithm")
public class MyCustomCheckpointAlgorithm {

	private static final String className = MyCustomCheckpointAlgorithm.class.getName();
	private static Logger logger  = Logger.getLogger(MyCustomCheckpointAlgorithm.class.getPackage().getName());
		
	boolean inCheckpoint = false;
	int checkpointIterations = 1;
	
   int threshold;
   long timeStarted = 0;
   int requests;
   
   @BatchProperty(name="writepoints")
   String writePointsString;
	
   int [] writePoints;
   
   boolean init = false;
   
   public void init(){
	   String[] writePointsStrArr = writePointsString.split(",");
	   writePoints = new int[writePointsString.length()];
	   
		for (int i = 0; i<writePointsStrArr.length; i++){
			writePoints[i] = Integer.parseInt(writePointsStrArr[i]);
			System.out.println("CUSTOMCHKPT: writePoints[" + i + "] = " + writePoints[i]);
		}
		
		threshold = writePoints[checkpointIterations];
		requests = writePoints[0];
		
		init = true;
   }
   
	@IsReadyToCheckpoint
	public boolean isReadyToCheckpoint() throws Exception {
      	String method = "isReadyToCheckpoint";
      	if(logger.isLoggable(Level.FINER)) { logger.entering(className, method); }

      	if (!init){
     	   String[] writePointsStrArr = writePointsString.split(",");
    	   writePoints = new int[writePointsString.length()];
    	   
    		for (int i = 0; i<writePointsStrArr.length; i++){
    			System.out.println("CUSTOMCHKPT: writePointsStrArr[" + i + "] = " + writePointsStrArr[i]);
    			writePoints[i] = Integer.parseInt(writePointsStrArr[i]);
    			System.out.println("CUSTOMCHKPT: writePoints[" + i + "] = " + writePoints[i]);
    		}
    		
    		threshold = writePoints[checkpointIterations];
    		requests = writePoints[0];
    		
    		init = true;
      	}
      	
      	requests++;
      	boolean ready = (requests >= threshold);
      	
       if ( ready) {
    	   checkpointIterations++;
    	   threshold = writePoints[checkpointIterations];
           long millis =  Long.valueOf( (new Date().getTime()) - timeStarted );
           if ( millis>0 ) { 
               String rate =  Integer.valueOf ( Long.valueOf( (requests*1000/millis) ).intValue()).toString();
               if(logger.isLoggable(Level.FINE)) { logger.fine(" - true [requests/second " + rate + "]"); }

           } else {
           	if(logger.isLoggable(Level.FINE)) { logger.fine(" - true [requests " + requests + "]"); }

           }
       }

       //if ( ready ) requests = 0;
       
       return ready;
	}

}
