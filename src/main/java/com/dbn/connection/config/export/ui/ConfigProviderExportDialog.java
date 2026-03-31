package com.dbn.connection.config.export.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.config.export.ConfigProviderExportManager;
import com.dbn.connection.config.export.ConfigProviderExportRequest;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

public class ConfigProviderExportDialog extends DBNDialog<ConfigProviderExportForm> {
    private final ConfigProviderExportManager exportService;

    public ConfigProviderExportDialog(
            @NotNull Project project,
            @NotNull ConfigProviderExportManager exportService) {

        super(project, "Export JSON", true);
        this.exportService = exportService;
        init();
    }

    @NotNull
    @Override
    protected ConfigProviderExportForm createForm() {
        return new ConfigProviderExportForm(this, exportService);
    }

    @NotNull
    @Override
    protected Action[] initializeActions() {
        renameAction(getOKAction(), "Export");
        return actions(getOKAction(), getCancelAction());
    }

    public ConfigProviderExportRequest getExportRequest() {
        return getForm().getExportRequest();
    }
}
