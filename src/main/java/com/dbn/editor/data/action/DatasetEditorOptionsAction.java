package com.dbn.editor.data.action;

import com.dbn.common.action.BasicActionGroup;
import com.dbn.common.icon.Icons;
import com.dbn.common.util.Actions;
import com.dbn.options.ConfigId;
import com.dbn.options.action.ProjectSettingsOpenAction;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

public class DatasetEditorOptionsAction extends BasicActionGroup {

    @NotNull
    @Override
    public AnAction[] loadChildren(AnActionEvent e) {
        return new AnAction[]{
                new DataSortingOpenAction(),
                new ColumnSetupOpenAction(),
                Actions.SEPARATOR,
                new ProjectSettingsOpenAction(ConfigId.DATA_EDITOR, false)};
    }


    @Override
    public void update(@NotNull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        presentation.setText("Options");
        presentation.setIcon(Icons.ACTION_OPTIONS);
    }
}
