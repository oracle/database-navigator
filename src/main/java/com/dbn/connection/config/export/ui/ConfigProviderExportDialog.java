package com.dbn.connection.config.export.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.connection.config.export.ConfigProviderExportManager;
import com.dbn.connection.config.export.ConfigProviderExportRequest;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;

public class ConfigProviderExportDialog extends DBNDialog<ConfigProviderExportForm> {
    private final ConnectionSettings connectionSettings;

    public ConfigProviderExportDialog(
            @NotNull Project project,
            @Nullable ConnectionHandler connection,
            @NotNull ConnectionSettings connectionSettings) {

        super(project, "Export JSON", true);
        this.connectionSettings = connectionSettings;
        setConnection(connection);
        init();
    }

    @NotNull
    @Override
    protected ConfigProviderExportForm createForm() {
        return new ConfigProviderExportForm(this, connectionSettings);
    }

    @NotNull
    @Override
    protected Action[] initializeActions() {
        renameAction(getOKAction(), "Export");
        return actions(getOKAction(), getCancelAction());
    }

    @Override
    protected void doOKAction() {
        ConfigProviderExportManager exportManager = ConfigProviderExportManager.getInstance();
        ConfigProviderExportRequest request = getForm().getExportRequest();
        Project project = getProject();
        if (!exportManager.confirmExport(project, request)) return;

        super.doOKAction();
        exportManager.submitExport(project, connectionSettings, request);
    }
}
