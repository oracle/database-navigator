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
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

public class StreamingChatModelInvoker extends AbstractModelInvoker<StreamingChatModel>{
    public StreamingChatModelInvoker() {
        super(StreamingChatModel.class);
    }

    @Override
    public void invokeModel(StreamingChatModel model, String prompt, AssistantResponseConsumer consumer) {
        model.chat(prompt, new StreamingChatResponseHandler() {
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
        });
    }
}
