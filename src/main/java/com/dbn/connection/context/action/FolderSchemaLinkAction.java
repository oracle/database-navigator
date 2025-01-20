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

package com.dbn.connection.context.action;

import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.Lookups;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.mapping.FileConnectionContext;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import static com.dbn.connection.ConnectionHandler.isLiveConnection;
import static com.dbn.nls.NlsResources.txt;

@BackgroundUpdate
public class FolderSchemaLinkAction extends AbstractFolderContextAction {

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        VirtualFile file = Lookups.getVirtualFile(e);
        if (isAvailableFor(file, project)) {
            DataContext dataContext = e.getDataContext();
            FileConnectionContextManager contextManager = getContextManager(project);
            contextManager.promptSchemaSelector(file, dataContext, null);
        }
    }

    private boolean isAvailableFor(VirtualFile file, @NotNull Project project) {
        FileConnectionContext mapping = getFileContext(file, project);
        if (mapping == null) return false;

        ConnectionHandler connection = mapping.getConnection();
        return isLiveConnection(connection);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        VirtualFile file = Lookups.getVirtualFile(e);
        String text = txt("app.fileContext.action.AssociateSchema");

        boolean visible = isAvailableFor(file, project);
        if (visible) {
            FileConnectionContext mapping = getFileContext(file, project);
            if (mapping != null && mapping.getSchemaId() != null) {
                text = txt("app.fileContext.action.ChangeSchemaAssociation");
            }
        }

        presentation.setVisible(visible);
        presentation.setText(text);
    }
}
