## camel-kafka-file-serialization
An implementation of [camel-ldp](https://github.com/trellis-ldp/camel-ldp) that:
* consumes an activity stream from Kafka 
* filters events of type `http://www.w3.org/ns/ldp#NonRDFSource`
* retrieves only `valid.formats` from TrellisLDP
* executes ImageMagik Convert 
* streams the resource to a configured file system directory.