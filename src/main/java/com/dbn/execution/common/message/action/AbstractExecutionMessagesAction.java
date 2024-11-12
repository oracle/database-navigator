package com.dbn.execution.common.message.action;

import com.dbn.common.action.ContextAction;
import com.dbn.common.action.DataKeys;
import com.dbn.execution.common.message.ui.tree.MessagesTree;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

abstract class AbstractExecutionMessagesAction extends ContextAction<MessagesTree> {

    AbstractExecutionMessagesAction(String name, String description, Icon icon) {
        super(name, description, icon);
    }

    @Nullable
    protected MessagesTree getTarget(@NotNull AnActionEvent e) {
        return DataKeys.MESSAGES_TREE.getData(e.getDataContext());
    }
}
