package com.dbn.language.editor.action;

import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.Lookups;
import com.dbn.common.action.ProjectAction;
import com.dbn.common.environment.EnvironmentManager;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.dbn.connection.session.DatabaseSession;
import com.dbn.editor.DBContentType;
import com.dbn.object.DBTable;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.connection.ConnectionHandler.isLiveConnection;

@BackgroundUpdate
public abstract class TransactionEditorAction extends ProjectAction {

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        VirtualFile virtualFile = Lookups.getVirtualFile(e);
        boolean enabled = false;
        boolean visible = false;

        ConnectionHandler connection = getConnection(e);
        if (isLiveConnection(connection)) {
            DatabaseSession session = getDatabaseSession(e);
            if (session != null && !session.isPool()) {
                DBNConnection conn = getTargetConnection(e);
                if (conn != null && !conn.isPoolConnection() && conn.hasDataChanges()) {
                    enabled = true;
                }

                if (!connection.isAutoCommit()) {
                    visible = true;
                    if (virtualFile instanceof DBEditableObjectVirtualFile) {
                        DBEditableObjectVirtualFile databaseFile = (DBEditableObjectVirtualFile) virtualFile;
                        DBSchemaObject object = databaseFile.getObject();
                        if (object instanceof DBTable) {
                            EnvironmentManager environmentManager = EnvironmentManager.getInstance(project);
                            visible = !environmentManager.isReadonly(object, DBContentType.DATA);
                        }
                    }
                }

            }
        }

        Presentation presentation = e.getPresentation();
        presentation.setEnabled(enabled);
        presentation.setVisible(visible);
    }

    @Nullable
    protected ConnectionHandler getConnection(@NotNull AnActionEvent e) {
        Project project = Lookups.getProject(e);
        VirtualFile virtualFile = Lookups.getVirtualFile(e);
        if (project != null && virtualFile != null) {
            FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
            return contextManager.getConnection(virtualFile);
        }
        return null;
    }

    @Nullable
    protected DatabaseSession getDatabaseSession(@NotNull AnActionEvent e) {
        Project project = Lookups.getProject(e);
        VirtualFile virtualFile = Lookups.getVirtualFile(e);
        if (project != null && virtualFile != null) {
            FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
            return contextManager.getDatabaseSession(virtualFile);
        }
        return null;
    }

    @Nullable
    protected DBNConnection getTargetConnection(@NotNull AnActionEvent e) {
        ConnectionHandler connection = getConnection(e);
        DatabaseSession session = getDatabaseSession(e);
        if (connection != null && session != null) {
            return connection.getConnectionPool().getSessionConnection(session.getId());
        }
        return null;
    }
}
