/*
 * Copyright 2024 Oracle and/or its affiliates
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

import com.dbn.common.options.ConfigMonitor;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.ui.form.DBNContentWithHeaderForm;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.config.ConnectionFilterSettings;
import com.dbn.connection.config.ConnectionSettings;
import com.dbn.options.ProjectSettingsManager;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class ConnectionFilterSettingsDialog extends DBNDialog<DBNContentWithHeaderForm> {
    private final ConnectionRef connection;
    private ConnectionFilterSettingsForm configurationEditor;
    private ConnectionFilterSettings filterSettings;

    public ConnectionFilterSettingsDialog(@NotNull ConnectionHandler connection) {
        super(connection.getProject(), "Object filters", true);
        this.connection = connection.ref();
        setModal(true);
        setResizable(true);
        init();
    }

    @NotNull
    @Override
    protected DBNContentWithHeaderForm createForm() {
        ConnectionHandler connection = this.connection.ensure();
        return new DBNContentWithHeaderForm(this) {
            @Override
            public DBNHeaderForm createHeaderForm() {
                return new DBNHeaderForm(this, connection);
            }

            @Override
            public DBNForm createContentForm() {
                ProjectSettingsManager settingsManager = ProjectSettingsManager.getInstance(ensureProject());
                ConnectionSettings connectionSettings = settingsManager.getConnectionSettings().getConnectionSettings(connection.getConnectionId());
                filterSettings = connectionSettings.getFilterSettings().clone();
                configurationEditor = filterSettings.createConfigurationEditor();
                return configurationEditor;
            }
        };
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        return new Action[]{
                getOKAction(),
                getCancelAction()
        };
    }

    @Override
    public void doOKAction() {
        try {
            // !!workaround!! apply settings is normally cascaded from top level settings
            configurationEditor.applyFormChanges();
            filterSettings.apply();

            ProjectSettingsManager settingsManager = ProjectSettingsManager.getInstance(ensureProject());
            ConnectionSettings connectionSettings = settingsManager.getConnectionSettings().getConnectionSettings(connection.getConnectionId());
            filterSettings.applyTo(connectionSettings.getFilterSettings());

            ConfigMonitor.notifyChanges();
            super.doOKAction();
        } catch (ConfigurationException e) {
            conditionallyLog(e);
            Messages.showErrorDialog(getProject(), txt("cfg.connection.title.InvalidConfiguration"), e.getMessage());
        }

    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
    }

    @Override
    public void disposeInner() {
        filterSettings.disposeUIResources();
    }
}
