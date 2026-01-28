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

public class AssistantResponseAdapter implements AssistantResponseConsumer {
    private Consumer<String> tokenConsumer;
    private Consumer<String> messageConsumer;
    private Consumer<Throwable> errorConsumer;
    private TriConsumer<String, String, String> toolRequestConsumer;
    private TriConsumer<String, String, String> toolResponseConsumer;
    private Runnable completionConsumer;

    private AssistantResponseAdapter() {

    }

    public static AssistantResponseAdapter create() {
        return new AssistantResponseAdapter();
    }

    public AssistantResponseAdapter withTokenConsumer(Consumer<String> tokenConsumer) {
        this.tokenConsumer = tokenConsumer;
        return this;
    }

    public AssistantResponseAdapter withMessageConsumer(Consumer<String> messageConsumer) {
        this.messageConsumer = messageConsumer;
        return this;
    }

    public AssistantResponseAdapter withErrorConsumer(Consumer<Throwable> errorConsumer) {
        this.errorConsumer = errorConsumer;
        return this;
    }

    public AssistantResponseAdapter withToolRequestConsumer(TriConsumer<String, String, String> toolRequestConsumer) {
        this.toolRequestConsumer = toolRequestConsumer;
        return this;
    }

    public AssistantResponseAdapter withToolResponseConsumer(TriConsumer<String, String, String> toolResponseConsumer) {
        this.toolResponseConsumer = toolResponseConsumer;
        return this;
    }

    public AssistantResponseAdapter withCompletionConsumer(Runnable completionConsumer) {
        this.completionConsumer = completionConsumer;
        return this;
    }

    @Override
    public void acceptToken(String token) {
        if (tokenConsumer != null) {
            tokenConsumer.accept(token);
        }
    }

    @Override
    public void acceptMessage(String message) {
        if (messageConsumer != null) {
            messageConsumer.accept(message);
        }
    }

    @Override
    public void acceptError(Throwable exception) {
        if (errorConsumer != null) {
            errorConsumer.accept(exception);
        }
    }

    @Override
    public void acceptToolRequest(String requestId, String toolName, String toolArguments) {
        if (toolRequestConsumer != null) {
            toolRequestConsumer.accept(requestId, toolName, toolArguments);
        }
    }

    @Override
    public void acceptToolResponse(String requestId, String toolName, String toolResponse) {
        if (toolResponseConsumer != null) {
            toolResponseConsumer.accept(requestId, toolName, toolResponse);
        }
    }

    @Override
    public void acceptCompletion() {
        if (completionConsumer != null) {
            completionConsumer.run();
        }
    }
}
