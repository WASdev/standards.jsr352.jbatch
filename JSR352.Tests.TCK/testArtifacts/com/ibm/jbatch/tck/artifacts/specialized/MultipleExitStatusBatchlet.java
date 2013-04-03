package com.ibm.jbatch.tck.artifacts.specialized;

import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

@javax.inject.Named("multipleExitStatusBatchlet")
public class MultipleExitStatusBatchlet extends AbstractBatchlet {

    @Inject
    StepContext stepCtx;

    @Inject
    @BatchProperty(name = "stop.job.after.this.step")
    String stop_job_after_this_step;

    @Inject
    @BatchProperty(name = "stop.job.after.this.step2")
    String stop_job_after_this_step2;

    @Inject
    @BatchProperty(name = "fail.job.after.this.step")
    String fail_job_after_this_step;

    @Inject
    @BatchProperty(name = "step.complete.but.force.job.stopped.status")
    String step_complete_but_force_job_stopped_status;

    @Inject
    @BatchProperty(name = "step.complete.but.force.job.failed.status")
    String step_complete_but_force_job_failed_status;

    @Override
    public String process() throws Exception {

        if (stepCtx.getStepName().equalsIgnoreCase(stop_job_after_this_step)
                || stepCtx.getStepName().equalsIgnoreCase(stop_job_after_this_step2)) {
            
            stepCtx.setExitStatus(step_complete_but_force_job_stopped_status);
            return step_complete_but_force_job_stopped_status;
        }

        if (stepCtx.getStepName().equalsIgnoreCase(fail_job_after_this_step)) {
            stepCtx.setExitStatus(step_complete_but_force_job_failed_status);
            return step_complete_but_force_job_failed_status;
        }

        return stepCtx.getStepName() + "_CONTINUE";

    }

}
