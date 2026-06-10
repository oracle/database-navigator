package com.dbn.menu.action;

import com.dbn.common.action.ProjectAction;
import com.dbn.event.notification.EventNotificationManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.database.DatabaseFeature.DATA_CHANGE_NOTIFICATION;
import static com.dbn.nls.NlsResources.txt;

public class MonitorDataChangeOpenAction extends ProjectAction {
    public MonitorDataChangeOpenAction() {
        super(txt("app.menu.action.DataEventsMonitor"));
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        EventNotificationManager eventNotificationManager = EventNotificationManager.getInstance(project);
        eventNotificationManager.showEventNotificationConsole();
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        boolean visible = isVisible(project);

        Presentation presentation = e.getPresentation();
        presentation.setVisible(visible);
    }

    private boolean isVisible(@NotNull Project project) {
        return DATA_CHANGE_NOTIFICATION.isSupported(project);
    }
}
