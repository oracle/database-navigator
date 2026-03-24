package com.dbn.connection.config.configprovider.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.config.configprovider.ConfigProviderExportService;
import com.dbn.connection.config.configprovider.ConfigProviderExportRequest;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

public class ConfigProviderExportDialog extends DBNDialog<ConfigProviderExportForm> {
    private final ConnectionSettings connectionSettings;
    private final ConfigProviderExportService exportService;

    public ConfigProviderExportDialog(
            @NotNull Project project,
            @NotNull ConnectionSettings connectionSettings,
            @NotNull ConfigProviderExportService exportService) {

        super(project, "Export JSON", true);
        this.connectionSettings = connectionSettings;
        this.exportService = exportService;
        init();
    }

    @NotNull
    @Override
    protected ConfigProviderExportForm createForm() {
        return new ConfigProviderExportForm(this, connectionSettings, exportService);
    }

    @NotNull
    @Override
    protected Action[] initializeActions() {
        renameAction(getOKAction(), "Export");
        return actions(getOKAction(), getCancelAction());
    }

    @Override
    protected void doOKAction() {
        getForm().validateEntries(() -> super.doOKAction());
    }

    public ConfigProviderExportRequest getExportRequest() {
        return getForm().getExportRequest();
    }
}