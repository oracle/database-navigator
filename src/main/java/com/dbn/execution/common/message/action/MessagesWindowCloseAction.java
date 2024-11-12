package com.dbn.execution.common.message.action;

import com.dbn.common.icon.Icons;
import com.dbn.execution.ExecutionManager;
import com.dbn.execution.common.message.ui.tree.MessagesTree;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MessagesWindowCloseAction extends AbstractExecutionMessagesAction {
    public MessagesWindowCloseAction() {
        super("Close", null, Icons.EXEC_RESULT_CLOSE);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull MessagesTree messagesTree) {
        ExecutionManager executionManager = ExecutionManager.getInstance(project);
        executionManager.removeMessagesTab();
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable MessagesTree target) {
        presentation.setText("Close");
        presentation.setIcon(Icons.EXEC_RESULT_CLOSE);
    }
}
