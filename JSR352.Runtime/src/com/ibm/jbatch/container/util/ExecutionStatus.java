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
package com.ibm.jbatch.container.util;


public class ExecutionStatus {
    
    private boolean executionFailedBeforeStarting = false;
    
    public ExecutionStatus() {
        super();
    }

//    public static String getStringValue(BatchStatus batchStatus) {
//        switch (batchStatus) {
//        case STARTING:
//            return "STARTING";
//        case STARTED:
//            return "STARTED";
//        case STOPPING:
//            return "STOPPING";
//        case STOPPED:
//            return "STOPPED";
//        case FAILED:
//            return "FAILED";
//        case COMPLETED:
//            return "COMPLETED";
//        }
//        return null;
//    }
//    
//    public static BatchStatus getBatchStatusEnum(String batchStatus){
//        if (batchStatus.equals("STARTING")) {
//            return BatchStatus.STARTING;
//        } else if (batchStatus.equals("STARTED")) {
//            return BatchStatus.STARTED;
//        } else if (batchStatus.equals("STOPPING")) {
//            return BatchStatus.STOPPING;
//        } else if (batchStatus.equals("STOPPED")) {
//            return BatchStatus.STOPPED;
//        } else if (batchStatus.equals("FAILED")) {
//            return BatchStatus.FAILED;
//        } else if (batchStatus.equals("COMPLETED")) {
//            return BatchStatus.COMPLETED;
//        } 
//        return null;
//    }

    public void markExecutionFailedBeforeStarting() {
        this.executionFailedBeforeStarting = true;
    }

    // Careful, this isn't meant to capture that we didn't re-execute on restart.
    // What this means is that we blew up before we even set the status to 'STARTING', so we don't count
    // this even as a failed step execution.
    public boolean executionFailedBeforeStarting() {
        return this.executionFailedBeforeStarting;
    }


}
