jbatch - Former JSR 352 reference implementation
=======================

## Branch history (why did I get a merge conflict when merging 'master'?)

This isn't a good practice, so we give at least this explanation.  We changed what master means without a clean merge to preserve history.  Sorry for any incovenience this caused, we decided this wasn't an active-enough project to worry about the downside here.

The branch structure/history now:

* [**master**](https://github.com/WASdev/standards.jsr352.jbatch/tree/master/) - EE 9, javax.* -> jakarta.* package rename ("big bang")
* [**1.0.x**](https://github.com/WASdev/standards.jsr352.jbatch/tree/1.0.x/)  - Service branch for the original JSR 352, EE 7 codebase

* [**pre-jakarta-master**](https://github.com/WASdev/standards.jsr352.jbatch/tree/pre-jakarta-master/) - This had been the **master** branch until the EE 9 work began.  At this point, this branch was moving towards something more like a "1.1", with "minor"-level changes to the previous branch.  We never released anything off this branch, so we save this pointer for historical reference (and have moved whatever we wanted into the new [**master**](https://github.com/WASdev/standards.jsr352.jbatch/tree/master/) branch).

## Jakarta Batch compatible implementation

Implements [Jakarta Batch 1.0](https://github.com/eclipse-ee4j/batch-api).

## Contributing

[CLA details](https://github.com/WASdev/standards.jsr352.batch-spec/wiki/Contributor-License-Agreement)

#### Other IBM GitHub projects

Find more open source projects on the [IBM Github Page](http://ibm.github.io/)
