
DROP TABLE JOBSTATUS;

DROP TABLE STEPSTATUS;

DROP TABLE CHECKPOINTDATA;

DROP TABLE JOBINSTANCEDATA;

DROP TABLE EXECUTIONINSTANCEDATA;

DROP TABLE STEPEXECUTIONINSTANCEDATA;

CREATE TABLE JOBSTATUS (
  id		bigint,
  obj		bytea
);

CREATE TABLE STEPSTATUS(
  id		character varying (512),
  obj		bytea
);

CREATE TABLE CHECKPOINTDATA(
  id		character varying (512),
  obj		bytea
);

CREATE TABLE JOBINSTANCEDATA(
  id		character varying (512),
  name		character varying (512), 
  apptag VARCHAR(512)
);

CREATE TABLE EXECUTIONINSTANCEDATA(
  id			character varying (512),
  createtime	timestamp,
  starttime		timestamp,
  endtime		timestamp,
  updatetime	timestamp,
  parameters	bytea,
  jobinstanceid	character varying (512),
  batchstatus		character varying (512),
  exitstatus		character varying (512)
);
  
CREATE TABLE STEPEXECUTIONINSTANCEDATA(
	id			character varying (512),
	jobexecid	character varying (512),
	stepexecid			character varying (512),
	batchstatus         character varying (512),
    exitstatus			character varying (512),
    stepname			character varying (512),
	readcount			character varying (512),
	writecount			character varying (512),
	commitcount         character varying (512),
	rollbackcount		character varying (512),
	readskipcount		character varying (512),
	processskipcount	character varying (512),
	filtercount			character varying (512),
	writeskipcount		character varying (512),
	startTime           timestamp,
	endTime             timestamp,
	persistentData		bytea
);  
