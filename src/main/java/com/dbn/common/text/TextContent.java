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

package com.dbn.common.text;

import lombok.Data;

import java.util.Objects;

@Data
public class TextContent {
    public static TextContent EMPTY_PLAIN_TEXT = TextContent.plain("");

    private String text;
    private final MimeType type;

    public TextContent(String text, MimeType type) {
        this.text = text;
        this.type = type;
    }

    public TextContent replaceFields(String identifier, String replacement) {
        String text = this.text.replaceAll("\\$\\$" + identifier + "\\$\\$", replacement);
        if (Objects.equals(this.text, text)) return this;
        return new TextContent(text, type);
    }

    public String getTypeId() {
        return type.id();
    }

    public boolean isHtml() {
        return type == MimeType.TEXT_HTML;
    }

    public static TextContent plain(String text) {
        return new TextContent(text, MimeType.TEXT_PLAIN);
    }

    public static TextContent html(String text) {
        return new TextContent(text, MimeType.TEXT_HTML);
    }

    public static TextContent xml(String text) {
        return new TextContent(text, MimeType.TEXT_XML);
    }

    public static TextContent css(String text) {
        return new TextContent(text, MimeType.TEXT_CSS);
    }
}
