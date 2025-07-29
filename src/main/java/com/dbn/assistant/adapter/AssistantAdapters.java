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

package com.dbn.assistant.adapter;

import com.dbn.assistant.AssistantType;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public class AssistantAdapters {
    private static final Map<AssistantType, AssistantAdapter> cache = new ConcurrentHashMap<>();

    public static AssistantAdapter get(AssistantType assistantType) {
        return cache.computeIfAbsent(assistantType, t -> find(t));
    }

    private static @NotNull AssistantAdapter find(AssistantType assistantType) {
        List<AssistantAdapter> adapters = AssistantAdapter.EP.getExtensionList();
        for (AssistantAdapter adapter : adapters) {
            if (adapter.getAssistantType() == assistantType) return adapter;
        }

        throw new UnsupportedOperationException("No assistant adapter registered for " + assistantType);
    }
}
