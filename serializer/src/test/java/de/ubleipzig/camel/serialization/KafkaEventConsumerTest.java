/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.ubleipzig.camel.serialization;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.FILE_NAME;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.Exchange.HTTP_RESPONSE_CODE;
import static org.apache.camel.Exchange.HTTP_URI;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.builder.PredicateBuilder.and;
import static org.apache.camel.builder.PredicateBuilder.in;
import static org.apache.camel.builder.PredicateBuilder.or;
import static org.apache.camel.component.exec.ExecBinding.EXEC_COMMAND_ARGS;
import static org.trellisldp.camel.ActivityStreamProcessor.ACTIVITY_STREAM_OBJECT_ID;
import static org.trellisldp.camel.ActivityStreamProcessor.ACTIVITY_STREAM_OBJECT_TYPE;
import static org.trellisldp.camel.ActivityStreamProcessor.ACTIVITY_STREAM_TYPE;
import static de.ubleipzig.camel.serialization.ProcessorUtils.tokenizePropertyPlaceholder;

import java.io.InputStream;
import java.net.URI;
import java.util.Hashtable;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.camel.CamelContext;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.util.IOHelper;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trellisldp.camel.ActivityStreamProcessor;

/**
 * KafkaEventConsumerTest.
 *
 * @author christopher-johnson
 */
public final class KafkaEventConsumerTest {

