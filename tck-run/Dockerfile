ARG BASE
FROM ${BASE}

USER root

# Maven install
ARG MAVEN_VERSION=3.8.4
ARG SHA=a9b2d825eacf2e771ed5d6b0e01398589ac1bfa4171f36154d1b5787879605507802f699da6f7cfc80732a5282fd31b28e4cd6052338cbef0fa1358b48a5e3c8
ARG BASE_URL=https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries

RUN mkdir -p /usr/share/maven /usr/share/maven/ref \
  && curl -fsSL -o /tmp/apache-maven.tar.gz ${BASE_URL}/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
  && echo "${SHA}  /tmp/apache-maven.tar.gz" | sha512sum -c - \
  && tar -xzf /tmp/apache-maven.tar.gz -C /usr/share/maven --strip-components=1 \
  && rm -f /tmp/apache-maven.tar.gz \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

RUN  groupadd --gid 1000 java_group \
  && useradd --uid 1000 --gid java_group --shell /bin/bash --create-home java_user
USER java_user

COPY --chown=java_user:java_group ./run-tck-against-jbatch-script.sh /home/java_user/
COPY --chown=java_user:java_group ./settings.xml /home/java_user/.m2/
RUN chmod 777 /home/java_user/run-tck-against-jbatch-script.sh
RUN chmod 444 /home/java_user/.m2/settings.xml
WORKDIR /home/java_user
CMD /bin/bash

