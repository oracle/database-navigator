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

package com.dbn.assistant.service.generic.model.factory;

import com.dbn.assistant.service.generic.model.AssistantModelInput;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.language.LanguageModel;
import org.jetbrains.annotations.Nullable;

import static com.dbn.assistant.provider.AIProviderId.ANTHROPIC;


public class AnthropicModelFactory extends AbstractModelFactory {

    public AnthropicModelFactory() {
        super(ANTHROPIC);
    }

    @Nullable
    @Override
    protected ChatModel createChatModel(AssistantModelInput input) {
        return AnthropicChatModel.builder()
                .modelName(input.getModelName())
                .baseUrl(input.getUrl())
                .apiKey(input.getTokenString())
                .temperature(input.getTemperature())
                .httpClientBuilder(createHttpClientBuilder())
                .build();
    }

    @Nullable
    @Override
    protected StreamingChatModel createStreamingChatModel(AssistantModelInput input) {
        return AnthropicStreamingChatModel.builder()
                .modelName(input.getModelName())
                .baseUrl(input.getUrl())
                .apiKey(input.getTokenString())
                .temperature(input.getTemperature())
                .httpClientBuilder(createHttpClientBuilder())
                .build();
    }

    @Override
    protected @Nullable LanguageModel createLanguageModel(AssistantModelInput input) {
        return null;
    }

    @Nullable
    @Override
    protected EmbeddingModel createEmbeddingModel(AssistantModelInput input) {
        return null;
    }
}
