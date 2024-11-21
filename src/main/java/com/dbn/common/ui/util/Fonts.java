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
import com.intellij.util.ui.UIUtil;
import lombok.experimental.UtilityClass;

import java.awt.Font;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public final class Fonts {

    public static final Font REGULAR = UIUtil.getLabelFont();
    public static final Font BOLD = new Font(REGULAR.getName(), Font.BOLD, REGULAR.getSize());
    public static final Map<Font, Map<Float, Font>> SIZE_DERIVATIONS = new ConcurrentHashMap<>();
    public static final Map<Font, Map<Integer, Font>> STYLE_DERIVATIONS = new ConcurrentHashMap<>();

    public static Font getLabelFont() {
        return REGULAR;
    }
    
    public static Font getEditorFont() {
        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
        return new Font(scheme.getEditorFontName(), Font.PLAIN, getLabelFont().getSize());
    }
    
    public static Font deriveFont(Font font, float size) {
        Map<Float, Font> cache = SIZE_DERIVATIONS.computeIfAbsent(font, f -> new ConcurrentHashMap<>());
        return cache.computeIfAbsent(size, s -> font.deriveFont(s));
    }
    
    public static Font deriveFont(Font font, int style) {
        Map<Integer, Font> cache = STYLE_DERIVATIONS.computeIfAbsent(font, f -> new ConcurrentHashMap<>());
        return cache.computeIfAbsent(style, s -> font.deriveFont(s));
    }

    public static Font smaller(Font font, float delta) {
        return deriveFont(font, font.getSize() - delta);
    }

    public static Font bigger(Font font, float delta) {
        return deriveFont(font, font.getSize() + delta);
    }
}
