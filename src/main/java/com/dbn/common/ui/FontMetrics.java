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

package com.dbn.common.ui;

import com.dbn.common.latent.Latent;
import com.dbn.common.ref.WeakRef;

import javax.swing.JComponent;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FontMetrics {
    private final WeakRef<JComponent> component;
    private final Map<String, Map<Font, int[]>> cache = new HashMap<>();

    private final Latent<FontRenderContext> fontRenderContext = Latent.mutable(
            () -> getComponent().getFont(),
            () -> getComponent().getFontMetrics(getComponent().getFont()).getFontRenderContext());

    public FontMetrics(JComponent component) {
        this.component = WeakRef.of(component);
    }

    public JComponent getComponent() {
        return WeakRef.ensure(component);
    }

    public int getTextWidth(String group, String text) {
        Map<Font, int[]> cache = this.cache.computeIfAbsent(group, k -> new HashMap<>());
        int length = text.length();
        if (length == 0) {
            return 0;
        }

        Font font = getComponent().getFont();
        int len = Math.min(100, length);

        int[] widths = cache.compute(font, (f, v) -> v == null ? new int[len] : v.length < len ? Arrays.copyOf(v, len) : v);
        int index = len - 1;
        if (widths[index] == 0) {
            widths[index] = (int) font.getStringBounds(text, fontRenderContext.get()).getWidth();
        }

        return widths[index];
    }

}
