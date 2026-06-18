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

package com.dbn.common.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dbn.common.util.Strings.isEmpty;

/**
 * Redaction helpers for exporting JDBC URLs.
 * <p>
 * Pragmatic scope:
 * <ul>
 *     <li>URI user-info credentials ({@code //user:pass@host})</li>
 *     <li>Oracle thin inline credentials ({@code jdbc:oracle:thin:user/pass@...})</li>
 *     <li>Flat key/value parameters ({@code ?k=v&k=v} and {@code ;k=v;k=v})</li>
 *     <li>SQL Server braced values ({@code ;password={secret;value}})</li>
 *     <li>MySQL packed address entries ({@code (host=...,user=...,password=...)})</li>
 * </ul>
 * Optional lightweight support:
 * <ul>
 *     <li>Leaf parenthesized assignments ({@code (KEY=value)})</li>
 * </ul>
 */
@UtilityClass
public class JdbcUrls {
    private static final String REDACTED_PREFIX = "redacted_";
    private static final String REDACTED_USER = REDACTED_PREFIX + "user";
    private static final String REDACTED_PASSWORD = REDACTED_PREFIX + "password";
    private static final String DEFAULT_REDACTED_KEY = "value";
    private static final String ORACLE_THIN_PREFIX = "jdbc:oracle:thin:";

    /*
     * Covers flat key/value syntaxes accepted by common JDBC URLs:
     *   ?user=...&password=...          query parameters
     *   ;user=...;password=...          SQL Server / jTDS properties
     *   (host=...,user=...,password=...) MySQL packed address blocks
     *
     * Braced values keep SQL Server values containing semicolons together:
     *   ;password={myPass;word}
     */
    private static final Pattern DELIMITED_PARAMETER =
            Pattern.compile("([?&;,(])([^?&;,=#()\\s]+)=(\\{[^}]*}|[^&;,#)]*)");

    // Lightweight leaf block support only: (KEY=value)
    private static final Pattern LEAF_BLOCK_PARAMETER =
            Pattern.compile("(\\(\\s*)([A-Za-z0-9_.-]+)(\\s*=\\s*)([^()]*?)(\\s*\\))");

    private static final Set<String> SENSITIVE_KEY_NAMES = Set.of(
            "user",
            "username",
            "userid",
            "uid",
            "password",
            "password1",
            "password2",
            "password3",
            "pass",
            "passwd",
            "pwd",
            "secret",
            "token",
            "apikey",
            "accesstoken",
            "refreshtoken",
            "clientsecret",
            "privatekey",
            "passphrase",
            "credential",
            "credentials",
            "secretkey",
            "sessionkey",
            "signingkey");

    private static final Set<String> SENSITIVE_KEY_SUFFIXES = Set.of(
            "password",
            "secret",
            "token",
            "apikey",
            "passphrase",
            "credential",
            "credentials");

    @Nullable
    public static String redactSensitiveParameters(@Nullable String url) {
        if (isEmpty(url)) return url;

        String result = redactOracleThinCredentials(url);
        result = redactUriUserInfo(result);
        result = redactDelimitedParameters(result);
        result = redactLeafAssignments(result);
        return result;
    }

    private static String redactOracleThinCredentials(@NotNull String url) {
        if (!startsWithIgnoreCase(url, ORACLE_THIN_PREFIX)) return url;

        int start = ORACLE_THIN_PREFIX.length();
        int atIndex = url.indexOf('@', start);
        int slashIndex = url.indexOf('/', start);

        // Only redact the documented user/password@ segment, not ordinary connect targets.
        if (atIndex < 0 || slashIndex < 0 || slashIndex > atIndex) return url;

        return url.substring(0, start) +
                REDACTED_USER + "/" + REDACTED_PASSWORD +
                url.substring(atIndex);
    }

    private static String redactUriUserInfo(@NotNull String url) {
        int authorityStart = url.indexOf("://");
        if (authorityStart < 0) return url;

        authorityStart += 3;
        int authorityEnd = findAuthorityEnd(url, authorityStart);
        int atIndex = url.lastIndexOf('@', authorityEnd - 1);
        if (atIndex < authorityStart) return url;

        String userInfo = url.substring(authorityStart, atIndex);
        if (looksLikeParameterBlock(userInfo)) return url;

        int colonIndex = url.indexOf(':', authorityStart);
        String redactedUserInfo = colonIndex > -1 && colonIndex < atIndex ?
                REDACTED_USER + ":" + REDACTED_PASSWORD :
                REDACTED_USER;

        return url.substring(0, authorityStart) + redactedUserInfo + url.substring(atIndex);
    }

    private static String redactDelimitedParameters(@NotNull String url) {
        Matcher matcher = DELIMITED_PARAMETER.matcher(url);
        StringBuilder result = new StringBuilder(url.length());

        while (matcher.find()) {
            String key = matcher.group(2);
            String value = isSensitiveKey(key) ? redactedValue(key) : matcher.group(3);
            String replacement = matcher.group(1) + key + "=" + value;

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String redactLeafAssignments(@NotNull String url) {
        Matcher matcher = LEAF_BLOCK_PARAMETER.matcher(url);
        StringBuffer result = new StringBuffer(url.length());

        while (matcher.find()) {
            String key = matcher.group(2);
            String replacement = isSensitiveKey(key) ?
                    matcher.group(1) + key + matcher.group(3) + redactedValue(key) + matcher.group(5) :
                    matcher.group(0);

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String redactedValue(@NotNull String rawKey) {
        return REDACTED_PREFIX + normalizeKey(rawKey);
    }

    private static boolean isSensitiveKey(@NotNull String rawKey) {
        String key = compactKey(rawKey);
        if (isEmpty(key)) return false;
        if (SENSITIVE_KEY_NAMES.contains(key)) return true;
        for (String suffix : SENSITIVE_KEY_SUFFIXES) {
            if (key.endsWith(suffix)) return true;
        }
        return false;
    }

    private static String compactKey(@NotNull String key) {
        return key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }

    private static String normalizeKey(@NotNull String key) {
        String normalized = key.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return normalized.isEmpty() ? DEFAULT_REDACTED_KEY : normalized;
    }

    // Finds where the authority section ends by locating the earliest path/query/property delimiter.
    private static int findAuthorityEnd(@NotNull String url, int authorityStart) {
        int authorityEnd = url.length();
        for (int i = authorityStart; i < url.length(); i++) {
            char c = url.charAt(i);
            if (c == '/' || c == '?' || c == '#' || c == ';') {
                authorityEnd = i;
                break;
            }
        }
        return authorityEnd;
    }

    private static boolean looksLikeParameterBlock(@NotNull String userInfo) {
        return userInfo.indexOf('=') > -1 || userInfo.indexOf('(') > -1 || userInfo.indexOf(',') > -1;
    }

    private static boolean startsWithIgnoreCase(@NotNull String value, @NotNull String prefix) {
        return value.regionMatches(true, 0, prefix, 0, prefix.length());
    }
}
