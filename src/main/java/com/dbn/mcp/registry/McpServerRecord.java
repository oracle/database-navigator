/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.mcp.registry;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.connection.ConnectionId;
import com.dbn.mcp.build.McpBuilderResult;
import com.dbn.mcp.build.McpDistPaths;
import com.dbn.mcp.model.McpServerDefinition;
import com.dbn.mcp.model.McpServerImplementation;
import com.dbn.mcp.model.McpTransportType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

import static com.dbn.common.options.setting.Settings.connectionIdAttribute;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.longAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setConstantAttribute;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setLongAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

@Data
@EqualsAndHashCode(of = "outputDirectory")
public class McpServerRecord implements PersistentStateElement {
    private String outputDirectory;
    private McpServerDefinition definition = new McpServerDefinition();
    private ConnectionId connectionId;
    private McpArtifactType artifactType;
    private String artifactPath;
    private String imageName;
    private long buildTimestamp;
    private @Nullable McpDeploymentInfo deployment;
    private McpServerStatus status = McpServerStatus.BUILT;

    public String getServerName() {
        return definition.getServerName();
    }

    public McpServerImplementation getImplementation() {
        return definition.getImplementation();
    }

    public McpTransportType getTransportType() {
        return definition.getTransportType();
    }

    public String getHttpPort() {
        return definition.getHttpPort();
    }

    public @NotNull Path getOutputPath() {
        return Path.of(outputDirectory);
    }

    public @NotNull Path getSourceProjectPath() {
        return getOutputPath().resolve(McpDistPaths.SOURCE_PROJECT);
    }

    public @NotNull Path getReadmePath() {
        return getOutputPath().resolve(McpDistPaths.README);
    }

    public boolean isDeployed() {
        return deployment != null && status == McpServerStatus.DEPLOYED;
    }

    public @NotNull McpBuilderResult toBuilderResult() {
        Path outputPath = getOutputPath();
        Path payloadPath = McpDistPaths.payloadDirectory(outputPath, getImplementation());

        McpBuilderResult result = new McpBuilderResult();
        result.setBaseDirectory(outputPath.getParent().getParent());
        result.setOutputDirectory(outputPath);
        result.setSourceDirectory(getSourceProjectPath());
        result.setConfigFile(payloadPath.resolve(McpDistPaths.CONFIG));
        result.setWalletDirectory(payloadPath.resolve(McpDistPaths.WALLET));
        result.setServerJar(artifactPath == null ? null : outputPath.resolve(artifactPath));
        result.setImageName(imageName);
        return result;
    }

    @Override
    public void readState(Element element) {
        outputDirectory = normalizePath(stringAttribute(element, "output-directory"));
        connectionId = connectionIdAttribute(element, "connection-id");
        status = enumAttribute(element, "status", McpServerStatus.BUILT);
        buildTimestamp = longAttribute(element, "build-timestamp", 0);

        Element definitionElement = element.getChild("mcp-server-definition");
        if (definitionElement != null) definition.readState(definitionElement);

        Element artifactElement = element.getChild("artifact");
        artifactType = enumAttribute(artifactElement, "type", McpArtifactType.class);
        artifactPath = stringAttribute(artifactElement, "path");
        imageName = stringAttribute(artifactElement, "image-name");

        Element deploymentElement = element.getChild("deployment");
        if (deploymentElement != null) {
            deployment = new McpDeploymentInfo();
            deployment.readState(deploymentElement);
        }
    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "output-directory", outputDirectory);
        setConstantAttribute(element, "connection-id", connectionId);
        setEnumAttribute(element, "status", status);
        setLongAttribute(element, "build-timestamp", buildTimestamp);

        definition.writeState(newElement(element, "mcp-server-definition"));

        Element artifactElement = newElement(element, "artifact");
        setEnumAttribute(artifactElement, "type", artifactType);
        setStringAttribute(artifactElement, "path", artifactPath);
        setStringAttribute(artifactElement, "image-name", imageName);

        if (deployment != null) {
            deployment.writeState(newElement(element, "deployment"));
        }
    }

    private static String normalizePath(String path) {
        return path == null ? null : Path.of(path).toAbsolutePath().normalize().toString();
    }
}
