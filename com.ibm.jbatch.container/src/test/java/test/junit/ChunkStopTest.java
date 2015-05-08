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

import java.util.List;

import javax.batch.api.chunk.AbstractItemReader;
import javax.batch.api.chunk.AbstractItemWriter;
import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.StepExecution;

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
        long execID = jobOp.start("chunkStopTest", null);
        Thread.sleep(sleepTime * 2);
        jobOp.stop(execID);

        Thread.sleep(sleepTime * 2);
        JobExecution je = jobOp.getJobExecution(execID);
        assertEquals("Job BatchStatus: ", BatchStatus.STOPPED, je.getBatchStatus());
        StepExecution step = jobOp.getStepExecutions(execID).get(0);
        assertEquals("Step BatchStatus: ", BatchStatus.STOPPED, step.getBatchStatus());
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
