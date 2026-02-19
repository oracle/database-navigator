package com.dbn.mcp.build;

import com.dbn.common.template.TemplateUtilities;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Json;
import com.dbn.common.util.Messages;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.mcp.model.ParamRow;
import com.dbn.mcp.model.ToolDefinitionModel;
import com.dbn.mcp.util.SqlParameterParser;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

@Slf4j
public class McpBuildManager {
    private static final String TEMPLATE = "DBN - MCP Server Main";
    private static final String CONFIG = "mcp-config.yaml";
    private static final String DIST = "mcp-dist";
    private static final String MCP_SDK = "io.modelcontextprotocol.sdk:mcp:0.11.1";
    private static final String JDBC = "com.oracle.database.jdbc:ojdbc11:23.8.0.25.04";

    private final Project project;
    private final ConnectionHandler connection;
    private final String serverName;
    private final List<ToolDefinitionModel> tools;

    public McpBuildManager(Project project, ConnectionHandler connection, String serverName, List<ToolDefinitionModel> tools) {
        this.project = project;
        this.connection = connection;
        this.serverName = serverName;
        this.tools = tools;
    }

    public void execute() {
        McpBuildConfig cfg = createConfig();
        Properties templateProps = new Properties();
        templateProps.setProperty("SERVER_NAME", serverName);
        String template = TemplateUtilities.generateCode(project, TEMPLATE, templateProps);
        build(cfg, template);
    }

