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

import com.dbn.mcp.model.McpServerDefinition;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

/**
 * Generates the "Micronaut Container Image" MCP server: the same Micronaut
 * application as the native variant, but compiled inside a Linux builder
 * container (micronaut-maven-plugin "docker-native" packaging). The build
 * produces a container image named "&lt;serverName&gt;:latest" in the local
 * Docker-compatible runtime instead of a file artifact; no local GraalVM is
 * required. The image targets linux/&lt;host-arch&gt; — other architectures
 * are covered by the standalone Dockerfile exported with the source project.
 */
class McpMicronautContainerGenerator extends McpMicronautNativeGenerator {

    McpMicronautContainerGenerator(@NotNull Project project, @NotNull McpServerDefinition definition) {
        super(project, definition);
    }

    @Override
    public List<String> getMavenGoals() {
        return List.of("clean", "package", "-Dpackaging=docker-native");
    }

    @Override
    public @Nullable Path locateArtifact(Path targetDirectory) {
        // the artifact is a container image registered with the local runtime;
        // a failed image build fails the Maven build, so reaching this point
        // means the image exists
        return null;
    }
}
