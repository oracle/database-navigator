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

package com.dbn.ml.model;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Cloneable;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.ml.model.source.MLSourceConfig;
import com.dbn.ml.model.feature.MLFeatureConfig;
import com.dbn.ml.model.trainer.MLTrainerConfig;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.state.PersistentStateElement.cloneElement;

@Getter
@Setter
public class MLRequest implements PersistentStateElement, Cloneable<MLRequest> {
    private final ConnectionId connectionId;
    private transient boolean template;

    private MLSourceConfig sourceConfig = new MLSourceConfig();
    private MLFeatureConfig featureConfig = new MLFeatureConfig();
    private MLTrainerConfig trainerConfig = new MLTrainerConfig();
    private MLBackendConfig backendConfig = new MLBackendConfig();

    public MLRequest(ConnectionId connectionId) {
        this.connectionId = connectionId;
    }

    @NotNull
    public final ConnectionHandler getConnection() {
        return ConnectionHandler.ensure(connectionId);
    }

    public void initialize(SchemaId userSchema) {
        if (userSchema == null) return;
        
        String schemaName = userSchema.getName();
        sourceConfig.getTableSourceConfig().setSchemaName(schemaName);
    }

    public void reset(SchemaId userSchema) {
        sourceConfig = new MLSourceConfig();
        featureConfig = new MLFeatureConfig();
        trainerConfig = new MLTrainerConfig();
        backendConfig = new MLBackendConfig();

        initialize(userSchema);
    }

    @Override
    public void readState(Element element) {
        if (element == null) return;

        Element sourceConfigElement = element.getChild("source-config");
        Element featureConfigElement = element.getChild("feature-config");
        Element trainerConfigElement = element.getChild("trainer-config");
        Element backendConfigElement = element.getChild("backend-config");

        sourceConfig.readState(sourceConfigElement);
        featureConfig.readState(featureConfigElement);
        trainerConfig.readState(trainerConfigElement);
        backendConfig.readState(backendConfigElement);
    }

    @Override
    public void writeState(Element element) {
        Element sourceConfigElement = newElement(element, "source-config");
        Element featureConfigElement = newElement(element, "feature-config");
        Element trainerConfigElement = newElement(element, "trainer-config");
        Element backendConfigElement = newElement(element, "backend-config");

        sourceConfig.writeState(sourceConfigElement);
        featureConfig.writeState(featureConfigElement);
        trainerConfig.writeState(trainerConfigElement);
        backendConfig.writeState(backendConfigElement);
    }

    @Override
    public MLRequest clone() {
        return cloneElement(this, new MLRequest(connectionId));
    }
}
