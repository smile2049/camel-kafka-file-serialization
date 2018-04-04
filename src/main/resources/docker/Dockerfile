FROM ubuntu:16.04

MAINTAINER Christopher Johnson <christopher_hanna.johnson@uni-leipzig.de>
LABEL description = "Provides a Camel Kafka File Serializer"

ENV SERIALIZER_VERSION 0.1.3-SNAPSHOT
ENV SERIALIZER_RUNTIME /opt

ENV JVM_BASE /usr/lib/jvm
ENV JDK_JAVA_OPTIONS="-Xms250m -Xmx1024m"
ENV JDK_SPEC 10
ENV JDK_VERSION 10
ENV JAVA_HOME ${JVM_BASE}/jdk-${JDK_VERSION}
ENV JDK_ARCHIVE openjdk-${JDK_VERSION}_linux-x64_bin.tar.gz
ENV PATH ${JAVA_HOME}/bin:$PATH

RUN apt-get update && \
    apt-get -y install \
    wget \
    build-essential autoconf libfcgi0ldbl libtool libtiff5-dev libpng-dev libmemcached-dev \
    memcached liblcms2-2 liblcms2-dev libgomp1 libpthread-stubs0-dev liblzma5 \
    liblzma-dev libjbig-dev libjbig0 libz80ex1 libz80ex-dev pkg-config zlib1g-dev libopenjp2-7 bash \
    git cmake libltdl-dev libperl-dev

RUN mkdir ${JVM_BASE} && \
    wget -O ${JVM_BASE}/${JDK_ARCHIVE} https://download.java.net/java/GA/jdk${JDK_SPEC}/${JDK_VERSION}/binaries/${JDK_ARCHIVE} && \
    cd ${JVM_BASE} && \
    tar -xzf ${JDK_ARCHIVE} && \
    java --version && \
    rm -rf /var/lib/apt/lists/* && apt-get purge -y

### Download and compile openjpeg2 (master)
WORKDIR /tmp/openjpeg
RUN git clone https://github.com/uclouvain/openjpeg.git ./
RUN cmake -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX=/usr && make && make install

WORKDIR /tmp/image-magick
RUN git clone https://github.com/ImageMagick/ImageMagick.git ./
RUN ./configure --prefix=/usr --with-modules --with-perl=/usr/bin/perl --with-jp2 --enable-shared --disable-static && \
make && make install

WORKDIR ${SERIALIZER_RUNTIME}
COPY *.tar .
RUN tar -xvf *.tar && \
mv serializer-${SERIALIZER_VERSION} serializer && \
chmod +x ${SERIALIZER_RUNTIME}/serializer/bin/serializer
COPY entrypoint.sh .
COPY trellis.crt .
RUN /usr/lib/jvm/jdk-10/bin/keytool -cacerts -storepass changeit -noprompt -import -alias trellis -file trellis.crt
RUN chmod +x entrypoint.sh
ENTRYPOINT [ "./entrypoint.sh" ]