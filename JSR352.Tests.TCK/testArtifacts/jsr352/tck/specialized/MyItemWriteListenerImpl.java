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
package jsr352.tck.specialized;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

import javax.batch.annotation.AfterWrite;
import javax.batch.annotation.BeforeWrite;
import javax.batch.annotation.ItemWriteListener;
import javax.batch.annotation.OnWriteError;

import jsr352.tck.chunktypes.WriteRecord;

@ItemWriteListener("MyItemWriteListener")
@javax.inject.Named("MyItemWriteListener")
public class MyItemWriteListenerImpl {
	private final static String sourceClass = MyItemWriteListenerImpl.class.getName();
	private final static Logger logger = Logger.getLogger(sourceClass);

	@BeforeWrite
	public void beforeWrite(List<WriteRecord> items) throws Exception {
		logger.finer("In beforeWrite()");
	}
	
	@AfterWrite
	public void afterWrite(List<WriteRecord> items) throws Exception {
		logger.finer("In afterWrite()");
	}
	
	@OnWriteError
	public void onWriteError (SQLException e, List<WriteRecord> items) throws Exception {
		logger.finer("In onWriteError()" + e);
	}
	
}
