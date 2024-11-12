package com.dbn.execution.common.message.action;

import com.dbn.common.icon.Icons;
import com.dbn.common.ui.tree.Trees;
import com.dbn.execution.common.message.ui.tree.MessagesTree;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MessagesTreeExpandAction extends AbstractExecutionMessagesAction {

    public MessagesTreeExpandAction() {
        super("Expand All", null, Icons.ACTION_EXPAND_ALL);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull MessagesTree messagesTree) {
        Trees.expandAll(messagesTree);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable MessagesTree target) {
        presentation.setText("Expand All");
        presentation.setIcon(Icons.ACTION_EXPAND_ALL);
    }
}