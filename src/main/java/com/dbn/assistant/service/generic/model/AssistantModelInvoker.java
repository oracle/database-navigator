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

package com.dbn.assistant.service.generic.model;

import com.dbn.assistant.adapter.AssistantResponseConsumer;
import com.dbn.assistant.state.AssistantState;
import com.intellij.openapi.extensions.ExtensionPointName;

public interface AssistantModelInvoker<T> {
    ExtensionPointName<AssistantModelInvoker> EP = ExtensionPointName.create("com.dbn.assistantModelInvoker");

    AssistantModelType getModelType();

    void invokeModel(T model, AssistantState state, String chatId, String prompt, AssistantResponseConsumer consumer);

}
