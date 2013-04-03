package com.ibm.jbatch.container.status;

public class SplitStatus {
	private JobOrFlowBatchStatus determiningFlowBatchStatus;
	private boolean couldMoreThanOneFlowHaveTerminatedJob;
	
	public JobOrFlowBatchStatus getDeterminingFlowBatchStatus() {
		return determiningFlowBatchStatus;
	}

	public void setDeterminingFlowBatchStatus(JobOrFlowBatchStatus determiningFlowBatchStatus) {
		this.determiningFlowBatchStatus = determiningFlowBatchStatus;
	}

	public boolean isCouldMoreThanOneFlowHaveTerminatedJob() {
		return couldMoreThanOneFlowHaveTerminatedJob;
	}

	public void setCouldMoreThanOneFlowHaveTerminatedJob(
			boolean couldMoreThanOneFlowHaveTerminatedJob) {
		this.couldMoreThanOneFlowHaveTerminatedJob = couldMoreThanOneFlowHaveTerminatedJob;
	}
	
	@Override
	public String toString() {
		return "determiningFlowBatchStatus: " + determiningFlowBatchStatus.name() + ", couldMoreThanOneFlowHaveTerminatedJob = " + couldMoreThanOneFlowHaveTerminatedJob; 
	}

}
