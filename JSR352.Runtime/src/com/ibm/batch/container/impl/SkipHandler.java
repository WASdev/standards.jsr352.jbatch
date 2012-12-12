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

import com.ibm.batch.container.artifact.proxy.SkipListenerProxy;
import com.ibm.batch.container.exception.BatchContainerRuntimeException;

public class SkipHandler {

	/**
	 *
	 * Logic for handling skipped records.
	 *
	 * A SkipHandler object is attached to every BDS that inherits from AbstractBatchDataStream.
	 *
	 */

	  private static final String className = SkipHandler.class.getName();
		private static Logger logger = Logger.getLogger(SkipHandler.class.getPackage().getName());

	  public static final String SKIP_COUNT      = "skip-limit";
	  public static final String SKIP_INCLUDE_EX = "include class";
	  public static final String SKIP_EXCLUDE_EX = "exclude class";

	  private SkipListenerProxy _skipListener = null;

	  private long _jobId = 0;
	  private String _stepId = null;
	  private Set<String> _skipIncludeExceptions = null;
	  private Set<String> _skipExcludeExceptions = null;
	  private int _skipLimit = 0;
	  private long _skipCount = 0;
	  private Exception _skipException = null;


	  public SkipHandler(Chunk chunk, long l, String stepId)
	  {
	    _jobId = l;
	    _stepId = stepId;

	    initialize(chunk);
	  }


	  /**
	   * Add the user-defined SkipListener.
	   *
	   */
	  public void addSkipListener(SkipListenerProxy skipListener)
	  {
	    _skipListener = skipListener;
	  }


