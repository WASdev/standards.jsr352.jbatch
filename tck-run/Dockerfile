ARG JDK_BASE_QUAL=8-openj9
FROM adoptopenjdk/openjdk${JDK_BASE_QUAL}:ubi
ARG SIGTEST_LEVEL=8

RUN yum upgrade --disableplugin=subscription-manager -y \
   && yum clean --disableplugin=subscription-manager packages \
   && echo 'Finished installing dependencies'

USER root

# Dependency install
RUN yum install --disableplugin=subscription-manager -y https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm \
   && yum install --disableplugin=subscription-manager -y unzip curl ca-certificates wget xmlstarlet

# Maven install
ARG MAVEN_VERSION=3.6.3
ARG SHA=c35a1803a6e70a126e80b2b3ae33eed961f83ed74d18fcd16909b2d44d7dada3203f1ffe726c17ef8dcca2dcaa9fca676987befeadc9b9f759967a8cb77181c0
ARG BASE_URL=https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/

RUN mkdir -p /usr/share/maven /usr/share/maven/ref \
  && curl -fsSL -o /tmp/apache-maven.tar.gz ${BASE_URL}/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
  && echo "${SHA}  /tmp/apache-maven.tar.gz" | sha512sum -c - \
  && tar -xzf /tmp/apache-maven.tar.gz -C /usr/share/maven --strip-components=1 \
  && rm -f /tmp/apache-maven.tar.gz \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

ENV ANT_VERSION=1.10.8
RUN wget http://archive.apache.org/dist/ant/binaries/apache-ant-${ANT_VERSION}-bin.tar.gz \
    && tar xvfvz apache-ant-${ANT_VERSION}-bin.tar.gz -C /opt \
    && ln -sfn /opt/apache-ant-${ANT_VERSION} /opt/ant \
    && sh -c 'echo ANT_HOME=/opt/ant >> /etc/environment' \
    && ln -sfn /opt/ant/bin/ant /usr/bin/ant \
    && rm apache-ant-${ANT_VERSION}-bin.tar.gz

RUN  groupadd --gid 1000 java_group \
  && useradd --uid 1000 --gid java_group --shell /bin/bash --create-home java_user
USER java_user

# Kind of asymmetric to do just one but it'll make the runs go decently quicker to cache this
RUN mvn org.apache.maven.plugins:maven-dependency-plugin:3.1.2:get -Dartifact=org.apache.derby:derby:10.10.1.1 -DrepoUrl=https://repo1.maven.org/maven2/

ENV SIGTEST_LEVEL=${SIGTEST_LEVEL}

COPY ./how-to-run-tck-against-jbatch-script.sh /home/java_user
WORKDIR /home/java_user
CMD /bin/bash

