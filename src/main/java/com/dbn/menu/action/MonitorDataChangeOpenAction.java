package com.dbn.menu.action;

import com.dbn.common.action.ProjectAction;
import com.dbn.event.notification.EventNotificationManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class MonitorDataChangeOpenAction extends ProjectAction {
  @Override
  protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
    EventNotificationManager eventNotificationManager = EventNotificationManager.getInstance(project);
    eventNotificationManager.showEventNotificationConsole();
  }

  @Override
  protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
    super.update(e, project);
  }
}
