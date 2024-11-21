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

import com.intellij.ui.ColorUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;

public enum ColorAdjustment {
    BRIGHTER,
    DARKER,

    SOFTER,
    STRONGER;



    public Color adjust(Color color, int tones) {
        switch (this) {
            case BRIGHTER: return hackBrightness(color, tones, 1.03F);
            case DARKER: return hackBrightness(color, tones, 1 / 1.03F);

            case SOFTER: return tuneSaturation(color, tones, 1 / 1.03F);
            case STRONGER: return tuneSaturation(color, tones, 1.03F);
        }

        return color;
    }

    /*****************************************************************
     *           Copied over from {@link ColorUtil}
     *****************************************************************/

    private static Color hackBrightness(@NotNull Color color, int tones, float factor) {
        return tuneHSBComponent(color, 2, tones, factor);
    }

    private static Color tuneSaturation(@NotNull Color color, int tones, float factor) {
        return tuneHSBComponent(color, 1, tones, factor);
    }

    @NotNull
    private static Color tuneHSBComponent(@NotNull Color color, int componentIndex, int howMuch, float factor) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        float component = hsb[componentIndex];
        for (int i = 0; i < howMuch; i++) {
            component = Math.min(1, Math.max(factor * component, 0));
            if (component == 0 || component == 1) break;
        }
        hsb[componentIndex] = component;
        return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
    }
}
