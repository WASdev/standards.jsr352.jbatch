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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jsr352.batch.jsl.Chunk;

import com.ibm.batch.container.artifact.proxy.RetryListenerProxy;
import com.ibm.batch.container.exception.BatchContainerRuntimeException;

public class RetryHandler {

	/**
	 *
	 * Logic for handling retryable records.
	 *
	 * A RetryHandler object is attached to every BDS that inherits from AbstractBatchDataStream.
	 *
	 */

	  private static final String className = RetryHandler.class.getName();
		private static Logger logger = Logger.getLogger(RetryHandler.class.getPackage().getName());

	  public static final String RETRY_COUNT      = "retry-limit";
	  public static final String RETRY_INCLUDE_EX = "include class";
	  public static final String RETRY_EXCLUDE_EX = "exclude class";

	  private RetryListenerProxy _retryListener = null;

	  private long _jobId = 0;
	  private String _stepId = null;
	  private Set<String> _retryNoRBIncludeExceptions = null;
	  private Set<String> _retryNoRBExcludeExceptions = null;
	  private int _retryLimit = 0;
	  private long _retryCount = 0;
	  private Exception _retryNoRBException = null;


	  public RetryHandler(Chunk chunk, long l, String stepId)
	  {
	    _jobId = l;
	    _stepId = stepId;

	    initialize(chunk);
	  }


	  /**
	   * Add the user-defined RetryListener.
	   *
	   */
	  public void addRetryListener(RetryListenerProxy retryListener)
	  {
	    _retryListener = retryListener;
	  }


	  /**
	   * Read the retry exception lists from the BDS props.
	   */
	  private void initialize(Chunk chunk)
	  {
	    final String mName = "initialize";

	    if(logger.isLoggable(Level.FINER)) 
	      logger.entering(className, mName);

	    try
	    {
	    	if (chunk.getRetryLimit() != null){
	    		_retryLimit = Integer.parseInt(chunk.getRetryLimit());
	    	}
	    }
	    catch (NumberFormatException nfe)
	    {
	      throw new RuntimeException("NumberFormatException reading " + RETRY_COUNT, nfe);
	    }

	    if (_retryLimit > 0)
	    {
	      // Read the include/exclude exceptions.
	  
	      _retryNoRBIncludeExceptions = new HashSet<String>();
	      _retryNoRBExcludeExceptions = new HashSet<String>();

	      boolean done = false;
	      String includeEx = null;
	      String excludeEx = null;
	      
			if (chunk.getNoRollbackExceptionClasses() != null) {
				if (chunk.getNoRollbackExceptionClasses().getInclude() != null) {
					includeEx = chunk.getNoRollbackExceptionClasses().getInclude().getClazz();
					logger.finer("RETRYHANDLE: include: " + includeEx);
				}
			}
			if (chunk.getNoRollbackExceptionClasses() != null) {
				if (chunk.getNoRollbackExceptionClasses().getExclude() != null) {
					excludeEx = chunk.getNoRollbackExceptionClasses().getExclude().getClazz();
					logger.finer("RETRYHANDLE: exclude: " + excludeEx);
				}
			}

			if (includeEx != null)
				_retryNoRBIncludeExceptions.add(includeEx.trim());
			if (excludeEx != null)
				_retryNoRBExcludeExceptions.add(excludeEx.trim());

			done = (includeEx == null && excludeEx == null);

			if (logger.isLoggable(Level.FINE))
				logger.logp(Level.FINE, className, mName,
						"added include exception " + includeEx
								+ "; added exclude exception " + excludeEx);
		}
	        
	    if(logger.isLoggable(Level.FINER)) 
	      logger.exiting(className, mName, this.toString());
	  }


	  /**
	   * Handle exception from a read failure.
	   */
	  public void handleNoRollbackExceptionRead(Exception e)
	  {
	    final String mName = "handleException";
	    
	    logger.finer("RETRYHANDLE: in retryhandler handle exception on a read:" + e.toString());

	    if(logger.isLoggable(Level.FINER)) 
	      logger.logp(Level.FINE, className, mName, e.getClass().getName() + "; " + this.toString());
	    
	    if (!isRetryLimitReached() && isRetryable(e))
	    {
	      // Retry it.  Log it.  Call the RetryListener.
	      ++_retryCount;
	      logRetry(e);

	      if (_retryListener != null)
	        _retryListener.onRetryReadItem(e);
	    }
	    else
	    {
	      // No retry.  Throw it back.
	      if(logger.isLoggable(Level.FINER)) 
	        logger.logp(Level.FINE, className, mName, "No retry.  Rethrow", e);
	      	throw new BatchContainerRuntimeException(e);
	    }

	    if(logger.isLoggable(Level.FINER)) 
	      logger.exiting(className, mName, e);
	  }