    private static final String CREATE = "Create";
    private static final String UPDATE = "Update";
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaEventConsumerTest.class);
    private static final String IMAGE_OUTPUT = "CamelImageOutput";
    private static final String IMAGE_INPUT = "CamelImageInput";
    private static final String CONVERT_OPTIONS = " -set colorspace sRGB -depth 8 -";

    private KafkaEventConsumerTest() {
    }

    public static void main(final String[] args) throws Exception {

        LOGGER.info("About to run Kafka-camel integration...");

        final JndiRegistry registry = new JndiRegistry(createInitialContext());
        registry.bind("x509HostnameVerifier", new AllowAllHostnameVerifier());
        final CamelContext camelContext = new DefaultCamelContext(registry);

        camelContext.addRoutes(new RouteBuilder() {
            public void configure() {
                final PropertiesComponent pc = getContext().getComponent("properties", PropertiesComponent.class);
                pc.setLocation("classpath:application.properties");

                LOGGER.info("About to start route: Kafka Server -> Log ");

                from("kafka:{{consumer.topic}}?brokers={{kafka.host}}:{{kafka.port}}"
                        + "&maxPollRecords={{consumer.maxPollRecords}}" + "&consumersCount={{consumer.consumersCount}}"
                        + "&seekTo={{consumer.seekTo}}" + "&groupId={{consumer.group}}")
                        .routeId("FromKafka")
                        .unmarshal()
                        .json(JsonLibrary.Jackson)
                        .process(new ActivityStreamProcessor())
                        .marshal()
                        .json(JsonLibrary.Jackson, true)
                        .log(INFO, LOGGER, "Marshalling ActivityStreamMessage to JSON-LD")
                        //.to("file://{{serialization.log}}");
                        .to("direct:get");
                from("direct:get")
                        .routeId("BinaryGet")
                        .choice()
                        .when(and(in(tokenizePropertyPlaceholder(getContext(), "{{indexable.types}}", ",")
                                .stream()
                                .map(type -> header(ACTIVITY_STREAM_OBJECT_TYPE).contains(type))
                                .collect(toList())), or(header(ACTIVITY_STREAM_TYPE).contains(CREATE),
                                header(ACTIVITY_STREAM_TYPE).contains(UPDATE))))
                        .setHeader(HTTP_METHOD)
                        .constant("GET")
                        .setHeader(HTTP_URI)
                        .header(ACTIVITY_STREAM_OBJECT_ID)
                        .to("https4://localhost?x509HostnameVerifier=#x509HostnameVerifier")
                        .choice()
                        .when(header(CONTENT_TYPE).startsWith("image/"))
                        .log(INFO, LOGGER, "Image Processing ${headers[ActivityStreamObjectId]}")
                        .to("direct:convert")
                        .when(header("Link").contains("<http://www.w3.org/ns/ldp#NonRDFSource>;rel=\"type\""))
                        .setBody(constant("Error: this resource is not an image"))
                        .to("direct:invalidFormat")
                        .when(header(HTTP_RESPONSE_CODE).isEqualTo(200))
                        .setBody(constant("Error: this resource is not an ldp:NonRDFSource"))
                        .to("direct:invalidFormat")
                        .otherwise()
                        .   to("direct:error");
                from("direct:invalidFormat")
                        .routeId("ImageInvalidFormat")
                        .removeHeaders("*")
                        .setHeader(CONTENT_TYPE).constant("text/plain")
                        .setHeader(HTTP_RESPONSE_CODE).constant(400);
                from("direct:error")
                        .routeId("ImageError")
                        .setBody(constant("Error: this resource is not accessible"))
                        .setHeader(CONTENT_TYPE).constant("text/plain");
                from("direct:convert")
                        .routeId("ImageConvert")
                        .setHeader(IMAGE_INPUT)
                        .header(CONTENT_TYPE)
                        .process(exchange -> {
                            final String accept = exchange
                                    .getIn()
                                    .getHeader(IMAGE_OUTPUT, "", String.class);
                            final String fmt = accept.matches("^image/\\w+$") ? accept.replace(
                                    "image/", "") : getContext().resolvePropertyPlaceholders(
                                    "{{default.output.format}}");
                            final boolean valid;
                            try {
                                valid = stream(getContext()
                                        .resolvePropertyPlaceholders("{{valid.formats}}")
                                        .split(",")).anyMatch(fmt::equals);
                            } catch (final Exception ex) {
                                throw new RuntimeCamelException("Couldn't resolve property placeholder", ex);
                            }

                            if (valid) {
                                exchange
                                        .getIn()
                                        .setHeader(IMAGE_OUTPUT, "image/" + fmt);
                                exchange
                                        .getIn()
                                        .setHeader(EXEC_COMMAND_ARGS, CONVERT_OPTIONS + " " + fmt + ":-");
                            } else {
                                throw new RuntimeCamelException("Invalid format: " + fmt);
                            }
                        })
                        .log(INFO, LOGGER,
                                "Converting from ${headers[CamelImageInput]} to " + "${headers[CamelImageOutput]}")
                        .to("exec:{{convert.path}}")
                        .log(INFO, LOGGER, "Converting Resource: ${headers[CamelHttpUri]}")
                        .process(exchange -> {
                            final String resource = exchange
                                    .getIn()
                                    .getHeader(HTTP_URI, String.class);
                            final URI uri = new URI(resource);
                            final String path = uri.getPath();
                            final String outpath = path.replace(
                                    "tif", getContext().resolvePropertyPlaceholders("{{default.output.format}}"));
                            exchange
                                    .getIn()
                                    .setHeader(FILE_NAME, outpath);
                        })
                        .removeHeaders("CamelHttp*")
                        .to("direct:serialize");
                from("direct:serialize")
                        .process(exchange -> {
                            exchange
                                    .getOut()
                                    .setBody(exchange
                                            .getIn()
                                            .getBody(InputStream.class));
                            final String filename = exchange
                                    .getIn()
                                    .getHeader(FILE_NAME, String.class);
                            exchange
                                    .getOut()
                                    .setHeader(FILE_NAME, filename);
                        })
                        .log(INFO, LOGGER, "Filename: ${headers[CamelFileName]}")
                        .to("file://{{serialization.binaries}}");
            }
        });
        camelContext.start();

        // let it run for 5 minutes before shutting down
        Thread.sleep(5 * 60 * 1000);

        camelContext.stop();
    }

    public static Context createInitialContext() throws Exception {
        final InputStream in = KafkaEventConsumerTest.class
                .getClassLoader()
                .getResourceAsStream("jndi.properties");
        try {
            final Properties properties = new Properties();
            properties.load(in);
            return new InitialContext(new Hashtable<>(properties));
        } finally {
            IOHelper.close(in);
        }
    }
}