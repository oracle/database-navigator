package com.dbn.connection.config.export;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.ApplicationComponentBase;
import com.dbn.common.component.PersistentState;
import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.export.ExportDestination;
import com.dbn.common.state.StateAttributes;
import com.dbn.common.state.StateCategory;
import com.dbn.common.state.StateContainer;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.config.export.ui.ConfigProviderExportDialog;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Desktop;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.dbn.common.component.Components.applicationService;
import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.dbn.common.util.Chars.isNotEmpty;
import static com.dbn.common.util.Commons.matchArrays;
import static com.dbn.common.util.Conditional.when;
import static com.dbn.common.util.Passwords.clearPassword;
import static com.dbn.connection.config.export.JsonExistingContentWriteMode.NONE;
import static com.dbn.connection.config.export.JsonExistingContentWriteMode.REPLACE_ROOT;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

@State(
        name = ConfigProviderExportManager.COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class ConfigProviderExportManager extends ApplicationComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Application.ConfigProviderExportService";
    private static final StateCategory EXPORT_FORM = StateCategory.get("EXPORT_FORM");
    private static final String STATES = "states";

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

    public void exportConnection(
            @NotNull Project project,
            @Nullable ConnectionHandler connection,
            @NotNull ConnectionSettings settings) {
        Dialogs.show(() -> new ConfigProviderExportDialog(project, connection, settings));
    }

    public boolean confirmExport(
            @NotNull Project project,
            @NotNull ConfigProviderExportRequest request) {
        if (!confirmFileReplacement(project, request)) {
            request.clearDatabasePassword();
            return false;
        }
        return true;
    }

    public void submitExport(
            @NotNull Project project,
            @NotNull ConnectionSettings settings,
            @NotNull ConfigProviderExportRequest request) {
        Progress.modal(project, null, true,
                "Exporting configuration",
                "Writing configuration file...",
                progress -> doExport(project, settings, request));
    }

    private static boolean confirmFileReplacement(@NotNull Project project, @NotNull ConfigProviderExportRequest request) {
        if (request.getDestination() == ExportDestination.CLIPBOARD) return true;

        Path outputFile = request.getOutputFile();
        if (outputFile == null) return true;

        try {
            ConfigProviderFormatProcessor processor =
                    ConfigProviderFormatRegistry.getInstance().get(request.getFormatId());
            if (!(processor instanceof JsonConfigProviderProcessor jsonProcessor)) return true;

            JsonExistingContentWriteMode writeMode =
                    jsonProcessor.getExistingContentWriteMode(outputFile, request.getWrapperKey());
            if (writeMode == NONE) return true;

            String question = writeMode == REPLACE_ROOT
                    ? txt("msg.connection.question.ReplaceExportFile", outputFile)
                    : txt("msg.connection.question.ReplaceExportEntry", outputFile, request.getWrapperKey().trim());
            int option = Messages.showConfirmationDialog(
                    project,
                    txt("msg.connection.title.ExportConfiguration"),
                    question,
                    Messages.OPTIONS_YES_NO,
                    1);
            return option == 0;
        } catch (Exception e) {
            conditionallyLog(e);
            Messages.showErrorDialog(project, txt("msg.connection.title.ExportFailed"), userMessage(e));
            return false;
        }
    }

    public @NotNull StateAttributes getExportFormState() {
        return states.ensureAttributes(EXPORT_FORM);
    }

    private void doExport(Project project, ConnectionSettings settings, ConfigProviderExportRequest request) {
        try {
            validateRequest(settings, request);

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
            if (request.getDestination() == ExportDestination.CLIPBOARD) {
                CopyPasteManager.getInstance().setContents(new StringSelection(processor.render(payload, request.getWrapperKey())));
                Messages.showInfoDialog(
                        project,
                        txt("msg.connection.title.ExportConfiguration"),
                        txt("msg.connection.info.ConfigExportedToClipboard"));
            } else {
                processor.write(payload, request.getOutputFile(), request.getWrapperKey());
                showFileExportedDialog(project, request.getOutputFile());
            }
        } catch (Exception e) {
            boolean sensitive = request.isIncludeWallet() || request.isIncludeDatabasePassword();

            if (sensitive) {
                conditionallyLog(new RuntimeException(
                        "Connection settings export failed while processing sensitive content. " +
                                "Exception type: " + e.getClass().getName()));
                Messages.showErrorDialog(
                        project,
                        txt("msg.connection.title.ExportFailed"),
                        txt("msg.connection.error.SensitiveExportFailed"));
            } else {
                conditionallyLog(e);
                Messages.showErrorDialog(project, txt("msg.connection.title.ExportFailed"), userMessage(e));
            }
        } finally {
            request.clearDatabasePassword();
        }
    }

    private static void showFileExportedDialog(@NotNull Project project, @NotNull Path outputFile) {
        String title = txt("msg.connection.title.ExportConfiguration");
        String message = txt("msg.connection.info.ConfigExportedToFile", outputFile);
        if (Desktop.isDesktopSupported()) {
            Messages.showInfoDialog(
                    project,
                    title,
                    message,
                    new String[]{txt("msg.shared.button.OK"), txt("msg.shared.button.OpenFile")},
                    0,
                    option -> when(option == 1, () -> openFile(project, outputFile)));
        } else {
            Messages.showInfoDialog(project, title, message);
        }
    }

    private static void openFile(@NotNull Project project, @NotNull Path outputFile) {
        try {
            Desktop.getDesktop().open(outputFile.toFile());
        } catch (IOException e) {
            conditionallyLog(e);
            Messages.showErrorDialog(
                    project,
                    txt("msg.connection.title.ExportConfiguration"),
                    txt("msg.connection.error.FailedToOpenExportFile", outputFile));
        }
    }

    private static void validateRequest(ConnectionSettings settings, ConfigProviderExportRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Export request is missing.");
        }

        validateDatabasePassword(settings, request);

        if (request.getDestination() != ExportDestination.CLIPBOARD) {
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

    static void validateDatabasePassword(
            @NotNull ConnectionSettings settings,
            @NotNull ConfigProviderExportRequest request) {
        if (!request.isIncludeDatabasePassword()) return;

        char[] exportPassword = request.getDatabasePassword();
        if (!isNotEmpty(exportPassword)) {
            throw new IllegalArgumentException(txt("msg.connection.error.ExportPasswordRequired"));
        }

        AuthenticationInfo authentication = settings.getDatabaseSettings().getAuthenticationInfo();
        char[] configuredPassword = authentication == null ? null : authentication.getPassword();
        try {
            if (!matchArrays(configuredPassword, exportPassword)) {
                throw new IllegalArgumentException(txt("msg.connection.error.ExportPasswordMismatch"));
            }
        } finally {
            clearPassword(configuredPassword);
        }
    }

    private static String userMessage(Exception e) {
        String message = messageOf(e);
        return message == null ? txt("msg.connection.title.ExportFailed") : message;
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
