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

package com.dbn.common.ui.misc;

import com.intellij.ui.Expandable;
import com.intellij.ui.components.fields.ExpandableTextField;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.JTextArea;
import java.awt.Color;
import java.awt.Font;
import java.util.Arrays;
import java.util.List;

import static com.dbn.common.compatibility.CompatibilityUtil.addUndoRedoActions;
import static com.dbn.common.util.Commons.nvl;
import static com.intellij.openapi.util.text.StringUtil.convertLineSeparators;

/**
 * Expandable text field for prose values that may contain line breaks and blank lines.
 * <p>
 * Callers can use {@link #setText(String)} and {@link #getText()} with normal {@code \n} line breaks.
 * The component presents a single-line collapsed view and preserves the multiline value while expanded.
 */
public class DBNExpandableTextField extends ExpandableTextField {

    public static final String LINE_BREAK_SURROGATE = "\u00A0";
    private static final String EMPTY_LINE_SURROGATE = "\u200B";

    public DBNExpandableTextField() {
        super(t -> expandText(t), l -> collapseText(l));
        setMonospaced(false);
    }

    @NotNull
    @Override
    protected JTextArea createTextArea(@Nls @NotNull String text, boolean editable, Color background, Color foreground, Font font) {
        JTextArea area = new JTextArea(text) {
            @Override
            public String getText() {
                return createEmptyLineSurrogates(super.getText());
            }
        };
        area.putClientProperty(Expandable.class, this);
        area.setEditable(editable);
        area.setBackground(background);
        area.setForeground(foreground);
        area.setFont(font);
        area.setWrapStyleWord(true);
        area.setLineWrap(true);

        addUndoRedoActions(area);
        return area;
    }

    @Override
    public void collapse() {
        super.collapse();
    }

    private static List<String> expandText(String text) {
        text = convertLineSeparators(nvl(text, ""));
        String[] lines = text.split("\n", -1);
        return Arrays.asList(lines);
    }

    private static String collapseText(List<String> lines) {
        if (lines == null) return "";
        String text = String.join("\n", lines);
        text = removeEmptyLineSurrogates(text);
        return text;
    }

    @Override
    public void setText(String text) {
        text = nvl(text, "");
        text = convertLineSeparators(text);
        text = createLineBreakSurrogates(text);
        super.setText(text);
    }

    @Override
    public String getText() {
        String text = super.getText();
        text = nvl(text, "");
        text = removeLineBreakSurrogates(text);
        return text;
    }

    private static String createLineBreakSurrogates(String text) {
        return text.replace("\n", LINE_BREAK_SURROGATE);
    }

    private static String removeLineBreakSurrogates(String text) {
        return text.replace(LINE_BREAK_SURROGATE, "\n");
    }

    private static String createEmptyLineSurrogates(String text) {
        text = removeEmptyLineSurrogates(text); // remove surrogates
        text = text.replaceAll("(?<=\n)(?=\n)", EMPTY_LINE_SURROGATE);
        return text;
    }

    private static String removeEmptyLineSurrogates(String text) {
        return text.replace(EMPTY_LINE_SURROGATE, "");
    }

}
