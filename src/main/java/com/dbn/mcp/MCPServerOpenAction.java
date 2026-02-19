package com.dbn.mcp;

import com.dbn.common.action.ProjectAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class MCPServerOpenAction extends ProjectAction {
    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        MCPServerManager.getInstance(project).showMCPManager();
    }
}
