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
import com.dbn.assistant.service.generic.context.AssistantMemoryId;
import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.tool.execution.AssistantToolRequestNormalizer;
import com.dbn.common.compatibility.Workaround;
import com.dbn.common.util.Unsafe;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServiceTokenStream;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.dbn.assistant.service.generic.model.AssistantModelType.STREAMING_CHAT;
import static com.dbn.common.dispose.Failsafe.guarded;
import static com.dbn.common.util.TimeUtil.isOlderThan;

public class StreamingChatModelInvoker extends AbstractModelInvoker<StreamingChatModel> implements AssistantComponent {
    public StreamingChatModelInvoker() {
        super(STREAMING_CHAT);
    }

    @Override
    public void invokeModel(StreamingChatModel model, AssistantState state, AssistantMemoryId memoryId, String prompt, AssistantResponseConsumer consumer) {
        var builder = AiServices.builder(StreamingChatModelAdapter.class);
        builder.streamingChatModel(model);

        ModelInvocationContext context = creatInvocationContext(state, memoryId, consumer);
        initChatMemory(builder, context);
        initSystemMessage(builder, context);
        initInternalToolProvider(builder, context);
        initExternalToolProviders(builder, context);
        initToolExecutionErrorHandler(builder, context);

        StreamingChatModelAdapter adapter = builder.build();

        TokenStream tokenStream = adapter.chat(memoryId, prompt);
        initTokenStream(tokenStream, context);
    }

    private void initTokenStream(TokenStream tokenStream, ModelInvocationContext context) {
        AiServiceTokenStream modelTokenStream = (AiServiceTokenStream) tokenStream;

        TokenBuffer buffer = new TokenBuffer();
        AssistantResponseConsumer consumer = context.getResponseConsumer();

        modelTokenStream.beforeToolExecution(e -> {
            ToolExecutionRequest request = e.request();
            normalizeRequest(request);
            guarded(() -> consumer.acceptToolRequest(
                    request.id(),
                    request.name(), request.arguments()));
        });

        modelTokenStream.onToolExecuted(e -> {
            ToolExecutionRequest request = e.request();
            guarded(() -> consumer.acceptToolResponse(
                    request.id(),
                    request.name(), e.result()));
        });

        modelTokenStream.onPartialResponse(t -> {
            // avoid scroll flickering on incomplete markdown structures
            // (buffer consecutive tokens containing formating elements)
            Pattern pattern = Pattern.compile("[#*_`~\\[\\]()>+\\-!=|]");
            buffer.append(t);
            if (!pattern.matcher(t).matches()) {
                buffer.consume(consumer, false);
            }
        });

        modelTokenStream.onCompleteResponse(r -> {
            buffer.consume(consumer, true);

            guarded(() -> consumer.acceptMessage(r.aiMessage().text()));
            guarded(() -> consumer.acceptCompletion());
        });

        modelTokenStream.onError((e) -> {
            guarded(() -> consumer.acceptError(e.getMessage(), e));
            guarded(() -> consumer.acceptCompletion());
        });

        modelTokenStream.onRetrieved(l -> {
            return;
        });

        modelTokenStream.onIntermediateResponse(r -> {
            return;
        });

        modelTokenStream.onPartialThinking(t -> {
            // TODO display "thinking..." in chat box
            return;
        });

        wrapped(() -> tokenStream.start());
    }

    // avoid screen flickering when time-interval between tokens is below chatbox UI refresh time
    // (buffer tokens and release only if forced or buffer time exceeded)
    private static class TokenBuffer {
        private final StringBuilder buffer = new StringBuilder();
        private long consumeTimestamp = 0;

        private void consume(AssistantResponseConsumer consumer, boolean force) {
            force = force || isOlderThan(consumeTimestamp, 50, TimeUnit.MILLISECONDS);
            if (!force) return;

            // consume and reset
            consumeTimestamp = System.currentTimeMillis();
            consumer.acceptToken(buffer.toString());
            buffer.delete(0, buffer.length());
        }

        public void append(String token) {
            buffer.append(token);
        }
    }

    @Workaround
    private static void normalizeRequest(ToolExecutionRequest request) {
        Unsafe.logged(() -> AssistantToolRequestNormalizer.normalize(request));
    }

}
