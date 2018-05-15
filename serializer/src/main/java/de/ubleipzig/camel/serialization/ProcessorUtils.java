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
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;

import org.apache.camel.CamelContext;
import org.slf4j.Logger;

public final class ProcessorUtils {
    private static final Logger LOGGER  = getLogger(ProcessorUtils.class);

    private ProcessorUtils() {

    }

    /**
     * Tokenize a property placeholder value.
     *
     * @param context the camel context
     * @param property the name of the property placeholder
     * @param token the token used for splitting the value
     * @return a list of values
     */
    public static List<String> tokenizePropertyPlaceholder(final CamelContext context, final String property,
                                                           final String token) {
        try {
            return stream(context.resolvePropertyPlaceholders(property).split(token)).map(String::trim)
                    .filter(val -> !val.isEmpty()).collect(toList());
        } catch (final Exception ex) {
            LOGGER.debug("No property value found for {}", property);
            return emptyList();
        }
    }
}
