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
import dev.langchain4j.model.cohere.CohereEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.language.LanguageModel;
import org.jetbrains.annotations.Nullable;

import static com.dbn.assistant.provider.AIProviders.COHERE;

public class CohereModelFactory extends AbstractModelFactory {

    public CohereModelFactory() {
        super(COHERE);
    }

    @Nullable
    @Override
    public ChatModel createChatModel(AssistantModelInput input) {
        return null;
    }

    @Nullable
    @Override
    public StreamingChatModel createStreamingChatModel(AssistantModelInput input) {
        return null;
    }

    @Nullable
    @Override
    public LanguageModel createLanguageModel(AssistantModelInput input) {
        return null;
    }

    @Nullable
    @Override
    public EmbeddingModel createEmbeddingModel(AssistantModelInput input) {
        return CohereEmbeddingModel.builder()
                .modelName(input.getModel())
                .baseUrl(input.getUrl())
                .apiKey(input.getToken())
                //.httpClientBuilder(createHttpClientBuilder())
                .build();
    }
}
