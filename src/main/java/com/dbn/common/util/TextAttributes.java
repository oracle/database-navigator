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

import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.ui.SimpleTextAttributes;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TextAttributes {

    public static SimpleTextAttributes getSimpleTextAttributes(TextAttributesKey textAttributesKey) {
        EditorColorsManager colorManager = EditorColorsManager.getInstance();
        com.intellij.openapi.editor.markup.TextAttributes textAttributes = colorManager.getGlobalScheme().getAttributes(textAttributesKey);
        if (textAttributes == null) {
            textAttributes = HighlighterColors.TEXT.getDefaultAttributes();
        }
        return new SimpleTextAttributes(
                textAttributes.getBackgroundColor(),
                textAttributes.getForegroundColor(),
                textAttributes.getEffectColor(),
                textAttributes.getFontType());
    }
}
