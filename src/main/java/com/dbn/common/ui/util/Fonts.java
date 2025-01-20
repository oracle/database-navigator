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

package com.dbn.common.ui.util;

import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.UIUtil;
import lombok.experimental.UtilityClass;

import java.awt.Font;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.intellij.util.ui.JBUI.scaleFontSize;

@UtilityClass
public final class Fonts {

    private static final Font REGULAR = JBFont.create(UIUtil.getLabelFont(), false);
    private static final Font BOLD = REGULAR.deriveFont(Font.BOLD);

    public static final Map<Font, Map<Float, Font>> SIZE_DERIVATIONS = new ConcurrentHashMap<>();
    public static final Map<Font, Map<Integer, Font>> STYLE_DERIVATIONS = new ConcurrentHashMap<>();
    public static final Map<String, Map<Integer, Font>> NAMED_FONT_DERIVATIONS = new ConcurrentHashMap<>();

    public static Font regular() {
        return REGULAR;
    }

    public static Font regular(int sizeDeviation) {
        Font font = regular();
        return deriveFont(font, adjustedSize(font, sizeDeviation));
    }

    public static Font regularBold() {
        return BOLD;
    }

    public static Font regularBold(int sizeDeviation) {
        Font font = regularBold();
        return deriveFont(font, adjustedSize(font, sizeDeviation));
    }

    public static Font editor() {
        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
        String editorFontName = scheme.getEditorFontName();

        Map<Integer, Font> cache = NAMED_FONT_DERIVATIONS.computeIfAbsent(editorFontName, n -> new ConcurrentHashMap<>());
        return cache.computeIfAbsent(regular().getSize(), s -> JBFont.create(new Font(editorFontName, Font.PLAIN, s), false));
    }

    public static Font editor(int sizeDeviation) {
        Font font = editor();
        return deriveFont(font, adjustedSize(font, sizeDeviation));
    }

    private static float adjustedSize(Font font, int deviation) {
        return font.getSize() + scaleFontSize(deviation);
    }


    public static Font deriveFont(Font font, float size) {
        Map<Float, Font> cache = SIZE_DERIVATIONS.computeIfAbsent(font, f -> new ConcurrentHashMap<>());
        return cache.computeIfAbsent(size, s -> font.deriveFont(s));
    }

    public static Font deriveFont(Font font, int style) {
        Map<Integer, Font> cache = STYLE_DERIVATIONS.computeIfAbsent(font, f -> new ConcurrentHashMap<>());
        return cache.computeIfAbsent(style, s -> font.deriveFont(s));
    }
}
