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
            presentation.setDescription("Execute statements using main connection");
        } else if (session.isPool()) {
            presentation.setDescription("Execute statements in pool connections (async)");
        }


        presentation.setEnabled(enabled);
    }
}
