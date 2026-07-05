package com.dbn.mcp.build;

import com.dbn.common.template.TemplateUtilities;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.mcp.model.McpServerDefinition;
import com.dbn.mcp.model.McpTransportType;
import com.dbn.mcp.util.McpServerName;
import com.dbn.mcp.util.McpToolDefinitions;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

import static com.dbn.common.util.Messages.options;
import static com.dbn.common.util.Messages.showErrorDialog;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.mcp.build.McpJavaVersionManager.MIN_JAVA_VERSION;
import static com.dbn.mcp.build.McpMavenPluginSupport.verifyMavenAvailability;
import static com.dbn.nls.NlsResources.txt;

public class McpBuildTask {
    private static final @NonNls String TEMPLATE = "DBN - MCP Server Main";
    private static final @NonNls String CONFIG = "mcp-config.yaml";
    private static final @NonNls String DIST = "mcp-dist";
    private static final @NonNls String SOURCE_PROJECT = "source-project";
    private static final @NonNls String MCP_SDK = "io.modelcontextprotocol.sdk:mcp:1.1.1";
    private static final @NonNls String JDBC = "com.oracle.database.jdbc:ojdbc11:23.26.1.0.0";

    private final Project project;
    private final McpServerDefinition definition;
    private final McpBuilderResult result = new McpBuilderResult();
    private final McpServerConfigBuilder serverConfigBuilder;
    private final McpWalletBuilder walletBuilder;
    private final McpReadmeWriter readmeWriter;
    private final McpClientConfiguration clientConfiguration;

    public McpBuildTask(Project project, ConnectionHandler connection, McpServerDefinition definition) {
        this.project = project;
        this.definition = definition;
        this.serverConfigBuilder = new McpServerConfigBuilder(connection, definition);
        this.walletBuilder = new McpWalletBuilder(connection);
        this.readmeWriter = new McpReadmeWriter(project, definition);
        this.clientConfiguration = new McpClientConfiguration(definition);
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
            serverConfigBuilder.getRedactedConnectionUrl(); // fail fast before any dialog or file writing
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
            String yaml = serverConfigBuilder.build();
            Path configFile = baseDirectory.resolve(CONFIG);

            Files.createDirectories(baseDirectory);
            Files.writeString(configFile, yaml, StandardCharsets.UTF_8);

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
                walletBuilder.build(outputDirectory);

                indicator.setText2(txt("prc.mcp.text.WritingReadme"));
                readmeWriter.write(outputDirectory);
                indicator.setText2(txt("prc.mcp.text.Done"));
                showResult();
            } catch (Throwable e) {
                conditionallyLog(e);
                showErrorDialog(project, txt("msg.mcp.title.McpBuildError"), txt("msg.mcp.error.McpServerBuildFailed"), e);
                onBuildFailure.run();
            }
        });
    }

    private void showResult() {
        Path outputDirectory = result.getOutputDirectory();

        McpTransportType transportType = definition.getTransportType();

        result.setWalletDirectory(outputDirectory.resolve("wallet").toAbsolutePath().normalize());

        result.setClaudeSnippetJson(clientConfiguration.buildClaudeJson(result.getServerJar().toString()));
        result.setClineSnippetJson(transportType.isHttp() ? clientConfiguration.buildClineJson() : null);
        Dialogs.show(() -> new McpBuildResultDialog(project, definition, result));
    }

    private static void cancelProcess() {
        throw new ProcessCanceledException();
    }
}
