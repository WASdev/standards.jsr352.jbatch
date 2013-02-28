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
package com.ibm.jbatch.container.services.impl;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Logger;

import com.ibm.jbatch.container.exception.BatchContainerServiceException;
import com.ibm.jbatch.container.persistence.CheckpointData;
import com.ibm.jbatch.container.persistence.CheckpointDataKey;
import com.ibm.jbatch.container.services.IPersistenceManagerService;
import com.ibm.jbatch.container.status.JobStatus;
import com.ibm.jbatch.container.status.JobStatusKey;
import com.ibm.jbatch.container.status.StepStatus;
import com.ibm.jbatch.container.status.StepStatusKey;
import com.ibm.jbatch.spi.services.IBatchConfig;

public abstract class AbstractMapBasedPersistenceManagerImpl  extends
AbstractPersistenceManagerImpl  implements 	IPersistenceManagerService {

    private static final String CLASSNAME = AbstractMapBasedPersistenceManagerImpl.class.getName();
    private static Logger logger = Logger.getLogger(AbstractMapBasedPersistenceManagerImpl.class.getPackage().getName());;
    protected static boolean _isInited = false;
    protected static Hashtable<Long, JobStatus> _jobStatusStore =  null;
    protected static Hashtable<String,StepStatus> _stepStatusStore =  null;							
    protected static Hashtable<String,CheckpointData> _checkpointStore =  null;

    protected static Object _jobStoreLock = new Object();
    protected static Object _stepStoreLock = new Object();
    protected static Object _checkpointStoreLock = new Object();



    //	protected void _createPJMLogicalTX(LogicalTXKey key, LogicalTXData value) {
    //		// TODO Auto-generated method stub
    //		
    //	}
    //
    //
    //	protected void _createPJMJobContext(TLJContextKey key, TLJContextData value) {
    //		// TODO Auto-generated method stub
    //		
    //	}
    //
    //
    //	protected void _createPJMSubmittedJobs(SubmittedJobKey key,
    //			SubmittedJobData value) {
    //		// TODO Auto-generated method stub
    //		
    //	}


    protected void _createCheckpointData(CheckpointDataKey key,
    		CheckpointData value) {
    	synchronized(_checkpointStoreLock) {
    		if(!_checkpointStore.containsKey(key.getKeyPrimitive())) {
    			_checkpointStore.put(key.getKeyPrimitive(), value);
    			_saveStore(CHECKPOINT_STORE_ID);
    		}	
    	}
    	
    }


    @Override
    	protected void _createStepStatus(StepStatusKey key, StepStatus value) {
    		synchronized(_stepStoreLock) {
    			if(!_stepStatusStore.containsKey(key.getKeyPrimitive())) {
    				_stepStatusStore.put(key.getKeyPrimitive(), value);
    				_saveStore(STEP_STATUS_STORE_ID);
    			} else {
    			    throw new IllegalStateException("Already have step entry for key = " + key);
    			}
    		}
    		
    	}


    @Override
    protected void _createJobStatus(JobStatusKey key, JobStatus value) {
        synchronized(_jobStoreLock) {
            if(!_jobStatusStore.containsKey(key.getKeyPrimitive())) {
                _jobStatusStore.put(key.getKeyPrimitive(), value);
                _saveStore(JOB_STATUS_STORE_ID);
            } else {
                throw new IllegalStateException("Entry with key = " + key.getKeyPrimitive() + " already exists.");
            }
        }		
    }

    @Override
    protected List<JobStatus> _getJobStatus(JobStatusKey key) {
        List<JobStatus> statusList = new ArrayList<JobStatus>();
        synchronized(_jobStoreLock) {
            if(_jobStatusStore.containsKey(key.getKeyPrimitive())) {
                JobStatus status = _jobStatusStore.get(key.getKeyPrimitive());

                statusList.add(status);
            }   
        }

        return statusList;
    }
    
    @Override
    protected List<StepStatus> _getStepStatus(StepStatusKey key) {
        List<StepStatus> statusList = new ArrayList<StepStatus>();
        synchronized(_stepStoreLock) {
            if(_stepStatusStore.containsKey(key.getKeyPrimitive())) {
                StepStatus status = _stepStatusStore.get(key.getKeyPrimitive());
                statusList.add(status);
            }   
        }
        return statusList;
    }



    //	protected void _deletePJMLogicalTX(LogicalTXKey key) {
    //		// TODO Auto-generated method stub
    //		
    //	}
    //
    //
    //	protected void _deletePJMJobContext(TLJContextKey key) {
    //		// TODO Auto-generated method stub
    //		
    //	}
    //
    //
    //	protected void _deletePJMSubmittedJobs(SubmittedJobKey key) {
    //		// TODO Auto-generated method stub
    //		
    //	}

    //
    
    @Override
    protected void _deleteCheckpointData(CheckpointDataKey key) {
    		synchronized(_checkpointStoreLock) {
    			if(_checkpointStore.containsKey(key.getCommaSeparatedKey())) {
    				_checkpointStore.remove(key.getCommaSeparatedKey());
    				_saveStore(CHECKPOINT_STORE_ID);
    			}	
    		}
    		
    	}
    
    @Override
    	protected void _deleteStepStatus(StepStatusKey key) {
    		synchronized(_stepStoreLock) {
    			if(_stepStatusStore.containsKey(key.getKeyPrimitive())) {
    				_stepStatusStore.remove(key.getKeyPrimitive());
    				_saveStore(STEP_STATUS_STORE_ID);
    			}	
    		}
    		
    	}


    @Override
    protected void _deleteJobStatus(JobStatusKey key) {
        synchronized(_jobStoreLock) {
            if(_jobStatusStore.containsKey(key.getKeyPrimitive())) {
                _jobStatusStore.remove(key.getKeyPrimitive());
                _saveStore(JOB_STATUS_STORE_ID);
            }	
        }

    }

    //	protected List _getPJMLogicalTX(LogicalTXKey key) {
    //		// TODO Auto-generated method stub
    //		return null;
    //	}
    //
    //
    //	protected List _getPJMJobContext(TLJContextKey key) {
    //		// TODO Auto-generated method stub
    //		return null;
    //	}
    //
    //
    //	protected List _getPJMSubmittedJobsData(SubmittedJobKey key) {
    //		// TODO Auto-generated method stub
    //		return null;
    //	}

    //
    @Override
    protected List<CheckpointData> _getCheckpointData(CheckpointDataKey key) {
    		List<CheckpointData> dataList = new ArrayList<CheckpointData>();
    		synchronized(_checkpointStoreLock) {
    			if(_checkpointStore.containsKey(key.getCommaSeparatedKey())) {
    				CheckpointData data = _checkpointStore.get(key.getCommaSeparatedKey());
    				dataList.add(data);
    			}	
    		}
    		return dataList;
    }








    //	protected void _updatePJMLogicalTX(LogicalTXKey key, LogicalTXData value) {
    //		// TODO Auto-generated method stub
    //		
    //	}
    //
    //
    //	protected void _updatePJMJobContext(TLJContextKey key, TLJContextData value) {
    //		// TODO Auto-generated method stub
    //		
    //	}
    //
    //
    //	protected void _updatePJMSubmittedJobs(SubmittedJobKey key,
    //			SubmittedJobData value) {
    //		// TODO Auto-generated method stub
    //		
    //	}

    @Override
    protected void _updateCheckpointData(CheckpointDataKey key,
    			CheckpointData value) {
    		synchronized(_checkpointStoreLock) {
    			if(_checkpointStore.containsKey(key.getCommaSeparatedKey())) {
    				CheckpointData data = _checkpointStore.get(key.getCommaSeparatedKey());
    				data.setRestartToken(value.getRestartToken());
    				data = value;
    				_saveStore(CHECKPOINT_STORE_ID);
    			}	
    		}
    		
    }


    protected void _updateStepStatus(StepStatusKey key, StepStatus value) {
    		synchronized(_stepStoreLock) {
    			if(_stepStatusStore.containsKey(key.getKeyPrimitive())) {
    				_stepStatusStore.put(key.getKeyPrimitive(), value);
    				_saveStore(STEP_STATUS_STORE_ID);                 
                } else {
                    throw new IllegalStateException("Could not find entry for key = " + key.getKeyPrimitive());
                }
    		}
    		
    	}


    @Override
    protected void _updateJobStatus(JobStatusKey key, JobStatus value) {
        synchronized(_jobStoreLock) {
            if(_jobStatusStore.containsKey(key.getKeyPrimitive())) {
                _jobStatusStore.put(key.getKeyPrimitive(), value);
                _saveStore(JOB_STATUS_STORE_ID);
            } else {
                throw new IllegalStateException("Could not find entry for key = " + key.getKeyPrimitive());
            }
        }

    }


    public void init(IBatchConfig pgcConfig) throws BatchContainerServiceException {
        super.init(pgcConfig);

        _loadDataStores();

    }


    protected abstract void _loadDataStores();



    protected abstract void _saveStore(int storeId); 


    public void shutdown() throws BatchContainerServiceException {
        // TODO Auto-generated method stub

    }







}
