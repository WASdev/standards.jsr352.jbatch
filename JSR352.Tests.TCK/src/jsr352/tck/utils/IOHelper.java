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
package jsr352.tck.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.batch.operations.JobOperator.BatchStatus;
import javax.batch.runtime.JobExecution;

public class IOHelper {

    public static String readJobXML(String fileWithPath) throws FileNotFoundException, IOException {

        StringBuffer jobXMLBuffer = ( fileWithPath==null ? null : new StringBuffer() );
        if ( !(fileWithPath==null) ) {
            BufferedReader zin = new BufferedReader( new FileReader( new File(fileWithPath)));
            String input = zin.readLine();
            do {
                if (input != null) {
                	jobXMLBuffer.append(input);
                    input = zin.readLine();
                }
            } while (input!=null);
        }
        return ( jobXMLBuffer==null ? null : jobXMLBuffer.toString() );

    }
    
	public static void waitForBatchStatusOrTimeout(JobExecution jobExecution, BatchStatus batchStatus, long timeout) {

		long future = System.currentTimeMillis() + timeout;
		while(!jobExecution.getBatchStatus().equals(batchStatus) && future > System.currentTimeMillis()) {
			// loops while job equals batchStatus or it times out
		}
	}

}
