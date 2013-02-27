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
package com.ibm.jbatch.tck.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.api.Batchlet;
import javax.batch.api.CheckpointAlgorithm;
import javax.batch.api.ChunkListener;
import javax.batch.api.Decider;
import javax.batch.api.ItemProcessListener;
import javax.batch.api.ItemProcessor;
import javax.batch.api.ItemReadListener;
import javax.batch.api.ItemReader;
import javax.batch.api.ItemWriteListener;
import javax.batch.api.ItemWriter;
import javax.batch.api.JobListener;
import javax.batch.api.PartitionAnalyzer;
import javax.batch.api.PartitionCollector;
import javax.batch.api.PartitionMapper;
import javax.batch.api.PartitionReducer;
import javax.batch.api.RetryProcessListener;
import javax.batch.api.RetryReadListener;
import javax.batch.api.RetryWriteListener;
import javax.batch.api.SkipProcessListener;
import javax.batch.api.SkipReadListener;
import javax.batch.api.SkipWriteListener;
import javax.batch.api.StepListener;

public class BatchXMLGenerator {
    
    private final static Logger logger = Logger.getLogger(BatchXMLGenerator.class.getName());
    
    private static final String SLASH = System.getProperty("file.separator");
    
    List<BeanDefinition> beanDefinitions = new ArrayList<BeanDefinition>();

    private final static String BATCHXML = "META-INF/batch.xml";

    private void writeBatchXML(File dir) {
        
        
        try {

            File batchXMLFile = new File (dir, "batch.xml");
            
            logger.info("Writing batch.xml: " + batchXMLFile);
            
            BufferedWriter writer = new BufferedWriter(new FileWriter(batchXMLFile));
            
            writer.write("<batch-artifacts xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\">\n");

            for (BeanDefinition beanDef : this.beanDefinitions) {
                writer.write("    " + beanDef.getXMLString() + "\n");
            }
            writer.write("</batch-artifacts>\n");

            writer.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }



    /*
     * Implements default per:
     * http://docs.jboss.org/cdi/spec/1.0/html/implementation.html Sec. 3.1.5
     * Default name for a managed bean
     */
    private String generateId(String qualifiedClassName) {
        String retVal = null;

        int index = qualifiedClassName.lastIndexOf(".");
        
        //We don't check for the default package
        String simpleName = qualifiedClassName.substring(index+1);
        
        String simpleNameFirst = simpleName.substring(0, 1).toLowerCase();
        String simpleNameRest = simpleName.substring(1);
        retVal = simpleNameFirst + simpleNameRest; // Works on 1-char
                                                   // boundary condition
                                                   // where "rest" is empty
                                                   // string

        return retVal;
    }

    private void addBeanReftoBatchXML(String qualifiedClassName) {
        String beanID = generateId(qualifiedClassName);

        BeanDefinition beanXML = new BeanDefinition(beanID, qualifiedClassName);

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Adding bean to batch.xml: beanId=" + beanID + " className=" + qualifiedClassName);
        }

        this.beanDefinitions.add(beanXML);
    }

