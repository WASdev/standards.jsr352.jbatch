package com.ibm.jbatch.tck.artifacts.specialized;
import javax.batch.annotation.BatchProperty;
import javax.batch.api.chunk.ItemProcessor;
import javax.inject.Inject;

import com.ibm.jbatch.tck.artifacts.chunktypes.ReadRecord;


public class ListenerOnErrorProcessor implements
		ItemProcessor<ReadRecord, ReadRecord> {

	@Inject    
    @BatchProperty(name="process.fail.immediate")
    String failImmediateString;
	
	boolean failimmediate = false;
	
	
	@Override
	public ReadRecord processItem(ReadRecord item) throws Exception {
		// TODO Auto-generated method stub
		
		if (failImmediateString!=null){
			failimmediate = Boolean.parseBoolean(failImmediateString);
		}
		
		if (failimmediate){
			throw new Exception("process fail immediate");
		}
		else return new ReadRecord();
	}

}
