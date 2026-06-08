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

package com.dbn.common.text;

import com.dbn.common.Pair;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Localization;
import com.dbn.common.util.Unsafe;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@UtilityClass
public class TextResources {
    private static final @NonNls String HTML_TEMPLATE_SUFFIX = ".html.ft";
    private final Map<Pair<Class<?>, String>, String> RESOURCES = new ConcurrentHashMap<>();

    public static String get(Object object, @NonNls String resourceName) {
        return get(object.getClass(), resourceName);
    }

    public static String get(Class<?> clazz, @NonNls String resourceName) {
        String cacheKey = resourceCacheKey(resourceName);
        return RESOURCES.computeIfAbsent(Pair.of(clazz, cacheKey), k -> readResource(clazz, resourceName));
    }

    @NotNull
    private static String readResource(Class<?> clazz, @NonNls String resourceName) {
        return Unsafe.logged("", () -> {
            for (String candidate : resourceCandidates(resourceName)) {
                InputStream inputStream = clazz.getResourceAsStream(candidate);
                if (inputStream != null) {
                    return Commons.readInputStream(inputStream);
                }
            }
            return "";
        });
    }

    private static String resourceCacheKey(@NonNls String resourceName) {
        if (!isLocalizedResource(resourceName)) return resourceName;

        Locale locale = getLocale();
        return locale == null ? resourceName : resourceName + "|" + locale;
    }

    private static List<String> resourceCandidates(@NonNls String resourceName) {
        if (!isLocalizedResource(resourceName)) return List.of(resourceName);

        Locale locale = getLocale();
        if (locale == null || locale.getLanguage().isEmpty()) return List.of(resourceName);

        List<String> resourceNames = new ArrayList<>(3);
        String language = locale.getLanguage();
        String country = locale.getCountry();

        if (!country.isEmpty()) {
            resourceNames.add(localizedResourceName(resourceName, language + "_" + country));
        }
        resourceNames.add(localizedResourceName(resourceName, language));
        resourceNames.add(resourceName);
        return resourceNames;
    }

    private static boolean isLocalizedResource(@NonNls String resourceName) {
        return resourceName.endsWith(HTML_TEMPLATE_SUFFIX);
    }

    private static Locale getLocale() {
        Locale locale = Localization.getLocale();
        return locale == null ? Locale.getDefault() : locale;
    }

    private static String localizedResourceName(@NonNls String resourceName, @NonNls String localeId) {
        int suffixIndex = resourceName.length() - HTML_TEMPLATE_SUFFIX.length();
        return resourceName.substring(0, suffixIndex) + "_" + localeId + HTML_TEMPLATE_SUFFIX;
    }

}
