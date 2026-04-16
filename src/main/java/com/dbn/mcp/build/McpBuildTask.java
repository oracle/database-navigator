package com.dbn.mcp.build;

import com.dbn.common.database.DatabaseInfo;
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
    private static final String MCP_SDK = "io.modelcontextprotocol.sdk:mcp:0.12.1";
    private static final String JDBC = "com.oracle.database.jdbc:ojdbc11:23.26.1.0.0";

    private final Project project;
    private final ConnectionHandler connection;
    private final String serverName;
    private final List<ToolDefinitionModel> tools;

    public McpBuildTask(Project project, ConnectionHandler connection, String serverName, List<ToolDefinitionModel> tools) {
        this.project = project;
        this.connection = connection;
        this.serverName = McpServerName.normalize(serverName);
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
        String yaml = buildYaml(serverName);
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

            String descriptor = safe(profile.getDescriptor()).replaceAll("\\s", "");
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
                Path sourceProjectDir = serverOutputDir.resolve(SOURCE_PROJECT);
                Path tempJar = McpMavenBuild.buildWithMaven(
                        project,
                        cfg.getDir().resolve(DIST),
                        serverName,
                        MCP_SDK,
                        JDBC,
                        template,
                        cfg.getFile(),
                        sourceProjectDir,
                        useWrapper,
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
            throw new IOException("Failed to create Oracle SEPS wallet: " + e.getMessage(), e);
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
        String sourceProjectPath = serverOutputDir.resolve(SOURCE_PROJECT).toAbsolutePath().toString();
        Dialogs.show(() -> new McpBuildResultDialog(project, configPath, path, walletPath, sourceProjectPath,
                buildJson(serverName, path, true), buildJson(serverName, path, false)));
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
