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
package com.ibm.jbatch.container.jobinstance;

import java.io.Serializable;
import java.util.Properties;

import javax.batch.runtime.JobInstance;

public class JobInstanceImpl implements JobInstance, Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private long jobInstanceId = 0L;
    private String jobName = null;
    private String jobXML = null;
    
    private JobInstanceImpl() {        
    }

    public JobInstanceImpl(String jobXML, Properties jobParameters, long instanceId) {        
        this.jobXML = jobXML;
        this.jobInstanceId = instanceId;        
    }

    @Override
    public long getInstanceId() {
        return jobInstanceId;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobName() {
        return jobName;
    }

    public String getJobXML() {
        return jobXML;
    }


    @Override
    public String toString() {        

        StringBuffer buf = new StringBuffer();
        buf.append(" jobName: " + jobName);
        buf.append(" jobInstance id: " + jobInstanceId);
        int concatLen = jobXML.length() > 200 ? 200 : jobXML.length();
        buf.append(" jobXML: " + jobXML.subSequence(0, concatLen) + "...truncated ...\n");
        buf.append(" originalJobParameters: \n");
        String propsAsString = null;
        buf.append(propsAsString);
        return buf.toString();

    }

}
