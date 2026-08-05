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

import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionId;
import com.dbn.mcp.build.McpDistPaths;
import com.dbn.mcp.model.McpServerDefinition;
import com.dbn.mcp.model.McpServerImplementation;
import com.dbn.mcp.model.McpTransportType;
import com.intellij.openapi.project.Project;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.dbn.common.util.Messages.options;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

@UtilityClass
final class McpServerDiscovery {

    static @NotNull List<McpServerRecord> scan(
            @NotNull Project project,
            @NotNull Path distRoot,
            @NotNull Collection<McpServerRecord> knownRecords) {
        Map<String, McpServerRecord> records = new LinkedHashMap<>();
        for (McpServerRecord record : knownRecords) {
            Path outputPath = record.getOutputPath();
            if (Files.isDirectory(outputPath)) {
                record.setStatus(record.getDeployment() == null ? McpServerStatus.BUILT : McpServerStatus.DEPLOYED);
            } else {
                record.setStatus(McpServerStatus.STALE);
            }
            records.put(record.getOutputDirectory(), record);
        }

        if (!Files.isDirectory(distRoot)) return new ArrayList<>(records.values());

        List<Path> legacyDirectories = new ArrayList<>();
        try (Stream<Path> paths = Files.list(distRoot)) {
            paths.filter(Files::isDirectory).forEach(outputDirectory -> {
                String key = normalize(outputDirectory);
                if (records.containsKey(key)) return;

                McpServerRecord manifestRecord = McpServerManifest.read(outputDirectory);
                if (manifestRecord != null) {
                    records.put(key, manifestRecord);
                } else if (isRecognizable(outputDirectory)) {
                    legacyDirectories.add(outputDirectory);
                }
            });
        } catch (IOException e) {
            conditionallyLog(e);
        }

        if (confirmLegacyImport(project, legacyDirectories.size())) {
            for (Path outputDirectory : legacyDirectories) {
                McpServerRecord record = importLegacy(outputDirectory);
                records.put(record.getOutputDirectory(), record);
                try {
                    McpServerManifest.write(record);
                } catch (IOException e) {
                    conditionallyLog(e);
                }
            }
        }
        return new ArrayList<>(records.values());
    }

    private static boolean confirmLegacyImport(Project project, int count) {
        if (count == 0) return false;

        int option = Messages.showConfirmationDialog(
                project,
                txt("msg.mcp.title.ImportMcpServers"),
                txt("msg.mcp.question.ImportMcpServers", count),
                options(txt("msg.shared.button.Continue"), txt("msg.shared.button.Cancel")), 0);
        return option == 0;
    }

    private static boolean isRecognizable(Path outputDirectory) {
        if (!Files.isRegularFile(outputDirectory.resolve(McpDistPaths.README))) return false;

        return Files.isRegularFile(outputDirectory.resolve(McpDistPaths.CONFIG)) ||
                Files.isRegularFile(outputDirectory.resolve(McpDistPaths.CONTAINER_MOUNT_DIR).resolve(McpDistPaths.CONFIG));
    }

    private static McpServerRecord importLegacy(Path outputDirectory) {
        String serverName = outputDirectory.getFileName().toString();
        Path jar = findArtifact(outputDirectory, serverName + ".jar");
        Path executable = findArtifact(outputDirectory, serverName);
        Path windowsExecutable = findArtifact(outputDirectory, serverName + ".exe");
        boolean image = Files.isRegularFile(outputDirectory.resolve(McpDistPaths.CONTAINER_MOUNT_DIR).resolve(McpDistPaths.CONFIG)) ||
                jar == null && executable == null && windowsExecutable == null;

        McpServerImplementation implementation = image ? McpServerImplementation.MICRONAUT_CONTAINER :
                executable != null || windowsExecutable != null ? McpServerImplementation.MICRONAUT_NATIVE :
                McpServerImplementation.STANDARD_JAVA;

        McpServerDefinition definition = new McpServerDefinition();
        definition.setServerName(serverName);
        definition.setImplementation(implementation);
        definition.setTransportType(implementation.isNative() ? McpTransportType.HTTP : McpTransportType.STDIO);
        readRuntimeSettings(outputDirectory, definition);

        McpServerRecord record = new McpServerRecord();
        record.setOutputDirectory(normalize(outputDirectory));
        record.setConnectionId(ConnectionId.UNKNOWN);
        record.setDefinition(definition);
        record.setBuildTimestamp(lastModified(outputDirectory));
        record.setStatus(McpServerStatus.BUILT);
        if (image) {
            record.setArtifactType(McpArtifactType.IMAGE);
            record.setImageName(serverName + ":latest");
        } else {
            Path artifact = jar != null ? jar : executable != null ? executable : windowsExecutable;
            record.setArtifactType(jar != null ? McpArtifactType.JAR : McpArtifactType.EXECUTABLE);
            if (artifact != null) {
                record.setArtifactPath(outputDirectory.relativize(artifact).toString().replace('\\', '/'));
            }
        }
        return record;
    }

    private static void readRuntimeSettings(Path outputDirectory, McpServerDefinition definition) {
        Path config = McpDistPaths.payloadDirectory(outputDirectory, definition.getImplementation())
                .resolve(McpDistPaths.CONFIG);
        if (!Files.isRegularFile(config)) return;

        try {
            for (String line : Files.readAllLines(config)) {
                String value = line.trim();
                if (value.startsWith("transport:")) {
                    String transport = yamlScalar(value.substring("transport:".length()));
                    definition.setTransportType("http".equalsIgnoreCase(transport) ?
                            McpTransportType.HTTP : McpTransportType.STDIO);
                } else if (value.startsWith("httpPort:")) {
                    String port = value.substring("httpPort:".length()).split("#", 2)[0].trim();
                    if (!port.isEmpty()) definition.setHttpPort(yamlScalar(port));
                }
            }
        } catch (IOException e) {
            conditionallyLog(e);
        }
    }

    private static String yamlScalar(String value) {
        String result = value.trim();
        if (result.length() > 1 &&
                (result.startsWith("\"") && result.endsWith("\"") ||
                 result.startsWith("'") && result.endsWith("'"))) {
            return result.substring(1, result.length() - 1);
        }
        return result;
    }

    private static Path findArtifact(Path outputDirectory, String name) {
        Path artifact = outputDirectory.resolve(name);
        return Files.isRegularFile(artifact) ? artifact : null;
    }

    private static long lastModified(Path outputDirectory) {
        try {
            return Files.getLastModifiedTime(outputDirectory).toMillis();
        } catch (IOException e) {
            conditionallyLog(e);
            return 0;
        }
    }

    private static String normalize(Path path) {
        return path.toAbsolutePath().normalize().toString();
    }
}
