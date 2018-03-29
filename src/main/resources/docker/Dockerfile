FROM openjdk:8-jre

MAINTAINER Christopher Johnson <christopher_hanna.johnson@uni-leipzig.de>
LABEL description = "Provides a Camel Kafka File Serializer"

ENV DATASET_RUNTIME /opt

WORKDIR ${DATASET_RUNTIME}
COPY *.tar .
RUN tar -xvf *.tar && \
mv serialization-${version} serialization && \
chmod +x ${DATASET_RUNTIME}/serialization/bin/serialization && \
COPY entrypoint.sh .
RUN chmod +x entrypoint.sh
ENTRYPOINT [ "./entrypoint.sh" ]