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
import com.dbn.mcp.model.McpServerDefinition;
import com.dbn.mcp.model.McpToolDefinition;
import com.dbn.mcp.model.McpToolParam;
import com.dbn.mcp.model.McpTransportType;
import com.dbn.mcp.model.OracleSecretStore;
import com.dbn.mcp.model.OracleWallet;
import com.dbn.mcp.util.McpServerName;
import com.dbn.mcp.util.McpToolDefinitions;
import com.dbn.mcp.util.SqlParameterParser;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
import java.util.stream.Collectors;

import static com.dbn.common.util.Messages.options;
import static com.dbn.common.util.Messages.showErrorDialog;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.mcp.build.McpJavaVersionManager.MIN_JAVA_VERSION;
import static com.dbn.mcp.build.McpMavenPluginSupport.verifyMavenAvailability;
import static com.dbn.nls.NlsResources.txt;

@Slf4j
public class McpBuildTask {
    private static final @NonNls String DEFAULT_SEPS_USERNAME = "oracle.security.client.default_username";
    private static final @NonNls String DEFAULT_SEPS_PASSWORD = "oracle.security.client.default_password";
    private static final @NonNls String TEMPLATE = "DBN - MCP Server Main";
    private static final @NonNls String README_TEMPLATE = "DBN - MCP Server README";
    private static final @NonNls String CONFIG = "mcp-config.yaml";
    private static final @NonNls String DIST = "mcp-dist";
    private static final @NonNls String SOURCE_PROJECT = "source-project";
    private static final @NonNls String MCP_SDK = "io.modelcontextprotocol.sdk:mcp:1.1.1";
    private static final @NonNls String JDBC = "com.oracle.database.jdbc:ojdbc11:23.26.1.0.0";

    private final Project project;
    private final ConnectionHandler connection;
    private final McpServerDefinition definition;
    private final McpBuilderResult result = new McpBuilderResult();

    public McpBuildTask(Project project, ConnectionHandler connection, McpServerDefinition definition) {
        this.project = project;
        this.connection = connection;
        this.definition = definition;
    }

    public void execute(Runnable onInitSuccess, Runnable onBuildFailure) {
        Progress.prompt(project, null, true,
                txt("prc.mcp.title.BuildingMcpServer"),
                txt("prc.mcp.text.VerifyingBuildPrerequisites"),
                indicator -> {
            verifyBuilt(indicator);
            onInitSuccess.run();

            buildServerPackage(onBuildFailure);
        });
    }

    private void verifyBuilt(ProgressIndicator indicator) {
        indicator.setText2(txt("prc.mcp.text.VerifyingServerDefinition"));
        verifyServerDefinition();

        indicator.setText2(txt("prc.mcp.text.VerifyingMavenAvailability"));
        verifyMavenAvailability(project);

        indicator.setText2(txt("prc.mcp.text.VerifyingProjectJavaVersion"));
        verifyJavaVersion(project);

        indicator.setText2(txt("prc.mcp.text.VerifyingConnectionUrl"));
        verifyConnectionUrl();

        indicator.setText2(txt("prc.mcp.text.InitializingOutputDirectory"));
        initOutputDirectory();

        indicator.setText2(txt("prc.mcp.text.PreparingServerConfigurationContent"));
        initServerConfig();

        indicator.setText2(txt("prc.mcp.text.PreparingMainClassContent"));
        initMainClassContent();
    }

    public static void verifyJavaVersion(@NotNull Project project) {
        try {
            McpJavaVersionManager manager = McpJavaVersionManager.getInstance(project);
            if (manager == null) return;

            String javaVersion = manager.getConfiguredProjectJavaVersion();
            if (javaVersion == null) return;

            int feature = Integer.parseInt(javaVersion);
            if (feature < MIN_JAVA_VERSION) {
                showErrorDialog(project,
                        txt("msg.mcp.title.McpBuildError"),
                        txt("msg.mcp.error.JdkVersionRequired", MIN_JAVA_VERSION, javaVersion));
                cancelProcess();
            }
        } catch (ProcessCanceledException e) {
            throw e;
        } catch (Exception e) {
            conditionallyLog(e);
            showErrorDialog(project, txt("msg.mcp.title.McpBuildError"), txt("msg.mcp.error.JavaVersionVerificationFailed"), e);
            cancelProcess();
        }
    }

    private void verifyServerDefinition() {
        String serverName = definition.getServerName();
        String serverNameError = McpServerName.validationError(serverName);
        if (serverNameError != null) {
            showErrorDialog(project, txt("msg.mcp.title.McpBuildError"), serverNameError);
            cancelProcess();
        }
        String toolValidationError = McpToolDefinitions.validationError(definition.getTools());
        if (toolValidationError != null) {
            showErrorDialog(project, txt("msg.mcp.title.McpBuildError"), toolValidationError);
            cancelProcess();
        }
    }

