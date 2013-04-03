package com.ibm.jbatch.tck.artifacts.specialized;

import java.util.logging.Logger;

import javax.batch.api.chunk.listener.RetryReadListener;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

import org.testng.Reporter;

import com.ibm.jbatch.tck.artifacts.reusable.MyParentException;

@javax.inject.Named("myMultipleExceptionsRetryReadListener")
public class MyMultipleExceptionsRetryReadListener implements RetryReadListener {

    @Inject
    JobContext jobCtx;

    @Inject
    StepContext stepCtx;

    private final static String sourceClass = MySkipReadListener.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);

    public static final String GOOD_EXIT_STATUS = "MyMultipleExceptionsRetryReadListener: GOOD STATUS";
    public static final String BAD_EXIT_STATUS = "MyMultipleExceptionsRetryReadListener: BAD STATUS";

	
	@Override
	public void onRetryReadException(Exception ex) throws Exception {
        Reporter.log("In onSkipReadItem" + ex + "<p>");

        if (ex instanceof MyParentException) {
        	Reporter.log("SKIPLISTENER: onSkipReadItem, exception is an instance of: MyParentException<p>");
            jobCtx.setExitStatus(GOOD_EXIT_STATUS);
        } else {
        	Reporter.log("SKIPLISTENER: onSkipReadItem, exception is NOT an instance of: MyParentException<p>");
            jobCtx.setExitStatus(BAD_EXIT_STATUS);
        }
    }
		
}
