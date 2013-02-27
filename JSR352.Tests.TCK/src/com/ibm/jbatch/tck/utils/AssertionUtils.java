/*
 * Copyright 2012,2013 International Business Machines Corp.
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

public class AssertionUtils {
	
	static public void assertObjEquals(Object arg1, Object arg2) {
		assertWithMessage(null, arg1, arg2);
	}

	static public void assertWithMessage(String message, Object arg1, Object arg2)
	{
	    if (arg1 == null && arg2 == null) {
	        return;
	    }
		if (arg1 == null && arg2 != null) {
			if (message == null)
				throw new AssertionError("Expected 'null' but found value: " + arg2);
			else
				throw new AssertionError(message + "; Expected 'null' but found value: " + arg2);
		} 
		else if (!arg1.equals(arg2)) 
		{
			if (message == null)
				throw new AssertionError("Expected value: " + arg1 + ", but found value: " + arg2);
			else
				throw new AssertionError(message + "; Expected value: " + arg1 + ", but found value: " + arg2);
		}
	}
	
	static public void assertWithMessage(String message, boolean result)
	{
		if(!result)
		{
			if (message == null)
	            throw new AssertionError();
			else
				throw new AssertionError(message);
		}
	}
	
	static public void assertWithMessage(String message, int arg1, int arg2) {
     boolean result = (arg1 == arg2);
		
		if(!result)
		{
			if (message == null)
				throw new AssertionError("Expected value: " + arg1 + ", but found value: " + arg2);
			else
				throw new AssertionError(message + "; Expected value: " + arg1 + ", but found value: " + arg2);
		}
    }
}
