FROM strimzi/kafka-connect:0.11.1-kafka-2.1.0

ARG version=latest
ENV VERSION ${version}

USER root:root
COPY target/taxi-connect-1.0-SNAPSHOT.jar /opt/kafka/plugins/
USER kafka:kafka