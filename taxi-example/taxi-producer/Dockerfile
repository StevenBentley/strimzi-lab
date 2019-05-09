FROM centos:7

RUN yum -y install java-1.8.0-openjdk-headless && yum -y clean all

# Set JAVA_HOME env var
ENV JAVA_HOME /usr/lib/jvm/java

ARG version=latest
ENV VERSION ${version}

COPY ./scripts/ /bin

ADD target/taxi-producer-1.0-SNAPSHOT.jar /

CMD ["/bin/run.sh", "/taxi-producer-1.0-SNAPSHOT.jar", "/sorteddata.csv"]