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

package com.dbn.vector.model;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Cloneable;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.vector.model.request.EmbeddingChunkingConfig;
import com.dbn.vector.model.request.EmbeddingDestinationConfig;
import com.dbn.vector.model.request.EmbeddingModelConfig;
import com.dbn.vector.model.request.EmbeddingSourceConfig;
import com.dbn.vector.model.request.EmbeddingStagingConfig;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.state.PersistentStateElement.cloneElement;

@Getter
@Setter
public class VectorEmbeddingRequest implements PersistentStateElement, Cloneable<VectorEmbeddingRequest> {
    private final ConnectionId connectionId;
    private transient boolean template;

    private EmbeddingSourceConfig sourceConfig = new EmbeddingSourceConfig();
    private EmbeddingStagingConfig stagingConfig = new EmbeddingStagingConfig();
    private EmbeddingChunkingConfig chunkConfig = new EmbeddingChunkingConfig();
    private EmbeddingModelConfig modelConfig = new EmbeddingModelConfig();
    private EmbeddingDestinationConfig destinationConfig = new EmbeddingDestinationConfig();

    public VectorEmbeddingRequest(ConnectionId connectionId) {
        this.connectionId = connectionId;
    }

    @NotNull
    public final ConnectionHandler getConnection() {
        return ConnectionHandler.ensure(connectionId);
    }

    @NotNull
    public final Project getProject() {
        return getConnection().getProject();
    }

    public void initialize(SchemaId userSchema) {
        if (userSchema == null) return;

        // preselect user schema in relevant config blocks
        String schemaName = userSchema.getName();
        sourceConfig.getSourceTable().setSchemaName(schemaName);
        modelConfig.getDatabaseModelConfig().setSchemaName(schemaName);
        modelConfig.getThirdPartyModelConfig().setCredentialSchemaName(schemaName);
        destinationConfig.setSchemaName(schemaName);
        stagingConfig.setSchemaName(schemaName);
    }

    public int getRecordCount() {
        return sourceConfig.getRecordCount();
    }

    /**
     * Soft reset, to be used after a request has been executed.
     * <li>clear source files</li>
     * <li>switch store config to "existing table" assuming it has been created</li>
     */
    public void resetSoft() {
        sourceConfig.getSourceFiles().getFilePaths().clear();
        sourceConfig.getSourceTables().getSourceTables().clear();
    }

    /**
     * Hard reset, triggered intentionally by user.
     * (returns the request to its initial state)
     */
    public void resetHard(SchemaId userSchema) {
        sourceConfig = new EmbeddingSourceConfig();
        stagingConfig = new EmbeddingStagingConfig();
        chunkConfig = new EmbeddingChunkingConfig();
        modelConfig = new EmbeddingModelConfig();
        destinationConfig = new EmbeddingDestinationConfig();

        initialize(userSchema);
    }

    @Override
    public void readState(Element element) {
        if (element == null) return;

        Element sourceConfigElement = element.getChild("source-config");
        Element stagingConfigElement = element.getChild("staging-config");
        Element chunkConfigElement = element.getChild("chunk-config");
        Element embedConfigElement = element.getChild("embed-config");
        Element storeConfigElement = element.getChild("store-config");

        sourceConfig.readState(sourceConfigElement);
        stagingConfig.readState(stagingConfigElement);
        chunkConfig.readState(chunkConfigElement);
        modelConfig.readState(embedConfigElement);
        destinationConfig.readState(storeConfigElement);
    }

    @Override
    public void writeState(Element element) {
        Element sourceConfigElement = newElement(element, "source-config");
        Element stagingConfigElement = newElement(element, "staging-config");
        Element chunkConfigElement = newElement(element, "chunk-config");
        Element embedConfigElement = newElement(element, "embed-config");
        Element storeConfigElement = newElement(element, "store-config");

        sourceConfig.writeState(sourceConfigElement);
        stagingConfig.writeState(stagingConfigElement);
        chunkConfig.writeState(chunkConfigElement);
        modelConfig.writeState(embedConfigElement);
        destinationConfig.writeState(storeConfigElement);
    }

    @Override
    public VectorEmbeddingRequest clone() {
        return cloneElement(this, new VectorEmbeddingRequest(connectionId));
    }
}

