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

import com.dbn.common.event.ApplicationEvents;
import com.dbn.common.latent.Latent;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.util.containers.IntObjectMap;

import javax.swing.UIManager;
import java.awt.Color;
import java.util.Objects;
import java.util.function.Supplier;

import static com.intellij.concurrency.ConcurrentCollectionFactory.createConcurrentIntObjectMap;

public class ColorCache {
    private static final Latent<ColorCache> cache = Latent.basic(() -> new ColorCache());
    private final IntObjectMap<Color> store = createConcurrentIntObjectMap();

    private ColorCache() {
        ApplicationEvents.subscribe(null, EditorColorsManager.TOPIC, scheme -> store.clear());
        UIManager.addPropertyChangeListener(evt -> {
            if (Objects.equals(evt.getPropertyName(), "lookAndFeel")) {
                store.clear();
            }
        });
    }

    public static Color cached(int index, Supplier<Color> supplier) {
        Color color = store().get(index);
        if (color == null) {
            color = supplier.get();
            store().put(index, color);
        }
        return color;
    }

    private static IntObjectMap<Color> store() {
        return cache.get().store;
    }
}
