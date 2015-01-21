standards.jsr352.jbatch
=======================

*More to come*

## TCK modules explained

There are five TCK-related modules of interest:

### com.ibm.jbatch.tck

Collects (in a Maven JAR package) the test and artifact classes, as well as JSL(s), batch.xml, SigTest signature files, etc.  

Also has special profiles to update generated batch.xml file and generated SigTest files (which arguably could be factored
to fit in better than it does today).

### com.ibm.jbatch.tck.spi

The porting SPI, (if you wanted to use something besides the default polling to wait for a job to end)

### com.ibm.jbatch.tck.dist

Collects (into the "official" zip/archive) the TCK artifacts and dependencies along with the Ant buildfiles driving the TestNG and SigTest portions of the TCK.
This is basically the same form factor as the existing SE TCK zip (expanded to include the SigTest Ant buildfile too).

### com.ibm.jbatch.tck.exec

"Unofficial" execution of the TestNG (only, not SigTest) portion of the TCK against the RI (configured for SE mode).
In addition to providing testing of the various RI and TCK modules, this also serves as a sample for how other modules might consume the TCK
to test their own implementations against the TestNG portion of the TCK

### com.ibm.jbatch.tck.dist.exec

"Official" execution of the TestNG portion of the TCK (TestNG + SigTest) against the RI (configured for SE mode).
Again, in addition to providing testing of the RI and the TCK distribution, this also serves as a sample for how other modules might consume the
official TCK zip distribution and use the Ant scripts to test their own implementations against the TestNG portion of the TCK.


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
