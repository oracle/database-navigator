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

package com.dbn.common.expression;

import org.jetbrains.annotations.NonNls;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Matcher.quoteReplacement;

@NonNls
public class SqlToGroovyExpressionConverter {
    private static final Map<String, String> cache = new ConcurrentHashMap<>();

    public static String cachedSqlToGroovy(String expression) {
        return cache.computeIfAbsent(expression, e -> sqlToGroovy(e));
    }

    public static String sqlToGroovy(String expression) {
        expression = expression.replaceAll("\\s", " ");
        expression = expression.replaceAll("(?i)\\bOR\\b", "||");
        expression = expression.replaceAll("(?i)\\bAND\\b", "&&");
        expression = expression.replaceAll("(?i)\\bIS\\s+NULL\\b", "== null");
        expression = expression.replaceAll("(?i)\\bIS\\s+NOT\\s+NULL\\b", "!= null");

        expression = replace_EQUALS(expression);
        expression = replace_NOT_LIKE(expression);
        expression = replace_LIKE(expression);
        expression = replace_NOT_IN(expression);
        expression = replace_IN(expression);
        expression = expression.replaceAll("(?i)\\bNOT\\b", "!");

        return expression.replaceAll("\\s+", " ").trim();
    }

    private static String replace_EQUALS(String expression) {
        Pattern pattern = Pattern.compile("(?<!([=<>!]))=(?!=)");
        Matcher matcher = pattern.matcher(expression);
        expression = matcher.replaceAll("==");
        return expression;
    }

    private static String replace_NOT_LIKE(String expression) {
        Pattern p = Pattern.compile("(?i)(\\w+)\\s+NOT\\s+LIKE\\s+('[^']*')");
        Matcher m = p.matcher(expression);
        StringBuilder result = new StringBuilder();
        while (m.find()) {
            String name = m.group(1);
            String value = toRegexPattern(m.group(2));
            String transformed = String.format("!(%s ==~ %s)", name, value);
            m.appendReplacement(result, quoteReplacement(transformed));
        }
        m.appendTail(result);
        expression = result.toString();
        return expression;
    }

    private static String replace_LIKE(String expression) {
        Pattern p = Pattern.compile("(?i)(\\w+)\\s+LIKE\\s+('[^']*')");
        Matcher m = p.matcher(expression);
        StringBuilder result = new StringBuilder();
        while (m.find()) {
            String name = m.group(1);
            String value = toRegexPattern(m.group(2));
            String transformed = String.format("%s ==~ %s", name, value);
            m.appendReplacement(result, quoteReplacement(transformed));
        }
        m.appendTail(result);
        expression = result.toString();
        return expression;
    }

    private static String toRegexPattern(String quotedLikePattern) {
        String likePattern = quotedLikePattern.replace("'", "");
        StringBuilder regexPattern = new StringBuilder();

        for (int i = 0; i < likePattern.length(); i++) {
            char c = likePattern.charAt(i);
            switch (c) {
                case '%':
                case '*':
                    regexPattern.append(".*");
                    break;
                case '$':
                    regexPattern.append("[$]");
                    break;
                case '/':
                    regexPattern.append("\\/");
                    break;
                case '\\':
                case '.':
                case '^':
                case '|':
                case '?':
                case '+':
                case '(':
                case ')':
                case '[':
                case ']':
                case '{':
                case '}':
                    regexPattern.append('\\').append(c);
                    break;
                default:
                    regexPattern.append(c);
            }
        }

        return "/(?i)" + regexPattern + "/";
    }

    private static String replace_NOT_IN(String expression) {
        Pattern p = Pattern.compile("(?i)(\\w+)\\s+NOT\\s+IN\\s+\\((.*?)\\)");
        Matcher m = p.matcher(expression);
        StringBuilder result = new StringBuilder();
        while (m.find()) {
            String name = m.group(1);
            String values = m.group(2);
            String transformed = String.format("!(%s in [%s])", name, values);
            m.appendReplacement(result, transformed);
        }
        m.appendTail(result);
        expression = result.toString();
        return expression;
    }

    private static String replace_IN(String expression) {
        Pattern p = Pattern.compile("(?i)(\\w+)\\s+IN\\s+\\((.*?)\\)");
        Matcher m = p.matcher(expression);
        StringBuilder result = new StringBuilder();
        while (m.find()) {
            String name = m.group(1);
            String values = m.group(2);
            String transformed = String.format("%s in [%s]", name, values);
            m.appendReplacement(result, transformed);
        }
        m.appendTail(result);
        expression = result.toString();
        return expression;
    }
}
