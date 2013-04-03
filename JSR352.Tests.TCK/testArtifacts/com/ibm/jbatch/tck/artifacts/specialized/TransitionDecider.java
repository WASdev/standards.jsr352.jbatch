package com.ibm.jbatch.tck.artifacts.specialized;

import javax.batch.api.BatchProperty;
import javax.batch.api.Decider;
import javax.batch.runtime.StepExecution;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;

@javax.inject.Named("transitionDecider")
public class TransitionDecider implements Decider {

    @Inject
    JobContext jobCtx;
    
    @Inject    
    @BatchProperty(name="is.restart")
    String isRestart; 
    
    @Override
    public String decide(StepExecution[] executions) throws Exception {

        String stepExitStatus = "";
        String stepName = "";
        
        
        for (StepExecution stepExec : executions) {
            String tempExitStatus = stepExec.getExitStatus();
            String tempStepName = stepExec.getStepName();
            
            //Always choose the alphabetically later step name and exit status so we can end the test deterministically
            if (stepExitStatus.compareTo(tempExitStatus) < 0) {
                stepExitStatus = tempExitStatus;
            }
            
            //
            if (stepName.compareTo(tempStepName) < 0){
                stepName = tempStepName;
            }
        }
        
        Integer deciderCount = jobCtx.getTransientUserData() == null ? 0 : (Integer)jobCtx.getTransientUserData();
        //This will provide a count for how many times the decider is called.
        deciderCount++;
        jobCtx.setTransientUserData(deciderCount);
        
        String deciderExitStatus = null;
        
        //On a restart we always want everything to continue to the end.
        if ("true".equals(isRestart)){
            deciderExitStatus = deciderCount + ":" + stepName + "_CONTINUE";
        } else{
            deciderExitStatus = deciderCount + ":" + stepExitStatus;    
        }
        
        return deciderExitStatus;
        
    }

}
