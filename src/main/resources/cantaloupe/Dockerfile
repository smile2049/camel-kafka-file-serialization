FROM openjdk:8-jre

ENV CANTALOUPE_VERSION 3.4.1
EXPOSE 8182

# Update packages and install tools
RUN apt-get update -y && apt-get install -y git wget unzip graphicsmagick curl build-essential cmake

### Download and compile openjpeg2 (master)
WORKDIR /tmp/openjpeg
RUN git clone https://github.com/uclouvain/openjpeg.git ./
RUN cmake -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX=/usr && make && make install

# run non priviledged
RUN adduser --system cantaloupe

#
# Cantaloupe
#
WORKDIR /tmp
RUN curl -OL https://github.com/medusa-project/cantaloupe/releases/download/v$CANTALOUPE_VERSION/Cantaloupe-$CANTALOUPE_VERSION.zip \
 && mkdir -p /usr/local/ \
 && cd /usr/local \
 && unzip /tmp/Cantaloupe-$CANTALOUPE_VERSION.zip \
 && ln -s Cantaloupe-$CANTALOUPE_VERSION cantaloupe \
 && rm -rf /tmp/Cantaloupe-$CANTALOUPE_VERSION \
 && rm /tmp/Cantaloupe-$CANTALOUPE_VERSION.zip

COPY cantaloupe.properties /etc/cantaloupe.properties
COPY delegates.rb /etc/delegates.rb
RUN mkdir -p /var/log/cantaloupe \
 && mkdir -p /var/cache/cantaloupe \
 && chown -R cantaloupe /var/log/cantaloupe \
 && chown -R cantaloupe /var/cache/cantaloupe \
 && chown cantaloupe /etc/cantaloupe.properties \
 && chown cantaloupe /etc/delegates.rb

USER cantaloupe
CMD ["sh", "-c", "java -Dcantaloupe.config=/etc/cantaloupe.properties -Xmx2g -jar /usr/local/cantaloupe/Cantaloupe-$CANTALOUPE_VERSION.war"]