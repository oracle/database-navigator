/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.assistant.provider;

import com.dbn.common.property.PropertyHolderBase.ShortStore;
import com.dbn.common.ui.Presentable;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * AI models
 *
 * @author Emmanuel Jannetti (Oracle)
 */
@Getter
public final class AIModel extends ShortStore<AIModelProperty> implements Presentable {
    private final String id;
    private final String apiName;
    private final AIProvider provider;

    AIModel(AIProvider provider, String id, String apiName) {
        this.id = id;
        this.provider = provider;
        this.apiName = apiName;
    }

    @Override
    protected AIModelProperty[] properties() {
        return AIModelProperty.VALUES;
    }

    @Override
    public @NotNull String getName() {
        return apiName; // TODO presentable profile names
    }

    @Nullable
    public static AIModel forId(String id) {
        return get(m -> m.getId().equals(id));
    }

    @Nullable
    public static AIModel forApiName(String apiName) {
        return get(m -> m.getApiName().equals(apiName));
    }

    @Nullable
    private static AIModel get(Predicate<AIModel> condition) {
        List<AIProvider> providers = AIProvider.values();
        for (AIProvider provider : providers) {
            List<AIModel> models = provider.getModels();
            for (AIModel model : models) {
                if (condition.test(model)) return model;
            }
        }

        return null;

    }

    @Override
    public String toString() {
        return getName();
    }
}
