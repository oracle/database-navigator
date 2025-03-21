/*
 * Copyright 2025 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.common.util;

import lombok.experimental.UtilityClass;

import java.util.HashMap;
import java.util.Map;

import static com.dbn.common.util.Strings.isEmpty;

/**
 * Utility class providing methods for working with HTTP-like query parameters.
 * This class includes functionality for converting a map of parameters to a query string
 * and for parsing a query string into a map of parameters.
 *
 * @author Dan Cioca (Oracle)
 */
@UtilityClass
public class Parameters {

    /**
     * Converts a map of key-value pairs into a query parameter string.
     * The resulting string starts with a "?" and includes key-value pairs joined by "&".
     * Keys and values that are empty are ignored.
     *
     * @param parameters the map containing key-value pairs to convert to a query parameter string
     * @return a query parameter string in the format "?key1=value1&key2=value2", or an empty string if the input map is null or contains no valid pairs
     */
    public static String toParameterString(Map<String, String> parameters) {
        StringBuilder builder = new StringBuilder();
        for (String key : parameters.keySet()) {
            String value = parameters.get(key);

            if (isEmpty(key)) continue;
            if (isEmpty(value)) continue;

            builder.append(builder.length() == 0 ? "?" : "&");
            builder.append(key);
            builder.append("=");
            builder.append(value);
        }

        return builder.toString();
    }

    /**
     * Parses a query parameter string into a map of key-value pairs.
     * The input string is expected to be in the format "key1=value1&key2=value2".
     * If the string starts with a "?", it will be stripped.
     * Keys or values that are null or empty are ignored.
     *
     * @param parameterString the query parameter string, which may start with "?"
     *                        and contain key-value pairs separated by "&"
     * @return a map containing the parsed key-value pairs, or an empty map if the input string is null, empty, or invalid
     */
    public static Map<String, String> toParameterMap(String parameterString) {
        Map<String, String> parameters = new HashMap<>();
        if (isEmpty(parameterString)) return parameters;

        if (parameterString.startsWith("?")) {
            parameterString = parameterString.substring(1);
        }

        String[] keyValues = parameterString.split("&");
        for (String keyValue : keyValues) {
            String[] tokens = keyValue.split("=");
            String key = tokens[0];
            String value = tokens.length > 1 ? tokens[1] : "";

            if (isEmpty(key)) continue;
            if (isEmpty(value)) continue;

            parameters.put(key, value);
        }

        return parameters;
    }
}
