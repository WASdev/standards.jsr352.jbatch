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
package com.ibm.batch.container.jobinstance;

import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.runtime.StepExecution;
import javax.batch.runtime.context.StepContext;

import jsr352.batch.jsl.JSLJob;

import com.ibm.batch.container.modelresolver.PropertyResolver;
import com.ibm.batch.container.modelresolver.PropertyResolverFactory;
import com.ibm.batch.container.services.IJobIdManagementService;
import com.ibm.batch.container.services.IJobStatusManagerService;
import com.ibm.batch.container.services.IPersistenceManagerService;
import com.ibm.batch.container.services.ServicesManager;
import com.ibm.batch.container.services.ServicesManager.ServiceType;
import com.ibm.batch.container.status.JobStatus;
import com.ibm.batch.container.xjcl.ModelResolverFactory;
import com.ibm.batch.container.xjcl.Navigator;
import com.ibm.batch.container.xjcl.NavigatorFactory;

public class JobExecutionHelper {

    private final static String CLASSNAME = JobExecutionHelper.class.getName();
    private final static Logger logger = Logger.getLogger(CLASSNAME);
    
    private static final String GENERATED_JOB = "GENERATED_JOB";

    private static ServicesManager servicesManager = ServicesManager.getInstance();

    private static IJobIdManagementService _jobIdManagementService = 
        (IJobIdManagementService)servicesManager.getService(ServiceType.JOB_ID_MANAGEMENT_SERVICE);

    private static IJobStatusManagerService _jobIdStatusManagerService = 
        (IJobStatusManagerService)servicesManager.getService(ServiceType.JOB_STATUS_MANAGEMENT_SERVICE);
    
    private static IPersistenceManagerService _persistenceManagementService = 
        (IPersistenceManagerService)servicesManager.getService(ServiceType.PERSISTENCE_MANAGEMENT_SERVICE);


    public static RuntimeJobExecutionImpl startJob(String jobXML, Properties jobParameters) {
        long instanceId = _jobIdManagementService.getInstanceId();
        long executionId = _jobIdManagementService.getExecutionId();

        JobInstanceImpl jobInstanceImpl = new JobInstanceImpl(jobXML, jobParameters, instanceId);                

        JSLJob jobModel = ModelResolverFactory.createJobResolver().resolveModel(jobXML); 
  
        //Resolve the properties for this job
        PropertyResolver<JSLJob> propResolver = PropertyResolverFactory.createJobPropertyResolver();
        propResolver.substituteProperties(jobModel, jobParameters);
        
        Navigator jobNavigator = NavigatorFactory.createJobNavigator(jobModel);       

        jobInstanceImpl.setJobName(jobNavigator.getId());                                

        _jobIdStatusManagerService.createJobStatus(jobInstanceImpl, executionId);
                
        // register jobName in the jobop job information table
        Timestamp starttime = new Timestamp(0); // what is the arg here?
        Timestamp updatetime = new Timestamp(0); // what is the arg here?
        Timestamp endtime = new Timestamp(0);
        Timestamp createtime = new Timestamp(0);
        _persistenceManagementService.jobOperatorCreateJobInstanceData(instanceId, jobNavigator.getId());
        _persistenceManagementService.jobOperatorCreateExecutionData(executionId, starttime, updatetime, endtime, createtime, jobParameters, instanceId, "STARTING", "EXITSTATUS");

        return new RuntimeJobExecutionImpl(jobNavigator, jobInstanceImpl, executionId);
    }

    public static RuntimeJobExecutionImpl restartJob(long jobInstanceId, Properties overrideJobParameters) {
        JobStatus jobStatus = _jobIdStatusManagerService.getJobStatus(jobInstanceId);

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("On restartJob with jobInstance Id = " + jobInstanceId + " , found JobStatus: " + jobStatus );            
        }

        JobInstanceImpl jobInstance = jobStatus.getJobInstance();

        //TODO - check that original job ended in restartable state and that it's restartable.
        // Don't get a new execution id until we do this check.

        long nextExecutionId = _jobIdManagementService.getExecutionId();

        Properties originalJobParameters = jobInstance.getOriginalJobParams();

        Properties mergedRestartJobParameters = new Properties(originalJobParameters);

        if (overrideJobParameters != null) {
            for (String key : overrideJobParameters.stringPropertyNames()) {
                mergedRestartJobParameters.setProperty(key, overrideJobParameters.getProperty(key));
            }
        }
                                                
        JSLJob jobModel = ModelResolverFactory.createJobResolver().resolveModel(jobInstance.getJobXML());
        
        //Resolve the merged restart properties for this job
        PropertyResolver<JSLJob> propResolver = PropertyResolverFactory.createJobPropertyResolver();
        propResolver.substituteProperties(jobModel, mergedRestartJobParameters);  
        
        Navigator jobNavigator = NavigatorFactory.createJobNavigator(jobModel);
        
        _jobIdStatusManagerService.updateJobStatusWithNewExecution(jobInstance.getInstanceId(), nextExecutionId);        

        return new RuntimeJobExecutionImpl(jobNavigator, jobInstance, nextExecutionId, jobStatus.getRestartOn());                     
    }

    public static RuntimeJobExecutionImpl startJob(JSLJob jobModel, Properties jobParameters) {
        long instanceId = _jobIdManagementService.getInstanceId();
        long executionId = _jobIdManagementService.getExecutionId();

        JobInstanceImpl jobInstanceImpl = new JobInstanceImpl(GENERATED_JOB, jobParameters, instanceId);                

        //Resolve the properties for this job
        PropertyResolver<JSLJob> propResolver = PropertyResolverFactory.createJobPropertyResolver();
        propResolver.substituteProperties(jobModel, jobParameters);    
        
        Navigator jobNavigator = NavigatorFactory.createJobNavigator(jobModel);

        jobInstanceImpl.setJobName(jobNavigator.getId());                                

        _jobIdStatusManagerService.createJobStatus(jobInstanceImpl, executionId);        

        return new RuntimeJobExecutionImpl(jobNavigator, jobInstanceImpl, executionId);

    }
    
    public static void persistStepExecution(long jobExecutionInstanceID, StepContext stepContext){
    	
    	String stepExecutionKey = getJobStepExecId(jobExecutionInstanceID, stepContext.getStepExecutionId());
    	_persistenceManagementService.stepExecutionCreateStepExecutionData(stepExecutionKey, jobExecutionInstanceID, stepContext.getStepExecutionId());
    	
    }
  
    public static long getstepExecutionIDInfo(String key, String idtype){
    	return _persistenceManagementService.stepExecutionQueryID(key, idtype);
    }
    
    public static List<StepExecution> getstepExecutionIDInfoList(long key, String idtype){
    	return _persistenceManagementService.stepExecutionQueryIDList(key, idtype);
    }
    
    /*
     * creates unique key to get StepExecution
     */
    private static String getJobStepExecId(long jobExecutionId, long stepExecutionId) {
    	return String.valueOf(jobExecutionId) + ':' + String.valueOf(stepExecutionId);
    }
}
