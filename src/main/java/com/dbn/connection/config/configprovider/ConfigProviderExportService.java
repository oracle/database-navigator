package com.dbn.connection.config.configprovider;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.ApplicationComponentBase;
import com.dbn.common.component.PersistentState;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Messages;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.config.configprovider.ui.ConfigProviderExportDialog;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

import static com.dbn.common.component.Components.applicationService;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.dbn.common.options.setting.Settings.setBoolean;
import static com.dbn.common.options.setting.Settings.setString;
import static com.dbn.common.options.setting.Settings.getString;
import static com.dbn.common.options.setting.Settings.getBoolean;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Getter
@Setter
@State(
        name = ConfigProviderExportService.COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class ConfigProviderExportService extends ApplicationComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Application.ConfigProviderExportService";

    private String lastFormatId = "json";
    private String lastExportDir = "";
    private String lastWrapperKey = "";
    private boolean lastIncludePassword = false;
    private boolean lastIncludeWallet = false;

    private ConfigProviderExportService() {super(COMPONENT_NAME);}

    @Override
    public Element getComponentState() {

        Element element = newStateElement();
        Element options = newElement(element, "configprovider-export-options");

        setString(options, "last-format-id", lastFormatId);
        setString(options, "last-export-dir", lastExportDir);
        setString(options, "last-wrapper-key", lastWrapperKey);
        setBoolean(options, "last-include-password", lastIncludePassword);
        setBoolean(options, "last-include-wallet", lastIncludeWallet);

        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        Element options = element.getChild("configprovider-export-options");
        if (options == null) return;

        lastFormatId = getString(options, "last-format-id", lastFormatId);
        lastExportDir = getString(options, "last-export-dir", lastExportDir);
        lastWrapperKey = getString(options, "last-wrapper-key", lastWrapperKey);
        lastIncludePassword = getBoolean(options, "last-include-password", lastIncludePassword);
        lastIncludeWallet = getBoolean(options, "last-include-wallet", lastIncludeWallet);
    }
    public void exportConnection(@NotNull Project project, @NotNull ConnectionSettings settings){
        Dialogs.show(
                () -> new ConfigProviderExportDialog(project, settings, this),
                (dialog, exitCode) -> {
                    if (exitCode != DialogWrapper.OK_EXIT_CODE) return;

                    ConfigProviderExportRequest request = dialog.getExportRequest();
                    remember(request);

                    Progress.modal(project, null, true,
                            "Exporting configuration",
                            "Writing configuration file...",
                            progress -> doExport(project, settings, request));
                }
        );
    }

    private void doExport(Project project, ConnectionSettings settings, ConfigProviderExportRequest request) {
        try {
            // 1) map settings -> domain payload (format-agnostic)
            ConfigProviderPayload payload = ConfigProviderMapper.map(settings, request);

            // 2) validate required field early (friendly error)
            if (payload.getConnectDescriptor() == null || payload.getConnectDescriptor().isBlank()) {
                throw new IllegalArgumentException(
                        "connect_descriptor is required.\n\n" +
                                "Fix one of the following:\n" +
                                " - Provide Host/Port/Service(SID)\n" +
                                " - Select a TNS profile\n" +
                                " - For Custom URL, add an alias/descriptor after '@' (not only ?TNS_ADMIN=...)"
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
            boolean sensitive = request.isIncludePassword() || request.isIncludeWallet();

            if (sensitive) {
                conditionallyLog(new RuntimeException("ConfigProvider export failed (" + e.getClass().getName() + ")"));
            } else {
                conditionallyLog(e);
            }

            Messages.showErrorDialog(project, "Export failed", "Export failed.");
        }
    }

    private void remember(ConfigProviderExportRequest request) {
        if (request == null) return;

        if (request.getFormatId() != null && !request.getFormatId().isBlank()) {
            lastFormatId = request.getFormatId();
        }
        lastWrapperKey = nvl(request.getWrapperKey(), "");

        lastIncludePassword = request.isIncludePassword();
        lastIncludeWallet = request.isIncludeWallet();

        Path out = request.getOutputFile();
        if (out != null && out.getParent() != null) {
            lastExportDir = out.getParent().toString();
        }
    }

    public static ConfigProviderExportService getInstance() {
        return applicationService(ConfigProviderExportService.class);
    }
}
