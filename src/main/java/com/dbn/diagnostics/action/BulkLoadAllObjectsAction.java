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

package com.dbn.diagnostics.action;

import com.dbn.common.icon.Icons;
import com.dbn.common.thread.Progress;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.action.AbstractConnectionAction;
import com.dbn.diagnostics.Diagnostics;
import com.dbn.object.common.DBObjectRecursiveLoaderVisitor;
import com.dbn.object.common.list.DBObjectListContainer;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.nls.NlsResources.txt;

public class BulkLoadAllObjectsAction extends AbstractConnectionAction {
    public BulkLoadAllObjectsAction(ConnectionHandler connection) {
        super(connection);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull ConnectionHandler connection) {
        Progress.prompt(project, connection, true,
                txt("prc.diagnostics.title.LoadingDataDictionary"),
                txt("prc.diagnostics.text.LoadingAllObjectsOf", connection.getName()),
                progress -> {
                    DBObjectListContainer objectListContainer = connection.getObjectBundle().getObjectLists();
                    objectListContainer.visit(DBObjectRecursiveLoaderVisitor.INSTANCE, false);
                });
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable ConnectionHandler connection) {
        presentation.setVisible(Diagnostics.isBulkActionsEnabled());
        presentation.setText(txt("app.diagnostics.action.LoadAllObjects"));
        presentation.setIcon(Icons.DATA_EDITOR_RELOAD_DATA);
    }
}
