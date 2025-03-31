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
import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Json {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
    public static String createJsonPreview(String json, int maxAttributes) {
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
                if (count > 0) builder.append(",");
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
}
