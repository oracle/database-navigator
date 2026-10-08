/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.connection.config.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.config.ConnectionBundleSettings;
import com.dbn.connection.config.ConnectionConfigType;
import com.dbn.connection.config.tns.TnsImportData;
import com.dbn.options.ProjectSettings;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;

import static com.dbn.common.exception.Exceptions.getLocalizedMessage;
import static com.dbn.common.util.Messages.showErrorDialog;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

public class ConnectionBundleSettingsDialog extends DBNDialog<ConnectionBundleSettingsForm> {
    private final ProjectSettings projectSettings;
    private final ConnectionBundleSettings connectionSettings;
    private final ConnectionId selectedConnectionId;
    private final TnsImportData importData;
    private final DatabaseType newDatabaseType;
    private final ConnectionConfigType newConfigType;

    public ConnectionBundleSettingsDialog(@NotNull Project project) {
        this(project, null, null, null, null);
    }

    public ConnectionBundleSettingsDialog(
            @NotNull Project project,
            @Nullable ConnectionId selectedConnectionId) {
        this(project, selectedConnectionId, null, null, null);
    }

    public ConnectionBundleSettingsDialog(
            @NotNull Project project,
            @NotNull TnsImportData importData) {
        this(project, null, importData, null, null);
    }

    public ConnectionBundleSettingsDialog(
            @NotNull Project project,
            @NotNull DatabaseType databaseType,
            @NotNull ConnectionConfigType configType) {
        this(project, null, null, databaseType, configType);
    }

    private ConnectionBundleSettingsDialog(
            @NotNull Project project,
            @Nullable ConnectionId selectedConnectionId,
            @Nullable TnsImportData importData,
            @Nullable DatabaseType newDatabaseType,
            @Nullable ConnectionConfigType newConfigType) {
        super(project, txt("msg.connection.title.ConnectionsConfiguration"), true);
        this.projectSettings = ProjectSettings.get(project).clone();
        this.connectionSettings = projectSettings.getConnectionSettings();
        this.selectedConnectionId = selectedConnectionId;
        this.importData = importData;
        this.newDatabaseType = newDatabaseType;
        this.newConfigType = newConfigType;
        setModal(true);
        setResizable(true);
        setDefaultSize(1200, 960);
        init();
    }

    @NotNull
    @Override
    protected ConnectionBundleSettingsForm createForm() {
        connectionSettings.createComponent();
        ConnectionBundleSettingsForm form = connectionSettings.ensureSettingsEditor();
        if (selectedConnectionId != null) {
            form.selectConnection(selectedConnectionId);
        }
        if (importData != null) {
            form.importTnsNames(importData);
        }
        if (newDatabaseType != null && newConfigType != null) {
            form.createNewConnection(newDatabaseType, newConfigType);
        }
        return form;
    }

    @Override
    protected Action[] initializeActions() {
        return actions(
                getOKAction(),
                getApplyAction(),
                getCancelAction());
    }

    @Override
    protected void doOKAction() {
        try {
            projectSettings.apply();
            super.doOKAction();
            projectSettings.disposeUIResources();
        } catch (ConfigurationException e) {
            conditionallyLog(e);
            showErrorDialog(getProject(), txt("cfg.connection.title.InvalidConfiguration"), getLocalizedMessage(e));
        }
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
        projectSettings.disposeUIResources();
    }

    private @NotNull Action getApplyAction() {
        return createApplyAction(
                () -> projectSettings.isModified(),
                () -> projectSettings.apply());
    }

    @Override
    public void disposeInner() {
        projectSettings.disposeUIResources();
    }
}
