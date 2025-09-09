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

package com.dbn.assistant.http;

import com.intellij.util.net.HttpConnectionUtils;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.http.client.HttpMethod;
import dev.langchain4j.http.client.HttpRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

class AssistantHttpClientBuilder implements HttpClientBuilder {
    private Duration readTimeout;
    private Duration connectTimeout;

    @Override
    public Duration connectTimeout() {
        return connectTimeout;
    }

    @Override
    public HttpClientBuilder connectTimeout(Duration timeout) {
        this.connectTimeout = timeout;
        return this;
    }

    @Override
    public Duration readTimeout() {
        return readTimeout;
    }

    @Override
    public HttpClientBuilder readTimeout(Duration timeout) {
        this.readTimeout = timeout;
        return this;
    }

    @Override
    public HttpClient build() {
        return new AssistantHttpClient(r -> createConnection(r));
    }


    private HttpURLConnection createConnection(HttpRequest request) throws IOException {
        HttpURLConnection connection = HttpConnectionUtils.openHttpConnection(request.url());

        initConnectionTimeouts(connection);
        initRequestMethod(request, connection);
        initRequestHeaders(request, connection);
        initRequestBody(request, connection);

        connection.connect();
        return connection;
    }

    private void initConnectionTimeouts(HttpURLConnection connection) {
        connection.setConnectTimeout((int) connectTimeout.toMillis());
        connection.setReadTimeout((int) readTimeout.toMillis());
    }

    private static void initRequestMethod(HttpRequest request, HttpURLConnection connection) throws ProtocolException {
        HttpMethod method = request.method();
        connection.setRequestMethod(method.name());
        connection.setDoOutput(method == HttpMethod.POST || method == HttpMethod.DELETE);
        connection.setDoInput(true);
    }

    private static void initRequestHeaders(HttpRequest request, HttpURLConnection connection) {
        Map<String, List<String>> headers = request.headers();
        for (String key : headers.keySet()) {
            List<String> values = headers.get(key);
            connection.setRequestProperty(key, String.join(",", values));
        }
    }

    private static void initRequestBody(HttpRequest request, HttpURLConnection connection) throws IOException {
        String body = request.body();
        if (body != null && !body.isEmpty()) {
            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
            connection.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));
            try (OutputStream os = connection.getOutputStream()) {
                os.write(bodyBytes);
            }
        }
    }

}
