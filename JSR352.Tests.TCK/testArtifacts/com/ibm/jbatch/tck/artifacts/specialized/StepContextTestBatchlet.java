package com.ibm.jbatch.tck.artifacts.specialized;

import javax.batch.api.Batchlet;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

@javax.inject.Named("stepContextTestBatchlet")
public class StepContextTestBatchlet implements Batchlet {

	@Inject
	JobContext jobCtx;
	
	@Inject
	StepContext stepCtx;
	
	@Override
	public String process() throws Exception {
		jobCtx.setExitStatus("StepName=" + stepCtx.getStepName() + ";StepExecutionId=" + stepCtx.getStepExecutionId());
		return "GOOD";
	}

	@Override
	public void stop() throws Exception {
		// TODO Auto-generated method stub

	}

}
