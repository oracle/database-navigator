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
    private final AIProviderId baseProviderId;

    AIModel(String id, String apiName, AIProvider provider, AIProviderId baseProviderId) {
        this.id = id;
        this.apiName = apiName;
        this.provider = provider;
        this.baseProviderId = baseProviderId;
    }

    @Override
    protected AIModelProperty[] properties() {
        return AIModelProperty.VALUES;
    }

    @Override
    public @NotNull String getName() {
        return apiName; // TODO presentable profile names
    }

    public AIProviderId getProviderId() {
        return provider.getId();
    }

    public boolean isDefault() {
        return is(AIModelProperty.DEFAULT);
    }

    public boolean isExperimental() {
        return is(AIModelProperty.EXPERIMENTAL);
    }

    public boolean isDeprecated() {
        return is(AIModelProperty.DEPRECATED);
    }

    @Override
    public String toString() {
        return getName();
    }
}
