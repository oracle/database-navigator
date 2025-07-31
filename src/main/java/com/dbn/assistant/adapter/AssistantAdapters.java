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
import com.dbn.common.extension.ExtensionPointCache;

public class AssistantAdapters extends ExtensionPointCache<AssistantType, AssistantAdapter> {
    private static final AssistantAdapters INSTANCE = new AssistantAdapters();
    private AssistantAdapters() {
        super(AssistantAdapter.EP, a -> a.getAssistantType());
    }

    public static AssistantAdapter get(AssistantType assistantType) {
        return INSTANCE.find(assistantType);
    }
}
