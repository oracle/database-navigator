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

import com.dbn.connection.ConnectionId;
import com.dbn.mcp.model.McpServerDefinition;
import com.intellij.openapi.util.JDOMUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static com.dbn.common.options.setting.Settings.connectionIdAttribute;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.longAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setConstantAttribute;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setLongAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
@NonNls
@UtilityClass
public final class McpServerManifest {
    public static final String FILE_NAME = ".mcp-server.xml";

    private static final String ROOT_ELEMENT = "mcp-server-manifest";
    private static final String VERSION = "1";

    public static void write(@NotNull McpServerRecord record) throws IOException {
        Path outputDirectory = record.getOutputPath();
        Files.createDirectories(outputDirectory);

        Element root = newElement(ROOT_ELEMENT);
        setStringAttribute(root, "version", VERSION);
        record.getDefinition().writeState(newElement(root, "mcp-server-definition"));

        Element build = newElement(root, "build");
        setEnumAttribute(build, "artifact-type", record.getArtifactType());
        setStringAttribute(build, "artifact-path", record.getArtifactPath());
        setStringAttribute(build, "image-name", record.getImageName());
        setLongAttribute(build, "build-timestamp", record.getBuildTimestamp());
        setConstantAttribute(build, "connection-id", record.getConnectionId());

        McpDeploymentInfo deployment = record.getDeployment();
        if (deployment != null) {
            deployment.writeState(newElement(root, "deployment"));
        }

        Path manifest = outputDirectory.resolve(FILE_NAME);
        Path temporaryManifest = outputDirectory.resolve(FILE_NAME + ".tmp");
        Files.writeString(temporaryManifest, JDOMUtil.write(root), StandardCharsets.UTF_8);
        moveManifest(temporaryManifest, manifest);
    }

    public static @Nullable McpServerRecord read(@NotNull Path outputDirectory) {
        Path manifest = outputDirectory.resolve(FILE_NAME);
        if (!Files.isRegularFile(manifest)) return null;

        try {
            Element root = JDOMUtil.load(manifest);
            if (!ROOT_ELEMENT.equals(root.getName())) return null;

            Element definitionElement = root.getChild("mcp-server-definition");
            Element buildElement = root.getChild("build");
            if (definitionElement == null || buildElement == null) return null;

            McpServerDefinition definition = new McpServerDefinition();
            definition.readState(definitionElement);

            McpServerRecord record = new McpServerRecord();
            record.setOutputDirectory(outputDirectory.toAbsolutePath().normalize().toString());
            record.setDefinition(definition);
            record.setConnectionId(connectionIdAttribute(buildElement, "connection-id"));
            record.setArtifactType(enumAttribute(buildElement, "artifact-type", McpArtifactType.class));
            record.setArtifactPath(stringAttribute(buildElement, "artifact-path"));
            record.setImageName(stringAttribute(buildElement, "image-name"));
            record.setBuildTimestamp(longAttribute(buildElement, "build-timestamp", 0));

            Element deploymentElement = root.getChild("deployment");
            if (deploymentElement == null) {
                record.setStatus(McpServerStatus.BUILT);
            } else {
                McpDeploymentInfo deployment = new McpDeploymentInfo();
                deployment.readState(deploymentElement);
                record.setDeployment(deployment);
                record.setStatus(McpServerStatus.DEPLOYED);
            }
            return record;
        } catch (Exception e) {
            conditionallyLog(e);
            log.warn("Could not read MCP server manifest {}", manifest, e);
            return null;
        }
    }

    private static void moveManifest(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
