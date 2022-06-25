######################
# Used to produce results recorded at:
#   https://github.com/WASdev/standards.jsr352.jbatch/wiki/<version combo>
######################

docker build -t scottkurz/jbatch-2.1-tck-run:jbatch-2.1.1-temurin-11-centos7 --build-arg BASE=eclipse-temurin:11-centos7 .
docker build -t scottkurz/jbatch-2.1-tck-run:jbatch-2.1.1-temurin-17-centos7 --build-arg BASE=eclipse-temurin:17-centos7 .
docker build -t scottkurz/jbatch-2.1-tck-run:jbatch-2.1.1-ibmsemeruruntime-open-11-jdk-centos7  --build-arg BASE=ibm-semeru-runtimes:open-11-jdk-centos7 .
docker build -t scottkurz/jbatch-2.1-tck-run:jbatch-2.1.1-ibmsemeruruntime-open-17-jdk-centos7  --build-arg BASE=ibm-semeru-runtimes:open-17-jdk-centos7 .

