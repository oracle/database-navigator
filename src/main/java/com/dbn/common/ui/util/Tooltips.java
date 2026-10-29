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

package com.dbn.common.ui.util;

import com.intellij.openapi.util.text.HtmlChunk;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

import static com.dbn.common.util.Strings.isEmpty;

@UtilityClass
public final class Tooltips {

    /** Escapes literal text for HTML tooltip content, preserving line breaks. */
    public static @NotNull String escapeHtml(@NotNull String text) {
        return HtmlChunk.text(text).toString();
    }

    /** Sets literal text, escaping characters interpreted as HTML. */
    @SuppressWarnings("UseHtmlChunkToolTip")
    public static void setToolTipText(@NotNull JComponent component, @Nullable String text) {
        if (isEmpty(text)) {
            component.setToolTipText(null);
            return;
        }

        component.setToolTipText(escapeHtml(text));
    }

    /**
     * Sets trusted HTML, either a complete document or a fragment.
     * Escape dynamic plain text before composing the markup.
     */
    @SuppressWarnings("UseHtmlChunkToolTip")
    public static void setHtmlToolTipText(@NotNull JComponent component, @Nullable String html) {
        if (isEmpty(html)) {
            component.setToolTipText(null);
            return;
        }

        component.setToolTipText(HtmlChunk.raw(html).toString());
    }

    @SuppressWarnings("UseHtmlChunkToolTip")
    public static void setPreformattedToolTipText(@NotNull JComponent component, @Nullable String text) {
        if (isEmpty(text)) {
            component.setToolTipText(null);
            return;
        }

        String escapedText = escapeHtml(text);
        String html = "<html><pre>" + escapedText + "</pre></html>";
        component.setToolTipText(HtmlChunk.raw(html).toString());
    }
}
