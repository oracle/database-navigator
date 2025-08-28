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

package com.dbn.assistant.service.generic.model;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.language.LanguageModel;
import lombok.Getter;

@Getter
public enum AssistantModelType {
    CHAT           (ChatModel.class),
    STREAMING_CHAT (StreamingChatModel.class),
    EMBEDDING      (EmbeddingModel.class),
    LANGUAGE       (LanguageModel.class),
    ;

    private final Class<?> modelClass;

    AssistantModelType(Class modelClass) {
        this.modelClass = modelClass;
    }

    public static AssistantModelType get(Object model) {
        return get(model.getClass());
    }

    public static AssistantModelType get(Class<?> modelClass) {
        for (AssistantModelType type : AssistantModelType.values()) {
            if (type.getModelClass().isAssignableFrom(modelClass)) {
                return type;
            }
        }
        return null;
    }
}
