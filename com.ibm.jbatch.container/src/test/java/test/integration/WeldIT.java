package test.integration;

import com.ibm.jbatch.container.api.impl.JobOperatorImpl;
import com.ibm.jbatch.container.services.impl.DelegatingBatchArtifactFactoryImpl;
import com.ibm.jbatch.container.services.impl.WeldSEBatchArtifactFactoryImpl;
import com.ibm.jbatch.spi.ServiceRegistry.ServiceImplClassNames;
import com.ibm.jbatch.spi.ServiceRegistry.ServicePropertyNames;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.operations.NoSuchJobException;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.StepExecution;

/**
 * @author <a href="mailto:brent.n.douglas@gmail.com">Brent Douglas</a>
 */
public class WeldIT {

    static final String PROP_PREFIX = "com.ibm.jbatch.spi.ServiceRegistry";

    private static JobOperator operator;

    @BeforeClass
    public static void beforeClass() {
		System.setProperty(PROP_PREFIX + "." + ServicePropertyNames.CONTAINER_ARTIFACT_FACTORY_SERVICE, ServiceImplClassNames.CONTAINER_ARTIFACT_FACTORY_WELD_SE);
		System.setProperty(PROP_PREFIX + "." + ServicePropertyNames.J2SE_MODE, "true");
		System.setProperty(PROP_PREFIX + "." + ServicePropertyNames.BATCH_THREADPOOL_SERVICE, ServiceImplClassNames.BATCH_THREADPOOL_GROWABLE);
    	operator = BatchRuntime.getJobOperator();
    }

    @Test
    public void testWeldWorks() throws InterruptedException {

        final JobExecution job = awaitJob("weldArtifactFactoryTest");
        Assert.assertEquals(BatchStatus.COMPLETED, job.getBatchStatus());

        final StepExecution step = operator.getStepExecutions(job.getExecutionId()).get(0);
        Assert.assertEquals(BatchStatus.COMPLETED, step.getBatchStatus());
    }

    private JobExecution awaitJob(final String job) {
        final long id = operator.start(job, null);

        try {
            for (;;) {
                operator.getRunningExecutions(job);
                try {
                    Thread.sleep(100);
                } catch (final InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (final NoSuchJobException e) {
            return operator.getJobExecution(id);
        }
    }
}
