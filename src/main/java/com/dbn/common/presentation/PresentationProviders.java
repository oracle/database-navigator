/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.common.presentation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.util.Unsafe.cast;

public class PresentationProviders {
    private static final PresentationProviders INSTANCE = new PresentationProviders();

    private final Map<Class<?>, PresentationProvider> cache = new ConcurrentHashMap<>();

    private PresentationProviders() {
    }

    public static <T> PresentationProvider<T> get(T object) {
        return cast(INSTANCE.find(object.getClass()));
    }

    private PresentationProvider find(Class<?> objectClass) {
        return cache.computeIfAbsent(objectClass, this::resolveProvider);
    }

    private PresentationProvider resolveProvider(Class<?> objectClass) {
        PresentationProvider<?> result = null;
        for (PresentationProvider<?> provider : PresentationProvider.EP.getExtensionList()) {
            if (provider.supports(objectClass) && isMoreSpecific(provider, result)) {
                result = provider;
            }
        }

        return result == null ? Presentation.GENERIC_PROVIDER : result;
    }

    private static boolean isMoreSpecific(PresentationProvider<?> provider, PresentationProvider<?> current) {
        if (current == null) return true;

        Class<?> providerType = provider.getObjectType();
        Class<?> currentType = current.getObjectType();
        return !providerType.equals(currentType) && currentType.isAssignableFrom(providerType);
    }
}
