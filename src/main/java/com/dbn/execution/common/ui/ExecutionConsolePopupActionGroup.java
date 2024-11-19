package com.dbn.execution.common.ui;

import com.dbn.common.action.BasicAction;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.tab.DBNTabbedPane;
import com.dbn.common.util.Dialogs;
import com.dbn.execution.statement.result.StatementExecutionCursorResult;
import com.dbn.execution.statement.result.ui.RenameExecutionResultDialog;
import com.dbn.execution.statement.result.ui.StatementExecutionResultForm;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import org.jetbrains.annotations.NotNull;

import java.awt.Component;

import static com.dbn.common.ui.util.ClientProperty.TAB_CONTENT;

public class ExecutionConsolePopupActionGroup extends DefaultActionGroup {
    private final WeakRef<ExecutionConsoleForm> executionConsoleForm;

    public ExecutionConsolePopupActionGroup(ExecutionConsoleForm executionConsoleForm) {
        this.executionConsoleForm = WeakRef.of(executionConsoleForm);
        add(renameAction());
        addSeparator();
        add(closeAction());
        add(closeAllAction());
        add(closeAllButThisAction());
    }

    public ExecutionConsoleForm getExecutionConsoleForm() {
        return executionConsoleForm.ensure();
    }

    private Component getTabComponent(AnActionEvent e) {
        DBNTabbedPane<DBNForm> tabs = getExecutionConsoleForm().getResultTabs();
        int popupTabIndex = tabs.getPopupTabIndex();
        if (popupTabIndex < 0) return null;

        return tabs.getComponentAt(popupTabIndex);
    }

    AnAction renameAction() {
        return new BasicAction("Rename Result...") {
            @Override
            public void update(@NotNull AnActionEvent e) {
                Component component = getTabComponent(e);
                boolean visible = false;
                if (component != null) {
                    Object object = TAB_CONTENT.get(component);
                    visible = object instanceof StatementExecutionResultForm;
                }
                e.getPresentation().setVisible(visible);
            }

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                Component component = getTabComponent(e);
                if (component == null) return;

                Object object = TAB_CONTENT.get(component);
                if (!(object instanceof StatementExecutionResultForm)) return;

                StatementExecutionResultForm resultForm = (StatementExecutionResultForm) object;
                StatementExecutionCursorResult executionResult = resultForm.getExecutionResult();
                Dialogs.show(() -> new RenameExecutionResultDialog(executionResult), (dialog, exitCode) -> {
                    DBNTabbedPane<DBNForm> tabs = getExecutionConsoleForm().getResultTabs();
                    tabs.setTabTitle(component, executionResult.getName());
                });
            }
        };
    }

    private @NotNull BasicAction closeAction() {
        return new BasicAction("Close") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                Component component = getTabComponent(e);
                if (component == null) return;

                getExecutionConsoleForm().removeTab(component);
            }
        };
    }

    private AnAction closeAllAction() {
        return new BasicAction("Close All") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                getExecutionConsoleForm().removeAllTabs();
            }
        };
    }

    private AnAction closeAllButThisAction() {
        return new BasicAction("Close All But This") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                Component component = getTabComponent(e);
                if (component == null) return;

                getExecutionConsoleForm().removeAllExceptTab(component);
            }
        };
    }
}
