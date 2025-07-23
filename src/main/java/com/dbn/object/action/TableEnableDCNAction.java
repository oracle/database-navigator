package com.dbn.object.action;

import com.dbn.common.icon.Icons;
import com.dbn.event.notification.EventNotificationManager;
import com.dbn.object.DBTable;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.nls.NlsResources.txt;

public class TableEnableDCNAction extends AnObjectAction<DBTable> {
    public TableEnableDCNAction(DBTable table) {
        super(table);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable DBTable target) {
        presentation.setText(txt("app.objects.action.EnableDataChangeNotifications"));
        presentation.setIcon(Icons.TABLE_ENABLE_DCN);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull DBTable target) {
        EventNotificationManager eventNotificationManager = EventNotificationManager.getInstance(project);
        eventNotificationManager.openEditorAndConfig(target);
    }

}
