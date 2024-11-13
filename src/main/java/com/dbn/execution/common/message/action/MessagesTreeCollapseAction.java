package com.dbn.execution.common.message.action;

import com.dbn.common.icon.Icons;
import com.dbn.common.ui.tree.Trees;
import com.dbn.execution.common.message.ui.tree.MessagesTree;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MessagesTreeCollapseAction extends AbstractExecutionMessagesAction {

    public MessagesTreeCollapseAction() {
        super("Collapse All", null, Icons.ACTION_COLLAPSE_ALL);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull MessagesTree messagesTree) {
        Trees.collapseAll(messagesTree);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable MessagesTree target) {
        presentation.setText("Collapse All");
        presentation.setIcon(Icons.ACTION_COLLAPSE_ALL);
    }
}