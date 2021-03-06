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
package com.ibm.jbatch.spi;

/**
 * 
 * Implemented by the host environment to allow the 'jbatch' implementation to get 
 * the "current" tag (here a generalized term for something like the current
 * "application), and whether the tag is "admin"-authorized or not.
 *
 * @author skurz
 */
public interface BatchSecurityHelper {
	/**
	 * @return The current "tag" (or "app name", generically speaking).
	 */
    public String getCurrentTag();
    
    /**
     * @param tag A "tag" (or "app name", generically speaking).
     * @return true if the "tag" is associated with an app recognized as 
     *              authorized for administrator-level read permissions.
     */
    public boolean isAdmin(String tag);
    
}