    private McpBuildConfig createConfig() {
        Path dir = project != null ? Paths.get(project.getBasePath()) : Paths.get(System.getProperty("user.home"));
        String yaml = buildYaml(serverName);
        Path configFile = dir.resolve(CONFIG);

        try {
            Files.createDirectories(dir);
            Files.write(configFile, yaml.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("Failed to write MCP config", e);
            Messages.showErrorDialog(project, "Error", "Config write failed: " + e.getMessage());
        }

        return new McpBuildConfig(dir, configFile, serverName);
    }

    private String buildYaml(String serverName) {
        String prefix = toEnvPrefix(serverName);
        StringBuilder sb = new StringBuilder();

        // dataSources section
        sb.append("dataSources:\n");
        sb.append("  default:\n");
        sb.append("    url: ").append(yamlScalar(safe(connection.getConnectionInfo().getUrl()))).append('\n');
        sb.append("    user: ${").append(prefix).append("_DB_USER}\n");
        sb.append("    password: ${").append(prefix).append("_DB_PASSWORD}\n");
        sb.append('\n');

        // tools section
        sb.append("tools:\n");
        for (ToolDefinitionModel t : tools) {
            String toolName = safe(t.getName(), "tool");
            sb.append("  ").append(toolName).append(":\n");
            sb.append("    dataSource: default\n");
            sb.append("    description: ").append(yamlScalar(safe(t.getDescription(), "SQL tool"))).append('\n');
            sb.append("    statement: ").append(yamlScalar(safe(t.getStatement(), "SELECT 1 FROM dual"))).append('\n');

            List<ParamRow> params = t.getParamsModel() != null ? t.getParamsModel().getRows() : List.of();
            if (!params.isEmpty()) {
                sb.append("    parameters:\n");
                for (ParamRow row : params) {
                    String paramName = SqlParameterParser.stripColon(row.getName());
                    sb.append("      - name: ").append(paramName).append('\n');
                    sb.append("        type: ").append(row.getType().getYamlType()).append('\n');
                    if (Strings.isNotEmpty(row.getDescription())) {
                        sb.append("        description: ").append(yamlScalar(row.getDescription())).append('\n');
                    }
                    sb.append("        required: ").append(row.isRequired()).append('\n');
                }
            }
        }

        return sb.toString();
    }

    private String yamlScalar(String value) {
        if (value == null || value.isEmpty()) return "\"\"";
        if (value.contains(":") || value.contains("#") || value.contains("'") ||
                value.contains("\"") || value.contains("\n") || value.contains("{") ||
                value.contains("}") || value.contains("[") || value.contains("]") ||
                value.startsWith(" ") || value.endsWith(" ")) {
            return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
        }
        return value;
    }

    private static boolean wrapperApproved = false;

    private void build(McpBuildConfig cfg, String template) {
        if (wrapperApproved || McpMavenBuild.isMavenAvailable()) {
            runBuild(cfg, template, wrapperApproved);
        } else {
            Messages.showQuestionDialog(project,
                    "Maven Required",
                    "Maven was not found on this system.\n" +
                    "Would you like to download Maven Wrapper (~10MB) to build the MCP server?",
                    new String[]{"Download", "Cancel"}, 0,
                    option -> {
                        if (option != 0) return;
                        wrapperApproved = true;
                        runBuild(cfg, template, true);
                    });
        }
    }

    private void runBuild(McpBuildConfig cfg, String template, boolean useWrapper) {
        Progress.prompt(project, null, true, "Building MCP Server", "Maven build...", indicator -> {
            indicator.setIndeterminate(true);
            try {
                Path jar = McpMavenBuild.buildWithMaven(project, cfg.getDir().resolve(DIST), serverName, MCP_SDK, JDBC, template, cfg.getFile(), useWrapper, logger(indicator));
                writeEnvFile(jar.getParent());
                showResult(cfg, jar);
            } catch (Exception e) {
                log.error("MCP build failed", e);
                showError("Build failed: " + e.getMessage());
            }
        });
    }

    private void writeEnvFile(Path dir) {
        try {
            String prefix = toEnvPrefix(serverName);
            String encodedPassword = "base64:" + Base64.getEncoder().encodeToString(
                    getPassword(connection).getBytes(StandardCharsets.UTF_8));
            String content = prefix + "_DB_USER=" + safe(connection.getUserName()) + "\n" +
                             prefix + "_DB_PASSWORD=" + encodedPassword + "\n";
            Files.writeString(dir.resolve(serverName + ".env"), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to write .env file", e);
        }
    }

    private Consumer<String> logger(ProgressIndicator ind) {
        long[] last = {0L};
        return line -> {
            if (line == null || System.nanoTime() - last[0] < 50_000_000L) return;
            last[0] = System.nanoTime();
            String t = line.replaceAll("\\p{Cntrl}", " ").replaceAll("\\s+", " ").trim();
            ind.setText2(t.length() > 140 ? t.substring(0, 140) + "…" : t);
        };
    }

    private void showResult(McpBuildConfig cfg, Path jar) {
        String name = cfg.getServerName();
        String path = jar.toAbsolutePath().toString();
        String envPath = jar.getParent().resolve(serverName + ".env").toAbsolutePath().toString();
        Dialogs.show(() -> new McpBuildResultDialog(project, cfg.getFile().toAbsolutePath().toString(), path, envPath,
                buildJson(name, path, true), buildJson(name, path, false)));
    }

    private void showError(String msg) {
        Messages.showErrorDialog(project, "MCP Build Error", msg);
    }

    private String getPassword(ConnectionHandler conn) {
        if (conn == null || conn.getAuthenticationInfo() == null) return "";
        char[] pwd = conn.getAuthenticationInfo().getPassword();
        return pwd != null ? new String(pwd) : "";
    }

    private static String toEnvPrefix(String name) {
        return name.toUpperCase().replaceAll("[^A-Z0-9]", "_");
    }

    private String safe(String v) { return v != null ? v : ""; }
    private String safe(String v, String d) { return v != null && !v.isEmpty() ? v : d; }

    private String buildJson(String name, String jar, boolean full) {
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("command", "java");
        server.put("args", List.of("-jar", jar));
        Map<String, Object> servers = Map.of(name, server);
        return Json.writeAsString(full ? Map.of("mcpServers", servers) : servers);
    }
}
