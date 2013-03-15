package com.ibm.jbatch.tck.artifacts.specialized;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Logger;

import javax.batch.api.chunk.ItemWriter;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;

@javax.inject.Named("nullChkPtInfoWriter")
public class NullChkPtInfoWriter implements ItemWriter<String> {

	private final static Logger logger = Logger.getLogger(DoSomethingSimpleArrayWriter.class.getName());
	
    @Inject 
    JobContext jobCtx;
	
	@Override
	public void open(Serializable checkpoint) throws Exception {
		logger.fine("AJM: writer.open(checkpoint)");

		
		if (checkpoint == null){
			jobCtx.setExitStatus(jobCtx.getExitStatus()+"...checkpointInfo is null in writer.open");
		}
		
	}

	@Override
	public void close() throws Exception {
		// TODO Auto-generated method stub
		logger.fine("AJM: writer.close()");
		
	}

	@Override
	public Serializable checkpointInfo() throws Exception {
		// TODO Auto-generated method stub
		logger.fine("AJM: returing null from writer.checkpointInfo()");
		return null;
	}

	@Override
	public void writeItems(List<String> items) throws Exception {
		// TODO Auto-generated method stub
		logger.fine("AJM: writer.writeItems()");
	}

}
