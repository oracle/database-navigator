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

import com.dbn.common.options.ConfigMonitor;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.config.ConnectionSettings;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

import static com.dbn.common.exception.Exceptions.getLocalizedMessage;
import static com.dbn.common.util.Messages.showErrorDialog;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

public class ConnectionSettingsDialog extends DBNDialog<ConnectionSettingsForm> {
    private final ConnectionSettings connectionSettings;

    public ConnectionSettingsDialog(@NotNull ConnectionSettings connectionSettings) {
        super(connectionSettings.getProject(), txt("msg.connection.title.ConnectionConfiguration"), true);
        this.connectionSettings = connectionSettings;
        setModal(true);
        setResizable(true);
        setDefaultSize(900, 700);
        init();
    }

    @NotNull
    @Override
    protected ConnectionSettingsForm createForm() {
        return connectionSettings.createConfigurationEditor();
    }

    @Override
    protected Action[] initializeActions() {
        return actions(
                getOKAction(),
                getCancelAction());
    }

    @Override
    protected void doOKAction() {
        try {
            connectionSettings.apply();
            ConfigMonitor.notifyChanges();
            super.doOKAction();
        } catch (ConfigurationException e) {
            conditionallyLog(e);
            showErrorDialog(getProject(), txt("cfg.connection.title.InvalidConfiguration"), getLocalizedMessage(e));
        }
    }
}
