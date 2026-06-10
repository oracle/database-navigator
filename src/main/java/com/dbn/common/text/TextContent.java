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

import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.Data;
import org.jetbrains.annotations.NonNls;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;

import static com.intellij.ui.ColorUtil.toHex;

@Data
public class TextContent {
    public static final TextContent EMPTY_PLAIN_TEXT = TextContent.plain("");

    private String template;
    private String text;
    private final MimeType type;
    private Map<String, String> fields = new HashMap<>();
    private List<Function<String, String>> adjusters = new ArrayList<>();
    private boolean tooltip = false;

    public TextContent(String text, MimeType type) {
        this.template = text;
        this.text = text;
        this.type = type;
    }

    public String rebuild() {
        text = template;
        initFields();
        initFonts();
        adjustContent();
        return text;
    }

    public void adjustContent(Function<String, String> adjuster) {
        adjusters.add(adjuster);
        text = adjuster.apply(text);
    }

    private void adjustContent() {
        adjusters.forEach(a -> text = a.apply(text));
    }

    private void initFields() {
        fields.forEach((i, r) -> replaceFields(i, r));
    }

    public void initField(@NonNls String identifier, String replacement) {
        fields.put(identifier, replacement);
        replaceFields(identifier, replacement);
    }

    public void initFonts() {
        // quick hack for R3.5.0 accessibility:
        // TODO use velocity template engine instead / proper font family and size placeholders
        Font font = UIUtil.getLabelFont();
        String fontName = font.getFontName();
        int fontSize = font.getSize();

        Color color = tooltip ?
                UIUtil.getToolTipForeground() :
                UIUtil.getLabelForeground();
        String colorHex = toHex(color);

        replaceFields("REGULAR_FONT_STYLE",
                "font-family:" + fontName + ",Segoe UI,SansSerif,serif; " +
                        "font-size: " + fontSize + "pt; " +
                        "color: #" + colorHex + ";");

        replaceFields("REGULAR_LARGE_FONT_STYLE",
                "font-family:" + fontName + ",Segoe UI,SansSerif,serif; " +
                        "font-size: " + (fontSize + JBUI.scale(4)) + "pt; " +
                        "color: #" + colorHex + ";");

        replaceFields("MONOSPACE_FONT_STYLE",
                "font-family: Courier New, Courier, monospace; " +
                        "font-size: " + fontSize + "pt; " +
                        "color: #" + colorHex + ";");

        replaceFields("MONOSPACE_LARGE_FONT_STYLE",
                "font-family: Courier New, Courier, monospace; " +
                        "font-size: " + (fontSize + JBUI.scale(2)) + "pt; " +
                        "color: #" + colorHex + ";");

        replaceFields("TABLE_GRID_COLOR", "#" + toHex(UIUtil.getLabelDisabledForeground()));
    }

    private void replaceFields(@NonNls String identifier, @NonNls String replacement) {
        replacement = Matcher.quoteReplacement(replacement);
        text = text.replaceAll("\\$\\{" + identifier + "}", replacement);
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

    public static TextContent tooltip(String bodyContent, @NonNls String bodyStyle) {
        @NonNls
        TextContent content = html("<html><body style='${HTML_BODY_STYLE}; ${REGULAR_FONT_STYLE}'>${HTML_BODY_CONTENT}</body></html>");
        content.initField("HTML_BODY_STYLE", bodyStyle);
        content.initField("HTML_BODY_CONTENT", bodyContent);
        content.rebuild();
        return content;
    }

    public static TextContent html(Object object, @NonNls String resourceName) {
        String info = TextResources.get(object, resourceName);
        return html(info);
    }
    public static TextContent markdown(String text) {
        return new TextContent(text, MimeType.TEXT_MARKDOWN);
    }

    public static TextContent xml(String text) {
        return new TextContent(text, MimeType.TEXT_XML);
    }

    public static TextContent css(String text) {
        return new TextContent(text, MimeType.TEXT_CSS);
    }

    public static String asHtmlContent(String text) {
        return "<html><body>" + text + "</body></html>";
    }

    @Override
    public String toString() {
        return "[" + type + "] " + text;
    }
}
