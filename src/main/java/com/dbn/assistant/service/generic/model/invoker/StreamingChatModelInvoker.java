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

package com.dbn.assistant.service.generic.model.invoker;

import com.dbn.assistant.AssistantComponent;
import com.dbn.assistant.adapter.AssistantResponseConsumer;
import com.dbn.assistant.state.AssistantState;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServiceTokenStream;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;

import static com.dbn.assistant.service.generic.model.AssistantModelType.STREAMING_CHAT;

public class StreamingChatModelInvoker extends AbstractModelInvoker<StreamingChatModel> implements AssistantComponent {
    public StreamingChatModelInvoker() {
        super(STREAMING_CHAT);
    }

    @Override
    public void invokeModel(StreamingChatModel model, AssistantState state, String chatId, String prompt, AssistantResponseConsumer consumer) {

        var memory = prepareMemory(state);
        var context = prepareContext(state);
        var tools = prepareTools(state);

        StreamingChatModelAdapter adapter = AiServices.
                builder(StreamingChatModelAdapter.class).
                streamingChatModel(model).
                systemMessageProvider(context).
                chatMemoryProvider(memory).
                toolProvider(tools).
                build();

        TokenStream tokenStream = adapter.chat(chatId, prompt);
        initTokenStream(tokenStream, consumer);
    }

    private void initTokenStream(TokenStream tokenStream, AssistantResponseConsumer consumer) {
        if (tokenStream instanceof AiServiceTokenStream) {
            AiServiceTokenStream aiTokenStream = (AiServiceTokenStream) tokenStream;
            aiTokenStream.beforeToolExecution(e -> {
                ToolExecutionRequest request = e.request();
                consumer.acceptToolRequest(
                        request.id(),
                        request.name(),
                        request.arguments());
            });

            aiTokenStream.onToolExecuted(e -> {
                ToolExecutionRequest request = e.request();
                consumer.acceptToolResponse(
                        request.id(),
                        request.name(),
                        e.result());
            });

            aiTokenStream.onPartialResponse(t -> {
                consumer.acceptToken(t);
            });

            aiTokenStream.onCompleteResponse(r -> {
                consumer.acceptMessage(r.aiMessage().text());
                consumer.acceptCompletion();
            });

            aiTokenStream.onError((e) -> {
                consumer.acceptError(e);
                consumer.acceptCompletion();
            });

            aiTokenStream.onRetrieved(l -> {
                System.out.println();
            });

            aiTokenStream.onIntermediateResponse(r -> {
                System.out.println();
            });

            aiTokenStream.onPartialThinking(t -> {
                System.out.println();
            });

            wrapped(() -> tokenStream.start());
        }
    }

}
