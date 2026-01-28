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

import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class ExtensionPointCache<K, E> {
    private final ExtensionPointName<E> extensionPointName;
    private final Set<K> keys;
    private final Map<K, E> cache = new ConcurrentHashMap<>();
    private final Function<E, K> keyProvider;

    protected ExtensionPointCache(ExtensionPointName<E> extensionPointName, Function<E, K> keyProvider) {
        this.extensionPointName = extensionPointName;
        this.keyProvider = keyProvider;
        this.keys = extensionPointName.getExtensionList().stream().map(keyProvider).collect(Collectors.toSet());
    }

    protected E find(K key) {
        return cache.computeIfAbsent(key, t -> scan(t));
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

        throw new UnsupportedOperationException("No extension of type \"" + extensionPointName + "\" registered for key \"" + key + "\"");
    }

    protected List<E> all() {
        return extensionPointName.getExtensionList();
    }
}
