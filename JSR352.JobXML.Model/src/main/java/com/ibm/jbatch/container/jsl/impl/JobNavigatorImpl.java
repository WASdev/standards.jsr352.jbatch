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
package com.ibm.jbatch.container.jsl.impl;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


import com.ibm.jbatch.container.jsl.ControlElement;
import com.ibm.jbatch.container.jsl.ExecutionElement;
import com.ibm.jbatch.container.jsl.Navigator;
import com.ibm.jbatch.container.jsl.Transition;
import com.ibm.jbatch.jsl.model.Decision;
import com.ibm.jbatch.jsl.model.End;
import com.ibm.jbatch.jsl.model.Fail;
import com.ibm.jbatch.jsl.model.Flow;
import com.ibm.jbatch.jsl.model.JSLJob;
import com.ibm.jbatch.jsl.model.Next;
import com.ibm.jbatch.jsl.model.Split;
import com.ibm.jbatch.jsl.model.Step;
import com.ibm.jbatch.jsl.model.Stop;

public class JobNavigatorImpl implements Navigator<JSLJob> {

    private final static Logger logger = Logger.getLogger(JobNavigatorImpl.class.getName());
    private JSLJob job = null;

    public JobNavigatorImpl(JSLJob job) {
        this.job = job;
    }

    @Override
    public Transition getNextTransition(ExecutionElement currentElem, String currentExitStatus) {
        final String method = "getNextTransition";
        if (logger.isLoggable(Level.FINE))
            logger.fine(method + " ,currentExitStatus=" + currentExitStatus);

        String nextAttrId = null;
        ExecutionElement nextExecutionElement = null;
        Transition returnTransition = new TransitionImpl();

        if (currentElem instanceof Step) {
            nextAttrId = ((Step) currentElem).getNextFromAttribute();
            nextExecutionElement = getExecutionElementByID(nextAttrId);
        } else if (currentElem instanceof Split) {
            nextAttrId = ((Split) currentElem).getNextFromAttribute();
            nextExecutionElement = getExecutionElementByID(nextAttrId);
        } else if (currentElem instanceof Flow) {
        	nextAttrId = ((Flow) currentElem).getNextFromAttribute();
        	nextExecutionElement = getExecutionElementByID(nextAttrId);
        } else if (currentElem instanceof Decision) {
            // Nothing special to do in this case.
        }

        List<ControlElement> controlElements = currentElem.getControlElements();

        if (nextExecutionElement == null && controlElements.isEmpty()) {
            if (logger.isLoggable(Level.FINE))
                logger.fine(method + " return null, there is no next step");
            // Don't set anything special on return transition.
            return returnTransition;
        } else if (nextExecutionElement != null) {
            if (logger.isLoggable(Level.FINE))
                logger.fine(method + " return execution element:" + nextExecutionElement);
            returnTransition.setNextExecutionElement(nextExecutionElement);
            return returnTransition;
        } else if (controlElements.size() > 0) {

            Iterator<ControlElement> iterator = controlElements.iterator();
            while (iterator.hasNext()) {

                ControlElement elem = iterator.next();

                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(method + " Trying to match next control element: " + elem);
                }

                if (elem instanceof Stop) {
                    String exitStatusToMatch = ((Stop) elem).getOn();
                    boolean isMatched = matchSpecifiedExitStatus(currentExitStatus, exitStatusToMatch);
                    if (isMatched == true) {
                        if (logger.isLoggable(Level.FINE))
                            logger.fine(method + " , Stop element matches to " + exitStatusToMatch);

                        returnTransition.setControlElement(elem);
                        return returnTransition;
                    }
                } else if (elem instanceof End) {
                    String exitStatusToMatch = ((End) elem).getOn();
                    boolean isMatched = matchSpecifiedExitStatus(currentExitStatus, exitStatusToMatch);
                    if (isMatched == true) {
                        if (logger.isLoggable(Level.FINE))
                            logger.fine(method + " , End element matches to " + exitStatusToMatch);
                        returnTransition.setControlElement(elem);
                        return returnTransition;
                    }
                } else if (elem instanceof Fail) {
                    String exitStatusToMatch = ((Fail) elem).getOn();
                    boolean isMatched = matchSpecifiedExitStatus(currentExitStatus, exitStatusToMatch);
                    if (isMatched == true) {
                        if (logger.isLoggable(Level.FINE))
                            logger.fine(method + " , Fail element matches to " + exitStatusToMatch);
                        returnTransition.setControlElement(elem);
                        return returnTransition;
                    }
                } else if (elem instanceof Next) {
                    String exitStatusToMatch = ((Next) elem).getOn();
                    boolean isMatched = matchSpecifiedExitStatus(currentExitStatus, exitStatusToMatch);
                    if (isMatched == true) {
                        // go to next executionElement
                        nextExecutionElement = getExecutionElementByID(((Next) elem).getTo());

                        if (logger.isLoggable(Level.FINE))
                            logger.fine(method + " , match to " + exitStatusToMatch + ". Continue to step "
                                    + nextExecutionElement.getId());

                        // No point setting the ControlElement in the transition.
                        returnTransition.setNextExecutionElement(nextExecutionElement);
                        return returnTransition;
                    }
                } else {
                    throw new IllegalStateException("Shouldn't be possible to get here. Unknown control element,  " + elem.toString());
                }
            }
        }

