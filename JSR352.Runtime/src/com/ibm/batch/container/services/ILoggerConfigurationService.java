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
package com.ibm.batch.container.services;

import java.util.List;

/**
 * The purpose of this service is to delegate the initialization and configuration
 * of the Java logger used by the container to a plugin implementor. 
 * A typical implementation should
 * 1) Instantiate a Formatter and a Handler
 * 2) Set the formatter and handler on loggers belonging to package names passed in
 * via the configureSystemLogger method.
 * 
 */
public interface ILoggerConfigurationService extends IBatchServiceBase {

	/**
	 * Configure the java logger. 
	 * @param containerPackagePatterns - A list of package names. trace/debug statements
	 *  within classes that belong under the package names declared here should be visible
	 * in the system logs when tracing is enabled.
	 * The plugin implementor can add additional package names for custom code written
	 * by the plugin implementor that is not part of the core container.
	 * E.g.
	 * Iterator<String> packageNames = containerPackagePatterns.iterator();
			while(packageNames.hasNext()) {
				
				String packageName = packageNames.next();
				Logger logger1 = Logger.getLogger(packageName);
				logger1.setLevel(_logLevel);
				logger1.addHandler(_fileHandler);
				
				
			}	
	 */
	public void configureSystemLogger(List<String> containerPackagePatterns) ;
}
