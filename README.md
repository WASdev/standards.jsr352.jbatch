jbatch - implementation of Jakarta Batch 
=======================
## History 

(Former JSR 352 reference implementation)

### Move to schema 2.0 level

As of the 2.0.0-M5 release, this implementation only supports job XML definitions conforming to the Jakarta EE 9 schema at:
[https://jakarta.ee/xml/ns/jakartaee/jobXML_2_0.xsd](https://jakarta.ee/xml/ns/jakartaee/jobXML_2_0.xsd).

Note the attribute: `<job ... version="2.0">`

## Branch history (why did I get a merge conflict when merging 'master'?)


This isn't a good practice, so we give at least this explanation.  We changed what master means without a clean merge to preserve history.  Sorry for any incovenience this caused, we decided this wasn't an active-enough project to worry about the downside here.

The branch structure/history now:

* [**master**](https://github.com/WASdev/standards.jsr352.jbatch/tree/master/) - EE 9, javax.* -> jakarta.* package rename ("big bang")
* [**1.0.x**](https://github.com/WASdev/standards.jsr352.jbatch/tree/1.0.x/)  - Service branch for the original JSR 352, EE 7 codebase

* [**pre-jakarta-master**](https://github.com/WASdev/standards.jsr352.jbatch/tree/pre-jakarta-master/) - This had been the **master** branch until the EE 9 work began.  At this point, this branch was moving towards something more like a "1.1", with "minor"-level changes to the previous branch.  We never released anything off this branch, so we save this pointer for historical reference (and have moved whatever we wanted into the new [**master**](https://github.com/WASdev/standards.jsr352.jbatch/tree/master/) branch).

## Jakarta Batch compatible implementation

Implements [Jakarta Batch 2.0](https://github.com/eclipse-ee4j/batch-api).

## Contributing

[CLA details](https://github.com/WASdev/standards.jsr352.batch-spec/wiki/Contributor-License-Agreement)

#### Other IBM GitHub projects

Find more open source projects on the [IBM Github Page](http://ibm.github.io/)
