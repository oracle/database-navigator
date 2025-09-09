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

import com.dbn.common.compatibility.Workaround;
import com.dbn.common.routine.ParametricCallable;
import com.dbn.common.routine.ThrowableCallable;
import com.dbn.common.util.Classes;
import com.dbn.common.util.Unsafe;
import dev.langchain4j.http.client.HttpClient;
import dev.langchain4j.http.client.HttpRequest;
import dev.langchain4j.http.client.SuccessfulHttpResponse;
import dev.langchain4j.http.client.sse.ServerSentEventListener;
import dev.langchain4j.http.client.sse.ServerSentEventParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import static com.dbn.common.util.Streams.readInputStream;

class AssistantHttpClient implements HttpClient {
    private final ParametricCallable<HttpRequest, HttpURLConnection, IOException> connectionBuilder;

    public AssistantHttpClient(ParametricCallable<HttpRequest, HttpURLConnection, IOException> connectionBuilder) {
        this.connectionBuilder = connectionBuilder;
    }

    @Override
    public SuccessfulHttpResponse execute(HttpRequest request) {
        return wrapped(() -> doExecute(request));
    }

    private SuccessfulHttpResponse doExecute(HttpRequest request) {
        try {
            HttpURLConnection connection = connectionBuilder.call(request);
            if (isSuccessResponse(connection)) {
                return handleSuccess(connection);
            } else {
                return handleError(connection, request);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void execute(HttpRequest request, ServerSentEventParser parser, ServerSentEventListener listener) {
        wrapped(() -> doExecute(request, parser, listener));
    }

    private Object doExecute(HttpRequest request, ServerSentEventParser parser, ServerSentEventListener listener) {
        try {
            HttpURLConnection connection = connectionBuilder.call(request);
            if (isSuccessResponse(connection)) {
                handleSuccess(connection, listener, parser);
            } else {
                handleError(connection, request);
            }
        } catch (Exception e) {
            handleException(listener, e);
        }
        return null;
    }


    private static boolean isSuccessResponse(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        return responseCode >= 200 && responseCode < 300;
    }

    private static SuccessfulHttpResponse handleSuccess(HttpURLConnection connection) throws IOException {
        int responseCode = connection.getResponseCode();
        return SuccessfulHttpResponse
                .builder()
                .statusCode(responseCode)
                .build();
    }

    private static void handleSuccess(HttpURLConnection connection, ServerSentEventListener listener, ServerSentEventParser parser) throws IOException {
        int responseCode = connection.getResponseCode();
        SuccessfulHttpResponse response = SuccessfulHttpResponse.builder().statusCode(responseCode).build();
        listener.onOpen(response);

        InputStream inputStream = connection.getInputStream();
        parser.parse(inputStream, listener);

        listener.onClose();
    }

    private static <T> T handleError(HttpURLConnection connection, HttpRequest request) throws IOException {
        int responseCode = connection.getResponseCode();
        String responseMessage = connection.getResponseMessage();
        String message = String.format("HTTP %d: %s (Request URL: %s)", responseCode, responseMessage, request.url());

        InputStream errorStream = connection.getErrorStream();
        String error = Unsafe.logged("", () -> readInputStream(errorStream));

        message += "\n" + error;
        throw new IOException(message);
    }

    private static void handleException(ServerSentEventListener listener, Exception e) {
        listener.onError(e);
    }

    @Workaround
    private static <T> T wrapped(ThrowableCallable<T, RuntimeException> callable) {
        // the internal jackson initialization favors ide class loader,
        // causing it to initialize on old jackson libraries provided by intellij
        // (these are incompatible with the current version of langchain4j)
        return Classes.withClassLoader(AssistantHttpClientBuilder.class, callable);
    }
}
