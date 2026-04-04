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

import com.dbn.common.routine.Consumer;
import com.intellij.util.TriConsumer;
import lombok.Builder;

import java.util.function.BiConsumer;

@Builder
public class AssistantResponseAdapter implements AssistantResponseConsumer {
    private Consumer<String> tokenConsumer;
    private Consumer<String> messageConsumer;
    private BiConsumer<String, Throwable> errorConsumer;
    private BiConsumer<String, Throwable> toolErrorConsumer;
    private TriConsumer<String, String, String> toolRequestConsumer;
    private TriConsumer<String, String, String> toolResponseConsumer;
    private Runnable completionConsumer;

    @Override
    public void acceptToken(String token) {
        if (tokenConsumer == null) return;
        tokenConsumer.accept(token);
    }

    @Override
    public void acceptMessage(String message) {
        if (messageConsumer == null) return;
        messageConsumer.accept(message);
    }

    @Override
    public void acceptError(String message, Throwable exception) {
        if (errorConsumer == null) return;
        errorConsumer.accept(message, exception);
    }

    @Override
    public void acceptToolError(String message, Throwable exception) {
        if (toolErrorConsumer == null) return;
        toolErrorConsumer.accept(message, exception);
    }

    @Override
    public void acceptToolRequest(String requestId, String toolName, String toolArguments) {
        if (toolRequestConsumer == null) return;
        toolRequestConsumer.accept(requestId, toolName, toolArguments);
    }

    @Override
    public void acceptToolResponse(String requestId, String toolName, String toolResponse) {
        if (toolResponseConsumer == null) return;
        toolResponseConsumer.accept(requestId, toolName, toolResponse);
    }

    @Override
    public void acceptCompletion() {
        if (completionConsumer == null) return;
        completionConsumer.run();
    }
}
