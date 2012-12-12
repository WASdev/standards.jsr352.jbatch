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
package jsr352.test;

import java.sql.SQLException;
import java.util.logging.Logger;

import javax.batch.annotation.OnRetryReadException;
import javax.batch.annotation.OnRetryReadItem;
import javax.batch.annotation.RetryListener;

import jsr352.tck.chunktypes.ReadRecord;

@RetryListener("MyRetryListener")
public class MyRetryListenerImpl {
	 private final static String sourceClass = MyRetryListenerImpl.class.getName();
	    private final static Logger logger = Logger.getLogger(sourceClass);

	    @OnRetryReadException
	    void onRetryException(SQLException e, ReadRecord rec) throws Exception {
	    	logger.finer("In onRetryException()" + e + "input=" + rec.getCount());
	    }
	    
	    @OnRetryReadItem
	    void onRetryItem(SQLException e, ReadRecord rec) throws Exception {
	    	logger.finer("In onRetryItem()" + e + "input=" + rec.getCount());
	    }
}
