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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.batch.annotation.Batchlet;
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
import javax.batch.annotation.PartitionAnalyzer;
import javax.batch.annotation.PartitionCollector;
import javax.batch.annotation.CheckpointAlgorithm;
import javax.batch.annotation.RetryListener;
import javax.batch.annotation.SkipListener;
import javax.batch.annotation.StepListener;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import javax.tools.Diagnostic.Kind;

@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedAnnotationTypes("javax.batch.annotation.*")
public class BatchAnnotationProcessor extends AbstractProcessor {

    private final static Logger logger = Logger.getLogger(BatchAnnotationProcessor.class.getName());

    // We use a (synchronized)vector in case the annotation processing is
    // multithreaded
    Vector<XMLBeanDefinition> beanDefinitions = new Vector<XMLBeanDefinition>();

    private final static String BATCHXML = "META-INF/batch.xml";

    private Messager messager;
    private Filer filer;

    private FileObject resourceFile;

    private void writeBatchXML() {
        try {

            PrintWriter pw = new PrintWriter(resourceFile.openWriter());
            pw.print("<batch-artifacts>\n");

            for (XMLBeanDefinition beanDef : this.beanDefinitions) {
                pw.print("    " + beanDef.getXMLString() + "\n");
            }
            pw.print("</batch-artifacts>\n");

            pw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /*************************************************************************************
     * Core Processor starts here
     *************************************************************************************/

    public BatchAnnotationProcessor() {
        super();
    }

    @Override
    public void init(ProcessingEnvironment pe) {
        
        super.init(pe);
        messager = pe.getMessager();
        filer = pe.getFiler();
        if (filer == null)
            messager.printMessage(Kind.ERROR, "No filer!");

        // create the batch.xml
        try {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Overwriting " + BATCHXML);
            }
            
            resourceFile = filer.createResource(StandardLocation.CLASS_OUTPUT, "", BATCHXML, (Element[]) null);
            
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Successfully overwrote " + BATCHXML);
            }
        } catch (FilerException e) {
            if (logger.isLoggable(Level.SEVERE)) {
                logger.severe("Cannot create " + BATCHXML + " :" + e.getMessage());
            }
            
            throw new IllegalStateException(e);
            
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /*
     * Implements default per:
     * http://docs.jboss.org/cdi/spec/1.0/html/implementation.html
     * Sec. 3.1.5 Default name for a managed bean
     */
    private String getId(Element element, String value) {
        String retVal = null;
        if (value.equals("")) {
            String simpleName = element.getSimpleName().toString();
            String simpleNameFirst = simpleName.substring(0,1).toLowerCase();
            String simpleNameRest = simpleName.substring(1);
            retVal = simpleNameFirst + simpleNameRest;  // Works on 1-char boundary condition where "rest" is empty string
        } else {
            retVal = value;
        }
        return retVal;
    }

    private void processRootElement(Element element) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Processing root element: " + element.toString());
        }

        List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();

        XMLBeanDefinition beanXML = null;

        for (AnnotationMirror aMirror : annotationMirrors) {

            final String annotationType = aMirror.getAnnotationType().toString();
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("Processing annotation type: " + annotationType.toString());
            }

            if (annotationType.equals(JobListener.class.getName())) {
                String value = element.getAnnotation(JobListener.class).value();
                String beanID = getId(element, value);
                String xmlElement = XMLBeanDefinition.getXMLElement(JobListener.class);
                String className = element.toString();

                beanXML = new XMLBeanDefinition(xmlElement, beanID, className);

            } else if (annotationType.equals(CheckpointListener.class.getName())) {
                String value = element.getAnnotation(CheckpointListener.class).value();
                String beanID = getId(element, value);
                String xmlElement = XMLBeanDefinition.getXMLElement(CheckpointListener.class);
                String className = element.toString();

                beanXML = new XMLBeanDefinition(xmlElement, beanID, className);

            } else if (annotationType.equals(ItemProcessListener.class.getName())) {
                String value = element.getAnnotation(ItemProcessListener.class).value();
                String beanID = getId(element, value);
                String xmlElement = XMLBeanDefinition.getXMLElement(ItemProcessListener.class);
                String className = element.toString();

                beanXML = new XMLBeanDefinition(xmlElement, beanID, className);

            } else if (annotationType.equals(ItemProcessor.class.getName())) {
                String value = element.getAnnotation(ItemProcessor.class).value();
                String beanID = getId(element, value);
                String xmlElement = XMLBeanDefinition.getXMLElement(ItemProcessor.class);
                String className = element.toString();

                beanXML = new XMLBeanDefinition(xmlElement, beanID, className);

            } else if (annotationType.equals(ItemReader.class.getName())) {
                String value = element.getAnnotation(ItemReader.class).value();
                String beanID = getId(element, value);
                String xmlElement = XMLBeanDefinition.getXMLElement(ItemReader.class);
                String className = element.toString();

                beanXML = new XMLBeanDefinition(xmlElement, beanID, className);

            } else if (annotationType.equals(ItemReadListener.class.getName())) {
                String value = element.getAnnotation(ItemReadListener.class).value();
                String beanID = getId(element, value);
                String xmlElement = XMLBeanDefinition.getXMLElement(ItemReadListener.class);
                String className = element.toString();

                beanXML = new XMLBeanDefinition(xmlElement, beanID, className);

            } else if (annotationType.equals(ItemWriteListener.class.getName())) {
                String value = element.getAnnotation(ItemWriteListener.class).value();
                String beanID = getId(element, value);

                String xmlElement = XMLBeanDefinition.getXMLElement(ItemWriteListener.class);
                String className = element.toString();

                beanXML = new XMLBeanDefinition(xmlElement, beanID, className);

            } else if (annotationType.equals(ItemWriter.class.getName())) {
                String value = element.getAnnotation(ItemWriter.class).value();
                String beanID = getId(element, value);

                String xmlElement = XMLBeanDefinition.getXMLElement(ItemWriter.class);
                String className = element.toString();

                beanXML = new XMLBeanDefinition(xmlElement, beanID, className);

            } else if (annotationType.equals(RetryListener.class.getName())) {
                String value = element.getAnnotation(RetryListener.class).value();
                String beanID = getId(element, value);

                String xmlElement = XMLBeanDefinition.getXMLElement(RetryListener.class);
                String className = element.toString();

                beanXML = new XMLBeanDefinition(xmlElement, beanID, className);

            } else if (annotationType.equals(StepListener.class.getName())) {
                String value = element.getAnnotation(StepListener.class).value();
                String beanID = getId(element, value);

                String xmlElement = XMLBeanDefinition.getXMLElement(StepListener.class);
                String className = element.toString();

                beanXML = new XMLBeanDefinition(xmlElement, beanID, className);

            } else if (annotationType.equals(SkipListener.class.getName())) {
                String value = element.getAnnotation(SkipListener.class).value();
                String beanID = getId(element, value);

                String xmlElement = XMLBeanDefinition.getXMLElement(SkipListener.class);
                String className = element.toString();

                beanXML = new XMLBeanDefinition(xmlElement, beanID, className);

            } else if (annotationType.equals(Batchlet.class.getName())) {
                String value = element.getAnnotation(Batchlet.class).value();
                String beanID = getId(element, value);

                String xmlElement = XMLBeanDefinition.getXMLElement(Batchlet.class);
                String className = element.toString();

                beanXML = new XMLBeanDefinition(xmlElement, beanID, className);

            } else if (annotationType.equals(Decider.class.getName())) {
                String value = element.getAnnotation(Decider.class).value();
                String beanID = getId(element, value);

                String xmlElement = XMLBeanDefinition.getXMLElement(Decider.class);
                String className = element.toString();

                beanXML = new XMLBeanDefinition(xmlElement, beanID, className);

            } else if (annotationType.equals(PartitionCollector.class.getName())) {
                String value = element.getAnnotation(PartitionCollector.class).value();
                String beanID = getId(element, value);

                String xmlElement = XMLBeanDefinition.getXMLElement(PartitionCollector.class);
                String className = element.toString();

                beanXML = new XMLBeanDefinition(xmlElement, beanID, className);

            } else if (annotationType.equals(PartitionAnalyzer.class.getName())) {
                String value = element.getAnnotation(PartitionAnalyzer.class).value();
                String beanID = getId(element, value);

                String xmlElement = XMLBeanDefinition.getXMLElement(PartitionAnalyzer.class);
                String className = element.toString();

                beanXML = new XMLBeanDefinition(xmlElement, beanID, className);

            } else if (annotationType.equals(PartitionMapper.class.getName())) {
                String value = element.getAnnotation(PartitionMapper.class).value();
                String beanID = getId(element, value);

                String xmlElement = XMLBeanDefinition.getXMLElement(PartitionMapper.class);
                String className = element.toString();

                beanXML = new XMLBeanDefinition(xmlElement, beanID, className);

            } else if (annotationType.equals(CheckpointAlgorithm.class.getName())) {
                String value = element.getAnnotation(CheckpointAlgorithm.class).value();
                String beanID = getId(element, value);

                String xmlElement = XMLBeanDefinition.getXMLElement(CheckpointAlgorithm.class);
                String className = element.toString();

                beanXML = new XMLBeanDefinition(xmlElement, beanID, className);

            }
        	
            if (beanXML != null) {
                                
                if (logger.isLoggable(Level.FINER)) {
                    logger.finer("Create batch artifact bean definition: " + beanXML.getXMLString());
                }
                
                this.beanDefinitions.add(beanXML);
            }

        }

    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Processing annotations");
        }

        if (!roundEnv.processingOver()) {
            Set<? extends Element> elements = roundEnv.getRootElements();

            for (Element element : elements) {
                this.processRootElement(element);
            }

        } else {
            // on the last pass write out the entire batch.xml genned
            // from previous rounds
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Writing: " + BATCHXML);
            }
            this.writeBatchXML();
        }

        return true; // true means annotation has been "claimed" by this
        // processor
    }

}
