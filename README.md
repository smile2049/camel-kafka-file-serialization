## camel-kafka-file-serialization
An implementation of [camel-ldp](https://github.com/trellis-ldp/camel-ldp) that:
* consumes an activity stream from Kafka 
* filters events of type `http://www.w3.org/ns/ldp#NonRDFSource`
* retrieves only `valid.formats` from TrellisLDP
* executes ImageMagik Convert 
* streams the resource to a configured file system directory.

## Configuration
There are two deployment conditions, in a docker network or outside of one.

If you are testing, this will be done outside of Docker, so it is ok to use `localhost`
However, if you are running in a Docker network, the hostnames must match the container names.
So, the trellis and kafka endpoints should to be configured like this:
`
kafka.host=110_kafka_1
trellis.baseUrl=trellis:8445
`

* HTTPS
Note that there is a X509 certificate in the docker build so that Camel can handshake with Trellis over a secure channel.  
This file `trellis.crt` must match the certificate built into the Trellis image!

* Filesystem
The host serialization directory defaults to `/mnt/serialized-binaries`.  
This is a bound volume for both the image server and the serializer.

* Image server Filesystem resolver
See `cantaloupe.properties` to build project specific collection paths.  
Currently, it is set for `SERIALIZATION_DEFAULT_DIR` + `/collection/vp/res/`