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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.dbn.mcp.build.McpJavaVersionManager.resolveJavaVersion;
import static com.dbn.nls.NlsResources.txt;

/**
 * Generates the "Micronaut Native" MCP server: a Micronaut application compiled
 * to a platform-specific GraalVM native executable. Requires a GraalVM JDK as
 * the Maven runtime; ships curated GraalVM reachability metadata for the Oracle
 * JDBC driver and SEPS wallet support (re-capture when bumping driver versions).
 */
class McpMicronautNativeGenerator implements McpServerGenerator {
    private static final @NonNls String APPLICATION_TEMPLATE = "DBN - MCP Micronaut Application";
    private static final @NonNls String POM_TEMPLATE = "DBN - MCP Micronaut POM.xml";
    private static final @NonNls String METADATA_TEMPLATE = "DBN - MCP Micronaut Reachability Metadata.json";

    // keep the pair platform-blessed: the platform BOM maps the compatible micronaut-mcp version
    private static final @NonNls String MICRONAUT_PLATFORM_VERSION = "5.0.3";
    private static final @NonNls String MICRONAUT_MCP_VERSION = "1.0.0";
    private static final @NonNls String JDBC_VERSION = "23.26.2.0.0";
    private static final @NonNls String ORACLE_PKI_VERSION = "23.26.2.0.0";
    private static final @NonNls String ORACLE_OSDT_VERSION = "21.18.0.0";
    private static final @NonNls String SNAKEYAML_VERSION = "2.5";

    private static final @NonNls String APPLICATION_FILE = "src/main/java/com/dbn/mcp/server/Application.java";
    private static final @NonNls String LOGBACK_FILE = "src/main/resources/logback.xml";
    private static final @NonNls String METADATA_FILE = "src/main/resources/META-INF/native-image/com.dbn.mcp/mcp-server/reachability-metadata.json";

    // MCP stdio transport owns stdout; all logging must go to stderr
    private static final @NonNls String LOGBACK_XML = """
            <configuration>
                <appender name="STDERR" class="ch.qos.logback.core.ConsoleAppender">
                    <target>System.err</target>
                    <encoder>
                        <pattern>%d{HH:mm:ss.SSS} %-5level %logger{24} - %msg%n</pattern>
                    </encoder>
                </appender>
                <root level="INFO">
                    <appender-ref ref="STDERR"/>
                </root>
            </configuration>
            """;

    private final Project project;
    private final McpServerDefinition definition;

    private String applicationContent;
    private String metadataContent;

    McpMicronautNativeGenerator(@NotNull Project project, @NotNull McpServerDefinition definition) {
        this.project = project;
        this.definition = definition;
    }

    @Override
    public String getServerName() {
        return definition.getServerName();
    }

    @Override
    public void prepareContent() {
        Properties properties = new Properties();
        properties.setProperty("SERVER_NAME", definition.getServerName());
        applicationContent = TemplateUtilities.generateCode(project, APPLICATION_TEMPLATE, properties);
        metadataContent = TemplateUtilities.generateCode(project, METADATA_TEMPLATE, new Properties());
    }

    @Override
    public Map<String, String> getSourceFiles() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put(APPLICATION_FILE, applicationContent);
        files.put(LOGBACK_FILE, LOGBACK_XML);
        files.put(METADATA_FILE, metadataContent);
        return files;
    }

    @Override
    public String getPomTemplateName() {
        return POM_TEMPLATE;
    }

    @Override
    public Properties getPomProperties() {
        Properties properties = new Properties();
        properties.setProperty("SERVER_NAME", definition.getServerName());
        properties.setProperty("PROJECT_JAVA_VERSION", resolveJavaVersion(project));
        properties.setProperty("MICRONAUT_PLATFORM_VERSION", MICRONAUT_PLATFORM_VERSION);
        properties.setProperty("MICRONAUT_MCP_VERSION", MICRONAUT_MCP_VERSION);
        properties.setProperty("JDBC_VERSION", JDBC_VERSION);
        properties.setProperty("ORACLE_PKI_VERSION", ORACLE_PKI_VERSION);
        properties.setProperty("ORACLE_OSDT_VERSION", ORACLE_OSDT_VERSION);
        properties.setProperty("SNAKEYAML_VERSION", SNAKEYAML_VERSION);
        return properties;
    }

    @Override
    public List<String> getMavenGoals() {
        return List.of("clean", "package", "-Dpackaging=native-image");
    }

    @Override
    public Path locateArtifact(Path targetDirectory) throws IOException {
        String serverName = definition.getServerName();
        Path executable = targetDirectory.resolve(serverName);
        if (Files.isRegularFile(executable)) return executable;

        Path windowsExecutable = targetDirectory.resolve(serverName + ".exe");
        if (Files.isRegularFile(windowsExecutable)) return windowsExecutable;

        throw new IOException(txt("msg.mcp.exception.NativeExecutableNotFound", serverName));
    }
}
