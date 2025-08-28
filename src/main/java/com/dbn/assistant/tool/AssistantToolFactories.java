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

package com.dbn.assistant.tool;

import com.dbn.common.extension.ExtensionPointCache;

import java.util.List;

public class AssistantToolFactories extends ExtensionPointCache<AssistantToolType, AssistantToolFactory> {
    private static final AssistantToolFactories INSTANCE = new AssistantToolFactories();
    private AssistantToolFactories() {
        super(AssistantToolFactory.EP, a -> a.getToolType());
    }

    public static AssistantToolFactory get(AssistantToolType type) {
        return INSTANCE.find(type);
    }

    public static List<AssistantToolFactory> list() {
        return INSTANCE.all();
    }
}
