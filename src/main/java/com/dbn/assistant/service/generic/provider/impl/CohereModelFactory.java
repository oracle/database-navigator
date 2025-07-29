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

package com.dbn.assistant.service.generic.provider.impl;

import com.dbn.assistant.provider.AIProvider;
import com.dbn.assistant.service.generic.provider.AssistantModelFactoryBase;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.cohere.CohereEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.jetbrains.annotations.Nullable;

public class CohereModelFactory extends AssistantModelFactoryBase {
    public CohereModelFactory() {
        super(AIProvider.forId("COHERE"));
    }

    @Override
    public @Nullable ChatModel createChatModel(String user, String apiKey, String modelName) {
        return null;
    }

    @Override
    public @Nullable StreamingChatModel createStreamingChatModel(String user, String apiKey, String modelName) {
        return null;
    }

    @Override
    public EmbeddingModel createEmbeddingModel(String user, String apiKey, String modelName) {
        return CohereEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                //.httpClientBuilder(createHttpClientBuilder())
                .build();
    }
}
