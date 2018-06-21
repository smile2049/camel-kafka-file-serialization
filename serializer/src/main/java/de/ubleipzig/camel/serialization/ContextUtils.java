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

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.camel.util.IOHelper;

public final class ContextUtils {

    private ContextUtils() {
    }

    /**
     * createInitialContext.
     *
     * @return InitialContext Context
     */
    public static Context createInitialContext() {
        final InputStream in = ContextUtils.class.getClassLoader().getResourceAsStream("jndi.properties");
        try {
            final Properties properties = new Properties();
            properties.load(in);
            return new InitialContext(new Hashtable<>(properties));
        } catch (NamingException | IOException e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            IOHelper.close(in);
        }
    }
}
