package test.artifacts;

import java.util.logging.Logger;

import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

public class PropertyResolverBatchImplTest extends AbstractBatchlet{

	private final static Logger logger = Logger.getLogger(PropertyResolverBatchImplTest.class.getName());

	private volatile static int count = 1;
	private volatile static String data = "";

	public static String GOOD_EXIT_STATUS = "VERY GOOD INVOCATION";       

	@Inject
	JobContext jobCtx;
	
	@Inject
	StepContext stepCtx;
	
	@Inject @BatchProperty(name="partitionString")
	String partitionString;
	
	@Inject @BatchProperty
	public String sleepTime;
	int sleepVal = 0;

	@Inject @BatchProperty
	public String forceFailure = "false";
	Boolean fail;

	private void init() {
		try {
			fail = Boolean.parseBoolean(forceFailure);
		} catch (Exception e) { 
			fail = false;
		}
		try {
			sleepVal = Integer.parseInt(sleepTime);
		} catch (Exception e) { 
			sleepVal = 0;
		}
	}
	@Override
	public String process() throws Exception {	
		init();
		if (fail) {
			throw new IllegalArgumentException("Forcing failure");
		}
		if (sleepTime != null) {
			Thread.sleep(sleepVal);
		}
		logger.fine("Running batchlet process(): " + count);
		setDataValue();
		count++;

		return GOOD_EXIT_STATUS;
	}

	@Override
	public void stop() throws Exception { }
	
	private void setDataValue() {
		data = "#" + partitionString;
		stepCtx.setPersistentUserData(data);
	}
}
