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

package com.dbn.execution.common.input;

import lombok.Getter;
import lombok.Setter;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Getter
@Setter
public class CodeBlock {
    private final static String CODE_BLOCK_PREFIX = "DBN_CODE:";

    @Getter
    public enum Language {
        JAVA("java"),
        SQL("sql");

        private final String value;

        Language(String value) {
            this.value = value;
        }

        // Helper to get enum from string
        public static Language fromString(String language) {
            for (Language lang : Language.values()) {
                if (lang.value.equalsIgnoreCase(language)) {
                    return lang;
                }
            }

            return null;
        }
    }

    private String content;
    private Language language;

    public CodeBlock(String content, Language language) {
        this.content = content;
        this.language = language;
    }

    @Override
    public String toString() {
        return content;
    }

    public String serialize() {
        return CODE_BLOCK_PREFIX + language.getValue() + "[" + content + "]";
    }

    public static boolean isCodeBlock(String serialized) {
        return serialized != null && serialized.startsWith(CODE_BLOCK_PREFIX);
    }

    public static CodeBlock deserialize(String serialized) {
        if (!isCodeBlock(serialized)) return null;
        try {
            int langStart = CODE_BLOCK_PREFIX.length();
            int bracketIndex = serialized.indexOf('[', langStart);
            int lastBracket = serialized.lastIndexOf(']');
            if (bracketIndex < 0 || lastBracket < 0 || lastBracket <= bracketIndex) return null;

            String languageString = serialized.substring(langStart, bracketIndex);
            Language language = Language.fromString(languageString);
            if(language == null) return null;
            String content = serialized.substring(bracketIndex + 1, lastBracket);

            return new CodeBlock(content, Language.fromString(languageString));
        } catch (Exception e) {
            conditionallyLog(e);
            return null;
        }
    }
}