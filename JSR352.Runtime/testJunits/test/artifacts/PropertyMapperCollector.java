package test.artifacts;

import java.io.Serializable;

import javax.batch.api.partition.PartitionCollector;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

public class PropertyMapperCollector implements PartitionCollector {

	@Inject
	StepContext stepCtx;
		
	@Override
	public Serializable collectPartitionData() throws Exception {
		String stepProperty = String.valueOf(stepCtx.getProperties());
		String[] tokens = stepProperty.split("[{}=]");
		String stepPropValue = tokens[2];

		String data = (String) stepCtx.getPersistentUserData() + "?" + stepPropValue;
		
		return data;
	}

}
