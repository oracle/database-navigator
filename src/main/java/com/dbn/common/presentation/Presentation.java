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

package com.dbn.common.presentation;

import com.dbn.common.presentation.provider.ConnectionPresentationProvider;
import com.dbn.common.presentation.provider.ConnectionRefPresentationProvider;
import com.dbn.common.presentation.provider.DBObjectPresentationProvider;
import com.dbn.common.presentation.provider.DBObjectRefPresentationProvider;
import com.dbn.common.presentation.provider.DefaultPresentationProvider;
import com.dbn.common.presentation.provider.PsiFilePresentationProvider;
import com.dbn.common.presentation.provider.VirtualFilePresentationProvider;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.util.Unsafe.cast;

@Slf4j
@UtilityClass
public class Presentation {
    private static final List<PresentationProvider<?>> PROVIDERS = new ArrayList<>();
    private static final Map<Class, PresentationProvider> LOOKUP_CACHE = new ConcurrentHashMap<>();

    public static final DefaultPresentationProvider GENERIC_PROVIDER = new DefaultPresentationProvider();

    static {

        PROVIDERS.add(new ConnectionPresentationProvider());
        PROVIDERS.add(new ConnectionRefPresentationProvider());

        PROVIDERS.add(new DBObjectPresentationProvider());
        PROVIDERS.add(new DBObjectRefPresentationProvider());

        PROVIDERS.add(new VirtualFilePresentationProvider());
        PROVIDERS.add(new PsiFilePresentationProvider());
        //...
        // latest fallback (do not define providers after this)
        PROVIDERS.add(GENERIC_PROVIDER);
    }

    private static PresentationProvider resolveProvider(Class<?> objectClass) {
        for (PresentationProvider<?> provider : PROVIDERS) {
            if (provider.supports(objectClass)) {
                return provider;
            }
        }
        log.error("No presentation provider found for object type {}", objectClass);
        return GENERIC_PROVIDER;
    }


    private static <T> PresentationProvider<T> getProvider(Object object) {
        return cast(LOOKUP_CACHE.computeIfAbsent(object.getClass(), k -> resolveProvider(k)));
    }

    public static String presentableName(Object object) {
        return getProvider(object).getName(object);
    }

    public static String presentableTypeName(Object object) {
        return getProvider(object).getTypeName(object);
    }

    public static String presentableDescription(Object object) {
        return getProvider(object).getDescription(object);
    }

    public static Icon presentableIcon(Object object) {
        return getProvider(object).getIcon(object);
    }
}
