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
    private static final @NonNls String POM_TEMPLATE = "DBN - MCP Micronaut POM.xml";
    private static final @NonNls String METADATA_TEMPLATE = "DBN - MCP Micronaut Reachability Metadata.json";
    private static final @NonNls String APPLICATION_YML_TEMPLATE = "DBN - MCP Micronaut Application Config.yaml";
    private static final @NonNls String DOCKERFILE_TEMPLATE = "DBN - MCP Micronaut Dockerfile";

    // generated class name -> file template name (one source file per class)
    private static final @NonNls Map<String, String> SOURCE_TEMPLATES = new LinkedHashMap<>();
    static {
        SOURCE_TEMPLATES.put("Application", "DBN - MCP Micronaut Application");
        SOURCE_TEMPLATES.put("McpConfigLoader", "DBN - MCP Micronaut McpConfigLoader");
        SOURCE_TEMPLATES.put("DataSourceConfig", "DBN - MCP Micronaut DataSourceConfig");
        SOURCE_TEMPLATES.put("ToolConfig", "DBN - MCP Micronaut ToolConfig");
        SOURCE_TEMPLATES.put("ToolParameterConfig", "DBN - MCP Micronaut ToolParameterConfig");
        SOURCE_TEMPLATES.put("McpBeanFactory", "DBN - MCP Micronaut McpBeanFactory");
        SOURCE_TEMPLATES.put("SqlToolExecutor", "DBN - MCP Micronaut SqlToolExecutor");
    }

    // keep the pair platform-blessed: the platform BOM maps the compatible micronaut-mcp version
    private static final @NonNls String MICRONAUT_PLATFORM_VERSION = "5.0.3";
    private static final @NonNls String MICRONAUT_MCP_VERSION = "1.0.0";
    private static final @NonNls String JDBC_VERSION = "23.26.2.0.0";
    private static final @NonNls String ORACLE_PKI_VERSION = "23.26.2.0.0";
    private static final @NonNls String ORACLE_OSDT_VERSION = "21.18.0.0";

    private static final @NonNls String SOURCE_ROOT = "src/main/java/com/dbn/mcp/server/";
    private static final @NonNls String LOGBACK_FILE = "src/main/resources/logback.xml";
    private static final @NonNls String APPLICATION_YML_FILE = "src/main/resources/application.yml";
    private static final @NonNls String METADATA_FILE = "src/main/resources/META-INF/native-image/com.dbn.mcp/mcp-server/reachability-metadata.json";
    // written under a neutral name: micronaut-maven-plugin treats a root "Dockerfile"
    // as an override of its docker-native packaging and would hijack the image build;
    // the source-project export renames it to "Dockerfile"
    private static final @NonNls String DOCKERFILE_FILE = "Dockerfile.ci";

    private static final @NonNls String LOGBACK_XML = """
            <configuration>
                <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
                    <encoder>
                        <pattern>%d{HH:mm:ss.SSS} %-5level %logger{24} - %msg%n</pattern>
                    </encoder>
                </appender>
                <root level="INFO">
                    <appender-ref ref="STDOUT"/>
                </root>
            </configuration>
            """;

    protected final Project project;
    protected final McpServerDefinition definition;

    private final Map<String, String> sourceContents = new LinkedHashMap<>();
    private String metadataContent;
    private String applicationYmlContent;
    private String dockerfileContent;

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
        @NonNls Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("SERVER_NAME", definition.getServerName());
        attributes.put("HTTP_PORT", definition.getHttpPort());

        sourceContents.clear();
        for (Map.Entry<String, String> entry : SOURCE_TEMPLATES.entrySet()) {
            String content = TemplateUtilities.generateCode(project, entry.getValue(), attributes);
            sourceContents.put(SOURCE_ROOT + entry.getKey() + ".java", content);
        }
        metadataContent = TemplateUtilities.generateCode(project, METADATA_TEMPLATE, new Properties());
        applicationYmlContent = TemplateUtilities.generateCode(project, APPLICATION_YML_TEMPLATE, attributes);

        // standalone multi-stage build usable on a CI runner of any target architecture
        attributes.put("GRAALVM_IMAGE_TAG", resolveJavaVersion(project));
        dockerfileContent = TemplateUtilities.generateCode(project, DOCKERFILE_TEMPLATE, attributes);
    }

    @Override
    public Map<String, String> getSourceFiles() {
        Map<String, String> files = new LinkedHashMap<>(sourceContents);
        files.put(LOGBACK_FILE, LOGBACK_XML);
        files.put(APPLICATION_YML_FILE, applicationYmlContent);
        files.put(METADATA_FILE, metadataContent);
        files.put(DOCKERFILE_FILE, dockerfileContent);
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