	  /** 
	   * Handle exception from a process failure.
	   */
	  public void handleNoRollbackExceptionWithRecordProcess(Exception e, Object w)
	  {
	    final String mName = "handleException";
	    
	    if(logger.isLoggable(Level.FINER)) 
	      logger.logp(Level.FINE, className, mName, e.getClass().getName() + "; " + this.toString());

	    if (!isRetryLimitReached() && isRetryable(e))
	    {
	      // Retry it.  Log it.  Call the RetryListener.
	      ++_retryCount;
	      logRetry(e);

	      if (_retryListener != null)
	        _retryListener.onRetryProcessException(e, w);
	    }
	    else
	    {
	      // No retry.  Throw it back.
	      if(logger.isLoggable(Level.FINER)) 
	        logger.logp(Level.FINE, className, mName, "No retry.  Rethrow ", e);
	      throw new BatchContainerRuntimeException(e);
	    }
	  }
	  
	  /** 
	   * Handle exception from a write failure.
	   */
	  public void handleNoRollbackExceptionWithRecordWriteItem(Exception e, Object w)
	  {
	    final String mName = "handleException";
	    
	    if(logger.isLoggable(Level.FINER)) 
	      logger.logp(Level.FINE, className, mName, e.getClass().getName() + "; " + this.toString());

	    if (!isRetryLimitReached() && isRetryable(e))
	    {
	      // Retry it.  Log it.  Call the RetryListener.
	      ++_retryCount;
	      logRetry(e);

	      if (_retryListener != null)
	        _retryListener.onRetryWriteItem(e, w);
	    }
	    else
	    {
	      // No retry.  Throw it back.
	      if(logger.isLoggable(Level.FINER)) 
	        logger.logp(Level.FINE, className, mName, "No retry.  Rethrow ", e);
	      throw new BatchContainerRuntimeException(e);
	    }
	  }


	  /**
	   * Check the retryCount and retryable exception lists to determine whether
	   * the given Exception is retryable.
	   */
	  private boolean isRetryable(Exception e)
	  {
	    final String mName = "isRetryable";

	    String exClassName = e.getClass().getName();

	    boolean retVal = ((_retryNoRBIncludeExceptions.isEmpty() || containsRetryableNoRB(_retryNoRBIncludeExceptions, e)) &&
	                      !containsRetryableNoRB(_retryNoRBExcludeExceptions, e));

	    if(logger.isLoggable(Level.FINE)) 
	      logger.logp(Level.FINE, className, mName, mName + ": " + retVal + ": " + exClassName);

	    return retVal;
	  }

	  /**
	   * Check whether given exception is in retyable exception list 
	   */
	  private boolean containsRetryableNoRB(Set<String> retryList, Exception e)
	  {
	    final String mName = "containsRetryableNoRB";
	    boolean retVal = false;

	    for ( Iterator it = retryList.iterator(); it.hasNext(); ) {
	        String exClassName = (String) it.next();   
	        try {
	            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
	        	if (retVal = tccl.loadClass(exClassName).isInstance(e))
	        		break;
	        } catch (ClassNotFoundException cnf) {
	        	logger.logp(Level.FINE, className, mName, cnf.getLocalizedMessage());
	        }
	    }

	    if(logger.isLoggable(Level.FINE)) 
	      logger.logp(Level.FINE, className, mName, mName + ": " + retVal );

	    return retVal;
	  }
	  

	  /**
	   * Check if the retry limit has been reached.
	   *
	   * Note: if retry handling isn't enabled (i.e. not configured in xJCL), then this method 
	   *       will always return TRUE.
	   */
	  private boolean isRetryLimitReached()
	  {
	    return (_retryCount >= _retryLimit);
	  }

	  
	  private void logRetry(Exception e)
	  {
	    String key = "record.retried.norollback.by.batch.container";
	    Object[] details = { _jobId, _stepId, e.getClass().getName() + ": " + e.getMessage() };
	    //String message = LoggerUtil.getFormattedMessage(key, details, true);
	    //logger.info(message);	
		}


	  public long getRetryCount()
	  {
	    return _retryCount;
	  }

	  public void setRetryCount(long retryCount)
	  {
	    final String mName = "setRetryCount";

	    _retryCount = retryCount;

	    if(logger.isLoggable(Level.FINE)) 
	      logger.logp(Level.FINE, className, mName, "setRetryCount: " + _retryCount);
	  }

	  public String toString()
	  {
	    return "RetryHandler{" + super.toString() + "}count:limit=" + _retryCount + ":" + _retryLimit;
	  }

	


}
