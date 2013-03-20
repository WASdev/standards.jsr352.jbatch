package com.ibm.jbatch.tck.artifacts.specialized;

import javax.batch.api.Batchlet;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;

@javax.inject.Named("jobContextTestBatchlet")
public class JobContextTestBatchlet implements Batchlet {

	@Inject
	JobContext jobCtx;
	
	@Override
	public String process() throws Exception {
		jobCtx.setExitStatus("JobName=" + jobCtx.getJobName() + ";JobInstanceId=" + jobCtx.getInstanceId() + ";JobExecutionId=" + jobCtx.getExecutionId());
		return "GOOD";
	}

	@Override
	public void stop() throws Exception {
		// TODO Auto-generated method stub

	}

}
