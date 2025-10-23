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
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.language.LanguageModel;
import org.jetbrains.annotations.Nullable;

import static com.dbn.assistant.provider.AIProviderId.GOOGLE;


public class GoogleAiGeminiModelFactory extends AbstractModelFactory {

    public GoogleAiGeminiModelFactory() {
        super(GOOGLE);
    }

    @Nullable
    @Override
    public ChatModel createChatModel(AssistantModelInput input) {
        return GoogleAiGeminiChatModel.builder()
                .modelName(input.getModelName())
                .apiKey(input.getTokenString())
                .temperature(input.getTemperature())
                .httpClientBuilder(createHttpClientBuilder())
                .build();
    }

    @Nullable
    @Override
    public StreamingChatModel createStreamingChatModel(AssistantModelInput input) {
        return GoogleAiGeminiStreamingChatModel.builder()
                .modelName(input.getModelName())
                .apiKey(input.getTokenString())
                .temperature(input.getTemperature())
                .httpClientBuilder(createHttpClientBuilder())
                .build();
    }

    @Nullable
    @Override
    public LanguageModel createLanguageModel(AssistantModelInput input) {
        return null;
    }

    @Nullable
    @Override
    public EmbeddingModel createEmbeddingModel(AssistantModelInput input) {
        return GoogleAiEmbeddingModel.builder()
                .modelName(input.getModelName())
                .apiKey(input.getTokenString())
                .httpClientBuilder(createHttpClientBuilder())
                .build();
    }
}