        //TODO - Is this an error case or a valid end/completion case?
        return null;
    }

    @Override
    public ExecutionElement getFirstExecutionElement(String restartOn) {
        final String method = "getFirstExecutionElement";

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(method + " , restartOn = " + restartOn);
        }

        ExecutionElement startElement = null;

        if (restartOn != null) {
            startElement = getExecutionElementByID(restartOn);
            if (startElement == null) {
                throw new IllegalStateException("Didn't find an execution element maching restart-on designated element: " + restartOn);
            }
        } else {
            if (job.getExecutionElements().size() > 0) {                
                startElement = job.getExecutionElements().get(0);
            } else {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(method + " , Job appears to contain no execution elements.  Returning.");
                }
                return null;
            }
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(method + " , Found start element: " + startElement);
        }                

        if (startElement instanceof Step) {
            return (Step)startElement;
        } else if (startElement instanceof Flow){
        	return (Flow)startElement;
        } else if (startElement instanceof Split){
        	return (Split)startElement;
        } else {
            throw new IllegalStateException("Didn't get this far yet implementing.\nOnly support <step> as first execution element.");
        }
    }

    @Override
    public JSLJob getJSL() {
        return this.job;
    }

    private ExecutionElement getExecutionElementByID(String id) {
        if (id != null) {
            for (ExecutionElement elem : job.getExecutionElements()) {
                if (elem.getId().equals(id)) {
                    return elem;
                }
            }
        }
        return null;
    }

    /*
     * 
     */
    private static boolean matchSpecifiedExitStatus(String currentStepExitStatus, String exitStatusPattern) {
    	
    	
        if (logger.isLoggable(Level.FINER)) {
            logger.finer("matchSpecifiedExitStatus, matching current exitStatus  " + currentStepExitStatus + 
            		" against pattern: " + exitStatusPattern);
        }
        
        GlobPatternMatcherImpl matcher = new GlobPatternMatcherImpl(); 
        boolean match = matcher.matchWithoutBackslashEscape(currentStepExitStatus, exitStatusPattern);
                
        if (match) {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("matchSpecifiedExitStatus, match=YES");
            }
            return true;
        }
        else {
            if (logger.isLoggable(Level.FINER)) {
                logger.finer("matchSpecifiedExitStatus, match=NO");
            }
            return false;
        }
    }
    
    
    /*


    private static String buildRegExFromJSLStringPattern(String jslPattern) {
    	String asterisk = "*";
    	String asteriskRegEx = ".*";
    	String questionMark = "?";
    	String questionMarkRegEx = ".?";
    	
    	String newString = jslPattern;
    	newString = newString.replace(asterisk, asteriskRegEx);
    	newString = newString.replace(questionMark, questionMarkRegEx);
    	
    	return newString;
    }
     */
    public String getId() {
        return job.getId();
    }



}
