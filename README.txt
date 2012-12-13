##########################
# DROP 2 of JSR 352 RI/TCK
##########################

###################
# GETTING STARTED  
###################

This is how to set up the RI in JSE and run the TCK tests:

0. The instructions assume you start from the distribution "Eclipse IDE for Java EE Developers" (with PDE).  

In addition, you will need the javax.transaction package available in some plugin in your PDE.  It seems
to be that this is bundled within the Indigo-level (SR2) Eclipse JEE distributions, but not in the Juno
distributions.  

For Juno distributions, you could get javax.transaction at the Eclipse Orbit project:
  http://www.eclipse.org/orbit/

E.g. at:
  http://fullmoon.rtp.raleigh.ibm.com/tools/orbit/downloads/drops/R20120526062928/repository/
  
Do Help->Install New Software-> ... etc. ...
  
Simply adding the plugin supplying javax.transaction to your Eclipse installation will by default make it 
available to your PDE projects. 

Once your Eclipse installation is configured (restart possibly necessary), you can proceed by: 

1. File->Import->General->Existing Projects Into Workspace -> Select archive file
  (point to the xxxx.zip)

2. Define a Java 6 JRE in your Eclipse environment and make it the default JRE (or the one used with the JSR352 work).

3. Satisfy the two builds' dependency on @Named.  

   Sorry for having to do this step ... we'd intended to have a self-contained package but 
   somehow this dependency got buried and we didn't notice it until it was too late for us to ship.    

   Anyway: 
   a) Download a JAR containing the class file for the @Named (@javax.inject.Named) dependency.  

   This can be found in a JSR 330 implementation.   (It seems to be part of the Juno Eclipse JEE
   distribution as well).

   b) Once you have one, define a Java classpath variable

     JAVAX_INJECT_JAR 
   
   to its location.   

   c) Now for the Ant build:

   Right-click JSR352.Tests.TCK/ant/build.xml and select

     Run As -> External Tools Configurations

   Under Ant Build you should see a pre-defined Configuration called: 
    "TCK.artifact.indexing.with.named.property.ant"
  
   Click on "Properties" tab, and set property
 
      javax.inject.jar

   to this same location (as in 3b).

   Make sure the "Use global properties as specified in the Ant runtime preferences" checkbox is NOT checked.

4. Run the Ant build you configured in 3c), at: JSR352.Tests.TCK/ant/build.xml 

   Make sure you use the build configuration named 
    "TCK.artifact.indexing.with.named.property.ant"
   which you edited with the javax.inject.jar location.

   You will JSR352.Tests.TCK/testArtifactsBin get populated.

5. Set up the Derby database tables.

   From the workspace home directory, run script:

    JSR352.Tests.TCK/gettingStarted/initDerbyDatabases.bat <DERBY_DATABASE_HOME>

   passing as a single parameter value the parent directory for the databases used by
   the RI runtime (database: RUNTIMEDB), and the TCK (two databases: NUMBERDB and ORDERDB). 

6. Start with the JUnit Run Configuration packaged within the TCK project.

   Right-click the JSR352.Tests.TCK project and select 
     Run As -> Run Configurations

   Under the JUnit selection in the left-hand panel, you should expand the twisty and select the choice named
   "TCK.Debug.LongWait"

   Under the "Arguments" tab, go to the bottom panel, "VM Arguments", and you'll see a few arguments.

   Edit the one to point to the "DERBY_DATABASE_HOME" directory you used in step 5).
     -Dderby.system.home=C:\aaa\work\tmp\db35

7. Run the tests.  You should see 80/80 (that includes 2 which are ignored).


###################
# FUNCTIONAL LIST
###################
--- new in drop 2 ---
 * JSL inheritance 
 * chunk processing
  + buffered writes
  + restart/checkpoint
  + integrated w/ global tran (JEE only)
  + item-based, time-based 
  + skip handling
  - Didn't implement retry
  - Didn't implement item read/process/write listeners
--- already in drop 1 ---
 - batchlet
 - restart processing
 - partitioned step
 - property substitution

#####################
# SPEC COVERAGE NOTES
#####################
This "drop 2" has gotten quite a bit behind the spec. 

We have items implemented at all of the levels: 
 - early drafts (before public draft)
 - public draft
 - post public-draft discussion that we've reached consensus on
 - things we haven't reached consensus on where the RI/TCK has its own
   temporary interpretation that we have to of course synch with the spec.
   
However, we don't have a fine-grained breakdown of which items map to
which exact spec versions.

###################################################
# SPEC COVERAGE DETAILS WORTH NOTING IN PARTICULAR
###################################################
1. jobOperator
   
   We have JobOperator.start() returning an EXECUTION id, not an INSTANCE id 
   like the public draft spec details.   The public draft spec does say that
   JobOperator.restart() returns an EXECUTION id, so for the RI/TCK we did
   something similar.
   
   I don't remember the original rationale for why we did this, but given the
   discussion still underway about JobOperator, we chose to not make any changes
   until this settled down.
 
2. Artifact loading

   This story is pretty confusing and inconsistent, but I'm not going to go into great detail to clarify.
   There were a lot of discussions in this space while we were doing the 
   development, and the place we ended up is going to have the artifacts looking
   a good bit different than they do now.
   
   So we do provide a mechanism for getting artifacts loaded so that the rest of the spec
   can be exercised, e.g. the details of the chunk loop and transactions, etc., but don't
   go building a bunch more artifacts using this pattern without consulting the latest 
   comments by Chris Vignola on the subject. 
   
   The main mechanism used in SE today is the annotation scanning done at compile time, which
   results in building an artifact index file at META-INF/batch.xml.   In the Eclipse workspace
   here we do NOT do this with the PDE Java build, but rather with a separate Ant build, for
   reasons I won't fully go into. 
   
   Also, we're using @Named in our artifacts partly so they can be loaded in JEE via
   CDI.   In SE the annotations aren't useful yet, though we do have an extension
   we didn't ship which uses Weld to load via CDI in SE, which we could make available
   upon request.  

