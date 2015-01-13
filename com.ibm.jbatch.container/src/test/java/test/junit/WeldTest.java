package test.junit;

import com.ibm.jbatch.container.api.impl.JobOperatorImpl;
import com.ibm.jbatch.container.services.impl.DelegatingBatchArtifactFactoryImpl;
import com.ibm.jbatch.container.services.impl.WeldSEBatchArtifactFactoryImpl;
import com.ibm.jbatch.container.servicesmanager.ServiceTypes;
import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.batch.operations.JobOperator;
import javax.batch.operations.NoSuchJobException;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.StepExecution;

/**
 * @author <a href="mailto:brent.n.douglas@gmail.com">Brent Douglas</a>
 */
public class WeldTest {

    private static JobOperator operator;

    @BeforeClass
    public static void beforeClass() {
        operator = new JobOperatorImpl();
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
