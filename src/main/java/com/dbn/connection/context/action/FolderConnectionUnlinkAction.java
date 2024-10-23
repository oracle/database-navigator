package com.dbn.connection.context.action;

import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.Lookups;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.mapping.FileConnectionContext;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

@BackgroundUpdate
public class FolderConnectionUnlinkAction extends AbstractFolderContextAction {

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        VirtualFile file = Lookups.getVirtualFile(e);
        if (isAvailableFor(file, project)) {
            FileConnectionContextManager contextManager = getContextManager(project);
            contextManager.removeMapping(file);
        }
    }

    private boolean isAvailableFor(VirtualFile file, @NotNull Project project) {
        return getFileContext(file, project) != null;
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        VirtualFile file = Lookups.getVirtualFile(e);
        boolean visible = isAvailableFor(file, project);
        presentation.setVisible(visible);
        String text = "Remove Connection Association";

        if (visible) {
            FileConnectionContext fileContext = getFileContext(file, project);
            if (fileContext != null) {
                ConnectionHandler connection = fileContext.getConnection();
                if (connection != null) {
                    text = "Remove Association from \"" + connection.getName() + "\"";
                }
            }

        }

        presentation.setText(text);
    }
}
