/*
 * Copyright 2012 International Business Machines Corp.
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

import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import jsr352.test.BadItemReaderImpl;
import jsr352.test.DoSomethingItemProcessorImpl;
import jsr352.test.MyBatchletImpl;
import jsr352.test.MyItemProcessListenerImpl;
import jsr352.test.MyItemReadListenerImpl;
import jsr352.test.MyItemWriteListenerImpl;
import jsr352.test.MyItemWriterImpl;
import jsr352.test.MyJobListenerImpl;
import jsr352.test.MyJobListenerImpl2;
import jsr352.test.MyRetryListenerImpl;
import jsr352.test.MySkipAndStepListener;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.ibm.batch.container.services.IBatchArtifactFactory;
import com.ibm.batch.container.services.ServicesManager;
import com.ibm.batch.container.services.ServicesManager.ServiceType;

public class LoadTests {

    private static ServicesManager servicesManager = ServicesManager.getInstance();
    private static IBatchArtifactFactory batchArtifactFactory = null;
    
    private final static Logger logger = Logger.getLogger(LoadTests.class.getName());
    
    @BeforeClass
    public static void setUp() throws Exception {
        batchArtifactFactory = 
            (IBatchArtifactFactory)servicesManager.getService(ServiceType.CONTAINER_ARTIFACT_FACTORY_SERVICE);
    }

    @AfterClass
    public static void tearDown() throws Exception {
    }
    
    
    @Test
    public void testJobListenerBIG() throws Exception {        
        Object listener = batchArtifactFactory.load("MyJobListenerBIG");
        assertTrue(listener instanceof MyJobListenerImpl);        
    }
    
    @Test
    public void testJobListenerSmall() throws Exception {        
        Object listener = batchArtifactFactory.load("MyJobListenerSmall");
        assertTrue(listener instanceof MyJobListenerImpl2);        
    }
    
    @Test
    public void testBatchletLoad() throws Exception {        
        Object batchlet = batchArtifactFactory.load("MyBatchlet");
        assertTrue(batchlet instanceof MyBatchletImpl);        
    }   

    @Test
    public void testBatchlet() throws Exception {
        Object bean = batchArtifactFactory.load("MyBatchlet");
        assertTrue(bean instanceof MyBatchletImpl);
    }

    @Ignore
    public void testCheckpointListenter() throws Exception {
        // FIXME
    }

    @Test
    public void testItemProcessor() throws Exception {
        Object retryListener = batchArtifactFactory.load("DoSomethingItemProcessor");
        assertTrue(retryListener instanceof DoSomethingItemProcessorImpl);
    }

    @Test
    public void testItemProcessListener() throws Exception {
    	Object itemProcessListener = batchArtifactFactory.load("MyItemProcessListener");
        assertTrue(itemProcessListener instanceof MyItemProcessListenerImpl);
    }
    
    @Test
    public void testItemReader() throws Exception {
        Object bean = batchArtifactFactory.load("BadItemReader");
        assertTrue(bean instanceof BadItemReaderImpl);
    }

    @Test
    public void testItemReadListener() throws Exception {
    	Object itemReadListener = batchArtifactFactory.load("MyItemReadListener");
        assertTrue(itemReadListener instanceof MyItemReadListenerImpl);
    }

    @Test
    public void testItemWriter() throws Exception {
        Object bean = batchArtifactFactory.load("MyItemWriter");
        assertTrue(bean instanceof MyItemWriterImpl);
    }

    @Test
    public void testItemWriteListener() throws Exception {
    	Object itemWriteListener = batchArtifactFactory.load("MyItemWriteListener");
        assertTrue(itemWriteListener instanceof MyItemWriteListenerImpl);
    }
    
    @Ignore
    public void testPartitionMapper() throws Exception {
        //FIXME
    }

    @Test
    public void testRetryListener() throws Exception {
        Object retryListener = batchArtifactFactory.load("MyRetryListener");
        assertTrue(retryListener instanceof MyRetryListenerImpl);
    }

    @Test
    public void testSkipListener() throws Exception {
    	 Object skipListener = batchArtifactFactory.load("mySkipAndStepListener");
         assertTrue(skipListener instanceof MySkipAndStepListener);
    }

    @Test
    public void testStepListener() throws Exception {
    	Object stepListener = batchArtifactFactory.load("mySkipAndStepListener");
        assertTrue(stepListener instanceof MySkipAndStepListener);
    }

    @Ignore
    public void testSubJobAnalyzer() throws Exception {
        //FIXME
    }

    @Ignore
    public void testSubJobCollector() throws Exception {
        //FIXME
    }
    
    
}
