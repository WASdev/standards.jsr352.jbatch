package com.ibm.jbatch.tck.artifacts.specialized;

import javax.batch.api.BatchProperty;
import javax.batch.api.Batchlet;
import javax.inject.Inject;

@javax.inject.Named("failRestartBatchlet")
public class FailRestartBatchlet implements Batchlet {

    @Inject    
    @BatchProperty(name="execution.number")
    String executionNumberString;
    
    @Inject    
    @BatchProperty(name="sleep.time")
    String sleepTimeString;
	
    boolean init = false;
    int executionNum = 0;
    int sleeptime;
    
	@Override
	public String process() throws Exception {

		if (!init) {
			init();
		}
		
		if (executionNum == 1){
			throw new Exception("fail on purpose, execution1");
		}
		else if (executionNum == 2){
			Thread.sleep(sleeptime);
		}
		return "FailRestartBatchlet Done";
	}

	@Override
	public void stop() throws Exception {
		// TODO Auto-generated method stub

	}
	
	private void init(){
		if (executionNumberString != null) {
			executionNum = Integer.parseInt(executionNumberString);
		}
		if (executionNumberString != null) {
			sleeptime = Integer.parseInt(sleepTimeString);
		}
		
		init = true;
	}

}
