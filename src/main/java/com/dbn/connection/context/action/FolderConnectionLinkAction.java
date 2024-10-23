package com.dbn.connection.context.action;

import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.Lookups;
import com.dbn.connection.ConnectionSelectorOptions;
import com.dbn.connection.mapping.FileConnectionContext;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import static com.dbn.connection.ConnectionSelectorOptions.Option.SHOW_VIRTUAL_CONNECTIONS;

@BackgroundUpdate
public class FolderConnectionLinkAction extends AbstractFolderContextAction {

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        VirtualFile file = Lookups.getVirtualFile(e);
        if (isAvailableFor(file)) {
            DataContext dataContext = e.getDataContext();
            ConnectionSelectorOptions options = ConnectionSelectorOptions.options(SHOW_VIRTUAL_CONNECTIONS);

            FileConnectionContextManager contextManager = getContextManager(project);
            contextManager.promptConnectionSelector(file, dataContext, options, null);
        }
    }

    private boolean isAvailableFor(VirtualFile virtualFile) {
        return virtualFile != null && virtualFile.isDirectory();
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        VirtualFile file = Lookups.getVirtualFile(e);
        presentation.setVisible(isAvailableFor(file));
        String text = "Associate Connection...";

        FileConnectionContext mapping = getFileContext(file, project);
        if (mapping != null && mapping.getConnection() != null) {
            text = "Change Connection Association...";
        }

        presentation.setText(text);
    }
}
