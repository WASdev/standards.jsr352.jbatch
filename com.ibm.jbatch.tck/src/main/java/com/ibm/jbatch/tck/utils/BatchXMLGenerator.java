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

import javax.inject.Named;

/**
 * This class is not needed at all by the executor of the TCK.   It
 * is used to generate the META-INF/batch.xml associated with the
 * set of batch artifacts used in the TCK
 * 
 * It was convenient to keep it in the same project with the TCK itself but it
 * can be ignored by someone simply running/executing the TCK.
 */
public class BatchXMLGenerator {

	private final static Logger logger = Logger.getLogger(BatchXMLGenerator.class.getName());

	private static final String SLASH = System.getProperty("file.separator");

	List<BeanDefinition> beanDefinitions = new ArrayList<BeanDefinition>();

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

	private void processClass(String qualifiedClassName) {
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Processing class: " + qualifiedClassName);
		}

		String namedAnnotationValue = null;
		Class<?> artifactClass = null;
		try {
			artifactClass = Class.forName(qualifiedClassName);

			boolean isBatchArtifact = isBatchArtifact(artifactClass);
			if (!isBatchArtifact) {
				return;
			}
			Named namedAnnotation = artifactClass.getAnnotation(Named.class);
			if (namedAnnotation != null) {
				namedAnnotationValue = namedAnnotation.value();
			}
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			// Continue and use classname-based defaulting
		}

		String beanID = null;
		// If we see a @Named with empty value (default), then use the classname-based calculation,
		// which is purposely designed to mirror the CDI default.
		if (namedAnnotationValue != null && !namedAnnotationValue.trim().isEmpty()) {
			beanID = namedAnnotationValue;
		} else {
			beanID = generateId(qualifiedClassName);
		}

		BeanDefinition beanXML = new BeanDefinition(beanID, qualifiedClassName);

		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Adding bean to batch.xml: beanId=" + beanID + " className=" + qualifiedClassName);
		}

		this.beanDefinitions.add(beanXML);
	}


	private boolean isBatchArtifact(Class artifactClass) {
		// logger.fine("Entering isBatchArtifact for class: " + artifactClass == null ? "<null>" : artifactClass.getCanonicalName());
		
		if (artifactClass == null) {
			logger.fine("End of the line, returning false.");
			return false;
		}
		
		//
		// All batch artifacts implement an API interface.
		//
		Class[] interfaces = artifactClass.getInterfaces(); 
		if (interfaces.length == 0) {
			logger.fine("No batch interfaces found for class: " + artifactClass.getCanonicalName() + ", since it doesn't implement any interfaces. Will try superclass (if one exists).");
			return isBatchArtifact(artifactClass.getSuperclass());
		} 
		for (Class interfaze : interfaces) {
			if (interfaze.getCanonicalName().startsWith("javax.batch")) {
				logger.fine("Found a batch interface found for class: " + artifactClass.getCanonicalName() + ".  Continuing to add this entry to batch.xml");
				return true;
			}
		}
		logger.fine("No batch interfaces found for class: " + artifactClass.getCanonicalName() + ".  Will try superclass (if one exists).");
		return isBatchArtifact(artifactClass.getSuperclass());
	}


	private static List<String> findClasses(final String dir, final String prefix) {
		File directory = new File(dir, prefix);
        logger.info("Searching : " + directory);
		if (!directory.exists()) {
			throw new IllegalArgumentException("This directory does not exist: " + directory.toString());
		} else if (!directory.isDirectory()) {
			throw new IllegalArgumentException("This is not a directory: " + directory.toString());
		}


		List<String> classList = new ArrayList<String>();

		findClasses(directory, prefix, classList);

		return classList;

	}

	private static void findClasses(File directory, String path, List<String> classList) {
        logger.info("Searching : " + directory);

		File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
		for (File file : files) {
			if (file.isDirectory()){
                final String nextPath = path.endsWith(SLASH)
                        ? path + file.getName() + SLASH
                        : path + SLASH + file.getName() + SLASH;
				findClasses(file, nextPath , classList);

			}

			String filename = file.getName();
			if (filename.endsWith(".class")) {

				String classname = filename.substring(0, filename.lastIndexOf("."));

                final String fqcn = path.replace(SLASH, ".") + classname;
                logger.info("Found " + fqcn);
				classList.add(fqcn);
			}

		}
	}

	public static void main(String[] args){
		logger.info("Starting BatchXMLGenerator");

        final String srcDir = args[0];
        final String startSearchAt = args[1];
        final String outputDir = args[2];

		BatchXMLGenerator bxg = new BatchXMLGenerator();

        final File src = new File(srcDir, startSearchAt);
        if (!src.isDirectory()) {
            throw new IllegalArgumentException("First and second arguments '" + srcDir + "' and '" + startSearchAt + "'\n"
                    + "must combine to be a directory where the second argument is the directories that form the package name.\n"
                    +" e.g. arg[0] = ${PROJECT_ROOT}/target/classes\n"
                    +"      arg[1] = com/ibm/jbatch/tck/artifacts\n"
                    + " Found: " + src
            );
        }
        File batchXMLDir = new File(outputDir);
        if (!batchXMLDir.isDirectory()) {
            throw new IllegalArgumentException("Third argument must be a directory. Found: '" + outputDir + "'.");
        }

		List<String> classList = BatchXMLGenerator.findClasses(srcDir, startSearchAt);

		for (String className : classList) {
			bxg.processClass(className);
		}

		bxg.writeBatchXML(batchXMLDir);

		logger.info("BatchXMLGenerator completed successfully.");
	}
}
