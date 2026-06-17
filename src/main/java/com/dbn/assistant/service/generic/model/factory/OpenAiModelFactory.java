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

import com.dbn.assistant.provider.AIProviderId;
import com.dbn.assistant.service.generic.model.AssistantModelInput;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiLanguageModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.jetbrains.annotations.Nullable;

import static com.dbn.assistant.provider.AIProviderId.OPEN_AI;

public class OpenAiModelFactory extends AbstractModelFactory {

    public OpenAiModelFactory() {
        super(OPEN_AI);
    }

    protected OpenAiModelFactory(AIProviderId providerId) {
        super(providerId);
    }

    @Nullable
    @Override
    public ChatModel createChatModel(AssistantModelInput input) {
        return OpenAiChatModel.builder()
                .modelName(input.getModelName())
                .baseUrl(input.getBaseUrl())
                .user(input.getUser())
                .apiKey(input.getTokenString())
                .temperature(input.getTemperature())
                .maxTokens(input.getMaxTokens())
                .maxCompletionTokens(input.getMaxOutputTokens())
                .customHeaders(input.getHeaders())
                .httpClientBuilder(createHttpClientBuilder())
                .build();
    }

    @Nullable
    @Override
    public StreamingChatModel createStreamingChatModel(AssistantModelInput input) {
        return OpenAiStreamingChatModel.builder()
                .modelName(input.getModelName())
                .baseUrl(input.getBaseUrl())
                .user(input.getUser())
                .apiKey(input.getTokenString())
                .temperature(input.getTemperature())
                .maxTokens(input.getMaxTokens())
                .maxCompletionTokens(input.getMaxOutputTokens())
                .customHeaders(input.getHeaders())
                .httpClientBuilder(createHttpClientBuilder())
                .build();
    }

    @Nullable
    @Override
    public LanguageModel createLanguageModel(AssistantModelInput input) {
        return OpenAiLanguageModel.builder()
                .modelName(input.getModelName())
                .baseUrl(input.getBaseUrl())
                .apiKey(input.getTokenString())
                .temperature(input.getTemperature())
                .customHeaders(input.getHeaders())
                .httpClientBuilder(createHttpClientBuilder())
                .build();
    }

    @Nullable
    @Override
    public EmbeddingModel createEmbeddingModel(AssistantModelInput input) {
        return OpenAiEmbeddingModel.builder()
                .modelName(input.getModelName())
                .baseUrl(input.getBaseUrl())
                .user(input.getUser())
                .apiKey(input.getTokenString())
                .customHeaders(input.getHeaders())
                .httpClientBuilder(createHttpClientBuilder())
                .build();
    }

}
