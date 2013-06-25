/*
 * Copyright 2013 International Business Machines Corp.
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
/**
 * The "porting package" SPI defined purely for the purpose of executing the
 * TCK (which the typical implementation will NOT need to implement).  
 * 
 * This provides a way for the TCK to determine test execution results
 * in a more convenient manner than if we had to only rely on the JSR 352 
 * specification-defined APIs.
 * 
 * <p>
 * The TCK itself ships with a default implementation of this SPI (using polling).
 * The expectation therefore is that the typical JSR 352 implementation will not need
 * to implement this SPI, but will use the default implementation.
 * 
 * <p>
 * For more details, see the documentation in the TCK Reference Guide.
 *  
 * @see <a href="https://java.net/projects/jbatch/downloads/download/jsr352-tck-reference-guide.pdf">TCK Reference Guide</a> 
 * 
 */
package com.ibm.jbatch.tck.spi;