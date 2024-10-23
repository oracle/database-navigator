package com.dbn.language.editor.action;

import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.Lookups;
import com.dbn.common.action.ProjectAction;
import com.dbn.common.icon.Icons;
import com.dbn.connection.console.DatabaseConsoleManager;
import com.dbn.debugger.DatabaseDebuggerManager;
import com.dbn.vfs.file.DBConsoleVirtualFile;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

@BackgroundUpdate
public class ConsoleSaveToFileAction extends ProjectAction {

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        VirtualFile file = Lookups.getVirtualFile(e);
        if (file instanceof DBConsoleVirtualFile) {
            DBConsoleVirtualFile consoleFile = (DBConsoleVirtualFile) file;
            DatabaseConsoleManager consoleManager = DatabaseConsoleManager.getInstance(project);
            consoleManager.saveConsoleToFile(consoleFile);
        }
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        VirtualFile virtualFile = Lookups.getVirtualFile(e);
        boolean visible = virtualFile instanceof DBConsoleVirtualFile && !DatabaseDebuggerManager.isDebugConsole(virtualFile);

        Presentation presentation = e.getPresentation();
        presentation.setEnabled(true);
        presentation.setVisible(visible);
        presentation.setText("Save to File");
        presentation.setDescription("Save console to file");
        presentation.setIcon(Icons.CODE_EDITOR_SAVE_TO_FILE);
    }
}