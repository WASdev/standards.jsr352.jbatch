package com.ibm.jbatch.container.services;

import java.sql.Timestamp;
import java.util.Properties;

import javax.batch.runtime.JobExecution;

import com.ibm.jbatch.container.context.impl.JobContextImpl;

public interface IJobExecution extends JobExecution {

	public void setBatchStatus(String status);

	public void setCreateTime(Timestamp ts);

	public void setEndTime(Timestamp ts);

	public void setExitStatus(String status);

	public void setLastUpdateTime(Timestamp ts);

	public void setStartTime(Timestamp ts);

	public void setJobParameters(Properties jProps);
	
	public long getInstanceId();

	public void setJobContext(JobContextImpl jobContext);
}
