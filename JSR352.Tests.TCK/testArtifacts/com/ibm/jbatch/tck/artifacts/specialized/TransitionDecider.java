package com.ibm.jbatch.tck.artifacts.specialized;

import javax.batch.api.BatchProperty;
import javax.batch.api.Decider;
import javax.batch.runtime.StepExecution;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;

public class TransitionDecider implements Decider {

    @Inject
    JobContext jobCtx;
    
    @Inject    
    @BatchProperty(name="is.restart")
    String isRestart; 
    
    @Override
    public String decide(StepExecution[] executions) throws Exception {

        String stepExitStatus = null;
        String stepName = null;
        
        for (StepExecution stepExec : executions) {
            stepExitStatus = stepExec.getExitStatus();
            stepName = stepExec.getStepName();
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
