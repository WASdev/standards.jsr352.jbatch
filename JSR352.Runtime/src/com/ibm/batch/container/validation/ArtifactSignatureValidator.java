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
package com.ibm.batch.container.validation;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.batch.annotation.*;

import com.ibm.batch.container.artifact.proxy.*;

/*
 * This validates the artifacts once they have been loaded.  Might
 * be useful to evaluate at compile-time / annotation-processing-time, 
 * but that's separate from this. 
 * 
 */
public class ArtifactSignatureValidator {

    private final static String sourceClass = ArtifactSignatureValidator.class.getName();
    private final static Logger logger = Logger.getLogger(sourceClass);

    /*
     * 4.1.2.1 @BeginStep 
     * 
     * The @BeginStep annotation identifies a method that
     * receives control at the beginning of Batchlet processing. This is an
     * optional method for a Batchlet.
     * 
     * 4.1.2.2 @Process 
     * 
     * The @Process annotation identifies a method that
     * receives control to do the business processing for a Batchlet. This is a
     * REQUIRED method for a Batchlet. It must return a non-null, non-empty
     * String that represents the user-defined exit status of the Batchlet.
     * 
     * 4.1.2.3 @Cancel 
     * 
     * The @Cancel annotation identifies a method that receives
     * control in response to a cancel job operation while the Batchlet is
     * running. The BatchLet @Process method must periodically check if @Cancel
     * has been invoked. If @Cancel has been invoked, @Process must return. This
     * is a REQUIRED method for a Batchlet.
     * 
     * 4.1.2.4 @EndStep 
     * 
     * The @EndStep annotation identifies a method that
     * receives control at the end of Batchlet processing. This is an optional
     * method for a Batchlet.
     */
    public static void validate(BatchletProxy proxy) throws ArtifactValidationException {
        
        Object delegate = proxy.getDelegate();
        String description = "Artifact: " + delegate + " of type: " + delegate.getClass();
        
        if (!delegate.getClass().isAnnotationPresent(Batchlet.class)) {
            throw new ArtifactValidationException(description + " does not contain a @Batchlet annotation.");
        }
        StringBuffer errorMsg = new StringBuffer(description + " has an invalid signature: \n");     
        boolean valid = true;
        
        Method processMethod = proxy.getProcessMethod();
        if (processMethod == null) {
            errorMsg.append("  @Process-annotated method is required, but none found.\n");
            valid = false;
        } else {
        	Class<?> returnType = processMethod.getReturnType();            
            if (!String.class.equals(returnType)) {
                errorMsg.append("  @Process-annotated method return type should be java.lang.String, but was: " + returnType + ".\n");
                valid = false;
            }        
        }        

        if (proxy.getStopMethod() == null) {
            errorMsg.append("  @Cancel-annotated method is required, but none found.\n");
            valid = false;
        }
        if (!valid) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(errorMsg.toString());
            }
            throw new ArtifactValidationException(errorMsg.toString());
        }
    }

    /*
     * 4.2.3.1 @BeforeCheckpoint
     * 
     * The @BeforeCheckpoint annotation identifies a method that receives
     * control before a checkpoint is taken. This is an optional method for a
     * checkpoint listener.
     * 
     * 4.2.3.2 @AfterCheckpoint
     * 
     * The @AfterCheckpoint annotation identifies a method that receives control
     * after a checkpoint has been taken. This is an optional method for a
     * checkpoint listener.
     */

    public static void validate(CheckpointListenerProxy proxy) throws ArtifactValidationException {
        // No required methods
    }

    /*
     * 4.1.1.2.1   @ProcessItem
     * 
     * The @ProcessItem annotation identifies a method that performs the business processing for
     * an item processor.  This method receives an input item for processing and returns an 
     * output item.  The item processor defines both the input and output types. This is a 
     * REQUIRED method for an item reader.
     */

    public static void validate(ItemProcessorProxy proxy) throws ArtifactValidationException {
        Object delegate = proxy.getDelegate();
        String description = "Artifact: " + delegate + " of type: " + delegate.getClass();
        
        if (!delegate.getClass().isAnnotationPresent(ItemProcessor.class)) {
            throw new ArtifactValidationException(description + " does not contain a @ItemProcessor annotation.");
        }
        
        StringBuffer errorMsg = new StringBuffer(description + " has an invalid signature: \n");     
        boolean valid = true;
        if (proxy.getProcessItemMethod() == null) {
            errorMsg.append("  @ProcessItem-annotated method is required, but none found.\n");
            valid = false;
        }
        if (!valid) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(errorMsg.toString());
            }
            throw new ArtifactValidationException(errorMsg.toString());
        }
    }

    /*
     *  4.1.1.1.1    @Open
     *  
     *  The @Open annotation identifies a method that does initialization processing for an 
     *  item reader.  This method receives a Java Externalizable that contains the last 
     *  checkpoint position for the reader.  If the checkpoint is null, this method should
     *  position the reader to the beginning of its input stream.  If the checkpoint is 
     *  non-null, this method should position the reader to the last checkpointed position.  
     *  This is a REQUIRED method for an item reader.
     *  
     *  4.1.1.1.2   @Close
     *  
     *  The @Close annotation identifies a method that does cleanup processing for an item
     *   reader.  This is an optional method for an item reader.
     *   
     *  4.1.1.1.3  @ReadItem
     *  
     *  The @ReadItem annotation identifies a method that reads the next item from an item
     *  reader.  This method returns an item of the type defined by this reader.   This is
     *  a REQUIRED method for an item reader.
     *  
     *  4.1.1.1.4   @CheckpointInfo
     *  
     *  The @CheckpointInfo annotation identifies a method that receives control to provide the 
     *  current checkpoint position of a reader.  This method returns a Java Externalizable.   This is 
     *  a REQUIRED method for an item reader.
     */

    public static void validate(ItemReaderProxy proxy) throws ArtifactValidationException {

        Object delegate = proxy.getDelegate();
        String description = "Artifact: " + delegate + " of type: " + delegate.getClass();

        if (!delegate.getClass().isAnnotationPresent(ItemReader.class)) {
            throw new ArtifactValidationException(description + " does not contain a @ItemReader annotation.");
        }
        
        StringBuffer errorMsg = new StringBuffer(description + " has an invalid signature: \n");     
        boolean valid = true;
        Method openMethod = proxy.getOpenMethod();
        if ( openMethod == null) {
            errorMsg.append("  @Open-annotated method is required, but none found.\n");
            valid = false;
        } else{
            Class[] parmTypes = openMethod.getParameterTypes();            
            if (parmTypes.length !=1) {
                errorMsg.append("  @Open-annotated method requires a single input parameter but found parm size = " + parmTypes.length + ".\n");
                valid = false;                      
            } else if (!java.io.Externalizable.class.isAssignableFrom(parmTypes[0])) {
                errorMsg.append("  @Open-annotated method input parameter should accept an argument implementing java.io.Externalizable.\n");
                valid = false;            
            }
        }

        if (proxy.getReadItemMethod() == null) {
            errorMsg.append("  @ReadItem-annotated method is required, but none found.\n");
            valid = false;
        }

        // Strange getter name!
        Method checkpointInfoMethod = proxy.getCheckpointInfoMethod();
        if ( checkpointInfoMethod == null) {
            errorMsg.append("  @CheckpointInfo-annotated method is required, but none found.\n");
            valid = false;
        } else{
            Class<?> returnType = checkpointInfoMethod.getReturnType();            
            if (!java.io.Externalizable.class.isAssignableFrom(returnType)) {
                errorMsg.append("  @CheckpointInfo-annotated method return type should implement java.io.Externalizable.\n");
                valid = false;
            }
        }
        
        if (!valid) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(errorMsg.toString());
            }
            throw new ArtifactValidationException(errorMsg.toString());
        }
    }

    /*
     * 4.2.4.1  @BeforeRead
     * 
     * The @BeforeRead annotation identifies a method that receives 
     * control before an item reader is called to read the next item.  
     * This is an optional method for an item read listener. 
     * 
     * 4.2.4.2 @AfterRead
     * 
     * The @AfterRead annotation identifies a method that receives control
     * after an item reader reads an item.  The method receives the item 
     * read as an input.  This is an optional method for an item read 
     * listener.
     *  
     * 4.2.4.3 @OnReadError
     * 
     * The @OnReadError annotation identifies a method that receives 
     * control after an item reader throws an exception.  This method 
     * receives the exception as an input. This is an optional method 
     * for an item read listener.
     */

    public static void validate(ItemReadListenerProxy proxy) throws ArtifactValidationException {
        // No required methods
    }

    /*
     * 4.2.6.1 @BeforeWrite
     * 
     * The @BeforeWrite annotation identifies a method that receives 
     * control before an item writer is called to write its items.  The 
     * method receives the list of items sent to the item reader as an 
     * input.  This is an optional method for an item write listener. 

     * 
     * 4.2.6.2  @AfterWrite
     * 
     * The @AfterWrite annotation identifies a method that receives 
     * control after an item writer writes its items.  The method receives
     * the list of items sent to the item reader as an input.  This is an
     * optional method for an item write listener.

     *  
     * 4.2.6.3  @OnWriteError
     * 
     * The @OnWriteError annotation identifies a method that receives 
     * control after an item writer throws an exception.  The method 
     * receives the list of items sent to the item reader as an input. 
     * This is an optional method for an item write listener.
     */

    public static void validate(ItemWriteListenerProxy proxy) throws ArtifactValidationException {
        // No required methods
    }

    /*
     *  4.1.1.3.1   @Open
     *  
     *  The @Open annotation identifies a method that does initialization
     *  processing for an item writer.  This method receives a Java 
     *  Externalizable that contains the last checkpoint position for the
     *  writer.  If the checkpoint is null, this method should position 
     *  the writer to the beginning of its output stream.  If the 
     *  checkpoint is non-null, this method should position the writer to
     *  the last checkpointed position.  This is a REQUIRED method for an
     *  item writer.
     *  
     *  4.1.1.3.2   @Close
     *  
     *  The @Close annotation identifies a method that does cleanup 
     *  processing for an item writer.  This is an optional method for an 
     *  item writer.
     *  
     *  4.1.1.3.3   @WriteItems
     *  
     *  The @WriteItems annotation identifies a method that writes the 
     *  next item for an item writer.  This method receives an item to 
     *  write to the writer's output stream.  The method defines the item
     *  type.   This is a REQUIRED method for an item writer.
     *  
     *  4.1.1.3.4   @CheckpointInfo
     *  
     *  The @CheckpointInfo annotation identifies a method that 
     *  receives control to provide the current checkpoint position of a 
     *  writer.  This method returns a Java Externalizable.   This is a 
     *  REQUIRED method for an item writer.
     */

    public static void validate(ItemWriterProxy proxy) throws ArtifactValidationException {
        Object delegate = proxy.getDelegate();
        String description = "Artifact: " + delegate + " of type: " + delegate.getClass();

        if (!delegate.getClass().isAnnotationPresent(ItemWriter.class)) {
            throw new ArtifactValidationException(description + " does not contain a @ItemWriter annotation.");
        }
        
        StringBuffer errorMsg = new StringBuffer(description + " has an invalid signature: \n");     
        boolean valid = true;

        Method openMethod = proxy.getOpenMethod();
        if ( openMethod == null) {
            errorMsg.append("  @Open-annotated method is required, but none found.\n");
            valid = false;
        } else{
            Class[] parmTypes = openMethod.getParameterTypes();            
            if (parmTypes.length !=1) {
                errorMsg.append("  @Open-annotated method requires a single input parameter but found parm size = " + parmTypes.length + ".\n");
                valid = false;                      
            } else if (!java.io.Externalizable.class.isAssignableFrom(parmTypes[0])) {
                errorMsg.append("  @Open-annotated method input parameter should accept an argument implementing java.io.Externalizable.\n");
                valid = false;            
            }
        }
        Method writeItemsMethod = proxy.getWriteItemsMethod(); 
        if ( writeItemsMethod == null) {
            errorMsg.append("  @WriteItems-annotated method is required, but none found.\n");
            valid = false;            
        } else {
            Class<?>[] parmTypes = writeItemsMethod.getParameterTypes();            
            if (parmTypes.length !=1) {
                errorMsg.append("  @WriteItems-annotated method requires a single input parameter but found parm size = " + parmTypes.length + ".\n");
                valid = false;                      
            } else if (!java.util.List.class.isAssignableFrom(parmTypes[0])) {
                // I guess no reason to blow up if an impl or subclass of List is found.
                errorMsg.append("  @WriteItems-annotated method input parameter should accept an argument of type List<item-type>.\n");
                valid = false;            
            }
        }

        // Strange getter name!
        Method checkpointInfoMethod = proxy.getCheckpointInfoMethod();
        if ( checkpointInfoMethod == null) {
            errorMsg.append("  @CheckpointInfo-annotated method is required, but none found.\n");
            valid = false;
        } else{
            Class<?> returnType = checkpointInfoMethod.getReturnType();            
            if (!java.io.Externalizable.class.isAssignableFrom(returnType)) {
                errorMsg.append("  @CheckpointInfo-annotated method return type should implement java.io.Externalizable.\n");
                valid = false;
            }
        }

        if (!valid) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(errorMsg.toString());
            }
            throw new ArtifactValidationException(errorMsg.toString());
        }
    }


    /*
     * 4.2.5.1 @BeforeProcess The @BeforeProcess annotation identifies a method
     * that receives control before an item processor is called to process the
     * next item. The method receives the item to be processed as an input. This
     * is an optional method for an item processor listener.
     * 
     * 4.2.5.2 @AfterProcess The @AfterProcess annotation identifies a method
     * that receives control after an item processor processes an item. The
     * method receives the item processed and the result item as an input. This
     * is an optional method for an item processor listener.
     * 
     * 4.2.5.3 @OnProcessorError The @OnProcessError annotation identifies a
     * method that receives control after an item processor throws an exception.
     * This method receives the exception and the input item. This is an
     * optional method for an item processor listener.
     */

    public static void validate(ItemProcessListenerProxy proxy) throws ArtifactValidationException {
        // No required methods
    }

    /*
     * 4.2.1.1 @BeforeJob
     * 
     * The @BeforeJob annotation identifies a method that receives control
     * before a job execution begins. This is an optional method for a job
     * listener.
     * 
     * 4.2.1.2 @AfterJob
     * 
     * The @AfterJob annotation identifies a method that receives control after
     * a job execution ends. This is an optional method for a job listener.
     */

    public static void validate(JobListenerProxy proxy) throws ArtifactValidationException {
        // No required methods
    }

    /*
     * 4.2.2.1 @BeforeStep
     * 
     * The @BeforeStep annotation identifies a method that receives control
     * before a step execution begins. This is an optional method for a step
     * listener.
     * 
     * 4.2.2.2 @AfterStep
     * 
     * The @AfterStep annotation identifies a method that receives control after
     * a step execution ends. This is an optional method for a step listener.
     */

    public static void validate(StepListenerProxy proxy) throws ArtifactValidationException {
        // No required methods
    }


    public static void validate(DeciderProxy proxy) throws ArtifactValidationException {
        Object delegate = proxy.getDelegate();
        String description = "Artifact: " + delegate + " of type: " + delegate.getClass();

        if (!delegate.getClass().isAnnotationPresent(Decider.class)) {
            throw new ArtifactValidationException(description + " does not contain a @Decider annotation.");
        }

        StringBuffer errorMsg = new StringBuffer(description + " has an invalid signature: \n");     
        boolean valid = true;
        
        Method decideMethod = proxy.getDecideMethod();
        if (decideMethod == null) {
            errorMsg.append("  @Decide-annotated method is required, but none found.\n");
            valid = false;
        } else {
        	Class<?> returnType = decideMethod.getReturnType();            
            if (!String.class.equals(returnType)) {
                errorMsg.append("  @Decide-annotated method return type should be java.lang.String, but was: " + returnType + ".\n");
                valid = false;
            }        
        }        
        
        if (!valid) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine(errorMsg.toString());
            }
            throw new ArtifactValidationException(errorMsg.toString());
        }
    }

    public static void validate(CheckpointAlgorithmProxy proxy) throws ArtifactValidationException {
        Object delegate = proxy.getDelegate();
        String description = "Artifact: " + delegate + " of type: " + delegate.getClass();

        if (!delegate.getClass().isAnnotationPresent(CheckpointAlgorithm.class)) {
            throw new ArtifactValidationException(description + " does not contain a @CheckpointAlgorithm annotation.");
        }
    }

    public static void validate(PartitionAnalyzerProxy proxy) throws ArtifactValidationException {
        Object delegate = proxy.getDelegate();
        String description = "Artifact: " + delegate + " of type: " + delegate.getClass();

        if (!delegate.getClass().isAnnotationPresent(PartitionAnalyzer.class)) {
            throw new ArtifactValidationException(description + " does not contain a @SubJobAnalyzer annotation.");
        }
    }

    public static void validate(PartitionCollectorProxy proxy) throws ArtifactValidationException {
        Object delegate = proxy.getDelegate();
        String description = "Artifact: " + delegate + " of type: " + delegate.getClass();

        if (!delegate.getClass().isAnnotationPresent(PartitionCollector.class)) {
            throw new ArtifactValidationException(description + " does not contain a @SubJobCollector annotation.");
        }
    }

    public static void validate(PartitionMapperProxy proxy) throws ArtifactValidationException {
        Object delegate = proxy.getDelegate();
        String description = "Artifact: " + delegate + " of type: " + delegate.getClass();

        if (!delegate.getClass().isAnnotationPresent(PartitionMapper.class)) {
            throw new ArtifactValidationException(description + " does not contain a @PartitionMapper annotation.");
        }
    }

    //TODO - add spec description if it's really worthwhile.
    // But note these don't have any REQUIRED methods.

    public static void validate(RetryListenerProxy proxy) throws ArtifactValidationException {
        // No required methods
    }
    public static void validate(SkipListenerProxy proxy) throws ArtifactValidationException {
        // No required methods
    }
    
    public static void validate(PartitionReducerProxy proxy) throws ArtifactValidationException {
        Object delegate = proxy.getDelegate();
        String description = "Artifact: " + delegate + " of type: " + delegate.getClass();

        if (!delegate.getClass().isAnnotationPresent(PartitionReducer.class)) {
            throw new ArtifactValidationException(description + " does not contain a @LogicalTX annotation.");
        }
    }
    
}
