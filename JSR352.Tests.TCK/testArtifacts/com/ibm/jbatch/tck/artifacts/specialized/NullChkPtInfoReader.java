package com.ibm.jbatch.tck.artifacts.specialized;

import java.io.Serializable;
import java.util.logging.Logger;

import javax.batch.api.chunk.ItemReader;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;

@javax.inject.Named("nullChkPtInfoReader")
public class NullChkPtInfoReader implements ItemReader {

	private final static Logger logger = Logger.getLogger(DoSomethingSimpleArrayWriter.class.getName());
	
    @Inject 
    JobContext jobCtx;
	
	@Override
	public void open(Serializable checkpoint) throws Exception {
		
		logger.fine("AJM: reader.open(checkpoint)");
		
		if (checkpoint == null){
			jobCtx.setExitStatus("checkpointInfo is null in reader.open");
		}
	}

	@Override
	public void close() throws Exception {
		logger.fine("AJM: reader.close()");
		
	}

	@Override
	public String readItem() throws Exception {
		logger.fine("AJM: in reader.readItem(), returning a null to shut down the app");
		return null;
	}

	@Override
	public Serializable checkpointInfo() throws Exception {
		logger.fine("AJM: returning null from reader.checkpointInfo()");
		return null;
	}

}
