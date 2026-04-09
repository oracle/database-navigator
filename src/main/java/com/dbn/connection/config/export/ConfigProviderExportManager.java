package com.dbn.connection.config.export;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.ApplicationComponentBase;
import com.dbn.common.component.PersistentState;
import com.dbn.common.state.StateAttributes;
import com.dbn.common.state.StateCategory;
import com.dbn.common.state.StateContainer;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Messages;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.config.export.ui.ConfigProviderExportDialog;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.dbn.common.component.Components.applicationService;
import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@State(
        name = ConfigProviderExportManager.COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class ConfigProviderExportManager extends ApplicationComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Application.ConfigProviderExportService";
    private static final StateCategory EXPORT_FORM = StateCategory.get("EXPORT_FORM");
    private static final String STATES = "states";

    public static final String LAST_OUTPUT_FILE = "last-output-file";
    public static final String LAST_WRAPPER_KEY = "last-wrapper-key";
    public static final String LAST_INCLUDE_WALLET = "last-include-wallet";
    public static final String LAST_WALLET_FILE = "last-wallet-file";

    private final StateContainer states = new StateContainer();

    private ConfigProviderExportManager() {super(COMPONENT_NAME);}

    @Override
    public Element getComponentState() {
        Element element = newStateElement();
        states.writeState(element, STATES);
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        states.readState(element, STATES);
    }

    public void exportConnection(@NotNull Project project, @NotNull ConnectionSettings settings){
        Dialogs.show(
                () -> new ConfigProviderExportDialog(project, this),
                (dialog, exitCode) -> {
                    if (exitCode != DialogWrapper.OK_EXIT_CODE) return;

                    ConfigProviderExportRequest request = dialog.getExportRequest();

                    Progress.modal(project, null, true,
                            "Exporting configuration",
                            "Writing configuration file...",
                            progress -> doExport(project, settings, request));
                }
        );
    }

    public @NotNull StateAttributes getExportFormState() {
        return states.ensureAttributes(EXPORT_FORM);
    }

    private void doExport(Project project, ConnectionSettings settings, ConfigProviderExportRequest request) {
        try {
            validateRequest(request);

            // 1) map settings -> domain payload (format-agnostic)
            ConfigProviderPayload payload = ConfigProviderMapper.map(settings, request);

            // 2) validate required field early (friendly error)
            if (payload.getConnectDescriptor() == null || payload.getConnectDescriptor().isBlank()) {
                throw new IllegalArgumentException(
                        "connect_descriptor is required.\n\n" +
                                "Fix one of the following:\n" +
                                " - Provide Host/Port/Service(SID)\n" +
                                " - Select a TNS profile\n"
                );
            }

            // 3) choose format processor (strategy)
            ConfigProviderFormatProcessor processor =
                    ConfigProviderFormatRegistry.getInstance().get(request.getFormatId());

            // 4) write output
            processor.write(payload, request.getOutputFile(), request.getWrapperKey());

            // 5) success message (safe)
            Messages.showInfoDialog(project, "Export configuration", "Configuration exported successfully.");
        } catch (Exception e) {
            boolean sensitive = request.isIncludeWallet();

            if (sensitive) {
                conditionallyLog(new RuntimeException("ConfigProvider export failed (" + e.getClass().getName() + "): " + messageOf(e)));
            } else {
                conditionallyLog(e);
            }

            Messages.showErrorDialog(project, "Export failed", userMessage(e));
        }
    }

    private static void validateRequest(ConfigProviderExportRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Export request is missing.");
        }

        Path outputFile = request.getOutputFile();
        if (outputFile == null) {
            throw new IllegalArgumentException("Output file is required.");
        }

        Path outputDirectory = outputFile.toAbsolutePath().getParent();
        if (outputDirectory == null) {
            throw new IllegalArgumentException("Output file must have a parent directory.");
        }
        if (!Files.exists(outputDirectory)) {
            throw new IllegalArgumentException("Output directory does not exist: " + outputDirectory);
        }
        if (!Files.isDirectory(outputDirectory)) {
            throw new IllegalArgumentException("Output path parent is not a directory: " + outputDirectory);
        }
        if (!Files.isWritable(outputDirectory)) {
            throw new IllegalArgumentException("Output directory is not writable: " + outputDirectory);
        }

        if (request.isIncludeWallet()) {
            Path walletFile = request.getWalletFile();
            if (walletFile == null) {
                throw new IllegalArgumentException("Wallet file is required when 'Include wallet' is selected.");
            }
            if (!Files.exists(walletFile)) {
                throw new IllegalArgumentException("Wallet file does not exist: " + walletFile);
            }
            if (!Files.isRegularFile(walletFile)) {
                throw new IllegalArgumentException("Wallet path is not a file: " + walletFile);
            }
            if (!Files.isReadable(walletFile)) {
                throw new IllegalArgumentException("Wallet file is not readable: " + walletFile);
            }
        }
    }

    private static String userMessage(Exception e) {
        String message = messageOf(e);
        return message == null ? "Export failed." : message;
    }

    private static String messageOf(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            message = e.getLocalizedMessage();
        }
        return message == null || message.isBlank() ? null : message.trim();
    }

    public static ConfigProviderExportManager getInstance() {
        return applicationService(ConfigProviderExportManager.class);
    }
}
