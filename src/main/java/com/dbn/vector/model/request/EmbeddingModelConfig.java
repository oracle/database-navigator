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

package com.dbn.vector.model.request;

import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import java.util.Map;

import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;

@Getter
@Setter
public class EmbeddingModelConfig extends EmbeddingRequestConfig {
    private EmbeddingModelLocation modelLocation = EmbeddingModelLocation.IN_DATABASE_MODEL;

    private EmbeddingModelDatabaseSpec databaseModelConfig = new EmbeddingModelDatabaseSpec();
    private EmbeddingModelThirdPartySpec thirdPartyModelConfig = new EmbeddingModelThirdPartySpec();

    public String getConfigJson() {
        return switch (modelLocation) {
            case IN_DATABASE_MODEL -> databaseModelConfig.getConfigJson();
            case THIRD_PARTY_MODEL -> thirdPartyModelConfig.getConfigJson();
        };
    }

    public Map<String, ?> getConfigMap() {
        return switch (modelLocation) {
            case IN_DATABASE_MODEL -> databaseModelConfig.getConfigMap();
            case THIRD_PARTY_MODEL -> thirdPartyModelConfig.getConfigMap();
        };
    }

    @Override
    public void readState(Element element) {
        if (element == null) return;

        super.readState(element);
        modelLocation = enumAttribute(element, "model-location", modelLocation);

        Element databaseModelElement = element.getChild("database-model");
        databaseModelConfig.readState(databaseModelElement);

        Element thirdPartyModelElement = element.getChild("third-party-model");
        thirdPartyModelConfig.readState(thirdPartyModelElement);
    }

    @Override
    public void writeState(Element element) {
        super.writeState(element);
        setEnumAttribute(element, "model-location", modelLocation);

        Element databaseModelElement = newElement(element, "database-model");
        databaseModelConfig.writeState(databaseModelElement);

        Element thirdPartyModelElement = newElement(element, "third-party-model");
        thirdPartyModelConfig.writeState(thirdPartyModelElement);
    }
}
