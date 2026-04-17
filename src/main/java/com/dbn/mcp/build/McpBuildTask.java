package com.dbn.mcp.build;

import com.dbn.common.database.DatabaseInfo;
import com.dbn.common.exception.Exceptions;
import com.dbn.common.template.TemplateUtilities;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Json;
import com.dbn.common.util.Messages;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionUtil;
import com.dbn.connection.DatabaseUrlPattern;
import com.dbn.connection.DatabaseUrlType;
import com.dbn.connection.config.tns.TnsNamesParser;
import com.dbn.connection.config.tns.TnsProfile;
import com.dbn.mcp.model.McpTransportType;
import com.dbn.mcp.model.OracleSecretStore;
import com.dbn.mcp.model.OracleWallet;
import com.dbn.mcp.model.ParamRow;
import com.dbn.mcp.model.ToolDefinitionModel;
import com.dbn.mcp.util.McpServerName;
import com.dbn.mcp.util.SqlParameterParser;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.sql.Driver;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class McpBuildTask {
    private static final String DEFAULT_SEPS_USERNAME = "oracle.security.client.default_username";
    private static final String DEFAULT_SEPS_PASSWORD = "oracle.security.client.default_password";
    private static final String TEMPLATE = "DBN - MCP Server Main";
    private static final String CONFIG = "mcp-config.yaml";
    private static final String DIST = "mcp-dist";
    private static final String SOURCE_PROJECT = "source-project";
    private static final String MCP_SDK = "io.modelcontextprotocol.sdk:mcp:1.1.1";
    private static final String JDBC = "com.oracle.database.jdbc:ojdbc11:23.26.1.0.0";

    private final Project project;
    private final ConnectionHandler connection;
    private final String serverName;
    private final McpTransportType transportType;
    private final int httpPort;
    private final List<ToolDefinitionModel> tools;

    public McpBuildTask(Project project, ConnectionHandler connection, String serverName, McpTransportType transportType, int httpPort, List<ToolDefinitionModel> tools) {
        this.project = project;
        this.connection = connection;
        this.serverName = McpServerName.normalize(serverName);
        this.transportType = transportType == null ? McpTransportType.STDIO : transportType;
        this.httpPort = httpPort >= 1 && httpPort <= 65535 ? httpPort : 8080;
        this.tools = tools;
    }

    public void execute() {
        String serverNameError = McpServerName.validationError(serverName);
        if (serverNameError != null) {
            showError(serverNameError);
            return;
        }

        try {
            resolveUrl(); // fail fast before any dialog or file writing
        } catch (UnsupportedOperationException e) {
            showError(e.getMessage());
            return;
        }

        Properties templateProps = new Properties();
        templateProps.setProperty("SERVER_NAME", serverName);
        String template = TemplateUtilities.generateCode(project, TEMPLATE, templateProps);

        Path basePath = resolveBasePath();
        Path distPath = basePath.resolve(DIST).toAbsolutePath().normalize();
        Path serverOutputDir = distPath.resolve(serverName).normalize();
        if (!serverOutputDir.startsWith(distPath)) {
            showError("Invalid server name. Please choose a different name.");
            return;
        }

        if (Files.exists(serverOutputDir)) {
            int option = Messages.showConfirmationDialog(project,
                    "Override Existing Server",
                    "An MCP server named '" + serverName + "' already exists.\nDo you want to override it?",
                    new String[]{"Override", "Cancel"}, 0);
            if (option != 0) return;
        }
        McpBuildConfig config;
        try {
            config = createConfig();
        } catch (IOException e) {
            log.error("Failed to write MCP config", e);
            showError("Config write failed: " + e.getMessage());
            return;
        }
        build(config, template, serverOutputDir);
    }

    private Path resolveBasePath() {
        return project != null && project.getBasePath() != null
                ? Paths.get(project.getBasePath())
                : Paths.get(System.getProperty("user.home"));
    }

    private McpBuildConfig createConfig() throws IOException {
        Path dir = resolveBasePath();
        String yaml = buildYaml();
        Path configFile = dir.resolve(CONFIG);

        Files.createDirectories(dir);
        Files.write(configFile, yaml.getBytes(StandardCharsets.UTF_8));

        return new McpBuildConfig(dir, configFile, serverName);
    }

    private String resolveUrl() {
        DatabaseInfo info = connection.getDatabaseInfo();
        DatabaseUrlType urlType = info.getUrlType();

        if (urlType == DatabaseUrlType.TNS) {
            return resolveTnsDescriptorUrl(info);
        }
        if (urlType == DatabaseUrlType.CUSTOM) {
            return safe(info.getUrl());
        }

        DatabaseUrlPattern pattern = DatabaseUrlPattern.get(connection.getDatabaseType(), urlType);
        return pattern.buildUrl(info);
    }

    private String resolveTnsDescriptorUrl(DatabaseInfo info) {
        String tnsFolder = safe(info.ensureTnsFolder());
        String tnsProfile = safe(info.getTnsProfile());

        if (Strings.isEmptyOrSpaces(tnsFolder)) {
            throw new UnsupportedOperationException("TNS folder is not configured for this connection.");
        }
        if (Strings.isEmptyOrSpaces(tnsProfile)) {
            throw new UnsupportedOperationException("TNS profile is not configured for this connection.");
        }

        File tnsFile = Paths.get(tnsFolder, "tnsnames.ora").toFile();
        if (!tnsFile.isFile()) {
            throw new UnsupportedOperationException("TNS file not found: " + tnsFile.getAbsolutePath());
        }

        try {
            TnsProfile profile = TnsNamesParser.get(tnsFile).getProfiles().stream()
                    .filter(p -> p.getProfile().equalsIgnoreCase(tnsProfile))
                    .findFirst()
                    .orElseThrow(() -> new UnsupportedOperationException(
                            "TNS profile '" + tnsProfile + "' not found in " + tnsFile.getAbsolutePath()));

            String descriptor = safe(profile.getDescriptor()).trim();
            if (descriptor.isEmpty()) {
                throw new UnsupportedOperationException("TNS profile '" + tnsProfile + "' has an empty descriptor.");
            }
            return "jdbc:oracle:thin:@" + descriptor;
        } catch (UnsupportedOperationException e) {
            throw e;
        } catch (Exception e) {
            throw new UnsupportedOperationException("Failed to parse TNS file: " + tnsFile.getAbsolutePath(), e);
        }
    }

    private String buildYaml() {
        StringBuilder sb = new StringBuilder();

        sb.append("transport: ").append(yamlValue(transportType.isHttp() ? "http" : "stdio")).append('\n');
        sb.append("httpPort: ").append(httpPort).append("  # used when transport is http").append('\n');
        sb.append('\n');

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

    private void build(McpBuildConfig cfg, String template, Path serverOutputDir) {
        if (!McpMavenBuild.isMavenPluginAvailable()) {
            int option = Messages.showConfirmationDialog(project,
                    "Maven Plugin Required",
                    "This feature requires the Maven plugin (org.jetbrains.idea.maven).\n" +
                    "Please enable or install it from IDE Plugins settings.",
                    new String[]{"Open Plugins", "Cancel"}, 0);
            if (option == 0) {
                McpMavenBuild.openMavenPluginSettings(project);
            }
            return;
        }

        if (!McpMavenBuild.isMavenAvailable(project)) {
            int option = Messages.showConfirmationDialog(project,
                    "Maven Required",
                    "Maven runtime is not available or invalid in IDE Maven settings.\n" +
                    "Please verify Maven settings and try again.",
                    new String[]{"Open Plugins", "Cancel"}, 0);
            if (option == 0) {
                McpMavenBuild.openMavenPluginSettings(project);
            }
            return;
        }
        runBuild(cfg, template, serverOutputDir);
    }

    private void runBuild(McpBuildConfig cfg, String template, Path serverOutputDir) {
        Progress.prompt(project, null, true, "Building MCP Server", "Maven build...", indicator -> {
            indicator.setIndeterminate(true);
            try {
                Path sourceProjectDir = serverOutputDir.resolve(SOURCE_PROJECT);
                Path tempJar = McpMavenBuild.buildWithMaven(
                        project,
                        cfg.getDir().resolve(DIST),
                        serverName,
                        MCP_SDK,
                        JDBC,
                        template,
                        sourceProjectDir,
                        indicator,
                        logger(indicator));
                Files.createDirectories(serverOutputDir);
                Path finalJar = serverOutputDir.resolve(tempJar.getFileName());
                Files.move(tempJar, finalJar, StandardCopyOption.REPLACE_EXISTING);
                Files.copy(cfg.getFile(), serverOutputDir.resolve(CONFIG), StandardCopyOption.REPLACE_EXISTING);
                Files.deleteIfExists(serverOutputDir.resolve("Main.java"));
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

        char[] user = safe(connection.getUserName()).toCharArray();
        char[] pwd = getPassword(connection);

        // Random password — used only to create ewallet.p12, never stored or shown.
        // cwallet.sso (used at runtime) needs no password.
        char[] walletPassword = generateWalletPassword();

        try {
            ClassLoader classLoader = getWalletClassLoader();
            OracleWallet wallet = OracleWallet.newInstance(classLoader);
            wallet.create(walletPassword);
            wallet.setLocation(walletDir.toAbsolutePath().toString());

            OracleSecretStore store = wallet.getSecretStore();
            // Use documented default SEPS keys to avoid connect-string lookup mismatches.
            store.setSecret(DEFAULT_SEPS_USERNAME, user);
            store.setSecret(DEFAULT_SEPS_PASSWORD, pwd);
            wallet.setSecretStore(store);

            wallet.save();
            wallet.saveSSO();
        } catch (Exception e) {
            Throwable root = Exceptions.rootCauseOf(Exceptions.unwrap(e));
            String message = root != null && root.getMessage() != null && !root.getMessage().isBlank()
                    ? root.getMessage()
                    : e.getClass().getSimpleName();
            throw new IOException("Failed to create Oracle SEPS wallet: " + message, e);
        } finally {
            Arrays.fill(walletPassword, '\0');
            Arrays.fill(user, '\0');
            Arrays.fill(pwd, '\0');
        }
    }

    private ClassLoader getWalletClassLoader() throws Exception {
        ClassLoader driverClassLoader = getDriverClassLoader();
        if (driverClassLoader != null && containsClass(driverClassLoader, "oracle.security.pki.OracleWallet")) {
            return driverClassLoader;
        }

        throw new ClassNotFoundException(
                "oracle.security.pki.OracleWallet is not available in the selected Oracle driver bundle. " +
                "Please add oraclepki to the driver libraries for this connection.");
    }

    private static boolean containsClass(ClassLoader classLoader, String className) {
        try {
            Class.forName(className, false, classLoader);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private ClassLoader getDriverClassLoader() throws Exception {
        Driver driver = ConnectionUtil.resolveDriver(connection.getSettings().getDatabaseSettings());
        return driver == null ? null : driver.getClass().getClassLoader();
    }

    private static char[] generateWalletPassword() {
        SecureRandom rng = new SecureRandom();
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String chars = letters + digits;
        char[] password = new char[32];
        password[0] = letters.charAt(rng.nextInt(letters.length()));
        password[1] = digits.charAt(rng.nextInt(digits.length()));
        for (int i = 2; i < password.length; i++) {
            password[i] = chars.charAt(rng.nextInt(chars.length()));
        }
        for (int i = password.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            char tmp = password[i];
            password[i] = password[j];
            password[j] = tmp;
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
            context.put("HTTP_PORT", Integer.toString(httpPort));
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
        String sourceProjectPath = serverOutputDir.resolve(SOURCE_PROJECT).toAbsolutePath().toString();
        String claudeSnippetJson = buildClaudeJson(serverName, path);
        String clineSnippetJson = transportType.isHttp() ? buildClineJson(serverName) : null;
        Dialogs.show(() -> new McpBuildResultDialog(project, configPath, path, walletPath, sourceProjectPath, transportType.isHttp(),
                claudeSnippetJson, clineSnippetJson));
    }

    private void showError(String msg) {
        Messages.showErrorDialog(project, "MCP Build Error", msg);
    }

    private static char[] getPassword(ConnectionHandler conn) {
        if (conn == null || conn.getAuthenticationInfo() == null) return new char[0];
        char[] pwd = conn.getAuthenticationInfo().getPassword();
        return pwd != null ? pwd.clone() : new char[0];
    }

    private String safe(String v) { return v != null ? v : ""; }
    private String safe(String v, String d) { return v != null && !v.isEmpty() ? v : d; }

    private String buildClaudeJson(String name, String jar) {
        String command;
        List<String> args;
        if (transportType.isHttp()) {
            command = "npx";
            args = List.of("-y", "mcp-remote", "http://127.0.0.1:" + httpPort + "/mcp");
        } else {
            command = "java";
            args = List.of("-jar", jar);
        }
        return buildCommandSnippetJson(name, command, args);
    }

    private String buildClineJson(String name) {
        Map<String, Object> server = new LinkedHashMap<>();
        server.put("type", "streamableHttp");
        server.put("url", "http://127.0.0.1:" + httpPort + "/mcp");
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put(name, server);
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
