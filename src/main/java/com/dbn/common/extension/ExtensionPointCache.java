/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.common.extension;

import com.dbn.common.util.Lists;
import com.intellij.openapi.extensions.ExtensionPointName;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static com.dbn.common.util.Unsafe.silent;
import static java.util.Collections.emptyList;

@Slf4j
public abstract class ExtensionPointCache<K, E extends ExtensionPoint> {
    private final ExtensionPointName<E> extensionPointName;
    private final Set<K> keys;
    private final Map<K, E> cache = new ConcurrentHashMap<>();
    private final Function<E, K> keyProvider;

    protected ExtensionPointCache(ExtensionPointName<E> extensionPointName, Function<E, K> keyProvider) {
        this.extensionPointName = extensionPointName;
        this.keyProvider = keyProvider;
        this.keys = initializeKeys();
    }

    private Set<K> initializeKeys() {
        List<E> extensionList = silent(emptyList(), () -> extensionPointName.getExtensionList());
        List<K> keys = Lists.convert(extensionList, keyProvider);
        reportDuplicateKeys(extensionPointName, keys);
        return new HashSet<>(keys);
    }

    private static <K, E extends ExtensionPoint> void reportDuplicateKeys(ExtensionPointName<E> extensionPointName, List<K> keys) {
        Set<K> uniqueKeys = new HashSet<>();
        for (K key : keys) {
            if (!uniqueKeys.add(key)) {
                log.error("Duplicate extension key \"{}\" for extension point \"{}\"", key, extensionPointName);
            }
        }
    }

    protected E find(K key) {
        return cache.computeIfAbsent(key, t -> scan(t));
    }

    public void register(E extension) {
        K key = keyProvider.apply(extension);
        cache.put(key, extension);
        keys.add(key);
    }

    protected Set<K> keys() {
        return keys;
    }

    @NotNull
    private E scan(K key) {
        List<E> extensions = extensionPointName.getExtensionList();
        for (E extension : extensions) {
            K extensionKey = keyProvider.apply(extension);
            if (Objects.equals(extensionKey, key)) return extension;
        }

        K alternativeKey = alternativeKey(key);
        if (alternativeKey != null) {
            return scan(alternativeKey);
        }

        throw new UnsupportedOperationException("No extension of type \"" + extensionPointName + "\" registered for key \"" + key + "\"");
    }

    @Nullable
    protected K alternativeKey(K key) {
        return null;
    }

    protected List<E> all() {
        return extensionPointName.getExtensionList();
    }
}
