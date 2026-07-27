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
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private static final @NonNls String DOCKERFILE_GRAAL_TEMPLATE = "DBN - MCP Micronaut Dockerfile Graal";
    // credential-free deployment image, used only when deploying to Graal; the local-runtime
    // "Dockerfile" is unaffected and still drives running the server on the user's machine
    private static final @NonNls String DOCKERFILE_GRAAL_FILE = "Dockerfile.graal";

    // the GraalOS SDK image ships no Maven, so the Graal build stage runs "./mvnw"; the wrapper
    // uses "only-script" distribution, which needs no maven-wrapper.jar
    private static final @NonNls String MVNW_FILE = "mvnw";
    private static final @NonNls String MVNW_PROPERTIES_FILE = ".mvn/wrapper/maven-wrapper.properties";
    private static final @NonNls String MVNW_RESOURCE = "/mcp/deploy/mvnw.template";
    private static final @NonNls String MVNW_PROPERTIES_RESOURCE = "/mcp/deploy/maven-wrapper.properties";

    private String graalDockerfileContent;

    McpMicronautContainerGenerator(@NotNull Project project, @NotNull McpServerDefinition definition) {
        super(project, definition);
    }

    @Override
    public void prepareContent() {
        super.prepareContent();

        @NonNls Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("SERVER_NAME", definition.getServerName());
        attributes.put("HTTP_PORT", definition.getHttpPort());
        graalDockerfileContent = TemplateUtilities.generateCode(project, DOCKERFILE_GRAAL_TEMPLATE, attributes);
    }

    @Override
    public Map<String, String> getSourceFiles() {
        Map<String, String> files = new LinkedHashMap<>(super.getSourceFiles());
        files.put(DOCKERFILE_GRAAL_FILE, graalDockerfileContent);
        files.put(MVNW_FILE, readResource(MVNW_RESOURCE));
        files.put(MVNW_PROPERTIES_FILE, readResource(MVNW_PROPERTIES_RESOURCE));
        return files;
    }

    /** Reads a bundled verbatim resource (not a template - these must not go through Velocity). */
    private static String readResource(@NonNls String resource) {
        try (InputStream stream = McpMicronautContainerGenerator.class.getResourceAsStream(resource)) {
            if (stream == null) throw new IllegalStateException("Missing bundled resource: " + resource);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read bundled resource: " + resource, e);
        }
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
