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
package com.ibm.batch.annotation.processor;

import java.lang.annotation.Annotation;
import java.util.HashMap;

import javax.batch.annotation.Batchlet;
import javax.batch.annotation.CheckpointAlgorithm;
import javax.batch.annotation.CheckpointListener;
import javax.batch.annotation.Decider;
import javax.batch.annotation.ItemProcessor;
import javax.batch.annotation.ItemProcessListener;
import javax.batch.annotation.ItemReadListener;
import javax.batch.annotation.ItemReader;
import javax.batch.annotation.ItemWriteListener;
import javax.batch.annotation.ItemWriter;
import javax.batch.annotation.JobListener;
import javax.batch.annotation.PartitionMapper;
import javax.batch.annotation.RetryListener;
import javax.batch.annotation.SkipListener;
import javax.batch.annotation.StepListener;
import javax.batch.annotation.PartitionAnalyzer;
import javax.batch.annotation.PartitionCollector;

public class XMLBeanDefinition {

    public final String xmlElementName;
    public final String beanID;
    public final String className;

    private StringBuffer buf = new StringBuffer(100);
    
    private static final HashMap<Class<? extends Annotation> ,String> annotation2XML;
    
    static {
        annotation2XML = new HashMap<Class<? extends Annotation> ,String>();
        annotation2XML.put(Batchlet.class, "batchlet");
        annotation2XML.put(CheckpointListener.class, "checkpoint-listener");
        annotation2XML.put(Decider.class, "decider");
        annotation2XML.put(ItemProcessor.class, "item-processor");
        annotation2XML.put(ItemProcessListener.class, "itemprocessor-listener");
        annotation2XML.put(ItemReader.class, "item-reader");
        annotation2XML.put(ItemReadListener.class, "itemread-listener");
        annotation2XML.put(ItemWriteListener.class, "itemwrite-listener");
        annotation2XML.put(ItemWriter.class, "item-writer");
        annotation2XML.put(JobListener.class, "job-listener");
        annotation2XML.put(PartitionMapper.class, "partition-mapper");
        annotation2XML.put(RetryListener.class, "retry-listener");
        annotation2XML.put(SkipListener.class, "skip-listener");
        annotation2XML.put(StepListener.class, "step-listener");
        annotation2XML.put(PartitionAnalyzer.class, "partition-analyzer");
        annotation2XML.put(PartitionCollector.class, "partition-collector");
        annotation2XML.put(CheckpointAlgorithm.class, "checkpoint-algorithm");
        
    }
    

    public XMLBeanDefinition(String xmlElementName, String beanID, String className) {
        this.xmlElementName = xmlElementName;
        this.beanID = beanID;
        this.className = className;

        buf.append("<");
        buf.append(this.xmlElementName);
        buf.append(" ");
        buf.append("id=\"");
        buf.append(this.beanID);
        buf.append("\" class=\"");
        buf.append(this.className);
        buf.append("\" />");
    }

    public String getXMLString() {
        return buf.toString();
    }
    
    public static String getXMLElement(Class<? extends Annotation> annotation) {
        return annotation2XML.get(annotation);
    }
}
