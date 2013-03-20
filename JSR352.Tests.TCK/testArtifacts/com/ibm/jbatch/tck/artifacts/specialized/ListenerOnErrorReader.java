package com.ibm.jbatch.tck.artifacts.specialized;

import java.io.Serializable;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemReader;
import javax.inject.Inject;
import javax.inject.Named;

import com.ibm.jbatch.tck.artifacts.chunktypes.ReadRecord;

@Named
public class ListenerOnErrorReader implements ItemReader {

	@Inject    
    @BatchProperty(name="read.fail.immediate")
    String failImmediateString;
	boolean failimmediate = false;
	
	@Override
	public void open(Serializable checkpoint) throws Exception {
		if (failImmediateString!=null){
			failimmediate = Boolean.parseBoolean(failImmediateString);
		}
	}

	@Override
	public void close() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ReadRecord readItem() throws Exception {
		// TODO Auto-generated method stub
		if (failimmediate){
			throw new Exception("read fail immediate");
		}
		else {
			return new ReadRecord();
		}
	}

	@Override
	public Serializable checkpointInfo() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
