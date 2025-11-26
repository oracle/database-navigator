/*
 * Copyright 2024 Oracle and/or its affiliates
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

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.context.DatabaseContext;
import com.dbn.connection.session.DatabaseSession;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dbn.common.util.Unsafe.silent;

@UtilityClass
public final class Titles {
    private static final Set<String> ACRONYMS = Set.of("AI", "DB", "API", "HTTP", "IT", "CPU", "RAM", "GPU", "OS", "SQL", "JSON", "XML", "URL", "HTML", "CSS", "SDK", "IDE", "VPN");

    @NonNls
    public static final String PRODUCT_NAME = "DB Navigator";
    public static final String TITLE_PREFIX = PRODUCT_NAME + " - ";

    public static String signed(String title) {
        if (title.startsWith(TITLE_PREFIX)) return title;
        return TITLE_PREFIX + title;
    }

    public static String suffixed(String title, @Nullable DatabaseContext databaseContext) {
        if (databaseContext == null) return title;

        ConnectionHandler connection = silent(null, () -> databaseContext.getConnection());
        if (connection == null) return title;

        title = title + " - " + connection.getName();

        DatabaseSession session = databaseContext.getSession();
        if (session == null) return title;

        return title + " (" + session + ")";
    }

    public static String prefixed(String title, @Nullable DatabaseContext databaseContext) {
        if (databaseContext == null) return title;

        ConnectionHandler connection = databaseContext.getConnection();
        if (connection == null) return title;

        return connection.getName()  + " - " + title;
    }

    public static String titleCased(String string) {
        if (Strings.isEmpty(string)) return string;

        Pattern delimiterPattern = Pattern.compile("([^a-zA-Z]+)");
        Matcher matcher = delimiterPattern.matcher(string);
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            if (lastEnd < matcher.start()) {
                String word = string.substring(lastEnd, matcher.start());
                result.append(titleCasedWord(word));
            }
            result.append(matcher.group());
            lastEnd = matcher.end();
        }

        if (lastEnd < string.length()) {
            String word = string.substring(lastEnd);
            result.append(titleCasedWord(word));
        }

        return result.toString();
    }

    private static String titleCasedWord(String word) {
        if (word == null || word.isEmpty()) {
            return word;
        }
        if (ACRONYMS.contains(word.toUpperCase())) {
            return word.toUpperCase(); // Keep acronym as uppercase
        }

        StringBuilder titleCased = new StringBuilder(word.toLowerCase());
        if (titleCased.length() > 0) {
            titleCased.setCharAt(0, Character.toUpperCase(titleCased.charAt(0)));
        }
        return titleCased.toString();
    }
}
