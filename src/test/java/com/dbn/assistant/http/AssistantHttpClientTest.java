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

package com.dbn.assistant.http;

import dev.langchain4j.http.client.HttpRequest;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static dev.langchain4j.http.client.HttpMethod.GET;

public class AssistantHttpClientTest {
    @Test
    public void httpErrorsDoNotExposeRequestUrls() {
        AssistantHttpClient client = new AssistantHttpClient(request ->
                new ErrorConnection(403, "Forbidden", "access denied"));
        HttpRequest request = HttpRequest.builder()
                .method(GET)
                .url("https://tenant:secret@assistant.internal.example.com/mcp/tenant-route?token=secret#fragment")
                .build();

        RuntimeException exception = Assert.assertThrows(RuntimeException.class, () -> client.execute(request));

        Throwable cause = exception.getCause();
        Assert.assertNotNull(cause);
        String message = cause.getMessage();
        Assert.assertTrue(message.contains("HTTP 403: Forbidden"));
        Assert.assertTrue(message.contains("access denied"));
        Assert.assertTrue(message.contains("Request URL: https://assistant.internal.example.com"));
        Assert.assertFalse(message.contains("tenant:secret"));
        Assert.assertFalse(message.contains("mcp"));
        Assert.assertFalse(message.contains("tenant-route"));
        Assert.assertFalse(message.contains("token=secret"));
        Assert.assertFalse(message.contains("fragment"));
    }

    @Test
    public void httpErrorsPreserveKnownPublicApiPaths() {
        AssistantHttpClient client = new AssistantHttpClient(request ->
                new ErrorConnection(429, "Too Many Requests", "rate limit exceeded"));
        HttpRequest request = HttpRequest.builder()
                .method(GET)
                .url("https://api.openai.com/v1/chat/completions?token=secret#fragment")
                .build();

        RuntimeException exception = Assert.assertThrows(RuntimeException.class, () -> client.execute(request));

        Throwable cause = exception.getCause();
        Assert.assertNotNull(cause);
        String message = cause.getMessage();
        Assert.assertTrue(message.contains("HTTP 429: Too Many Requests"));
        Assert.assertTrue(message.contains("Request URL: https://api.openai.com/v1/chat/completions"));
        Assert.assertFalse(message.contains("token=secret"));
        Assert.assertFalse(message.contains("fragment"));
    }

    private static class ErrorConnection extends HttpURLConnection {
        private final int responseCode;
        private final String responseMessage;
        private final String error;

        private ErrorConnection(int responseCode, String responseMessage, String error) throws IOException {
            super(new URL("https://assistant.example.com"));
            this.responseCode = responseCode;
            this.responseMessage = responseMessage;
            this.error = error;
        }

        @Override
        public int getResponseCode() {
            return responseCode;
        }

        @Override
        public String getResponseMessage() {
            return responseMessage;
        }

        @Override
        public InputStream getErrorStream() {
            return new ByteArrayInputStream(error.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public void disconnect() {
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() {
        }
    }
}
