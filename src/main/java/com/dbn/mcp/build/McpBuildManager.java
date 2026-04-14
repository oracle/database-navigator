package com.dbn.mcp.build;

import com.dbn.common.database.DatabaseInfo;
import com.dbn.common.template.TemplateUtilities;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Json;
import com.dbn.common.util.Messages;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.DatabaseUrlPattern;
import com.dbn.connection.DatabaseUrlType;
import com.dbn.mcp.model.ParamRow;
import com.dbn.mcp.model.ToolDefinitionModel;
import com.dbn.mcp.util.SqlParameterParser;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import oracle.security.pki.OracleSecretStore;
import oracle.security.pki.OracleWallet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class McpBuildManager {
    private static final String TEMPLATE = "DBN - MCP Server Main";
    private static final String CONFIG = "mcp-config.yaml";
    private static final String DIST = "mcp-dist";
    private static final String MCP_SDK = "io.modelcontextprotocol.sdk:mcp:0.12.1";
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
        try {
            resolveUrl(); // fail fast before any dialog or file writing
        } catch (UnsupportedOperationException e) {
            showError(e.getMessage());
            return;
        }

        Properties templateProps = new Properties();
        templateProps.setProperty("SERVER_NAME", serverName);
        String template = TemplateUtilities.generateCode(project, TEMPLATE, templateProps);

        Path basePath = project != null ? Paths.get(project.getBasePath()) : Paths.get(System.getProperty("user.home"));
        Path serverOutputDir = basePath.resolve(DIST).resolve(serverName);

        if (Files.exists(serverOutputDir)) {
            int option = Messages.showConfirmationDialog(project,
                    "Override Existing Server",
                    "An MCP server named '" + serverName + "' already exists.\nDo you want to override it?",
                    new String[]{"Override", "Cancel"}, 0);
            if (option != 0) return;
        }
        build(createConfig(), template, serverOutputDir);
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

    private String resolveUrl() {
        DatabaseInfo info = connection.getDatabaseInfo();
        connection.getSettings().getDatabaseSettings().getConnectionUrl();
        DatabaseUrlType urlType = info.getUrlType();
        //todo get connection url from database setting
      // but for the tns take the tns build the url based
//      info.getTnsFolder()
//      connection.getSettings().getDatabaseSettings().get
//      DatabaseUrlPattern.ORACLE_TNS.buildUrl(null,null,null,null,null,info.getTnsFolder(),"",null,null)

        if (urlType == DatabaseUrlType.TNS) {
            throw new UnsupportedOperationException(
                    "TNS alias connections are not yet supported by the MCP Builder.\n" +
                    "Please switch to a direct connection (EZConnect, SID, or Service Name) and rebuild.");
        }
        if (urlType == DatabaseUrlType.LDAP || urlType == DatabaseUrlType.LDAPS) {
            throw new UnsupportedOperationException(
                    "LDAP connections are not yet supported by the MCP Builder.\n" +
                    "Please switch to a direct connection (EZConnect, SID, or Service Name) and rebuild.");
        }

        if (urlType == DatabaseUrlType.CUSTOM) {
            return safe(info.getUrl());
        }

        DatabaseUrlPattern pattern = DatabaseUrlPattern.get(connection.getDatabaseType(), urlType);
        return pattern.buildUrl(info);
    }

    private String buildYaml(String serverName) {
        StringBuilder sb = new StringBuilder();

        sb.append("dataSource:\n");
        sb.append("  url: ").append(yamlValue(resolveUrl())).append('\n');
        sb.append("  # username: YOUR_USER  # uncomment to override wallet credentials\n");
        sb.append("  # password: YOUR_PASS  # uncomment to override wallet credentials\n");
        sb.append('\n');

        sb.append("tools:\n");
        for (ToolDefinitionModel t : tools) {
            sb.append("  ").append(safe(t.getName(), "tool")).append(":\n");
            sb.append("    description: ").append(yamlValue(safe(t.getDescription(), "SQL tool"))).append('\n');
            sb.append("    statement: ").append(yamlValue(safe(t.getStatement(), "SELECT 1 FROM dual"))).append('\n');

            List<ParamRow> params = t.getParamsModel() != null ? t.getParamsModel().getRows() : List.of();
            if (!params.isEmpty()) {
                sb.append("    parameters:\n");
                for (ParamRow row : params) {
                    sb.append("      - name: ").append(SqlParameterParser.stripColon(row.getName())).append('\n');
                    sb.append("        type: ").append(row.getType().getYamlType()).append('\n');
                    if (Strings.isNotEmpty(row.getDescription()))
                        sb.append("        description: ").append(yamlValue(row.getDescription())).append('\n');
                    sb.append("        required: ").append(row.isRequired()).append('\n');
                }
            }
        }

        return sb.toString();
    }

    private static String yamlValue(String v) {
        if (v == null || v.isEmpty()) return "\"\"";
        boolean needsQuotes = v.contains(":") || v.contains("#") || v.contains("\"")
                || v.contains("'") || v.contains("\n") || v.contains("{") || v.contains("}")
                || v.contains("[") || v.contains("]") || v.startsWith(" ") || v.endsWith(" ");
        if (!needsQuotes) return v;
        return "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    private static boolean wrapperApproved = false;

    private void build(McpBuildConfig cfg, String template, Path serverOutputDir) {
//      PluginManager pm = PluginManager.getInstance();
//      PluginManager.isPluginInstalled(PluginId.getId());
        if (!wrapperApproved && !McpMavenBuild.isMavenAvailable()) {
            int option = Messages.showConfirmationDialog(project,
                    "Maven Required",
                    "Maven was not found on this system.\n" +
                    "Would you like to download Maven Wrapper (~10MB) to build the MCP server?",
                    new String[]{"Download", "Cancel"}, 0);
            if (option != 0) return;
            wrapperApproved = true;
        }
        runBuild(cfg, template, serverOutputDir, wrapperApproved);
    }

    private void runBuild(McpBuildConfig cfg, String template, Path serverOutputDir, boolean useWrapper) {
        Progress.prompt(project, null, true, "Building MCP Server", "Maven build...", indicator -> {
            indicator.setIndeterminate(true);
            try {
                Path tempJar = McpMavenBuild.buildWithMaven(project, cfg.getDir().resolve(DIST), serverName, MCP_SDK, JDBC, template, cfg.getFile(), useWrapper, logger(indicator));
                Files.createDirectories(serverOutputDir);
                Path finalJar = serverOutputDir.resolve(tempJar.getFileName());
                Files.move(tempJar, finalJar, StandardCopyOption.REPLACE_EXISTING);
                Files.copy(cfg.getFile(), serverOutputDir.resolve(CONFIG), StandardCopyOption.REPLACE_EXISTING);
                Files.writeString(serverOutputDir.resolve("Main.java"), template, StandardCharsets.UTF_8);
                createWallet(serverOutputDir);
                writeReadme(serverOutputDir);
                showResult(serverOutputDir, finalJar);
            } catch (Throwable e) {
                log.error("MCP build failed", e);
                showError("Build failed: " + e.getMessage());
            }
        });
    }

    private void createWallet(Path dir) throws IOException {
        Path walletDir = dir.resolve("wallet");
        Files.createDirectories(walletDir);

        String user = safe(connection.getUserName());
        char[] pwd  = getPassword(connection).toCharArray();

        // Random password — used only to create ewallet.p12, never stored or shown.
        // cwallet.sso (used at runtime) needs no password.
        char[] walletPassword = generateWalletPassword();

        try {
            OracleWallet wallet = new OracleWallet();
            wallet.create(walletPassword);
            wallet.setLocation(walletDir.toAbsolutePath().toString());

            OracleSecretStore store = wallet.getSecretStore();
            // Fixed keys — no URL coupling, credentials are decoupled from connection string
            store.setSecret("mcp.username", user.toCharArray());
            store.setSecret("mcp.password", pwd);
            wallet.setSecretStore(store);

            wallet.save();
            wallet.saveSSO();
        } catch (Exception e) {
            throw new IOException("Failed to create Oracle SEPS wallet: " + e.getMessage(), e);
        } finally {
            java.util.Arrays.fill(walletPassword, '\0');
        }
    }

    private static char[] generateWalletPassword() {
        SecureRandom rng = new SecureRandom();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        char[] password = new char[32];
        for (int i = 0; i < password.length; i++) {
            password[i] = chars.charAt(rng.nextInt(chars.length()));
        }
        return password;
    }

    private void writeReadme(Path dir) {
        try {
            List<Map<String, String>> toolList = tools.stream()
                    .map(t -> Map.of("name", safe(t.getName(), "tool"), "description", safe(t.getDescription(), "SQL tool")))
                    .collect(Collectors.toList());
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("SERVER_NAME", serverName);
            context.put("JAR_NAME", serverName + ".jar");
            context.put("TOOLS", toolList);
            String content = TemplateUtilities.generateCode(project, "DBN - MCP Server README", context);
            Files.writeString(dir.resolve("README.md"), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to write README", e);
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

    private void showResult(Path serverOutputDir, Path jar) {
        String path = jar.toAbsolutePath().toString();
        String configPath = serverOutputDir.resolve(CONFIG).toAbsolutePath().toString();
        String walletPath = serverOutputDir.resolve("wallet").toAbsolutePath().toString();
        Dialogs.show(() -> new McpBuildResultDialog(project, configPath, path, walletPath,
                buildJson(serverName, path, true), buildJson(serverName, path, false)));
    }

    private void showError(String msg) {
        Messages.showErrorDialog(project, "MCP Build Error", msg);
    }

    private String getPassword(ConnectionHandler conn) {
        if (conn == null || conn.getAuthenticationInfo() == null) return "";
        char[] pwd = conn.getAuthenticationInfo().getPassword();
        return pwd != null ? new String(pwd) : "";
    }

    private String safe(String v) { return v != null ? v : ""; }
    private String safe(String v, String d) { return v != null && !v.isEmpty() ? v : d; }

    private String buildJson(String name, String jar, boolean full) {
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("command", "java");
        server.put("args", List.of("-jar", jar));
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put(name, server);
        if (full) {
            return Json.writeAsString(Map.of("mcpServers", entry));
        }
        // Fragment: strip outer {} so it pastes directly into an existing mcpServers block
        String json = Json.writeAsString(entry);
        return json.substring(1, json.length() - 1).trim();
    }
}
