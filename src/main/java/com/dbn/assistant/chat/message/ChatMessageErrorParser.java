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

package com.dbn.assistant.chat.message;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatMessageErrorParser {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static String convertJsonToHtml(String json) {
        try {
            Map<String, Object> jsonMap = OBJECT_MAPPER.readValue(json, Map.class);
            return convertToHtml(jsonMap, 0);
        } catch (Throwable e) {
            // If the JSON is not a valid object (e.g., it's an array or primitive), return the original content
            return json;
        }
    }

    private static Pattern urlPattern = Pattern.compile("(https?://[\\w\\-\\.]+(:\\d+)?(/[\\w\\-\\.~:/?#\\[\\]@!$&'()*+,;=]*)?)(?![\\w\\-\\./~])");

    private static String convertToHtml(Map<String, Object> map, int level) {
        StringBuilder html = new StringBuilder();
        html.append("<div>\n");
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String || value instanceof Number || value instanceof Boolean) {
                String text = value.toString();
                Matcher matcher = urlPattern.matcher(text);
                StringBuffer buffer = new StringBuffer();
                while (matcher.find()) {
                    matcher.appendReplacement(buffer, "<a href=\"" + matcher.group(0) + "\">" + matcher.group(0) + "</a>");
                }
                matcher.appendTail(buffer);
                html.append("  ".repeat(level + 1)).append("<div style='margin-left: 20px;'><b>").append(key).append("</b> = ").append(buffer.toString()).append("</div>\n");
            } else {
                html.append("  ".repeat(level + 1)).append("<div style='margin-left: 20px;'><b> ").append(key).append("</b>");
                if (value instanceof Map) {
                    html.append("\n").append(convertToHtml((Map<String, Object>) value, level + 1));
                } else if (value instanceof List) {
                    html.append("\n").append(processList((List<Object>) value, level + 1));
                }
                html.append("  ".repeat(level + 1)).append("</div>\n");
            }
        }
        html.append("  ".repeat(level)).append("</div>\n");
        return html.toString();
    }

    private static String processList(List<Object> list, int level) {
        StringBuilder htmlList = new StringBuilder();
        htmlList.append("<div>\n");
        for (Object item : list) {
            if (item instanceof Map) {
                htmlList.append(convertToHtml((Map<String, Object>) item, level + 1));
            } else if (item instanceof List) {
                htmlList.append(processList((List<Object>) item, level + 1));
            } else {
                String text = item.toString();
                Matcher matcher = urlPattern.matcher(text);
                StringBuffer buffer = new StringBuffer();
                while (matcher.find()) {
                    matcher.appendReplacement(buffer, "<a href=\"" + matcher.group(0) + "\">" + matcher.group(0) + "</a>");
                }
                matcher.appendTail(buffer);
                htmlList.append("  ".repeat(level + 1)).append("<div style='margin-left: 20px;'>").append(buffer.toString()).append("</div>\n");
            }
        }
        htmlList.append("  ".repeat(level)).append("</div>\n");
        return htmlList.toString();
    }
}

