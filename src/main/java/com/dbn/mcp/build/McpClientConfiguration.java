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

package com.dbn.mcp.build;

import com.dbn.common.util.Json;
import com.dbn.mcp.model.McpServerDefinition;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NonNls;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
final class McpClientConfiguration {
    private final McpServerDefinition definition;

    String buildClaudeJson(String serverArtifact) {
        String command;
        List<String> args;
        if (definition.getTransportType().isHttp()) {
            command = "npx";
            String httpPort = definition.getHttpPort();
            args = List.of("-y", "mcp-remote", "http://127.0.0.1:" + httpPort + "/mcp");
        } else if (definition.getImplementation().isNative()) {
            command = serverArtifact;
            args = List.of();
        } else {
            command = "java";
            args = List.of("-jar", serverArtifact);
        }
        return buildCommandSnippetJson(definition.getServerName(), command, args);
    }

    /**
     * Ready-to-use run command for the container image build target: publishes the
     * HTTP port, binds the server to all interfaces (the in-image application.yml
     * binds 127.0.0.1, unreachable from outside the container), and mounts the
     * output directory read-only at /config for mcp-config.yaml and the wallet.
     */
    @NonNls
    String buildContainerRunCommand(String containerRuntime, String imageName, Path outputDirectory) {
        String httpPort = definition.getHttpPort();
        return containerRuntime + " run -d --name " + definition.getServerName() +
                " -p " + httpPort + ":" + httpPort +
                " -e MICRONAUT_SERVER_HOST=0.0.0.0" +
                " -v " + outputDirectory.toAbsolutePath() + ":/config:ro " +
                imageName + " --config=/config/mcp-config.yaml";
    }

    String buildClineJson() {
        Map<String, Object> server = new LinkedHashMap<>();
        String httpPort = definition.getHttpPort();
        server.put("type", "streamableHttp");
        server.put("url", "http://127.0.0.1:" + httpPort + "/mcp");
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put(definition.getServerName(), server);
        String json = Json.writeAsFormattedString(entry);
        return json.substring(1, json.length() - 1).trim();
    }

    private static String buildCommandSnippetJson(String name, String command, List<String> args) {
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("command", command);
        server.put("args", args);

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put(name, server);

        String json = Json.writeAsFormattedString(entry);
        return json.substring(1, json.length() - 1).trim();
    }
}
