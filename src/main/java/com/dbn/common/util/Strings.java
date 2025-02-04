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

import com.intellij.openapi.util.text.StringUtil;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.lang.Character.isWhitespace;

@NonNls
@UtilityClass
public class Strings/* extends com.intellij.openapi.util.text.StringUtil*/ {

    private static final Map<String, String> UPPER_CASE_STRINGS = new ConcurrentHashMap<>();
    private static final Map<String, String> LOWER_CASE_STRINGS = new ConcurrentHashMap<>();

    @NotNull
    public static List<String> tokenize(@Nullable String string, @NotNull String separator) {
        if (isEmptyOrSpaces(string)) return Collections.emptyList();
        return Arrays
                .stream(string.split(separator))
                .map(t -> t.trim())
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public static String concatenate(Collection<String> tokens, String separator) {
        StringBuilder builder = new StringBuilder();
        int size = tokens.size();
        int index = 0;
        for (String token : tokens) {
            builder.append(token);
            if (index < size - 1) {
                builder.append(separator);
            }
            index++;
        }
        return builder.toString();
    }

    public static boolean containsOneOf(String string, String... tokens) {
        for (String token : tokens) {
            if (string.contains(token)) return true;
        }
        return false;
    }

    public static boolean isMixedCase(String string) {
        boolean upperCaseFound = false;
        boolean lowerCaseFound = false;
        for (int i=0; i<string.length(); i++) {
            char chr = string.charAt(i);

            if (!upperCaseFound && Character.isUpperCase(chr)) {
                upperCaseFound = true;
            } else if (!lowerCaseFound && Character.isLowerCase(chr)) {
                lowerCaseFound = true;
            }

            if (upperCaseFound && lowerCaseFound) return true;
        }

        return false;
    }

    public static boolean isEmpty(CharSequence string) {
        return StringUtil.isEmpty(string);
    }

    public static boolean isNotEmpty(String string) {
        return StringUtil.isNotEmpty(string);
    }

    public static boolean isNotEmptyOrSpaces(String string) {
        return !isEmptyOrSpaces(string);
    }

    public static boolean isEmptyOrSpaces(String string) {
        return StringUtil.isEmptyOrSpaces(string);
    }

    public static boolean isOneOf(String string, String ... values) {
        for (String value : values) {
            if (Objects.equals(value, string)) return true;
        }
        return false;
    }

    public static boolean isOneOfIgnoreCase(String string, String ... values) {
        for (String value : values) {
            if (equalsIgnoreCase(value, string)) return true;
        }
        return false;
    }

    public static boolean equals(@Nullable CharSequence s1, @Nullable CharSequence s2) {
        // assuming most of the strings in DBN are .intern() "==" may speed up the evaluation (??)
        return s1 == s2 || StringUtil.equals(s1, s2);
    }

    public static boolean equalsIgnoreCase(@Nullable CharSequence s1, @Nullable CharSequence s2) {
        // assuming most of the strings in DBN are .intern() "==" may speed up the evaluation (??)
        return s1 == s2 || StringUtil.equalsIgnoreCase(s1, s2);
    }

    public static String replace(String text, String oldS, String newS) {
        return StringUtil.replace(text, oldS, newS);
    }

    public static String repeatSymbol(char chr, int count) {
        return StringUtil.repeatSymbol(chr, count);
    }


    public static int parseInt(String string, int defaultValue) {
        return StringUtil.parseInt(string, defaultValue);
    }

    public static boolean isInteger(@Nullable String string) {
        try {
            if (isNotEmptyOrSpaces(string)) {
                Integer.parseInt(string);
                return true;
            }
        } catch (NumberFormatException ignore) {}

        return false;

    }

    public static boolean isIndex(@Nullable String string) {
        if (string == null) return false;
        for (int i = 0; i < string.length(); i++) {
            char chr = string.charAt(i);
            if (chr < '0' || chr > '9') return false;
        }
        return true;
    }

    public static boolean isNumber(@Nullable String string) {
        try {
            if (isNotEmptyOrSpaces(string)) {
                Double.parseDouble(string);
                return true;
            }
        } catch (NumberFormatException ignore) {}
        return false;
    }


    public static boolean isWord(String name) {
        boolean containsLetters = false;
        for (char c : name.toCharArray()) {
            boolean isLetter = Character.isLetter(c) || c == '_';
            containsLetters = containsLetters || isLetter;
            if (!isLetter && !Character.isDigit(c)) {
                return false;
            }
        }
        return containsLetters;
    }

    public static String removeCharacter(String content, char c) {
        int index = content.indexOf(c);
        if (index > -1) {
            int beginIndex = 0;
            int endIndex = index;
            StringBuilder buffer = new StringBuilder();
            while (endIndex > -1) {
                if (beginIndex < endIndex) buffer.append(content, beginIndex, endIndex);
                beginIndex = endIndex + 1;
                endIndex = content.indexOf(c, beginIndex);
            }
            if (beginIndex < content.length() - 1) {
                buffer.append(content.substring(beginIndex));
            }
            return buffer.toString();
        }
        return content;
    }

    public static @NotNull String trim(@Nullable String message) {
        return isEmptyOrSpaces(message) ? "" : message.trim();
    }

    public static String textWrap(String string, int maxRowLength, String wrapCharacters) {
        StringBuilder builder = new StringBuilder();
        if (string != null) {
            StringTokenizer tokenizer = new StringTokenizer(string, wrapCharacters, true);
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                int wrapIndex = builder.lastIndexOf("\n") + 1;
                if (wrapCharacters.contains(token)) {
                    builder.append(token);
                } else {
                    int tokenLength = token.length();
                    if (tokenLength >= maxRowLength) {
                        if (wrapIndex != builder.length()) {
                            builder.append("\n");
                        }
                        builder.append(token.trim());
                    } else {
                        if (builder.length() - wrapIndex + tokenLength > maxRowLength) {
                            builder.append("\n");
                        }
                        builder.append(token);
                    }
                }
            }
        }
        return builder.toString().trim();
    }

