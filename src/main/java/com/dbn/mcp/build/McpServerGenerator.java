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

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Runtime-specific strategy for generating an MCP server Maven project.
 * Implementations own the source templates, the POM template and its placeholders,
 * the Maven goals, and the shape of the build artifact produced under the Maven
 * "target" directory (e.g. runnable JAR vs. native executable).
 */
interface McpServerGenerator {

    static McpServerGenerator create(@NotNull Project project, @NotNull McpServerDefinition definition) {
        switch (definition.getImplementation()) {
            case STANDARD_JAVA: return new McpStandardJavaGenerator(project, definition);
            default: throw new UnsupportedOperationException(
                    "MCP server implementation not supported yet: " + definition.getImplementation());
        }
    }

    String getServerName();

    /**
     * Generates the source content up-front so that template resolution failures
     * surface during build verification, before any output is produced.
     */
    void prepareContent();

    /**
     * Generated project files as relative paths from the Maven project root
     * (e.g. "src/main/java/Main.java") mapped to their content.
     */
    Map<String, String> getSourceFiles();

    String getPomTemplateName();

    Properties getPomProperties();

    List<String> getMavenGoals();

    /**
     * Locates the distributable build artifact inside the Maven "target" directory.
     */
    Path locateArtifact(Path targetDirectory) throws IOException;
}
