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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dbn.mcp.build.McpJavaVersionManager.resolveJavaVersion;
import static com.dbn.nls.NlsResources.txt;

/**
 * Generates the "Standard Java" MCP server: a single main class packaged
 * as a shaded runnable JAR (current default behavior).
 */
class McpStandardJavaGenerator implements McpServerGenerator {
    private static final @NonNls String MAIN_TEMPLATE = "DBN - MCP Server Main";
    private static final @NonNls String POM_TEMPLATE = "DBN - MCP Server POM.xml";
    private static final @NonNls String MCP_SDK = "io.modelcontextprotocol.sdk:mcp:1.1.1";
    private static final @NonNls String JDBC = "com.oracle.database.jdbc:ojdbc11:23.26.1.0.0";

    private static final Pattern PKG = Pattern.compile("\\bpackage\\s+([a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)*)\\s*;");
    private static final Pattern PUB_CLASS = Pattern.compile("\\bpublic\\s+class\\s+([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern ANY_CLASS = Pattern.compile("\\bclass\\s+([A-Za-z_][A-Za-z0-9_]*)");

    private final Project project;
    private final McpServerDefinition definition;

    private String mainClassContent;
    private ServerMainClass mainClass;

    McpStandardJavaGenerator(@NotNull Project project, @NotNull McpServerDefinition definition) {
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
        mainClassContent = TemplateUtilities.generateCode(project, MAIN_TEMPLATE, properties);
        mainClass = resolveServerMainClass(mainClassContent);
    }

    @Override
    public Map<String, String> getSourceFiles() {
        String packageDir = mainClass.packageName == null ? "" : mainClass.packageName.replace('.', '/') + "/";
        return Map.of("src/main/java/" + packageDir + mainClass.className + ".java", mainClassContent);
    }

    @Override
    public String getPomTemplateName() {
        return POM_TEMPLATE;
    }

    @Override
    public Properties getPomProperties() {
        MavenCoordinate sdk = MavenCoordinate.parse(MCP_SDK);
        MavenCoordinate jdbc = MavenCoordinate.parse(JDBC);

        Properties properties = new Properties();
        properties.setProperty("SERVER_NAME", definition.getServerName());
        properties.setProperty("MCP_SDK_GROUP_ID", sdk.groupId);
        properties.setProperty("MCP_SDK_ARTIFACT_ID", sdk.artifactId);
        properties.setProperty("MCP_SDK_VERSION", sdk.version);
        properties.setProperty("JDBC_GROUP_ID", jdbc.groupId);
        properties.setProperty("JDBC_ARTIFACT_ID", jdbc.artifactId);
        properties.setProperty("JDBC_VERSION", jdbc.version);
        properties.setProperty("MAIN_CLASS_FQ", mainClass.fullyQualifiedName);
        properties.setProperty("PROJECT_JAVA_VERSION", resolveJavaVersion(project, definition.getImplementation()));
        return properties;
    }

    @Override
    public List<String> getMavenGoals() {
        return List.of("clean", "package");
    }

    @Override
    public Path locateArtifact(Path targetDirectory) throws IOException {
        try (var stream = Files.list(targetDirectory)) {
            return stream.filter(p -> p.toString().endsWith(".jar") && !p.toString().contains("original"))
                    .findFirst().orElseThrow(() -> new IOException(txt("msg.mcp.exception.JarNotFound")));
        }
    }

    private static ServerMainClass resolveServerMainClass(String src) {
        String packageName = find(PKG, src);
        String className = find(PUB_CLASS, src);
        if (className == null) className = find(ANY_CLASS, src);
        if (className == null) className = "GeneratedMcpServer";
        String fullyQualifiedName = packageName != null ? packageName + "." + className : className;
        return new ServerMainClass(packageName, className, fullyQualifiedName);
    }

    private static String find(Pattern p, String src) {
        Matcher m = p.matcher(src);
        return m.find() ? m.group(1) : null;
    }

    private record MavenCoordinate(String groupId, String artifactId, String version) {
        static MavenCoordinate parse(String s) {
            String[] p = s.split(":");
            return new MavenCoordinate(p[0], p[1], p[2]);
        }
    }

    private record ServerMainClass(String packageName, String className, String fullyQualifiedName) {}
}