    public static int textMaxRowLength(String string) {
        int offset = 0;
        int maxLength = 0;
        while (true) {
            int index = string.indexOf('\n', offset);
            if (index == -1) {
                maxLength = Math.max(maxLength, string.length() - offset);
                break;
            } else {
                int length = index - offset;
                maxLength = Math.max(maxLength, length);
                offset = index + 1;
            }

        }
        return maxLength;
    }

    public static int indexOfIgnoreCase(@NotNull @NonNls CharSequence where, @NotNull @NonNls CharSequence what, int fromIndex) {
        int targetCount = what.length();
        int sourceCount = where.length();

        if (fromIndex >= sourceCount) {
            return targetCount == 0 ? sourceCount : -1;
        }

        if (fromIndex < 0) {
            fromIndex = 0;
        }

        if (targetCount == 0) {
            return fromIndex;
        }

        char first = what.charAt(0);
        int max = sourceCount - targetCount;

        for (int i = fromIndex; i <= max; i++) {
            /* Look for first character. */
            if (!charsEqualIgnoreCase(where.charAt(i), first)) {
                //noinspection StatementWithEmptyBody,AssignmentToForLoopParameter
                while (++i <= max && !charsEqualIgnoreCase(where.charAt(i), first)) ;
            }

            /* Found first character, now look at the rest of v2 */
            if (i <= max) {
                int j = i + 1;
                int end = j + targetCount - 1;
                //noinspection StatementWithEmptyBody
                for (int k = 1; j < end && charsEqualIgnoreCase(where.charAt(j), what.charAt(k)); j++, k++) ;

                if (j == end) {
                    /* Found whole string. */
                    return i;
                }
            }
        }

        return -1;
    }

    private static boolean charsEqualIgnoreCase(char c, char first) {
        return StringUtil.charsEqualIgnoreCase(c, first);
    }

    public static String toUpperCase(CharSequence string) {
        return toUpperCase(string.toString());
    }

    public static String toLowerCase(CharSequence string) {
        return toLowerCase(string.toString());
    }

    public static String toUpperCase(String string) {
        return string.toUpperCase(Locale.ROOT);
    }

    public static String toLowerCase(String string) {
        return string.toLowerCase(Locale.ROOT);
    }

    public static String intern(String value) {
        return value == null ? null : value.intern();
    }

    @NotNull
    public static List<String> nonEmptyStrings(List<String> values) {
        if (values == null) return Collections.emptyList();
        return values.stream().filter(s -> isNotEmptyOrSpaces(s)).collect(Collectors.toList());
    }


    public static char firstChar(CharSequence s, CharPredicate predicate) {
        int index = 0;
        while (index < s.length()) {
            char chr = s.charAt(index);
            if (predicate.test(chr)) return chr;

            index++;
        }
        return ' ';
    }

    public static char lastChar(CharSequence s, CharPredicate predicate) {
        int index = s.length() -1;

        while (index >= 0) {
            char chr = s.charAt(index);
            if (predicate.test(chr)) return chr;

            index--;
        }
        return ' ';
    }

    public static int firstIndexOf(CharSequence s, CharPredicate predicate) {
        int index = 0;

        while (index < s.length()) {
            char chr = s.charAt(index);
            if (predicate.test(chr)) return index;

            index++;
        }
        return -1;
    }

    public static int lastIndexOf(CharSequence s, CharPredicate predicate) {
        int index = s.length() -1;

        while (index >= 0) {
            char chr = s.charAt(index);
            if (predicate.test(chr)) return index;

            index--;
        }
        return -1;
    }

    public static void trim(StringBuilder builder) {
        if (builder.length() == 0) return;

        int first = firstIndexOf(builder, chr -> !isWhitespace(chr));
        if (first > -1) builder.delete(0, first);

        int last = lastIndexOf(builder, chr -> !isWhitespace(chr));
        builder.delete(last + 1, builder.length());
    }

    public static String cachedUpperCase(String string) {
        return UPPER_CASE_STRINGS.computeIfAbsent(string, k -> toUpperCase(k).intern());
    }

    public static String cachedLowerCase(String string) {
        return LOWER_CASE_STRINGS.computeIfAbsent(string, k -> toLowerCase(k).intern());
    }

    public static boolean containsIgnoreCase(String name, @NonNls String option) {
        return StringUtil.containsIgnoreCase(name, option);
    }

    public static boolean containsWhitespaces(CharSequence s) {
        return StringUtil.containsWhitespaces(s);
    }

    public static String replaceIgnoreCase(String text, String oldS, String newS) {
        return StringUtil.replaceIgnoreCase(text, oldS, newS);
    }

    public static boolean endsWithIgnoreCase(String str, String suffix) {
        return StringUtil.endsWithIgnoreCase(str, suffix);
    }

    public static boolean containsLineBreak(CharSequence text) {
        return StringUtil.containsLineBreak(text);
    }

    public static String capitalize(String s) {
        return StringUtil.capitalize(s);
    }

    public static int countNewLines(String text) {
        return StringUtil.countNewLines(text);
    }

    public static boolean startsWith(CharSequence text, CharSequence prefix) {
        return StringUtil.startsWith(text, prefix);
    }

    public static String repeat(String space, int playgroundSize) {
        return StringUtil.repeat(space, playgroundSize);
    }

    public interface CharPredicate {
        boolean test(char chr);
    }
}

