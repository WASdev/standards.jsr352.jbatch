standards.jsr352.jbatch
=======================

*More to come*

## TCK modules explained

The big idea to understand here is that we're still viewing the TCK ZIP built in
the **com.ibm.jbatch.tck.dist** module as the official TCK, executed via Ant.

### com.ibm.jbatch.tck.dist.exec

* run TestNG & SigTest against the RI using the "official" distribution (only runs SigTest
against the current JVM level

### com.ibm.jbatch.tck
* compile test case and batch artifact classes into single JAR
* special profile to update generated batch.xml file
* special profile to update generated SigTest files (possibly better fit for tck.dist)
* massages build.xml a bit (possibly better fit for tck.dist)

### com.ibm.jbatch.tck.dist
* collects TCK jar and other dependencies into "official" TCK zip

### com.ibm.jbatch.tck.exec
* runs TestNG tests in Maven build using the failsafe plugin.

## TCK properties files explained

There are several properties files to understand, which can get confusing. 

Some important ones for the TCK execution within the build:

### com.ibm.jbatch.tck/sigtest/jsr352-sigtest-tck.properties
### com.ibm.jbatch.tck/testng/jsr352-tck.properties
These properties files will be read during the Ant execution of the official TCK (in the SigTest and TestNG portions, respectively).
Within the build these are just templates to be copied into the official distribution, to be populated by the executor later.

### com.ibm.jbatch.tck.dist.exec/tck.execution.for.ri.properties
In executing the official TCK against the RI, we use this properties file to supply the properties that could be set in the two
above files, **jsr352-tck.properties** and **jsr352-sigtest-tck.properties**.  Rather than trying to manipulate those two
properties in the filesystem within this Maven execution, we just read them into memory as system properties before executing the TCK.

### com.ibm.jbatch.tck.exec/test.properties
### com.ibm.jbatch.tck.exec/default.tck.exec.properties
These properties are purely there to configure the RI, during its run of the TestNG portion of the TCK using the failsafe plugin.
They configure the services (eg artifact factory) used by the RI as well as the sleep time properties used by the TCK.
However, we don't have to worry about using properties to point to paths and JAR locations like we do in the official Ant-based TCK.

The execution actually runs against the **test.properties** file, which is ignored in the Git
repo, though **default.tck.exec.properties** is committed into Git and will be copied into place as **test.properties** if one isn't present.

## TODO - come back to the SigTest section since I broke the previous constructs
*deleted for now*

## Contributing

[CLA details](CONTRIBUTING.md)

#### Other IBM GitHub projects

Find more open source projects on the [IBM Github Page](http://ibm.github.io/)
