package com.dbn.mcp;

import com.dbn.common.action.ProjectAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class MCPServerOpenAction extends ProjectAction {
  @Override
  protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
    try {
      MCPServerManager.getInstance(project).showMCPManager();
    } catch (MavenInvocationException | IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
    super.update(e, project);
  }
}
