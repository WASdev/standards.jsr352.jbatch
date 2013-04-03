package com.ibm.jbatch.tck.artifacts.specialized;

import java.io.Serializable;
import java.util.List;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemWriter;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ListenerOnErrorWriter implements ItemWriter {

	@Inject    
    @BatchProperty(name="write.fail.immediate")
    String failImmediateString;
	boolean failimmediate = false;
	
	@Override
	public void open(Serializable checkpoint) throws Exception {
		// TODO Auto-generated method stub
		if (failImmediateString!=null){
			failimmediate = Boolean.parseBoolean(failImmediateString);
		}
		
	}

	@Override
	public void close() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void writeItems(List<Object> items) throws Exception {
		// TODO Auto-generated method stub
		if (failimmediate){
			throw new Exception("writer fail immediate");
		}
		
	}

	@Override
	public Serializable checkpointInfo() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
