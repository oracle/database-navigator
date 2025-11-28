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


import com.intellij.util.containers.IntObjectMap;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;

import static com.intellij.concurrency.ConcurrentCollectionFactory.createConcurrentIntObjectMap;

final class ColorAdjustmentCache {
    private ColorAdjustmentCache() {}

    private static final Map<ColorAdjustment, IntObjectMap<IntObjectMap<Color>>> store = new EnumMap<>(ColorAdjustment.class);

    public static Color adjusted(Color color, ColorAdjustment adjustment, int tones) {
        int rgb = color.getRGB();
        IntObjectMap<Color> cache = adjustmentStore(adjustment).get(rgb);
        if (cache == null) {
            cache = createConcurrentIntObjectMap();
            adjustmentStore(adjustment).put(rgb, cache);
        }

        Color adjustedColor = cache.get(tones);
        if (adjustedColor == null) {
            adjustedColor = adjustment.adjust(color, tones);
            cache.put(tones, adjustedColor);
        }
        return adjustedColor;
    }

    private static IntObjectMap<IntObjectMap<Color>> adjustmentStore(ColorAdjustment adjustment) {
        return store.computeIfAbsent(adjustment, a -> createConcurrentIntObjectMap());
    }
}
