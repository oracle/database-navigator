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

import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.Lookups;
import com.dbn.common.action.ProjectAction;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.dbn.connection.session.DatabaseSession;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;

@BackgroundUpdate
public class DatabaseSessionSelectAction extends ProjectAction {
    private final DatabaseSession session;
    DatabaseSessionSelectAction(DatabaseSession session) {
        this.session = session;
    }


    @NotNull
    public DatabaseSession getSession() {
        return session;
    }


    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        Editor editor = Lookups.getEditor(e);
        if (editor == null) return;

        FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
        contextManager.setDatabaseSession(editor, session);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        boolean enabled = false;
        VirtualFile virtualFile = Lookups.getVirtualFile(e);
        if (virtualFile != null) {
            if (virtualFile instanceof DBEditableObjectVirtualFile) {
                enabled = false;
            } else {
                enabled = true;
/*
                // TODO allow selecting "hot" session?
                PsiFile currentFile = PsiUtil.getPsiFile(project, virtualFile);
                if (currentFile instanceof DBLanguagePsiFile) {
                    FileConnectionMappingManager connectionMappingManager = getComponent(e, FileConnectionMappingManager.class);
                    ConnectionHandler connection = connectionMappingManager.getCache(virtualFile);
                    if (connection != null) {
                        DBNConnection conn = connection.getConnectionPool().getSessionConnection(session.getId());
                        enabled = conn == null || !conn.hasDataChanges();
                    }
                }
*/

            }
        }

        Presentation presentation = e.getPresentation();
        presentation.setText(session.getName());
        presentation.setIcon(session.getIcon());
        if (session.isMain()) {
            presentation.setDescription(txt("app.codeEditor.tooltip.ExecuteUsingMainConnection"));
        } else if (session.isPool()) {
            presentation.setDescription(txt("app.codeEditor.tooltip.ExecuteUsingPoolConnection"));
        }


        presentation.setEnabled(enabled);
    }
}
