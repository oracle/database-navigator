package com.dbn.language.editor.action;

import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.Lookups;
import com.dbn.options.ConfigId;
import com.dbn.options.action.ProjectSettingsOpenAction;
import com.dbn.vfs.file.DBConsoleVirtualFile;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

@BackgroundUpdate
public class EditorSettingsOpenAction extends ProjectSettingsOpenAction {
    public EditorSettingsOpenAction() {
        super(ConfigId.CODE_COMPLETION, true);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        super.update(e, project);
        Presentation presentation = e.getPresentation();
        VirtualFile virtualFile = Lookups.getVirtualFile(e);
        presentation.setVisible(!(virtualFile instanceof DBConsoleVirtualFile));
    }
}
