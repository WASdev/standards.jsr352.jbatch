/*
 * Copyright 2014 International Business Machines Corp.
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package test.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import jakarta.batch.api.chunk.AbstractItemReader;
import jakarta.batch.api.chunk.AbstractItemWriter;
import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.StepExecution;

import org.junit.BeforeClass;
import org.junit.Test;

public class ChunkStopTest {

    private static JobOperator jobOp = null;
    private static int sleepTime = 2000;

    @BeforeClass
    public static void init() {
        jobOp = BatchRuntime.getJobOperator();
    }

    @Test
    public void testChunkStop() throws Exception {
        final List<BatchStatus> statuses=Arrays.asList(BatchStatus.STOPPED, BatchStatus.STOPPING);
        //the two potential states we expect for the job. Timing variance can lead to a job still 'stopping' when it is supposed to be 'stopped'
        long execID = jobOp.start("chunkStopTest", null);
        Thread.sleep(sleepTime * 2);
        jobOp.stop(execID);

        Thread.sleep(sleepTime * 2);
        JobExecution je = jobOp.getJobExecution(execID);
        assertTrue("Job BatchStatus: ", statuses.contains(je.getBatchStatus())); //see if job is in either expected state
        StepExecution step = jobOp.getStepExecutions(execID).get(0);
        assertTrue("Step BatchStatus: ", statuses.contains(step.getBatchStatus()));
    }

    public static class Reader extends AbstractItemReader {
        @Override
        public Object readItem() throws Exception {
            Thread.sleep(sleepTime);
            return Long.toString(System.currentTimeMillis());
        }
    }

    public static class Writer extends AbstractItemWriter {
        @Override
        public void writeItems(List<Object> items) throws Exception {
            Thread.sleep(sleepTime);
            for (Object item : items) {
                System.out.println(ChunkStopTest.class + ": " + item);
            }
        }
    }
}
