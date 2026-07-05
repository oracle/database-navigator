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
import java.util.LinkedHashMap;
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

    @Test(timeout = 1000)
    public void normalizeArguments8() {
        Map<Object, Object> arguments = new LinkedHashMap<>();
        arguments.put("invalid".repeat(8_000), "string value");
        arguments.put("arg1", Boolean.TRUE);
        arguments.put("arg2", 1);

        testNormalization(arguments, getTestMethod1());
    }

    @Test(timeout = 1000)
    public void normalizeArguments9() {
        Map<Object, Object> arguments = new LinkedHashMap<>();
        for (int i = 0; i < 32; i++) {
            arguments.put("invalid argument name " + i, "value " + i);
        }

        Map<String, ?> normalized = AssistantToolRequestNormalizer.normalizeArguments(arguments, getTestMethod4());
        Assert.assertEquals(16, normalized.size());
        Assert.assertTrue(normalized.keySet().stream().allMatch(k -> k.matches("^arg\\d+$")));
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
    private Method getTestMethod4() {
        return TestClass.class.getMethod(
                "testMethod4",
                String.class, String.class, String.class, String.class,
                String.class, String.class, String.class, String.class,
                String.class, String.class, String.class, String.class,
                String.class, String.class, String.class, String.class,
                String.class, String.class, String.class, String.class);
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

        public void testMethod4(
                @P("String value 0") String value0,
                @P("String value 1") String value1,
                @P("String value 2") String value2,
                @P("String value 3") String value3,
                @P("String value 4") String value4,
                @P("String value 5") String value5,
                @P("String value 6") String value6,
                @P("String value 7") String value7,
                @P("String value 8") String value8,
                @P("String value 9") String value9,
                @P("String value 10") String value10,
                @P("String value 11") String value11,
                @P("String value 12") String value12,
                @P("String value 13") String value13,
                @P("String value 14") String value14,
                @P("String value 15") String value15,
                @P("String value 16") String value16,
                @P("String value 17") String value17,
                @P("String value 18") String value18,
                @P("String value 19") String value19
        ) {}
    }
}
