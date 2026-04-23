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

package com.dbn.connection.action;

import com.dbn.common.icon.Icons;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionAction;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.config.datasource.ui.DataSourceConfigManagerDialog;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DataSourceConfigManagerOpenAction extends AbstractConnectionAction {

    DataSourceConfigManagerOpenAction(ConnectionHandler connection) {
        super(connection);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull ConnectionHandler connection) {
        ConnectionAction.invoke(
                "Opening Data Source Config Manager",
                true,
                connection,
                action -> Dialogs.show(() -> new DataSourceConfigManagerDialog(connection)));
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable ConnectionHandler target) {
        boolean enabled = target != null && target.getDatabaseType() == DatabaseType.ORACLE;
        presentation.setEnabled(enabled);
        presentation.setText("Manage Data Source Config...");
        presentation.setDescription("Manage DATA_SOURCE_CONFIG_STORE entries");
        presentation.setIcon(Icons.ACTION_OPTIONS);
    }
}
