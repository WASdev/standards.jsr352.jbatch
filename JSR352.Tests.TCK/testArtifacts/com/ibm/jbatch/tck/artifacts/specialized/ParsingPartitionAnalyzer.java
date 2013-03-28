package com.ibm.jbatch.tck.artifacts.specialized;

import javax.batch.api.partition.AbstractPartitionAnalyzer;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

import com.ibm.jbatch.tck.artifacts.reusable.MyParallelSubJobsExitStatusBatchlet;

@Named
public class ParsingPartitionAnalyzer extends AbstractPartitionAnalyzer {
	
    @Inject
	JobContext jobCtx;
    
    @Inject
	StepContext stepCtx;
    
    private int counter = 0;
    
	@Override
	public void analyzeStatus(BatchStatus batchStatus, String exitStatus)
			throws Exception {
	    
	    counter++;
	    
		String goodPrefix = MyParallelSubJobsExitStatusBatchlet.GOOD_EXIT_STATUS;
		int idx = goodPrefix.length() + 1;
		if (!exitStatus.startsWith(goodPrefix)) {
			throw new IllegalStateException("Expected exit status to start with: " + goodPrefix + ", but found :" + exitStatus);
		}
	
		jobCtx.setExitStatus("JOB EXIT STATUS: " + counter);
		stepCtx.setExitStatus("STEP EXIT STATUS: " + counter);
	}
}
