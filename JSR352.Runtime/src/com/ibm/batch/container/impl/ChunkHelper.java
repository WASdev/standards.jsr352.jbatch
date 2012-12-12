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
package com.ibm.batch.container.impl;

import jsr352.batch.jsl.Chunk;
import jsr352.batch.jsl.Property;

public class ChunkHelper {

	public static int getBufferSize(Chunk chunk) {

		String chunkSizeStr = chunk.getBufferSize();
		int size = 0;
		if (chunkSizeStr != null && ! chunkSizeStr.isEmpty()) {
			size = Integer.valueOf(chunk.getBufferSize());
			if (size == 0) {
				// Spec says to treat '0' as a signal to turn off buffering.
				size = 1;
			}
		} else {
			// set default value
			if (chunk.getCheckpointPolicy().equalsIgnoreCase("item")) {
				//chunk size = commit-interval if checkpoint policy is item
				size = getCommitInterval(chunk);
			} else {
				//chunk size = 10 for all other  type of check  point
				size = 10;
			}
		}
		return size;		
	}
    
    public static int getCommitInterval(Chunk chunk){    
    	return Integer.valueOf(chunk.getCommitInterval());
    }
    
    public static int getSkipLimit(Chunk chunk) {
    	return Integer.valueOf(chunk.getSkipLimit());
    }
    
    public static int getRetryLimit(Chunk chunk) {
    	return Integer.valueOf(chunk.getRetryLimit());
    }

	public static Property getSkipIncludeClass() {
		// TODO Auto-generated method stub
		return null;
	}

	public static Property getSkipExcludeClass() {
		// TODO Auto-generated method stub
		return null;
	}

	public static Property getRetryIncludeClass() {
		// TODO Auto-generated method stub
		return null;
	}

	public static Property getRetryExcludeClass() {
		// TODO Auto-generated method stub
		return null;
	}
    
    
}
