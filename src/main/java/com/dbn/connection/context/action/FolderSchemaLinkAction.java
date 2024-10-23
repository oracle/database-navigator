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
        String text = "Associate Schema...";

        boolean visible = isAvailableFor(file, project);
        if (visible) {
            FileConnectionContext mapping = getFileContext(file, project);
            if (mapping != null && mapping.getSchemaId() != null) {
                text = "Change Schema Association...";
            }
        }

        presentation.setVisible(visible);
        presentation.setText(text);
    }
}
