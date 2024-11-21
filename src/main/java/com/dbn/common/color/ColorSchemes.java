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

package com.dbn.common.color;

import com.dbn.common.latent.Latent;
import com.dbn.common.ui.util.LookAndFeel;
import com.dbn.common.util.Strings;
import com.intellij.openapi.editor.colors.ColorKey;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.dbn.common.util.Commons.coalesce;

public class ColorSchemes {
    private static final Latent<EditorColorsScheme> lightScheme = Latent.basic(() -> {
        EditorColorsScheme[] schemes = schemes();
        return coalesce(
                () -> find(schemes, "intellij", "light"),
                () -> find(schemes, "classic", "light"),
                () -> find(schemes, "light"),
                () -> globalScheme());
    });

    private static final Latent<EditorColorsScheme> darkScheme = Latent.basic(() -> {
        EditorColorsScheme[] schemes = schemes();
        return coalesce(
                () -> find(schemes, "darcula"),
                () -> find(schemes, "dark"),
                () -> globalScheme());
    });


    @NotNull
    public static EditorColorsScheme lightScheme() {
        return lightScheme.get();
    }

    @NotNull
    public static EditorColorsScheme darkScheme() {
        return darkScheme.get();
    }


    @NotNull
    public static Color foreground(@Nullable TextAttributesKey textAttributesKey, @Nullable ColorKey colorKey, @NotNull Supplier<Color> fallback) {
        return resolve(textAttributesKey, colorKey, textAttributes -> textAttributes.getForegroundColor(), fallback);
    }

    @NotNull
    public static Color background(@Nullable TextAttributesKey textAttributesKey, @Nullable ColorKey colorKey, @NotNull Supplier<Color> fallback) {
        return resolve(textAttributesKey, colorKey, textAttributes -> textAttributes.getBackgroundColor(), fallback);
    }

    @NotNull
    private static Color resolve(
            @Nullable TextAttributesKey textAttributesKey,
            @Nullable ColorKey colorKey,
            @NotNull Function<TextAttributes, Color> supplier,
            @NotNull Supplier<Color> fallback) {
        EditorColorsScheme lightScheme = ColorSchemes.lightScheme();
        EditorColorsScheme darkScheme = ColorSchemes.darkScheme();

        Color lightColor = null;
        Color darkColor = null;

        if (textAttributesKey != null) {
            TextAttributes lightAttributes = lightScheme.getAttributes(textAttributesKey);
            lightColor = lightAttributes == null ? null : supplier.apply(lightAttributes);

            TextAttributes darkAttributes = darkScheme.getAttributes(textAttributesKey);
            darkColor = darkAttributes == null ? null : supplier.apply(darkAttributes);
        }


        if (colorKey != null) {
            lightColor = lightColor == null ? lightScheme.getColor(colorKey) : lightColor;
            darkColor = darkColor == null ? darkScheme.getColor(colorKey) : darkColor;
        }

        if (lightColor != null && darkColor != null) {
            return lightDarkColor(lightColor, darkColor);
        }

        return new JBColor(() -> fallback.get());
    }

    @NotNull
    private static JBColor lightDarkColor(Color lightColor, Color darkColor) {
        return new JBColor(() -> LookAndFeel.isDarkMode() ? darkColor : lightColor);
    }


    @NotNull
    private static EditorColorsScheme[] schemes() {
        return EditorColorsManager.getInstance().getAllSchemes();
    }

    @NotNull
    private static EditorColorsScheme globalScheme() {
        return EditorColorsManager.getInstance().getGlobalScheme();
    }

    @Nullable
    private static EditorColorsScheme find(EditorColorsScheme[] schemes, String ... tokens) {
        return Arrays.stream(schemes).filter(s -> matches(s.getName(), tokens)).findFirst().orElse(null);
    }

    private static boolean matches(String name, String ... options) {
        for (String option : options) {
            if (!Strings.containsIgnoreCase(name, option)) {
                return false;
            }
        }
        return true;
    }
}
