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

package com.dbn.assistant.service.generic.provider;

import com.dbn.assistant.provider.AIProvider;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public class AssistantModelFactories {
    private static final Map<AIProvider, AssistantModelFactory> cache = new ConcurrentHashMap<>();

    public static AssistantModelFactory get(AIProvider provider) {
        return cache.computeIfAbsent(provider, t -> find(t));
    }

    private static @NotNull AssistantModelFactory find(AIProvider provider) {
        List<AssistantModelFactory> factories = AssistantModelFactory.EP.getExtensionList();
        for (AssistantModelFactory factory : factories) {
            if (Objects.equals(factory.getProvider(), provider)) return factory;
        }

        throw new UnsupportedOperationException("No assistant chat model factory registered for " + provider);
    }
}
