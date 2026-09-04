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

package com.dbn.assistant.tool.execution;

import dev.langchain4j.agent.tool.P;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static com.dbn.assistant.tool.execution.AssistantToolRequestLimits.MAX_TOOL_REQUEST_ARGUMENT_LENGTH;
import static com.dbn.assistant.tool.execution.AssistantToolRequestLimits.MAX_TOOL_REQUEST_PREVIEW_LENGTH;
import static com.dbn.assistant.tool.execution.AssistantToolRequestVerifier.verifyRequest;

public class AssistantToolRequestVerifierTest {
    @Test
    public void verifyParameterNamesAcceptsAllExpectedArguments() {
        AssistantToolRequest request = request("{\"arg1\":true,\"arg0\":\"schema\",\"arg2\":\"optional\"}");

        verifyParameterNames(request);
    }

    @Test
    public void verifyParameterNamesAcceptsMissingOptionalArguments() {
        AssistantToolRequest request = request("{\"arg0\":\"schema\",\"arg1\":true}");

        verifyParameterNames(request);
    }

    @Test
    public void verifyParameterNamesRejectsUnknownArguments() {
        AssistantToolRequest request = request("{\"arg0\":\"schema\",\"arg1\":true,\"arg99\":\"unknown\"}");

        IllegalArgumentException exception = Assert.assertThrows(
                IllegalArgumentException.class,
                () -> verifyParameterNames(request));

        Assert.assertTrue(exception.getMessage().contains("unknown arguments: \"arg99\""));
    }

    @Test
    public void verifyParameterNamesRejectsMissingRequiredArguments() {
        AssistantToolRequest request = request("{\"arg1\":true}");

        IllegalArgumentException exception = Assert.assertThrows(
                IllegalArgumentException.class,
                () -> verifyParameterNames(request));

        Assert.assertTrue(exception.getMessage().contains("missing arguments: \"arg0\""));
    }

    @Test
    public void verifyParameterTypesAcceptsMatchingArguments() {
        AssistantToolRequest request = request(typeMethod());

        verifyParameterTypes(request, new Object[]{"text", 42, Boolean.TRUE, List.of("one"), null});
    }

    @Test
    public void verifyParameterTypesRejectsWrongArgumentCount() {
        AssistantToolRequest request = request(typeMethod());

        IllegalArgumentException exception = Assert.assertThrows(
                IllegalArgumentException.class,
                () -> verifyParameterTypes(request, new Object[]{"text", 42, Boolean.TRUE, List.of("one")}));

        Assert.assertTrue(exception.getMessage().contains("expected 5, received 4"));
    }

    @Test
    public void verifyParameterTypesRejectsMissingRequiredValues() {
        AssistantToolRequest request = request(typeMethod());

        IllegalArgumentException exception = Assert.assertThrows(
                IllegalArgumentException.class,
                () -> verifyParameterTypes(request, new Object[]{"text", null, Boolean.TRUE, List.of("one"), null}));

        Assert.assertTrue(exception.getMessage().contains("missing argument values: \"arg1\""));
    }

    @Test
    public void verifyParameterTypesRejectsMismatchedArgumentTypes() {
        AssistantToolRequest request = request(typeMethod());

        IllegalArgumentException exception = Assert.assertThrows(
                IllegalArgumentException.class,
                () -> verifyParameterTypes(request, new Object[]{"text", 42, Boolean.TRUE, "one", null}));

        Assert.assertTrue(exception.getMessage().contains("expected argument type at index 3"));
        Assert.assertTrue(exception.getMessage().contains("interface java.util.Collection"));
        Assert.assertTrue(exception.getMessage().contains("interface java.lang.CharSequence"));
    }

    @Test
    public void verifyRequestRejectsUnknownArguments() {
        AssistantToolRequest request = request("{\"arg0\":\"schema\",\"arg1\":true,\"arg99\":\"unknown\"}");

        IllegalArgumentException exception = Assert.assertThrows(
                IllegalArgumentException.class,
                () -> verifyRequest(request, nameMethod(), "schema", true, null));

        Assert.assertTrue(exception.getMessage().contains("unknown arguments: \"arg99\""));
    }

