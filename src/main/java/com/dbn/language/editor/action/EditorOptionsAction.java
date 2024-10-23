package com.dbn.language.editor.action;

import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.BasicActionGroup;
import com.dbn.common.action.Lookups;
import com.dbn.common.icon.Icons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.DatabaseFeature;
import com.dbn.options.ConfigId;
import com.dbn.options.action.ProjectSettingsOpenAction;
import com.dbn.vfs.DBConsoleType;
import com.dbn.vfs.file.DBConsoleVirtualFile;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@BackgroundUpdate
public class EditorOptionsAction extends BasicActionGroup {
    @NotNull
    @Override
    protected AnAction[] loadChildren(AnActionEvent e) {
        List<AnAction> actions = new ArrayList<>();
        VirtualFile virtualFile = Lookups.getVirtualFile(e);
        if (virtualFile instanceof DBConsoleVirtualFile) {
            actions.add(new ConsoleRenameAction());
            actions.add(new ConsoleDeleteAction());
            actions.add(new ConsoleSaveToFileAction());
            actions.add(Separator.getInstance());

            DBConsoleVirtualFile consoleVirtualFile = (DBConsoleVirtualFile) virtualFile;
            if (consoleVirtualFile.getType() != DBConsoleType.DEBUG) {
                actions.add(new ConsoleCreateAction(DBConsoleType.STANDARD));
            }

            ConnectionHandler connection = consoleVirtualFile.getConnection();
            if (DatabaseFeature.DEBUGGING.isSupported(connection)) {
                actions.add(new ConsoleCreateAction(DBConsoleType.DEBUG));
            }
        }
        actions.add(Separator.getInstance());
        actions.add(new ProjectSettingsOpenAction(ConfigId.CODE_EDITOR, false));

        return actions.toArray(new AnAction[0]);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        VirtualFile virtualFile = Lookups.getVirtualFile(e);

        Presentation presentation = e.getPresentation();
        presentation.setVisible(virtualFile instanceof DBConsoleVirtualFile);
        presentation.setText("Options");
        presentation.setIcon(Icons.ACTION_OPTIONS);
    }
}