    private void verifyConnectionUrl() {
        try {
            resolveConnectionUrl(); // fail fast before any dialog or file writing
        } catch (UnsupportedOperationException e) {
            showErrorDialog(project, txt("msg.mcp.title.McpBuildError"), e);
            cancelProcess();
        }
    }

    private void initOutputDirectory() {
        String serverName = definition.getServerName();
        Path basePath = resolveBasePath();
        Path distPath = basePath.resolve(DIST).toAbsolutePath().normalize();
        Path outputDirectory = distPath.resolve(serverName).normalize();
        if (!outputDirectory.startsWith(distPath)) {
            showErrorDialog(project, txt("msg.mcp.title.McpBuildError"), txt("msg.mcp.error.InvalidServerName"));
            cancelProcess();
        }

        result.setOutputDirectory(outputDirectory);
        if (Files.exists(outputDirectory)) {
            int option = Messages.showConfirmationDialog(project,
                    txt("msg.mcp.title.OverrideExistingServer"),
                    txt("msg.mcp.question.OverrideExistingServer", serverName),
                    options(txt("msg.mcp.button.Override"), txt("msg.shared.button.Cancel")), 0);
            if (option != 0) {
                cancelProcess();
            }
        }
    }

    @Nullable
    private void initServerConfig() {
        try {
            Path baseDirectory = resolveBasePath();
            String yaml = buildYaml();
            Path configFile = baseDirectory.resolve(CONFIG);

            Files.createDirectories(baseDirectory);
            Files.write(configFile, yaml.getBytes(StandardCharsets.UTF_8));

            result.setBaseDirectory(baseDirectory);
            result.setConfigFile(configFile);
        } catch (Exception e) {
            conditionallyLog(e);
            showErrorDialog(project, txt("msg.mcp.title.McpBuildError"), txt("msg.mcp.error.ConfigFileWriteFailed"), e);
            cancelProcess();
        }
    }

    private void initMainClassContent() {
        try {
            String serverName = definition.getServerName();
            Properties templateProps = new Properties();
            templateProps.setProperty("SERVER_NAME", serverName);
            String mainClassContent = TemplateUtilities.generateCode(project, TEMPLATE, templateProps);
            result.setMainClassContent(mainClassContent);
        } catch (Exception e) {
            conditionallyLog(e);
            showErrorDialog(project, txt("msg.mcp.title.McpBuildError"), txt("msg.mcp.error.MainClassBuildFailed"), e);
            cancelProcess();
        }

    }

    private Path resolveBasePath() {
        return project != null && project.getBasePath() != null
                ? Paths.get(project.getBasePath())
                : Paths.get(System.getProperty("user.home"));
    }

    private String resolveConnectionUrl() {
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
        @NonNls
        StringBuilder sb = new StringBuilder();

        appendYamlField(sb, "", "transport", definition.getTransportType().isHttp() ? "http" : "stdio");
        sb.append("httpPort: ").append(definition.getHttpPort()).append("  # used when transport is http").append('\n');
        sb.append('\n');

        sb.append("dataSource:\n");
        appendYamlField(sb, "  ", "url", resolveConnectionUrl());
        sb.append("  # username: YOUR_USER  # uncomment to override wallet credentials\n");
        sb.append("  # password: YOUR_PASS  # uncomment to override wallet credentials\n");
        sb.append('\n');

        sb.append("tools:\n");
        for (McpToolDefinition t : definition.getTools()) {
            String toolName = t.getName();
            String description = t.getDescription();
            sb.append("  ").append(toolName).append(":\n");
            appendYamlField(sb, "    ", "description", safe(description, "SQL tool"));
            appendYamlField(sb, "    ", "statement", safe(t.getStatement(), "SELECT 1 FROM dual"));

            List<McpToolParam> params = t.getParameters() != null ? t.getParameters() : List.of();
            if (!params.isEmpty()) {
                sb.append("    parameters:\n");
                for (McpToolParam row : params) {
                    sb.append("      - name: ").append(SqlParameterParser.stripColon(row.getName())).append('\n');
                    sb.append("        type: ").append(row.getType().getSchemaType()).append('\n');
                    if (Strings.isNotEmpty(row.getType().getSchemaFormat())) {
                        sb.append("        format: ").append(row.getType().getSchemaFormat()).append('\n');
                    }
                    if (Strings.isNotEmpty(row.getDescription())) {
                        appendYamlField(sb, "        ", "description", row.getDescription());
                    }
                    sb.append("        required: ").append(row.isRequired()).append('\n');
                }
            }
        }

        return sb.toString();
    }

    private static void appendYamlField(StringBuilder sb, String indent, @NonNls String key, @NonNls String value) {
        String normalized = value == null ? "" : value;
        if (normalized.contains("\n")) {
            sb.append(indent).append(key).append(": |").append('\n');
            String[] lines = normalized.split("\\R", -1);
            for (String line : lines) {
                sb.append(indent).append("  ").append(line).append('\n');
            }
        } else {
            sb.append(indent).append(key).append(": ").append(yamlValue(normalized)).append('\n');
        }
    }

