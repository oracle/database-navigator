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

import com.dbn.common.exception.Exceptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.dbn.common.util.Strings.isEmpty;
import static java.util.Collections.emptyMap;

@Slf4j
@UtilityClass
public class Json {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public static FileType resolveJsonFileType() {
        return resolveJsonLanguage().getAssociatedFileType();
    }

    public static Language resolveJsonLanguage() {
        // optional dependency on json support
        return Commons.coalesce(
                () -> Language.findLanguageByID("JSON"),
                () -> Language.findLanguageByID("JavaScript"),
                () -> PlainTextLanguage.INSTANCE);
    }

    @SneakyThrows
    public static String writeAsString(Object value) {
        return OBJECT_MAPPER.writeValueAsString(value);
    }

    /**
     * Formats the given JSON content into a prettified or standardized format.
     * Ensures the formatting process is safely logged.
     *
     * @param json the JSON content to format. It should be a non-null string representing valid JSON.
     * @return a formatted and properly structured JSON string, or the original input if formatting fails.
     */
    public static String formatJsonContent(String json) {
        return Unsafe.logged(json, () -> doFormatJsonContent(json));
    }

    /**
     * Creates a preview of a JSON string containing up to the specified maximum number of attributes.
     * This method is intended to provide a truncated representation of the JSON object for limited display
     * or quick inspection purposes.
     *
     * @param json the JSON string to create a preview for
     * @param maxAttributes the maximum number of attributes to include in the preview; must be non-negative
     * @return a string representing the JSON preview, limited to the specified number of attributes
     */
    public static String createJsonPreview(String json, int maxAttributes) {
        return Unsafe.logged(json, () -> doCreateJsonPreview(json, maxAttributes));
    }

    /**
     * Removes specified attributes from a JSON string.
     * This method processes the provided JSON input and removes all occurrences of the specified attributes
     * from it.
     *
     * @param json the JSON string to process; must not be null
     * @param attributes the names of the attributes to be removed from the JSON string; must not be null or empty
     * @return a JSON string with the specified attributes removed
     */
    public static String removeJsonProperties(String json, @NonNls String... attributes) {
        return Unsafe.logged(json, () -> doRemoveJsonProperties(json, attributes));
    }

    /**
     * Compares the JSON contents of two strings to determine if they are equivalent.
     * The comparison considers the structural and value equality of the JSON content,
     * ignoring formatting differences such as whitespace.
     *
     * @param jsonContent1 the first JSON content string to compare; must not be null
     * @param jsonContent2 the second JSON content string to compare; must not be null
     * @return true if the JSON contents of both strings are equivalent, false otherwise
     */
    public static boolean checkJsonContentsEqual(String jsonContent1, String jsonContent2) {
        return Unsafe.logged(false, () -> doCheckJsonContentsEqual(jsonContent1, jsonContent2));
    }

    /**
     * Extracts the values of specific properties from a JSON string and returns them as a map.
     * If an exception occurs during the extraction, an empty map is returned.
     *
     * @param json the JSON string from which the property values are to be extracted;
     * @param attributeNames a collection of attribute names whose values are to be extracted; must not be null
     * @return a map where the keys are the attribute names and the values are the corresponding property values from the JSON;
     *         an empty map is returned if an exception occurs
     */
    public static Map<String, Object> getJsonPropertyValues(String json, Collection<String> attributeNames) {
        return Unsafe.logged(emptyMap(), () -> doGetJsonPropertyValues(json, attributeNames));
    }


    @SneakyThrows
    private static String doFormatJsonContent(String json) {
        if (isEmpty(json)) return "";

        Object jsonObject = OBJECT_MAPPER.readValue(json, Object.class);
        return OBJECT_MAPPER.writeValueAsString(jsonObject);
    }

    @SneakyThrows
    private static String doCreateJsonPreview(String json, int maxAttributes) {
        if (isEmpty(json)) return "";

        StringBuilder builder = new StringBuilder();
        builder.append("{");
        JsonNode rootNode = OBJECT_MAPPER.readTree(json);

        int count = 0;
        var fields = rootNode.fields();
        while (count < maxAttributes && fields.hasNext()) {
            var field = fields.next();

            String key = field.getKey();
            if (key.startsWith("_")) continue;

            JsonNode valueNode = field.getValue();
            if (valueNode.isValueNode()) {
                if (count > 0) builder.append(", ");
                count++;

                builder.append("\"");
                builder.append(key);
                builder.append("\": ");
                if (valueNode.isTextual()) builder.append("\"");
                builder.append(valueNode.asText());
                if (valueNode.isTextual()) builder.append("\"");
            }
        }
        builder.append("...}");
        return builder.toString();
    }

    @SneakyThrows
    private static String doRemoveJsonProperties(String json, @NonNls String... attributes) {
        if (isEmpty(json)) return "";

        JsonNode rootNode = OBJECT_MAPPER.readTree(json);
        if (rootNode instanceof ObjectNode) {
            ObjectNode objectNode = (ObjectNode) rootNode;
            for (String attribute : attributes) {
                objectNode.remove(attribute);
            }
            return objectNode.toString();
        }
        return json;
    }

    @SneakyThrows
    private static boolean doCheckJsonContentsEqual(String jsonContent1, String jsonContent2) {
        JsonNode jsonNode1 = OBJECT_MAPPER.readTree(jsonContent1);
        JsonNode jsonNode2 = OBJECT_MAPPER.readTree(jsonContent2);
        return jsonNode1.equals(jsonNode2);
    }

    @SneakyThrows
    private static Map<String, Object> doGetJsonPropertyValues(String json, Collection<String> attributeNames) {
        if (isEmpty(json)) return emptyMap();

        JsonNode rootNode = OBJECT_MAPPER.readTree(json);
        if (rootNode instanceof ObjectNode) {
            Map<String, Object> propertyValues = new HashMap<>();
            ObjectNode objectNode = (ObjectNode) rootNode;
            for (String propertyName : attributeNames) {
                Object propertyValue = getPropertyValue(objectNode, propertyName);
                propertyValues.put(propertyName, propertyValue);
            }

            return propertyValues;
        }

        return emptyMap();
    }


    private static Object getPropertyValue(ObjectNode objectNode, String propertyName) {
        JsonNode jsonNode = objectNode.get(propertyName);
        if (jsonNode.isNull()) return null;

        if (jsonNode.isValueNode()) {
            String valueText = jsonNode.asText();

            if (jsonNode.isTextual()) return valueText;
            if (jsonNode.isInt()) return jsonNode.asInt();
            if (jsonNode.isLong()) return jsonNode.asLong();
            if (jsonNode.isDouble()) return jsonNode.asDouble();
            if (jsonNode.isFloat()) return Float.valueOf(valueText);
            if (jsonNode.isBoolean()) return jsonNode.asBoolean();
            if (jsonNode.isBigInteger()) return new BigDecimal(valueText);
            if (jsonNode.isBigDecimal()) return new BigInteger(valueText);
        }
        return Exceptions.unsupported();
    }

}
