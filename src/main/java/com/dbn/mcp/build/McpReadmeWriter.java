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

import com.dbn.common.template.TemplateUtilities;
import com.dbn.mcp.model.McpServerDefinition;
import com.dbn.mcp.model.McpToolDefinition;
import com.intellij.openapi.project.Project;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
final class McpReadmeWriter {
    private static final @NonNls String STANDARD_TEMPLATE = "DBN - MCP Server README";
    private static final @NonNls String MICRONAUT_TEMPLATE = "DBN - MCP Micronaut README";

    private final Project project;
    private final McpServerDefinition definition;

    void write(Path dir) {
        try {
            String serverName = definition.getServerName();
            String httpPort = definition.getHttpPort();

            List<McpToolDefinition> tools = definition.getTools();
            List<Map<String, String>> toolList = tools.stream()
                    .map(t -> Map.of(
                            "name", t.getName(),
                            "description", safe(t.getDescription(), "SQL tool")))
                    .collect(Collectors.toList());

            @NonNls Map<String, Object> context = new LinkedHashMap<>();
            context.put("SERVER_NAME", serverName);
            context.put("JAR_NAME", serverName + ".jar");
            context.put("EXECUTABLE_NAME", serverName);
            context.put("HTTP_PORT", httpPort);
            context.put("TOOLS", toolList);
            context.put("IMAGE_NAME", serverName + ":latest");
            context.put("IS_CONTAINER", definition.getImplementation().isContainer());
            context.put("MOUNT_DIR", McpBuildTask.CONTAINER_MOUNT_DIR);

            String template = definition.getImplementation().isNative() ? MICRONAUT_TEMPLATE : STANDARD_TEMPLATE;
            String content = TemplateUtilities.generateCode(project, template, context);
            Files.writeString(dir.resolve("README.md"), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to write README", e);
        }
    }

    private static String safe(String value, String defaultValue) {
        return value != null && !value.isEmpty() ? value : defaultValue;
    }
}