    private static String yamlValue(String v) {
        if (v == null || v.isEmpty()) return "\"\"";
        boolean needsQuotes = v.contains(":") || v.contains("#") || v.contains("\"")
                || v.contains("'") || v.contains("{") || v.contains("}")
                || v.contains("[") || v.contains("]") || v.startsWith(" ") || v.endsWith(" ");
        if (!needsQuotes) return v;
        return "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private void buildServerPackage(Runnable onBuildFailure) {
        Progress.prompt(project, null, true,
                txt("prc.mcp.title.BuildingMcpServer"),
                txt("prc.mcp.text.MavenBuild"),
                indicator -> {
            indicator.setIndeterminate(true);
            try {
                indicator.setText2(txt("prc.mcp.text.PreparingProject"));
                Path outputDirectory = result.getOutputDirectory();
                Path sourceDirectory = outputDirectory.resolve(SOURCE_PROJECT).toAbsolutePath().normalize();
                result.setSourceDirectory(sourceDirectory);
                indicator.setText2(txt("prc.mcp.text.RunningMavenBuild"));
                Path tempJar = McpMavenBuilder.build(
                        project,
                        result.getBaseDirectory().resolve(DIST),
                        definition.getServerName(),
                        MCP_SDK,
                        JDBC,
                        result.getMainClassContent(),
                        sourceDirectory,
                        indicator,
                        null);
                indicator.setText2(txt("prc.mcp.text.FinalizingOutput"));
                Files.createDirectories(outputDirectory);
                Path serverJar = outputDirectory.resolve(tempJar.getFileName());
                result.setServerJar(serverJar);

                Files.move(tempJar, serverJar, StandardCopyOption.REPLACE_EXISTING);
                Path outputConfigFile = outputDirectory.resolve(CONFIG).toAbsolutePath().normalize();
                Files.copy(result.getConfigFile(), outputConfigFile, StandardCopyOption.REPLACE_EXISTING);
                result.setConfigFile(outputConfigFile);

                Files.deleteIfExists(outputDirectory.resolve("Main.java"));
                indicator.setText2(txt("prc.mcp.text.CreatingWallet"));
                createWallet(outputDirectory);

                indicator.setText2(txt("prc.mcp.text.WritingReadme"));
                writeReadme(outputDirectory);
                indicator.setText2(txt("prc.mcp.text.Done"));
                showResult();
            } catch (Throwable e) {
                conditionallyLog(e);
                showErrorDialog(project, txt("msg.mcp.title.McpBuildError"), txt("msg.mcp.error.McpServerBuildFailed"), e);
                onBuildFailure.run();
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
            String serverName = definition.getServerName();
            String httpPort = definition.getHttpPort();

            List<McpToolDefinition> tools = definition.getTools();
            List<Map<String, String>> toolList = tools.stream()
                    .map(t -> Map.of(
                            "name", t.getName(),
                            "description", safe(t.getDescription(), "SQL tool")))
                    .collect(Collectors.toList());

            @NonNls Map<String, Object> context = new LinkedHashMap<>();
            context.put("SERVER_NAME", serverName);
            context.put("JAR_NAME", serverName + ".jar");
            context.put("HTTP_PORT", httpPort);
            context.put("TOOLS", toolList);
            String content = TemplateUtilities.generateCode(project, README_TEMPLATE, context);
            Files.writeString(dir.resolve("README.md"), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to write README", e);
        }
    }

    private void showResult() {
        Path outputDirectory = result.getOutputDirectory();

        String serverName = definition.getServerName();
        McpTransportType transportType = definition.getTransportType();

        result.setWalletDirectory(outputDirectory.resolve("wallet").toAbsolutePath().normalize());

        result.setClaudeSnippetJson(buildClaudeJson(serverName, result.getServerJar().toString()));
        result.setClineSnippetJson(transportType.isHttp() ? buildClineJson(serverName) : null);
        Dialogs.show(() -> new McpBuildResultDialog(project, definition, result));
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
        McpTransportType transportType = definition.getTransportType();
        if (transportType.isHttp()) {
            command = "npx";
            String httpPort = definition.getHttpPort();
            args = List.of("-y", "mcp-remote", "http://127.0.0.1:" + httpPort + "/mcp");
        } else {
            command = "java";
            args = List.of("-jar", jar);
        }
        return buildCommandSnippetJson(name, command, args);
    }

    private String buildClineJson(String name) {
        Map<String, Object> server = new LinkedHashMap<>();
        String httpPort = definition.getHttpPort();
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

    private static void cancelProcess() {
        throw new ProcessCanceledException();
    }
}
