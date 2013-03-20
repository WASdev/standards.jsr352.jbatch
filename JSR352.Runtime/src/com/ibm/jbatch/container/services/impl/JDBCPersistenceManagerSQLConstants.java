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
package com.ibm.jbatch.container.services.impl;

 interface JDBCPersistenceManagerSQLConstants {

	 final String JOBSTATUS_TABLE = "JOBSTATUS";
	 final String STEPSTATUS_TABLE = "STEPSTATUS";
	 final String CHECKPOINTDATA_TABLE = "CHECKPOINTDATA";
	 final String JOBINSTANCEDATA_TABLE = "JOBINSTANCEDATA";
	 final String EXECUTIONINSTANCEDATA_TABLE = "EXECUTIONINSTANCEDATA";
	 final String STEPEXECUTIONINSTANCEDATA_TABLE = "STEPEXECUTIONINSTANCEDATA";
	
	 final String CREATE_TAB_JOBSTATUS = "CREATE TABLE JOBSTATUS("
			+ "id BIGINT CONSTRAINT JOBSTATUS_PK PRIMARY KEY," 
			+ "obj BLOB,"
			+ "CONSTRAINT JOBSTATUS_JOBINST_FK FOREIGN KEY (id) REFERENCES JOBINSTANCEDATA (jobinstanceid) ON DELETE CASCADE)";
	 final String CREATE_TAB_STEPSTATUS = "CREATE TABLE STEPSTATUS("
			+ "id BIGINT CONSTRAINT STEPSTATUS_PK PRIMARY KEY," 
			+ "obj BLOB,"
			+ "CONSTRAINT STEPSTATUS_STEPEXEC_FK FOREIGN KEY (id) REFERENCES STEPEXECUTIONINSTANCEDATA (stepexecid) ON DELETE CASCADE)";
	 final String CREATE_TAB_CHECKPOINTDATA = "CREATE TABLE CHECKPOINTDATA("
			+ "id VARCHAR(512),obj BLOB)";
	 final String CREATE_TAB_JOBINSTANCEDATA = "CREATE TABLE JOBINSTANCEDATA("
			+ "jobinstanceid BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) CONSTRAINT JOBINSTANCE_PK PRIMARY KEY,"
			+ "name VARCHAR(512),"
			+ "apptag VARCHAR(512))";
	 final String CREATE_TAB_EXECUTIONINSTANCEDATA = "CREATE TABLE EXECUTIONINSTANCEDATA("
			+ "jobexecid BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) CONSTRAINT JOBEXECUTION_PK PRIMARY KEY,"
			+ "jobinstanceid BIGINT,"
			+ "createtime TIMESTAMP,"
			+ "starttime TIMESTAMP,"
			+ "endtime TIMESTAMP,"
			+ "updatetime TIMESTAMP,"
			+ "parameters BLOB,"
			+ "batchstatus VARCHAR(512),"
			+ "exitstatus VARCHAR(512)," 
			+ "CONSTRAINT JOBINST_JOBEXEC_FK FOREIGN KEY (jobinstanceid) REFERENCES JOBINSTANCEDATA (jobinstanceid))";
	 final String CREATE_TAB_STEPEXECUTIONINSTANCEDATA = "CREATE TABLE STEPEXECUTIONINSTANCEDATA("
			+ "stepexecid BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) CONSTRAINT STEPEXECUTION_PK PRIMARY KEY,"
			+ "jobexecid BIGINT,"
			+ "batchstatus VARCHAR(512),"
			+ "exitstatus VARCHAR(512),"
			+ "stepname VARCHAR(512),"
			+ "readcount INTEGER,"
			+ "writecount INTEGER,"
			+ "commitcount INTEGER,"
			+ "rollbackcount INTEGER,"
			+ "readskipcount INTEGER,"
			+ "processskipcount INTEGER,"
			+ "filtercount INTEGER,"
			+ "writeskipcount INTEGER,"
			+ "startTime TIMESTAMP," 
			+ "endTime TIMESTAMP,"
			+ "persistentData BLOB," 
			+ "CONSTRAINT JOBEXEC_STEPEXEC_FK FOREIGN KEY (jobexecid) REFERENCES EXECUTIONINSTANCEDATA (jobexecid))";
	
	 final String INSERT_JOBSTATUS = "insert into jobstatus values(?, ?)";
	
	 final String UPDATE_JOBSTATUS = "update jobstatus set obj = ? where id = ?";

	 final String SELECT_JOBSTATUS = "select id, obj from jobstatus where id = ?";
	
	 final String DELETE_JOBSTATUS = "delete from jobstatus where id = ?";

	 final String INSERT_STEPSTATUS = "insert into stepstatus values(?, ?)";
	
	 final String UPDATE_STEPSTATUS = "update stepstatus set obj = ? where id = ?";

	 final String SELECT_STEPSTATUS = "select id, obj from stepstatus where id = ?";
	
	 final String DELETE_STEPSTATUS = "delete from stepstatus where id = ?";

	 final String INSERT_CHECKPOINTDATA = "insert into checkpointdata values(?, ?)";

	 final String UPDATE_CHECKPOINTDATA = "update checkpointdata set obj = ? where id = ?";

	 final String SELECT_CHECKPOINTDATA = "select id, obj from checkpointdata where id = ?";
	
	 final String CREATE_CHECKPOINTDATA_INDEX = "create index chk_index on checkpointdata(id)";
	
	 final String DELETE_CHECKPOINTDATA = "delete from checkpointdata where id = ?";
	
	// JOB OPERATOR QUERIES
	 final String INSERT_JOBINSTANCEDATA = "insert into jobinstancedata (name, apptag) values(?, ?)";
	
	 final String INSERT_EXECUTIONDATA = "insert into executionInstanceData (jobinstanceid, parameters) values(?, ?)";
	
	 final String SELECT_JOBINSTANCEDATA_COUNT = "select count(jobinstanceid) as jobinstancecount from jobinstancedata where name = ?";
	
	 final String SELECT_JOBINSTANCEDATA_IDS = "select jobinstanceid from jobinstancedata where name = ?";
	
	 final String SELECT_JOBINSTANCEDATA_NAMES = "select name from jobinstancedata where apptag = ?";
	 final String SELECT_JOBINSTANCEDATA_APPTAG = "select apptag from jobinstancedata where jobinstanceid = ?";
	
	final String START_TIME = "starttime";
	final String CREATE_TIME = "createtime";
	final String END_TIME = "endtime";
	final String UPDATE_TIME = "updatetime";
	final String BATCH_STATUS = "batchstatus";
	final String EXIT_STATUS = "exitstatus";
	final String INSTANCE_ID = "instanceId";
	final String JOBEXEC_ID = "jobexecid";
	final String STEPEXEC_ID = "stepexecid";
	final String STEPCONTEXT = "stepcontext";
	final String APPTAG = "apptag";
}