3. tran

   These tests are @Ignore(d) since the primary target in this distribution is SE. 
   However, they are fully functional in an EE environment, we just did not get around
   to putting a nicer framework around the SE execution such that these could be ignored
   in some other fashion.

4. inheritance 

   Currently the TCK tests inheritance without actually running the resultant job.  We
   just force the implementation to do a logical merge and show us the merged XML, but
   we don't actually run the job using inheritance, partly since the mechanism for 
   resolving/following a @parent value is not defined by the spec.

######################################
# MORE DETAILED INFO ON RUNNING TESTS
######################################

1. Timing issues

 a) We don't want the test to wait forever if something bad happens and the job never completes 
   (and/or if we don't get the job end callback).    But we don't want to end to soon and fail
   the test just because we didn't wait long enough (or to kill the JVM when doing debugging).
   
   A number of the tests use a specific JVM property to configure a timeout specifying the time
   to wait for job completion (more or less).
 
   To customize this sleep timeout (e.g. increase when debugging or decrease to make some run slightly faster)
      add this to the Arguments->VM Arguments panel of the Run configuration Wizard 
      
      -Djunit.jobOperator.sleep.time=900000 
      
   This value of 900 seconds then is optimized for debugging rather than "fast failure".   Note that some
   of the chunk tests should take at least 15-25 seconds to run, so any value less than that for the whole
   TCK will cause some false negatives due to timing. 

 b) Unfortunately there are probably some tests that should use the above pattern but don't.
 
 c) In addition, there are other timing tricks employed by various tests in defining their test logic.   
    Hopefully they won't be noticed, but it is theoretically possible that on say a slow machine, one 
    of the TCK tests could fail for no other reason than the fact that the machine got bogged down and an
    unexpected timing window or event occurred.
    

2. Classpath

 Note the Classpath used in the TCK Run Configuration is very particular, adding three entries in addition
 to the default, build classpath.   I don't go into an explanation of why this is, but if you're trying to 
 set up your own Run Config you might need to look at this closely.
  
######################################
# PROJECT-BY-PROJECT DESCRIPTION
######################################

COMPILE/RUNTIME PROJECTS:

1. JSR352.Annotations

	This project contains the javax.batch.annotation.* definitions

2. JSR352.API

	This contains further non-annotation APIs like JobContext/StepContext which any project should
	be able to depend on (i.e. this shouldn't have any further dependencies).

3. JSR352.BinaryDependencies

    Contains IBM Derby 10,8.3.0, XMLUnit (used in inheritance TCK tests), and JUnit 4.10

3. JSR352.JobXML.Model
	
	In lieu of having to come up with a temporary runtime model representing job flow, we
	create a skeleton schema based on a stripped down version of what we expect to see in job XML,
	and then generate JAXBs to use as the model classes.
	
	This project should not have any dependencies on other projects.  The only dependency is on the schema itself.
	
    We do have a JAXB bindings file to customize the model a bit. 
	
4. JSR352.Processors

	This project contains the annotation processor implementation.  This processor detects 
	javax.batch.annotation.* definitions on the classes being compiled and writes the accumulated
	info from all classes to a single file: META-INF/batch.xml.

5. JSR352.Runtime

	This project contains the bulk of all interesting runtime function.  Would be nice
	to list out aspects individually.  
	
	Note that com.ibm.batch.container.services.ServicesManager is the master class from which all
	service providers are loaded, even the core container impl, which we will think of as the container 
	kernel service.
	
	It should have dependency on JobXML.Model, Annotations, API, TCK.SPI, and also Processors.
	
	The TCK.SPI dependency reflects the fact that we will provide a hook for the TCK to drive our functionality
	at a lower, more fine-grained level than simply "submit job", which will be useful for delivering more focused,
	scoped-down tests in the TCK.  
	
	For now we'll have the internal SPI, (e.g. com.ibm.batch.container.services.IBatchArtifactFactory) extend the
	TCK SPI (e.g. com.ibm.batch.container.tck.spi.BatchArtifactFactory).
	
	We might revisit that at some point, e.g. we could collapse them into one conceivably.
	
	The Processors dependency might seem a bit odd, since in a sense Processors is purely compile-time
	function.   It is useful though to have the mapping between artifact annotation classes
	and batch.xml entries in a single place, so either one will depend on the other or there'd need
	to be a third place.   Seems like not a big deal to throw a dev-time thing into the runtime, whereas
	in something like RAD you might want a stripped down API JAR as the dev-time environment.   So let's 
	not have Processors depend on Runtime but have Runtime depend on Processors.

6. JSR352.TCK.SPI

	This will hold bootstrapping and convenience/utility methods used by the TCK suite to drive the runtime.

    The definition of the JobEndCallback is particularly worth noting as it gives us a way to get notified
    when the jobs have run and it's OK to check the results (as they run asynchronously from their
    submission).
	
	
TEST PROJECTS:


1. JSR352.Tests.TCK

	Depends on TCK.SPI at compile time.
	
	To run you must add the JSR352.Runtime project to the runtime classpath, in addition to the 
	build time class path.   This reflects the fact that the tests do not depend on the runtime 
	but that they are run against the runtime.
	 