    private void processClass(String qualifiedClassName) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Processing class: " + qualifiedClassName);
        }

        Class<?> artifactClass = null;
        try {
            artifactClass = Class.forName(qualifiedClassName);
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (JobListener.class.isAssignableFrom(artifactClass)) {
            this.addBeanReftoBatchXML(qualifiedClassName);
        }

        if (ChunkListener.class.isAssignableFrom(artifactClass)) {
            this.addBeanReftoBatchXML(qualifiedClassName);

        }

        if (ItemProcessListener.class.isAssignableFrom(artifactClass)) {
            this.addBeanReftoBatchXML(qualifiedClassName);

        }

        if (ItemProcessor.class.isAssignableFrom(artifactClass)) {
            this.addBeanReftoBatchXML(qualifiedClassName);

        }

        if (ItemReader.class.isAssignableFrom(artifactClass)) {
            this.addBeanReftoBatchXML(qualifiedClassName);

        }

        if (ItemReadListener.class.isAssignableFrom(artifactClass)) {
            this.addBeanReftoBatchXML(qualifiedClassName);

        }

        if (ItemWriteListener.class.isAssignableFrom(artifactClass)) {
            this.addBeanReftoBatchXML(qualifiedClassName);

        }

        if (ItemWriter.class.isAssignableFrom(artifactClass)) {
            this.addBeanReftoBatchXML(qualifiedClassName);

        }

        if (RetryReadListener.class.isAssignableFrom(artifactClass)) {
            this.addBeanReftoBatchXML(qualifiedClassName);

        }
        
        if (RetryWriteListener.class.isAssignableFrom(artifactClass)) {
            this.addBeanReftoBatchXML(qualifiedClassName);

        }
        
        if (RetryProcessListener.class.isAssignableFrom(artifactClass)) {
            this.addBeanReftoBatchXML(qualifiedClassName);

        }

        if (StepListener.class.isAssignableFrom(artifactClass)) {
            this.addBeanReftoBatchXML(qualifiedClassName);
        }

        if (SkipProcessListener.class.isAssignableFrom(artifactClass)) {
            this.addBeanReftoBatchXML(qualifiedClassName);

        }
        
        if (SkipReadListener.class.isAssignableFrom(artifactClass)) {
            this.addBeanReftoBatchXML(qualifiedClassName);

        }
        
        if (SkipWriteListener.class.isAssignableFrom(artifactClass)) {
            this.addBeanReftoBatchXML(qualifiedClassName);

        }

        if (Batchlet.class.isAssignableFrom(artifactClass)) {
            this.addBeanReftoBatchXML(qualifiedClassName);

        }

        if (Decider.class.isAssignableFrom(artifactClass)) {
            this.addBeanReftoBatchXML(qualifiedClassName);

        }

        if (PartitionCollector.class.isAssignableFrom(artifactClass)) {
            this.addBeanReftoBatchXML(qualifiedClassName);

        }

        if (PartitionAnalyzer.class.isAssignableFrom(artifactClass)) {
            this.addBeanReftoBatchXML(qualifiedClassName);

        }

        if (PartitionReducer.class.isAssignableFrom(artifactClass)) {
            this.addBeanReftoBatchXML(qualifiedClassName);

        }

        if (PartitionMapper.class.isAssignableFrom(artifactClass)) {
            this.addBeanReftoBatchXML(qualifiedClassName);

        }

        if (CheckpointAlgorithm.class.isAssignableFrom(artifactClass)) {
            this.addBeanReftoBatchXML(qualifiedClassName);

        }

    }



    
    private static List<String> findClasses(String dir) {
        File directory = new File(dir);
        if (!directory.exists()) {
            throw new IllegalArgumentException("This directory does not exist: " + directory.toString());
        } else if (!directory.isDirectory()) {
            throw new IllegalArgumentException("This is not a directory: " + directory.toString());
        }
        
        
        List<String> classList = new ArrayList<String>();
        
        findClasses(directory, "" , classList);
        
        return classList;
        
    }
    
    private static void findClasses(File directory, String path, List<String> classList) {
        
        File[] files = directory.listFiles();

        for (File file : files) {
            if (file.isDirectory()){
                findClasses(file, path + file.getName() + SLASH , classList);
              
            }
            
            String filename = file.getName();
            if (filename.endsWith(".class")) {
                
                String classname = filename.substring(0, filename.lastIndexOf("."));
                
                classList.add(path.replace(SLASH, ".") + classname);

            }
            
        }
        
    }
    
    public static void main(String[] args){
        logger.info("Starting BatchXMLGenerator");
        
        BatchXMLGenerator bxg = new BatchXMLGenerator();
        
        //FIXME Need more input validation here
        
        List<String> classList = bxg.findClasses(args[0]);
        
        for (String className : classList) {
            bxg.processClass(className);
        }
        
        File batchXMLDir = new File(args[1]);
        
        if (!batchXMLDir.exists()) {
            throw new IllegalArgumentException("This directory does not exist: " + args[1]);
        }
        
        if (!batchXMLDir.isDirectory()) {
            throw new IllegalArgumentException("This is not a directory: " + args[1]);
            
        }
        
        bxg.writeBatchXML(batchXMLDir);
        
        logger.info("BatchXMLGenerator completed successfully.");
        
    }
}
