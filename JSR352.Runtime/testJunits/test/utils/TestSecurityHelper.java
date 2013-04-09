/**
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
package test.utils;

import com.ibm.jbatch.spi.BatchSecurityHelper;

public class TestSecurityHelper implements BatchSecurityHelper {

	public final static String defaultTag = "internal.default.tag.for.TestSecurityHelper";
	
	private String currentTag;
	private boolean isAdmin = false;
	
	public TestSecurityHelper(boolean isAdmin, String currentTag) {
		this.isAdmin = isAdmin;
		this.currentTag = currentTag;
	}
	
	public TestSecurityHelper(String currentTag) {
		this(false, currentTag);
	}
	
	@Override
	public String getCurrentTag() {
		if (currentTag == null) {
			return defaultTag;
		} else {
			return currentTag;
		}
	}

	@Override
	public boolean isAdmin(String tag) {
		return isAdmin;
	}

}
