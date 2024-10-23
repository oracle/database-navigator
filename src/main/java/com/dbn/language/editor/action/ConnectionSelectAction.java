package com.dbn.language.editor.action;

import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.Lookups;
import com.dbn.common.action.ProjectAction;
import com.dbn.common.icon.Icons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.dbn.language.common.DBLanguageFileType;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

@BackgroundUpdate
public class ConnectionSelectAction extends ProjectAction {
    private final ConnectionHandler connection;

    ConnectionSelectAction(ConnectionHandler connection) {
        super();
        Presentation presentation = getTemplatePresentation();
        presentation.setText(connection == null ? "No Connection" : connection.getName(), false);
        presentation.setIcon(connection == null ? Icons.SPACE : connection.getIcon());
        this.connection = connection;
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        Editor editor = Lookups.getEditor(e);
        if (editor != null) {
            FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
            contextManager.setConnection(editor, connection);
        }
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        boolean enabled = true;
        VirtualFile virtualFile = Lookups.getVirtualFile(e);
        if (virtualFile instanceof DBEditableObjectVirtualFile) {
            enabled = false;
        } else {
            if (virtualFile != null && virtualFile.getFileType() instanceof DBLanguageFileType) {
                if (connection == null) {
                    enabled = true;
                }
            } else {
                enabled = false;
            }
        }
        presentation.setEnabled(enabled);

    }
}
