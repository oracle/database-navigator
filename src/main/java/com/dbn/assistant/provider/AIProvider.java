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

import com.dbn.common.ui.Presentable;
import com.dbn.common.util.Commons;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static com.dbn.common.util.Lists.first;
import static com.dbn.common.util.Lists.firstElement;

/**
 * This enum is for listing the possible credential providers we have
 * And the associated list of AI module they support
 *
 * @author Ayoub Aarrasse (Oracle)
 * @author Emmanuel Jannetti (Oracle)
 */
@Getter
@Setter
@EqualsAndHashCode(of = "id")
public final class AIProvider implements Presentable {

    private final AIProviderId id;
    private final String name;
    private String host;
    private String baseUrl;
    private String apiName;

    private List<AIModel> models;
    private AIAuthentication authentication;
    private Map<ProviderUrlType, String> urls;

    AIProvider(AIProviderId id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getBasicCredentialName() {
        return id == AIProviderId.OCI_GEN_AI ? "OCI Connection Config" : name + " API Key";
    }

    public AIModel getModel(String id) {
        return first(models, m -> m.getId().equals(id));
    }

    @Nullable
    public AIModel getModel(Predicate<AIModel> predicate) {
        return first(models, predicate);
    }

    public AIModel getDefaultModel() {
        return Commons.coalesce(
                () -> first(models, m -> m.isDefault()),
                () -> firstElement(models));
    }

    public String getDefaultModelId() {
        return getDefaultModel().getId();
    }

    public String getUrl(ProviderUrlType type) {
        return urls.get(type);
    }

    @Override
    public String toString() {
        return getName();
    }
}
