package com.dbn.common.color;


import com.intellij.util.containers.IntObjectMap;

import java.awt.*;
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
