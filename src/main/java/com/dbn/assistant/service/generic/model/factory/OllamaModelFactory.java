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
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaLanguageModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import org.jetbrains.annotations.Nullable;

import static com.dbn.assistant.provider.AIProviderId.OLLAMA;


public class OllamaModelFactory extends AbstractModelFactory {

    public OllamaModelFactory() {
        super(OLLAMA);
    }

    @Nullable
    @Override
    public ChatModel createChatModel(AssistantModelInput input) {
        return OllamaChatModel.builder()
                .modelName(input.getModelName())
                .baseUrl(input.getBaseUrl())
                .temperature(input.getTemperature())
                .numPredict(input.getMaxTokens())
                .customHeaders(input.getHeaders())
                .httpClientBuilder(createHttpClientBuilder())
                .build();
    }

    @Nullable
    @Override
    public StreamingChatModel createStreamingChatModel(AssistantModelInput input) {
        return OllamaStreamingChatModel.builder()
                .modelName(input.getModelName())
                .baseUrl(input.getBaseUrl())
                .temperature(input.getTemperature())
                .numPredict(input.getMaxTokens())
                .customHeaders(input.getHeaders())
                .httpClientBuilder(createHttpClientBuilder())
                .build();
    }

    @Nullable
    @Override
    public LanguageModel createLanguageModel(AssistantModelInput input) {
        return OllamaLanguageModel.builder()
                .modelName(input.getModelName())
                .baseUrl(input.getBaseUrl())
                .temperature(input.getTemperature())
                .numPredict(input.getMaxTokens())
                .customHeaders(input.getHeaders())
                .httpClientBuilder(createHttpClientBuilder())
                .build();
    }

    @Nullable
    @Override
    public EmbeddingModel createEmbeddingModel(AssistantModelInput input) {
        return OllamaEmbeddingModel.builder()
                .modelName(input.getModelName())
                .baseUrl(input.getBaseUrl())
                .customHeaders(input.getHeaders())
                .httpClientBuilder(createHttpClientBuilder())
                .build();
    }
}
