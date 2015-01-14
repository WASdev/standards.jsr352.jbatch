standards.jsr352.jbatch
=======================

*More to come*

## Building

To generate the sigtest files maven requires the location of the JDK to
be used. You can specify these by copying [default.sigtest.properties](default.sigtest.properties)
to `sigtest.properties` and filling in the locations of the available
JDK's.

Because not all of these may be available on every system, generation
of each JDK's signature must be activated with a build profile. These
are `sigtest6`, `sigtest7` and `sigtest8`.

For example on a Fedora system with no JDK 6, OpenJDK 7 and Oracle
JDK 8 the `sigtest.properties` might look like:

```ini
sigtest.java6.home=
sigtest.java7.home=/lib/jdk/openjdk-1.7.0
sigtest.java8.home=/usr/java/latest
```

And the command to generate the sigtest files for the available JDK's
would be:

```shell
mvn install -Psigtest7,sigtest8
```

## Contributing

[CLA details](CONTRIBUTING.md)

#### Other IBM GitHub projects

Find more open source projects on the [IBM Github Page](http://ibm.github.io/)
