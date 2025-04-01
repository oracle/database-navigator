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

import static com.dbn.common.util.Strings.isEmpty;

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

    public static String formatJsonContent(String json) {
        return Unsafe.logged(json, () -> doFormatJsonContent(json));
    }

    public static String createJsonPreview(String json, int maxAttributes) {
        return Unsafe.logged(json, () -> doCreateJsonPreview(json, maxAttributes));
    }

    public static String removeJsonAttributes(String json, @NonNls String... attributes) {
        return Unsafe.logged(json, () -> doRemoveJsonAttributes(json, attributes));
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
    private static String doRemoveJsonAttributes(String json, @NonNls String... attributes) {
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
}
