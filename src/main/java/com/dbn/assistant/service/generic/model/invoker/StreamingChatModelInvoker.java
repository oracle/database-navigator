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

import java.util.regex.Pattern;

import static com.dbn.assistant.service.generic.model.AssistantModelType.STREAMING_CHAT;

public class StreamingChatModelInvoker extends AbstractModelInvoker<StreamingChatModel> implements AssistantComponent {
    public StreamingChatModelInvoker() {
        super(STREAMING_CHAT);
    }

    @Override
    public void invokeModel(StreamingChatModel model, AssistantState state, AssistantMemoryId memoryId, String prompt, AssistantResponseConsumer consumer) {

        boolean stateless = memoryId.isStateless();

        var builder = AiServices.builder(StreamingChatModelAdapter.class);
        builder.streamingChatModel(model);

        initChatMemory(builder, state, stateless);
        initSystemMessage(builder, state);
        initToolProvider(builder, state, stateless);

        StreamingChatModelAdapter adapter = builder.build();

        TokenStream tokenStream = adapter.chat(memoryId, prompt);
        initTokenStream(tokenStream, consumer);
    }

    private void initTokenStream(TokenStream tokenStream, AssistantResponseConsumer consumer) {
        StringBuilder buffer = new StringBuilder();
        if (tokenStream instanceof AiServiceTokenStream aiTokenStream) {
            aiTokenStream.beforeToolExecution(e -> {
                ToolExecutionRequest request = e.request();
                normalizeRequest(request);
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
                // avoid scroll flickering on incomplete markdown structures
                // (buffer consecutive tokens containing formating elements)
                Pattern pattern = Pattern.compile("[#*_`~\\[\\]()>+\\-!=|]");
                buffer.append(t);
                if (!pattern.matcher(t).matches()) {
                    consumeBuffer(buffer, consumer);
                }
            });

            aiTokenStream.onCompleteResponse(r -> {
                consumeBuffer(buffer, consumer);

                consumer.acceptMessage(r.aiMessage().text());
                consumer.acceptCompletion();
            });

            aiTokenStream.onError((e) -> {
                consumer.acceptError(e);
                consumer.acceptCompletion();
            });

            aiTokenStream.onRetrieved(l -> {
                return;
            });

            aiTokenStream.onIntermediateResponse(r -> {
                return;
            });

            aiTokenStream.onPartialThinking(t -> {
                // TODO display "thinking..." in chat box
                return;
            });

            wrapped(() -> tokenStream.start());
        }
    }

    private static void consumeBuffer(StringBuilder buffer, AssistantResponseConsumer consumer) {
        consumer.acceptToken(buffer.toString());
        buffer.delete(0, buffer.length());
    }

    @Workaround
    private static void normalizeRequest(ToolExecutionRequest request) {
        Unsafe.logged(() -> AssistantToolRequestNormalizer.normalize(request));
    }

}
