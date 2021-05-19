######################
# Used to produce results recorded at:
#
#   https://github.com/WASdev/standards.jsr352.jbatch/wiki/jbatch-v2.0.0-tck-v2.0.0

# Default build args: Java 8 + OpenJ9
docker build -t scottkurz/jbatch-2.0-tck-run:jbatch-2.0.0-openjdk8-openj9 .
# Java 8 + HotSpot
docker build -t scottkurz/jbatch-2.0-tck-run:jbatch-2.0.0-openjdk8 --build-arg JDK_BASE_QUAL=8 --build-arg SIGTEST_LEVEL=8 .
# Java 11 - same two variations
docker build -t scottkurz/jbatch-2.0-tck-run:jbatch-2.0.0-openjdk11-openj9 --build-arg JDK_BASE_QUAL=11-openj9 --build-arg SIGTEST_LEVEL=11 .
docker build -t scottkurz/jbatch-2.0-tck-run:jbatch-2.0.0-openjdk11 --build-arg JDK_BASE_QUAL=11 --build-arg SIGTEST_LEVEL=11 .

