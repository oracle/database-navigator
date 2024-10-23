package com.dbn.connection.config.action;

import com.dbn.common.action.BasicActionGroup;
import com.dbn.common.icon.Icons;
import com.dbn.common.util.Actions;
import com.dbn.connection.DatabaseType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

public class CreateConnectionDropdownAction extends BasicActionGroup {
    private final AnAction[] actions = new AnAction[] {
            new ConnectionCreateAction(DatabaseType.ORACLE),
            new ConnectionCreateAction(DatabaseType.MYSQL),
            new ConnectionCreateAction(DatabaseType.POSTGRES),
            new ConnectionCreateAction(DatabaseType.SQLITE),
            new ConnectionCreateAction(null),
            Actions.SEPARATOR,
            new TnsNamesImportAction()
    };

    @NotNull
    @Override
    public AnAction[] loadChildren(AnActionEvent e) {
        return actions;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        presentation.setText("New Connection");
        presentation.setIcon(Icons.ACTION_ADD);
    }
}
