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

import com.dbn.assistant.http.AssistantHttpClientBuilderFactory;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.assistant.service.generic.model.AssistantModelFactory;
import com.dbn.assistant.service.generic.model.AssistantModelInput;
import dev.langchain4j.http.client.HttpClientBuilder;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.language.LanguageModel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.util.Unsafe.cast;

@Getter
abstract class AbstractModelFactory implements AssistantModelFactory {
    private final AIProvider provider;

    public AbstractModelFactory(AIProvider provider) {
        this.provider = provider;
    }

    protected static @NotNull HttpClientBuilder createHttpClientBuilder() {
        return AssistantHttpClientBuilderFactory.createBuilder();
    }

    @Override
    public <T> T createModel(Class<T> modelType, AssistantModelInput input) {
        if (modelType.equals(ChatModel.class))          return cast(createChatModel(input));
        if (modelType.equals(StreamingChatModel.class)) return cast(createStreamingChatModel(input));
        if (modelType.equals(LanguageModel.class))      return cast(createLanguageModel(input));
        if (modelType.equals(EmbeddingModel.class))     return cast(createEmbeddingModel(input));

        return null;
    }

    @Nullable
    protected abstract ChatModel createChatModel(AssistantModelInput input);

    @Nullable
    protected abstract StreamingChatModel createStreamingChatModel(AssistantModelInput input);

    @Nullable
    protected abstract LanguageModel createLanguageModel(AssistantModelInput input);

    @Nullable
    protected abstract EmbeddingModel createEmbeddingModel(AssistantModelInput input);
}
