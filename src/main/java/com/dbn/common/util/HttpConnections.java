/*
 * Copyright 2026 Oracle and/or its affiliates
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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

@UtilityClass
public class HttpConnections {

    public static String readResponseBody(HttpURLConnection connection) throws IOException {
        InputStream inputStream = connection.getInputStream();
        if (inputStream == null) return "";

        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            StringBuilder buffer = new StringBuilder();
            char[] chars = new char[4096];
            int count;
            while ((count = reader.read(chars)) != -1) {
                buffer.append(chars, 0, count);
            }
            return buffer.toString();
        }
    }
}
