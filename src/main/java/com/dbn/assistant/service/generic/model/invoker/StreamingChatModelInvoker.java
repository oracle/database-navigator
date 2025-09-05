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

import com.dbn.assistant.adapter.AssistantResponseConsumer;
import com.dbn.assistant.state.AssistantState;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.CompleteToolCall;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.chat.response.PartialToolCall;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.service.AiServiceTokenStream;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import org.jetbrains.annotations.NotNull;

import static com.dbn.assistant.service.generic.model.AssistantModelType.STREAMING_CHAT;

public class StreamingChatModelInvoker extends AbstractModelInvoker<StreamingChatModel>{
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
                tools((Object[]) tools).
                build();

        StreamingChatResponseHandler responseHandler = createResponseHandler(consumer);
        TokenStream tokenStream = adapter.chat(chatId, prompt);

        if (tokenStream instanceof AiServiceTokenStream) {
            AiServiceTokenStream aiTokenStream = (AiServiceTokenStream) tokenStream;
            aiTokenStream.beforeToolExecution(e -> {
                System.out.println(); // TODO tool interceptor
            });
            aiTokenStream.onToolExecuted(e -> {
                System.out.println(); // TODO tool interceptor
            });
        }

        startTokenStream(tokenStream, responseHandler);
    }

    private static void startTokenStream(TokenStream tokenStream, StreamingChatResponseHandler responseHandler) {
        tokenStream.
            onPartialResponse(responseHandler::onPartialResponse).
            onCompleteResponse(responseHandler::onCompleteResponse).
            onError(responseHandler::onError).
            start();
    }

    private static @NotNull StreamingChatResponseHandler createResponseHandler(AssistantResponseConsumer consumer) {
        return new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String s) {
                consumer.acceptToken(s);
            }

            @Override
            public void onCompleteResponse(ChatResponse response) {
                consumer.acceptMessage(response.aiMessage().text());
                consumer.acceptCompletion();
            }

            @Override
            public void onError(Throwable throwable) {
                consumer.acceptError(throwable);
                consumer.acceptCompletion();
            }

            @Override
            public void onPartialToolCall(PartialToolCall partialToolCall) {
                System.out.println(); // TODO tool interceptor
            }

            @Override
            public void onCompleteToolCall(CompleteToolCall completeToolCall) {
                System.out.println(); // TODO tool interceptor
            }

            @Override
            public void onPartialThinking(PartialThinking partialThinking) {
                System.out.println(); // TODO tool interceptor
            }
        };
    }
}
