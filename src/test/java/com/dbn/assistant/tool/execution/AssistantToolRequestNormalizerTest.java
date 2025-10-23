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

package com.dbn.assistant.tool.execution;

import dev.langchain4j.agent.tool.P;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class AssistantToolRequestNormalizerTest {

    public static final String EXPECTED_ARGS = "{\"arg0\":\"Select a function to open in the editor\",\"arg1\":\"Which function would you like to open in the editor?\",\"arg2\":[\"GET_PROPERTY\",\"OJVM_FUNCTION_COM_DBN_SHAPES_GET_INTERSECTION_AREA\"]}";
    public static final String QUOTED_ARGS =    "\"{\\\"arg0\\\": \\\"Select a function to open in the editor\\\", \\\"arg1\\\": \\\"Which function would you like to open in the editor?\\\", \\\"arg2\\\": \\\"[ \\\\\\\"GET_PROPERTY\\\\\\\", \\\\\\\"OJVM_FUNCTION_COM_DBN_SHAPES_GET_INTERSECTION_AREA\\\\\\\" ]\\\"}\"";
    public static final String MALFORMED_ARGS = "{\"arg0\": \"Select a function to open in the editor\", \"arg1\": \"Which function would you like to open in the editor?\", \"arg2\": \"[ \\\"GET_PROPERTY\\\", \\\"OJVM_FUNCTION_COM_DBN_SHAPES_GET_INTERSECTION_AREA\\\" ]\"}";

    public static final Map<Object, Object> NORMALIZED = Map.of(
            "arg0", "string value",
            "arg1", Boolean.TRUE,
            "arg2", 1);

    @Test
    public void normalizeArguments1() {
        Map<Object, Object> arguments = Map.of(
                "arg1", Boolean.TRUE,
                "arg0", "string value",
                "arg2", 1);

        testNormalization(arguments, getTestMethod1());
    }

    @Test
    public void normalizeArguments2() {
        Map<Object, Object> arguments = Map.of(
                "arg1", "true",
                "value", "string value",
                "arg2", "1");

        testNormalization(arguments, getTestMethod1());
    }

    @Test
    public void normalizeArguments3() {
        Map<Object, Object> arguments = Map.of(
                "a", "string value",
                "b", "true",
                "c", "1");

        testNormalization(arguments, getTestMethod1());
    }
    @Test
    public void normalizeArguments4() {
        String normalized = AssistantToolRequestNormalizer.normalize(MALFORMED_ARGS, getTestMethod2());
        Assert.assertEquals(EXPECTED_ARGS, normalized);
    }

    @Test
    public void normalizeArguments5() {
        String normalized = AssistantToolRequestNormalizer.normalize(MALFORMED_ARGS, getTestMethod3());
        Assert.assertEquals(EXPECTED_ARGS, normalized);
    }

    @Test
    public void normalizeArguments6() {
        // quoted args block
        String normalized = AssistantToolRequestNormalizer.normalize(QUOTED_ARGS, getTestMethod2());
        Assert.assertEquals(EXPECTED_ARGS, normalized);
    }

    @Test
    public void normalizeArguments7() {
        // quoted args block
        String normalized = AssistantToolRequestNormalizer.normalize(QUOTED_ARGS, getTestMethod3());
        Assert.assertEquals(EXPECTED_ARGS, normalized);
    }

    @SneakyThrows
    private Method getTestMethod1() {
        return TestClass.class.getMethod("testMethod1", String.class, boolean.class, int.class);
    }

    @SneakyThrows
    private Method getTestMethod2() {
        return TestClass.class.getMethod("testMethod2", String.class, String.class, String[].class);
    }

    @SneakyThrows
    private Method getTestMethod3() {
        return TestClass.class.getMethod("testMethod3", String.class, String.class, List.class);
    }

    @SneakyThrows
    private static void testNormalization(Map<?, ?> arguments, Method method) {
        Map<String, ?> normalized = AssistantToolRequestNormalizer.normalizeArguments(arguments, method);
        Assert.assertEquals(NORMALIZED, normalized);
    }

    private static class TestClass {
        public void testMethod1(
                @P("String value") String arg0,
                @P("Boolean value") boolean arg1,
                @P("Integer value") int arg2) {}

        public void testMethod2(
                @P("Confirmation title") String title,
                @P("Confirmation message") String message,
                @P("Confirmation options") String[] options
        ) {}

        public void testMethod3(
                @P("Confirmation title") String title,
                @P("Confirmation message") String message,
                @P("Confirmation options") List<String> options
        ) {}
    }
}