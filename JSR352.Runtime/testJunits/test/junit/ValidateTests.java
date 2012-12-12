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


import org.junit.Test;

import com.ibm.batch.container.artifact.proxy.ProxyFactory;
import com.ibm.batch.container.validation.ArtifactValidationException;

public class ValidateTests {
    
    @Test(expected=ArtifactValidationException.class)
    public void validateIncompleteBatchlet() throws Exception {        
        ProxyFactory.createBatchletProxy("MyIncompleteBatchlet", null);
    }
    
    @Test(expected=ArtifactValidationException.class)
    public void validateArtifactIdNotABatchlet() throws Exception {
        ProxyFactory.createBatchletProxy("MyJobListenerBIG", null);
    }
    
    @Test(expected=ArtifactValidationException.class)
    public void validateIncompleteItemReader() throws Exception {
        ProxyFactory.createItemReaderProxy("BadItemReader", null);
    }
    
    @Test(expected=ArtifactValidationException.class)
    public void validateNoAccessItemReader() throws Exception {
        ProxyFactory.createItemReaderProxy("AccessLackingItemReader", null);
    }
    
    @Test
    public void validateGoodItemReader() throws Exception {
        ProxyFactory.createItemReaderProxy("GoodItemReader", null);        
    }
    
    @Test
    public void validateDoSomethingItemWriter() throws Exception {
        ProxyFactory.createItemWriterProxy("DoSomethingItemWriter", null);        
    }    
    
    @Test(expected=ArtifactValidationException.class)
    public void validateBatchletProcessDoesntReturnString() throws Exception {
        ProxyFactory.createBatchletProxy("BadBatchletProcessSignature", null);
    }
    
    @Test(expected=ArtifactValidationException.class)
    public void validateDeciderDecideDoesntReturnString() throws Exception {
        ProxyFactory.createDeciderProxy("DeciderDoesntDecide", null);
    }
    
    @Test
    public void validateMyBatchletImplNoId() throws Exception {
        ProxyFactory.createBatchletProxy("myBatchletImplNoId", null);        
    }    
    
    @Test
    public void validateMyItemWriterImplNoId() throws Exception {
        ProxyFactory.createItemWriterProxy("myItemWriterImplNoId", null);        
    }    
}