	  /**
	   * Read the skip exception lists from the BDS props.
	   */
	  private void initialize(Chunk chunk)
	  {
	    final String mName = "initialize";

	    if(logger.isLoggable(Level.FINER)) 
	      logger.entering(className, mName);

	    try
	    {
	    	if (chunk.getSkipLimit() != null){
	    		_skipLimit = Integer.parseInt(chunk.getSkipLimit());
	    	}
	    }
	    catch (NumberFormatException nfe)
	    {
	      throw new RuntimeException("NumberFormatException reading " + SKIP_COUNT, nfe);
	    }

	    if (_skipLimit > 0)
	    {
	      // Read the include/exclude exceptions.
	  
	      _skipIncludeExceptions = new HashSet<String>();
	      _skipExcludeExceptions = new HashSet<String>();

	      boolean done = false;
	      String includeEx = null;
	      String excludeEx = null;
	      
			if (chunk.getSkippableExceptionClasses() != null) {
				if (chunk.getSkippableExceptionClasses().getInclude() != null) {
					includeEx = chunk.getSkippableExceptionClasses()
							.getInclude().getClazz();
					logger.finer("SKIPHANDLE: include: " + includeEx);
				}
			}
			if (chunk.getSkippableExceptionClasses() != null) {
				if (chunk.getSkippableExceptionClasses().getExclude() != null) {
					excludeEx = chunk.getSkippableExceptionClasses()
							.getExclude().getClazz();
					logger.finer("SKIPHANDLE: exclude: " + excludeEx);
				}
			}

			if (includeEx != null)
				_skipIncludeExceptions.add(includeEx.trim());
			if (excludeEx != null)
				_skipExcludeExceptions.add(excludeEx.trim());

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
	  public void handleException(Exception e)
	  {
	    final String mName = "handleException";
	    
	    logger.finer("SKIPHANDLE: in skiphandler handle exception on a read");

	    if(logger.isLoggable(Level.FINER)) 
	      logger.logp(Level.FINE, className, mName, e.getClass().getName() + "; " + this.toString());
	    
	    if (!isSkipLimitReached() && isSkippable(e))
	    {
	      // Skip it.  Log it.  Call the SkipListener.
	      ++_skipCount;
	      logSkip(e);

	      if (_skipListener != null)
	        _skipListener.onSkipInRead(e);
	    }
	    else
	    {
	      // No skip.  Throw it back. don't throw it back - we might want to retry ...
	      if(logger.isLoggable(Level.FINER)) 
	        logger.logp(Level.FINE, className, mName, "No skip.  Rethrow", e);
	      	throw new BatchContainerRuntimeException(e);
	    }

	    if(logger.isLoggable(Level.FINER)) 
	      logger.exiting(className, mName, e);
	  }

	  /** 
	   * Handle exception from a process failure.
	   */
	  public void handleExceptionWithRecordProcess(Exception e, Object w)
	  {
	    final String mName = "handleException";
	    
	    if(logger.isLoggable(Level.FINER)) 
	      logger.logp(Level.FINE, className, mName, e.getClass().getName() + "; " + this.toString());

	    if (!isSkipLimitReached() && isSkippable(e))
	    {
	      // Retry it.  Log it.  Call the RetryListener.
	      ++_skipCount;
	      logSkip(e);

	      if (_skipListener != null)
	        _skipListener.onSkipInProcess(e, w);
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
	  public void handleExceptionWithRecordWrite(Exception e, Object w)
	  {
	    final String mName = "handleException";
	    
	    if(logger.isLoggable(Level.FINER)) 
	      logger.logp(Level.FINE, className, mName, e.getClass().getName() + "; " + this.toString());

	    if (!isSkipLimitReached() && isSkippable(e))
	    {
	      // Skip it.  Log it.  Call the SkipListener.
	      ++_skipCount;
	      logSkip(e);

	      if (_skipListener != null)
	        _skipListener.onSkipInWrite(e, w);
	    }
	    else
	    {
	      // No skip.  Throw it back. - No, exit without throwing
	      if(logger.isLoggable(Level.FINER)) 
	        logger.logp(Level.FINE, className, mName, "No skip.  Rethrow ", e);
	      throw new BatchContainerRuntimeException(e);
	    }
	  }


	  /**
	   * Check the skipCount and skippable exception lists to determine whether
	   * the given Exception is skippable.
	   */
	  private boolean isSkippable(Exception e)
	  {
	    final String mName = "isSkippable";

	    String exClassName = e.getClass().getName();

	    boolean retVal = ((_skipIncludeExceptions.isEmpty() || containsSkippable(_skipIncludeExceptions, e)) &&
	                      !containsSkippable(_skipExcludeExceptions, e));

	    if(logger.isLoggable(Level.FINE)) 
	      logger.logp(Level.FINE, className, mName, mName + ": " + retVal + ": " + exClassName);

	    return retVal;
	  }

	  /**
	   * Check whether given exception is in skippable exception list 
	   */
	  private boolean containsSkippable(Set<String> skipList, Exception e)
	  {
	    final String mName = "containsSkippable";
	    boolean retVal = false;

	    for ( Iterator it = skipList.iterator(); it.hasNext(); ) {
	        String exClassName = (String) it.next();   
	        try {
	        	if (retVal = Class.forName(exClassName).isInstance(e))
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
	   * Check if the skip limit has been reached.
	   *
	   * Note: if skip handling isn't enabled (i.e. not configured in xJCL), then this method 
	   *       will always return TRUE.
	   */
	  private boolean isSkipLimitReached()
	  {
	    return (_skipCount >= _skipLimit);
	  }

	  
	  private void logSkip(Exception e)
	  {
	    String key = "record.skipped.by.batch.data.stream";
	    Object[] details = { _jobId, _stepId, e.getClass().getName() + ": " + e.getMessage() };
	    //String message = LoggerUtil.getFormattedMessage(key, details, true);
	    //logger.info(message);	
	    //ServicesManager.getInstance().getJobLogManagerService(_jobId).println(message);
		}


	  public long getSkipCount()
	  {
	    return _skipCount;
	  }

	  public void setSkipCount(long skipCount)
	  {
	    final String mName = "setSkipCount";

	    _skipCount = skipCount;

	    if(logger.isLoggable(Level.FINE)) 
	      logger.logp(Level.FINE, className, mName, "setSkipCount: " + _skipCount);
	  }

	  public String toString()
	  {
	    return "SkipHandler{" + super.toString() + "}count:limit=" + _skipCount + ":" + _skipLimit;
	  }

	


}