    @Test
    public void verifyRequestRejectsMismatchedArgumentTypes() {
        AssistantToolRequest request = request(typeMethod(), "{\"arg0\":\"text\",\"arg1\":42,\"arg2\":true,\"arg3\":\"one\"}");

        IllegalArgumentException exception = Assert.assertThrows(
                IllegalArgumentException.class,
                () -> verifyRequest(request, typeMethod(), "text", 42, true, "one", null));

        Assert.assertTrue(exception.getMessage().contains("expected argument type at index 3"));
    }

    @Test
    public void oversizedToolArgumentsAreCappedAndRejected() {
        AssistantToolRequest request = request(nameMethod(), "x".repeat(MAX_TOOL_REQUEST_ARGUMENT_LENGTH + 1));

        Assert.assertTrue(request.isToolArgumentsTruncated());
        Assert.assertEquals(MAX_TOOL_REQUEST_ARGUMENT_LENGTH + 1, request.getToolArgumentsLength());
        Assert.assertTrue(request.getToolArguments().length() <= MAX_TOOL_REQUEST_PREVIEW_LENGTH);
        Assert.assertTrue(request.getToolArguments().contains("Tool request arguments truncated"));

        IllegalStateException exception = Assert.assertThrows(
                IllegalStateException.class,
                request::assertExecutable);

        Assert.assertTrue(exception.getMessage().contains("exceeded the maximum allowed size"));
    }

    @Test
    public void boundedToolArgumentsRemainExecutable() {
        AssistantToolRequest request = request(nameMethod(), "{\"arg0\":\"schema\",\"arg1\":true}");

        Assert.assertFalse(request.isToolArgumentsTruncated());
        Assert.assertEquals("{\"arg0\":\"schema\",\"arg1\":true}", request.getToolArguments());

        request.assertExecutable();
    }

    @SneakyThrows
    private static AssistantToolRequest request(String toolArguments) {
        return request(nameMethod(), toolArguments);
    }

    @SneakyThrows
    private static AssistantToolRequest request(Method method) {
        return request(method, "{}");
    }

    private static AssistantToolRequest request(Method method, String toolArguments) {
        AssistantToolRequest request = new AssistantToolRequest();
        request.setMethod(method);
        request.setToolArguments(toolArguments);
        return request;
    }

    @SneakyThrows
    private static void verifyParameterNames(AssistantToolRequest request) {
        Method verifier = AssistantToolRequestVerifier.class.getDeclaredMethod("verifyParameterNames", AssistantToolRequest.class);
        verifier.setAccessible(true);

        try {
            verifier.invoke(null, request);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw e;
        }
    }

    @SneakyThrows
    private static void verifyParameterTypes(AssistantToolRequest request, Object[] args) {
        Method verifier = AssistantToolRequestVerifier.class.getDeclaredMethod("verifyParameterTypes", AssistantToolRequest.class, Object[].class);
        verifier.setAccessible(true);

        try {
            verifier.invoke(null, new Object[]{request, args});
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw e;
        }
    }

    @SneakyThrows
    private static Method nameMethod() {
        return TestTool.class.getMethod("method", String.class, boolean.class, String.class);
    }

    @SneakyThrows
    private static Method typeMethod() {
        return TestTool.class.getMethod("typedMethod", CharSequence.class, int.class, boolean.class, List.class, String.class);
    }

    private static class TestTool {
        public void method(
                @P("Required string argument") String arg0,
                @P("Required boolean argument") boolean arg1,
                @P(value = "Optional string argument", required = false) String arg2) {}

        public void typedMethod(
                @P("Required text argument") CharSequence arg0,
                @P("Required number argument") int arg1,
                @P("Required boolean argument") boolean arg2,
                @P("Required list argument") List<String> arg3,
                @P(value = "Optional string argument", required = false) String arg4) {}
    }
}
