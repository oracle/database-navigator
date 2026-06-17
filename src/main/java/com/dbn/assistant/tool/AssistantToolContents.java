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

package com.dbn.assistant.tool;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

@NonNls
@UtilityClass
public class AssistantToolContents {
    private static final int MAX_CONTENT_LENGTH = 8192;
    private static final String CONTENT_TRUNCATED_SUFFIX = "\n[Content truncated]";
    private static final String UNTRUSTED_CONTENT_BEGIN = "[BEGIN UNTRUSTED DATABASE CONTENT]";
    private static final String UNTRUSTED_CONTENT_END = "[END UNTRUSTED DATABASE CONTENT]";
    private static final String EMBEDDED_CONTENT_BEGIN = "[EMBEDDED BEGIN UNTRUSTED DATABASE CONTENT]";
    private static final String EMBEDDED_CONTENT_END = "[EMBEDDED END UNTRUSTED DATABASE CONTENT]";
    private static final Pattern UNTRUSTED_CONTENT_BEGIN_PATTERN = Pattern.compile("\\[\\s*BEGIN\\s+UNTRUSTED\\s+DATABASE\\s+CONTENT\\s*\\]", CASE_INSENSITIVE);
    private static final Pattern UNTRUSTED_CONTENT_END_PATTERN = Pattern.compile("\\[\\s*END\\s+UNTRUSTED\\s+DATABASE\\s+CONTENT\\s*\\]", CASE_INSENSITIVE);

    public static String prepareUntrustedDatabaseContent(String content) {
        content = normalizeContent(content);
        content = escapeContentDelimiters(content);
        content = wrapUntrustedContent(content);

        return content;
    }

    private static String normalizeContent(String content) {
        if (content == null) return "";

        String normalized = stripUnsupportedControlCharacters(content);
        if (normalized.length() <= MAX_CONTENT_LENGTH) return normalized;

        int length = MAX_CONTENT_LENGTH - CONTENT_TRUNCATED_SUFFIX.length();
        return normalized.substring(0, length) + CONTENT_TRUNCATED_SUFFIX;
    }

    private static String stripUnsupportedControlCharacters(String content) {
        StringBuilder buffer = null;
        for (int i = 0; i < content.length(); i++) {
            char character = content.charAt(i);
            if (isUnsupportedControlCharacter(character)) {
                if (buffer == null) buffer = new StringBuilder(content.length()).append(content, 0, i);
                continue;
            }

            if (buffer != null) buffer.append(character);
        }

        return buffer == null ? content : buffer.toString();
    }

    private static boolean isUnsupportedControlCharacter(char character) {
        return Character.isISOControl(character) && character != '\n' && character != '\r' && character != '\t';
    }

    private static String escapeContentDelimiters(String content) {
        // Prevent retrieved content from spoofing the trusted wrapper boundaries, including case variants.
        content = UNTRUSTED_CONTENT_BEGIN_PATTERN.matcher(content).replaceAll(Matcher.quoteReplacement(EMBEDDED_CONTENT_BEGIN));
        content = UNTRUSTED_CONTENT_END_PATTERN.matcher(content).replaceAll(Matcher.quoteReplacement(EMBEDDED_CONTENT_END));
        return content;
    }

    private static String wrapUntrustedContent(String content) {
        return UNTRUSTED_CONTENT_BEGIN + "\n" +
                content + "\n" +
                UNTRUSTED_CONTENT_END;
    }
}
