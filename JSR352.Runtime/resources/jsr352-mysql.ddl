
CREATE TABLE IF NOT EXISTS JOBSTATUS (
  id		BIGINT,
  obj		BLOB
);

CREATE TABLE IF NOT EXISTS STEPSTATUS(
  id		VARCHAR(512),
  obj		BLOB
);

CREATE TABLE IF NOT EXISTS CHECKPOINTDATA(
  id		VARCHAR(512),
  obj		BLOB
);

CREATE TABLE IF NOT EXISTS JOBINSTANCEDATA(
  id		VARCHAR(512),
  name		VARCHAR(512)
);

CREATE TABLE IF NOT EXISTS EXECUTIONINSTANCEDATA(
  id			VARCHAR(512),
  createtime	TIMESTAMP,
  starttime		TIMESTAMP,
  endtime		TIMESTAMP,
  updatetime	TIMESTAMP,
  parameters	BLOB,
  jobinstanceid	VARCHAR(512),
  batchstatus		VARCHAR(512),
  exitstatus		VARCHAR(512)
  );
  
CREATE TABLE IF NOT EXISTS STEPEXECUTIONINSTANCEDATA(
	id			VARCHAR(512),
	jobexecid	VARCHAR(512),
	stepexecid			VARCHAR(512),
	batchstatus         VARCHAR(512),
    exitstatus			VARCHAR(512),
    stepname			VARCHAR(512),
	readcount			VARCHAR(512),
	writecount			VARCHAR(512),
	commitcount         VARCHAR(512),
	rollbackcount		VARCHAR(512),
	readskipcount		VARCHAR(512),
	processskipcount	VARCHAR(512),
	filtercount			VARCHAR(512),
	writeskipcount		VARCHAR(512),
	startTime           TIMESTAMP,
	endTime             TIMESTAMP,
	persistentData		BLOB
);  
  
