package test.artifacts;

import java.util.Properties;

import javax.batch.api.BatchProperty;
import javax.batch.api.partition.PartitionMapper;
import javax.batch.api.partition.PartitionPlan;
import javax.batch.api.partition.PartitionPlanImpl;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;


public class MyPropertyMapper implements PartitionMapper{
	
	@Inject
	StepContext stepCtx;
	
	@Inject @BatchProperty(name="stepProp2")
	String stepProp2;

	@Override
	public PartitionPlan mapPartitions() throws Exception {

		PartitionPlanImpl pp = new PartitionPlanImpl();
		pp.setPartitions(4);
		Properties p0 = new Properties();
		p0.setProperty("part", "");
		Properties p1 = new Properties();
		p1.setProperty("part", "");
		Properties p2 = new Properties();
		p2.setProperty("part", "");
		Properties p3 = new Properties();
		p3.setProperty("part", "");
		Properties[] partitionProps = new Properties[4];
		partitionProps[0] = p0;
		partitionProps[1] = p1;
		partitionProps[2] = p2;
		partitionProps[3] = p3;
		pp.setPartitionProperties(partitionProps);
		
		stepCtx.setExitStatus(stepCtx.getExitStatus() + ":" + stepProp2 + ":" + pp.getPartitions());

		return pp;
	}

}
