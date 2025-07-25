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
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@UtilityClass
public class TextResources {
    private final Map<Pair<Class, String>, String> RESOURCES = new ConcurrentHashMap<>();

    public static String get(Object object, String resourceName) {
        return get(object.getClass(), resourceName);
    }

    public static String get(Class clazz, String resourceName) {
        return RESOURCES.computeIfAbsent(Pair.of(clazz, resourceName), k -> readResource(clazz, resourceName));
    }

    @NotNull
    private static String readResource(Class clazz, String resourceName) {
        try {
            return Commons.readInputStream(clazz.getResourceAsStream(resourceName));
        } catch (Exception e) {
            log.error("Failed to read resource \"{}\". Returning empty string", resourceName, e);
            return "";
        }
    }

}
