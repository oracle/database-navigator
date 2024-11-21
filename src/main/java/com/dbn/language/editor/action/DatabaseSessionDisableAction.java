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

package com.dbn.language.editor.action;

import com.dbn.common.action.Lookups;
import com.dbn.common.action.ProjectAction;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.config.ConnectionDetailSettings;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.util.Conditional.when;

public class DatabaseSessionDisableAction extends ProjectAction {
    private final ConnectionRef connection;

    DatabaseSessionDisableAction(ConnectionHandler connection) {
        this.connection = connection.ref();
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        Editor editor = Lookups.getEditor(e);
        if (editor == null) return;

        ConnectionHandler connection = this.connection.ensure();
        Messages.showQuestionDialog(
                project,
                "Disable session support",
                "Are you sure you want to disable the session support for connection \"" + connection.getName() + "\"\n(you can re-enable at any time in connection details settings)",
                Messages.OPTIONS_YES_NO,
                0,
                option -> when(option == 0, () -> {
                    ConnectionDetailSettings detailSettings = connection.getSettings().getDetailSettings();
                    detailSettings.setEnableSessionManagement(false);
                }));
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        e.getPresentation().setText("Disable Session Support...");
    }
}
