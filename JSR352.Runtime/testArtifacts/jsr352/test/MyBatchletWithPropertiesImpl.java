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
package jsr352.test;

import java.lang.reflect.Field;

import javax.batch.annotation.BatchProperty;
import javax.batch.annotation.Batchlet;
import javax.batch.annotation.Process;
import javax.batch.annotation.Stop;

@Batchlet("MyBatchletWithProperties")
public class MyBatchletWithPropertiesImpl {

    private static int count = 1;

    public static String GOOD_EXIT_CODE = "VERY GOOD INVOCATION";

    @BatchProperty
    private String myProperty1;

    @BatchProperty
    public String myProperty2 = "This EYECATCHER should get overwritten from the job xml!!";

    @BatchProperty
    public String myDefaultProp1 = "Should get overwritten by default value";
    
    @BatchProperty
    public String mySubmittedProp = "This EYECATCHER should get overwritten by a submitted prop.";
    
    @BatchProperty
    public String mySystemProp = "This EYECATCHER should get overwritten by a system prop.";

    @BatchProperty
    public String batchletProp = "This EYECATCHER should get overwritten.";
    
    @BatchProperty
    private String javaDefaultValueProp;
    
    @BatchProperty(name="myProperty4")
    private String property4;

    @BatchProperty
    String myConcatProp;
    
    @BatchProperty
    String myJavaSystemProp;
    

    @Process
    public String process() throws Exception {

        //FIXME use a submitted job parameter here instead so all tests are independent of each other.
        String propName = System.getProperty("property.junit.propName");
        String propertyValue =  this.getBatchPropertyValue(propName);
        System.setProperty("property.junit.result", "" + propertyValue);
        
        return this.getBatchPropertyValue(propName);
            
    }

    
    @Stop
    public void cancel() throws Exception {
        System.out.println("MyBatchletWithProperties.cancel() - @Cancel #" + count);
    }

    
    private String getBatchPropertyValue(String name) throws IllegalArgumentException, IllegalAccessException {
        Field[] fields = MyBatchletWithPropertiesImpl.class.getDeclaredFields();
        
        for (Field field: fields) {
            BatchProperty batchProperty = field.getAnnotation(BatchProperty.class);
            if (batchProperty != null) {
                if (!!!batchProperty.name().equals("") && batchProperty.name().equals(name)){
                    return (String)field.get(this);
                } else if (field.getName().equals(name)){
                    return (String)field.get(this);
                }
            } 
        }
        
        return null;
        
    }
}
