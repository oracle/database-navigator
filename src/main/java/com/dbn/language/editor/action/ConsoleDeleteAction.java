package com.dbn.language.editor.action;

import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.Lookups;
import com.dbn.common.action.ProjectAction;
import com.dbn.common.icon.Icons;
import com.dbn.connection.console.DatabaseConsoleManager;
import com.dbn.vfs.file.DBConsoleVirtualFile;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

@BackgroundUpdate
public class ConsoleDeleteAction extends ProjectAction {

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        VirtualFile virtualFile = Lookups.getVirtualFile(e);
        if (virtualFile instanceof DBConsoleVirtualFile) {
            DBConsoleVirtualFile consoleVirtualFile = (DBConsoleVirtualFile) virtualFile;
            DatabaseConsoleManager consoleManager = DatabaseConsoleManager.getInstance(project);
            consoleManager.deleteConsole(consoleVirtualFile.getConsole());
        }
    }


    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        VirtualFile virtualFile = Lookups.getVirtualFile(e);
        boolean enabled = virtualFile instanceof DBConsoleVirtualFile;

        Presentation presentation = e.getPresentation();
        presentation.setEnabled(enabled);
        presentation.setText("Delete Console");
        presentation.setIcon(Icons.ACTION_CLOSE);
    }


}