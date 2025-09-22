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

package com.dbn.assistant.profile;

import com.dbn.assistant.AssistantType;
import com.dbn.assistant.provider.AIProvider;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class PotentialAssistantProfile implements AssistantProfile{
    private final AIProvider provider;

    public PotentialAssistantProfile(AIProvider provider) {
        this.provider = provider;
    }

    @Override
    public AssistantType getAssistantType() {
        return AssistantType.PUBLIC;
    }

    @Override
    public String getProviderId() {
        return provider.getId();
    }

    @Override
    public String getDefaultModelId() {
        return provider.getDefaultModelId();
    }

    @Override
    public String getCredentialId() {
        return null;
    }

    @Override
    public @NotNull String getName() {
        return provider.getName();
    }
}
