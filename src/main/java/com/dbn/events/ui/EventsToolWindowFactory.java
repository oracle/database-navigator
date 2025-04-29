package com.dbn.events.ui;

import com.dbn.common.ui.window.DBNToolWindowFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.icon.Icons.WINDOW_DATABASE_DIAGNOSTICS;

public class EventsToolWindowFactory extends DBNToolWindowFactory {
  @Override
  protected void initialize(@NotNull ToolWindow toolWindow) {
    toolWindow.setTitle("DB Events");
    toolWindow.setStripeTitle("DB Events");
    toolWindow.setIcon(WINDOW_DATABASE_DIAGNOSTICS.get());
  }

  @Override
  public void createContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    // no content by default - will be created on demand
    // (just set default visibility behavior)

    toolWindow.setToHideOnEmptyContent(true);
    toolWindow.setAutoHide(false);
    toolWindow.setAvailable(false, null);
  }

  @Override
  public boolean shouldBeAvailable(@NotNull Project project) {
    return false;
  }
}