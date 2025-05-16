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

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ParametersTest {

    /**
     * Test class for the {@link Parameters#toParameterString(Map)} method.
     * The tested method converts a map of parameters into a URL query string.
     */

    @Test
    public void testToParameterString_WithEmptyMap_ReturnsEmptyString() {
        // Arrange
        Map<String, String> parameters = new HashMap<>();

        // Act
        String result = Parameters.toParameterString(parameters);

        // Assert
        assertEquals("", result);
    }

    @Test
    public void testToParameterString_WithSingleValidEntry_ReturnsCorrectString() {
        // Arrange
        Map<String, String> parameters = new HashMap<>();
        parameters.put("key1", "value1");

        // Act
        String result = Parameters.toParameterString(parameters);

        // Assert
        assertEquals("?key1=value1", result);
    }

    @Test
    public void testToParameterString_WithMultipleValidEntries_ReturnsConcatenatedString() {
        // Arrange
        Map<String, String> parameters = new HashMap<>();
        parameters.put("key1", "value1");
        parameters.put("key2", "value2");
        parameters.put("key3", "value3");

        // Act
        String result = Parameters.toParameterString(parameters);

        // Assert
        assertEquals("?key1=value1&key2=value2&key3=value3", result);
    }

    @Test
    public void testToParameterString_WithEmptyKey_ExcludesEntry() {
        // Arrange
        Map<String, String> parameters = new HashMap<>();
        parameters.put("", "value1");
        parameters.put("key2", "value2");

        // Act
        String result = Parameters.toParameterString(parameters);

        // Assert
        assertEquals("?key2=value2", result);
    }

    @Test
    public void testToParameterString_WithEmptyValue_ExcludesEntry() {
        // Arrange
        Map<String, String> parameters = new HashMap<>();
        parameters.put("key1", "");
        parameters.put("key2", "value2");

        // Act
        String result = Parameters.toParameterString(parameters);

        // Assert
        assertEquals("?key2=value2", result);
    }

    @Test
    public void testToParameterString_WithNullValue_ExcludesEntry() {
        // Arrange
        Map<String, String> parameters = new HashMap<>();
        parameters.put("key1", null);
        parameters.put("key2", "value2");

        // Act
        String result = Parameters.toParameterString(parameters);

        // Assert
        assertEquals("?key2=value2", result);
    }

    @Test
    public void testToParameterString_WithNullKey_ExcludesEntry() {
        // Arrange
        Map<String, String> parameters = new HashMap<>();
        parameters.put(null, "value1");
        parameters.put("key2", "value2");

        // Act
        String result = Parameters.toParameterString(parameters);

        // Assert
        assertEquals("?key2=value2", result);
    }

    @Test
    public void testToParameterString_WithAllEmptyData_ReturnsEmptyString() {
        // Arrange
        Map<String, String> parameters = new HashMap<>();
        parameters.put("", "");
        parameters.put(null, null);

        // Act
        String result = Parameters.toParameterString(parameters);

        // Assert
        assertEquals("", result);
    }

    @Test
    public void testToParameterString_WithMixedValidAndInvalidEntries_ReturnsValidString() {
        // Arrange
        Map<String, String> parameters = new HashMap<>();
        parameters.put("key1", "value1");
        parameters.put("", "value2");
        parameters.put(null, "value3");
        parameters.put("key4", "");

        // Act
        String result = Parameters.toParameterString(parameters);

        // Assert
        assertEquals("?key1=value1", result);
    }
}