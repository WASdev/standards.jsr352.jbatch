package com.ibm.jbatch.tck.artifacts.specialized;

import javax.batch.annotation.BatchProperty;
import javax.batch.api.chunk.listener.AbstractChunkListener;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

import com.ibm.jbatch.tck.artifacts.reusable.MyParentException;

@javax.inject.Named("myChunkListener")
public class MyChunkListener extends AbstractChunkListener {
	
	@Inject 
    StepContext stepCtx; 
	
	@Inject 
    JobContext jobCtx; 
	
    @Inject    
    @BatchProperty(name="fail.immediate")
    String failImmediateString;
 
    boolean failThrowEx = false;
	
    @Override
    public void beforeChunk() throws Exception {
    	
    	if (failImmediateString!=null){
			failThrowEx = Boolean.parseBoolean(failImmediateString);
		}
    	
    	if (failThrowEx){
			throw new MyParentException("Testing getException");
		}
    }

    /**
     * Override this method if the ChunkListener will do something after the
     * chunk ends. The default implementation does nothing.
     * 
     * @throws Exception
     *             (or subclass) if an error occurs.
     */
    @Override
    public void afterChunk() throws Exception {
    	
    	Exception ex = stepCtx.getException();
    	
    	if (ex instanceof MyParentException){
    		jobCtx.setExitStatus("MyChunkListener: found instanceof MyParentException");
    	}
    	else {
    		jobCtx.setExitStatus("MyChunkListener: did not find instanceof MyParentException");
    	}
    	
    }
    
    /**
     * Override this method if the ChunkListener will do something when an error
     * occurs, before it is rolled back. The default implementation does nothing.
     * 
     * @throws Exception
     *             (or subclass) if an error occurs.
     */
    @Override
    public void onError() throws Exception {
    	Exception ex = stepCtx.getException();
    	
    	if (ex instanceof MyParentException){
    		jobCtx.setExitStatus("MyChunkListener: found instanceof MyParentException");
    	}
    	else {
    		jobCtx.setExitStatus("MyChunkListener: did not find instanceof MyParentException");
    	}
    }
    
}
