/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.liquibase.workflows.ui;

import com.dbn.common.action.DataKeys;
import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.list.ColoredListCellRenderer;
import com.dbn.common.ui.util.Accessibility;
import com.dbn.common.ui.util.Splitters;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Actions;
import com.dbn.execution.common.result.ui.ExecutionResultFormBase;
import com.dbn.liquibase.execution.LiquibaseExecutionResult;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.execution.action.LiquibaseWorkflowCloseAction;
import com.dbn.liquibase.execution.action.LiquibaseWorkflowRerunAction;
import com.dbn.liquibase.execution.action.LiquibaseWorkflowSettingsAction;
import com.dbn.liquibase.execution.action.LiquibaseWorkflowStopAction;
import com.dbn.liquibase.execution.ui.LiquibaseExecutionResultForm;
import com.dbn.liquibase.workflows.LiquibaseWorkflowResult;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.ListModel;
import java.util.List;
import java.util.Map;

import static com.dbn.common.ui.util.Lists.onSelectionChange;
import static com.dbn.nls.NlsResources.txt;

/** Result form for a Liquibase workflow, with one operation result available at a time. */
public class LiquibaseWorkflowResultForm extends ExecutionResultFormBase<LiquibaseWorkflowResult> {
    private JPanel mainPanel;
    private JPanel resultPanel;
    private JList<LiquibaseOperation> operationsList;
    private JPanel actionsPanel;
    private JSplitPane splitPane;

    private final Runnable resultListener = this::refreshSelectedOperation;
    private final Map<LiquibaseOperation, LiquibaseExecutionResultForm> resultForms =
            DisposableContainers.map(this);
    private boolean operationSelectionChanged;
    private boolean updatingOperationSelection;

    public LiquibaseWorkflowResultForm(@NotNull LiquibaseWorkflowResult result) {
        super(result);

        initActionsPanel();
        operationsList.setCellRenderer(new OperationListCellRenderer());
        onSelectionChange(operationsList, e -> {
            if (!e.getValueIsAdjusting() && !updatingOperationSelection) {
                operationSelectionChanged = operationsList.getSelectedValue() !=
                        getExecutionResult().getContext().getCurrentOperation();
            }
            showSelectedOperation();
        });
        operationsList.setModel(createModel(result));
        selectProcessedOperation();

        Splitters.setSplitPaneProportion(splitPane, 0.2);
        result.addListener(resultListener);
    }

    private void initActionsPanel() {
        ActionToolbar actionToolbar = Actions.createActionToolbar(
                actionsPanel,
                false,
                new LiquibaseWorkflowCloseAction(),
                new LiquibaseWorkflowStopAction(),
                new LiquibaseWorkflowRerunAction(),
                Actions.SEPARATOR,
                new LiquibaseWorkflowSettingsAction());
        Accessibility.setAccessibleName(actionToolbar, txt("app.liquibase.aria.ExecutionResultActions"));
        actionsPanel.add(actionToolbar.getComponent());
    }

    private void refreshSelectedOperation() {
        dispatch(() -> {
            if (isDisposed()) return;
            if (!operationSelectionChanged) selectProcessedOperation();
            operationsList.repaint();
            showSelectedOperation();
        });
    }

    private void selectProcessedOperation() {
        LiquibaseOperation operation = getExecutionResult().getContext().getCurrentOperation();
        if (operation != null) {
            selectOperation(operation);
        } else if (operationsList.getSelectedIndex() < 0 && operationsList.getModel().getSize() > 0) {
            selectOperation(operationsList.getModel().getElementAt(0));
        }
    }

    private void selectOperation(@NotNull LiquibaseOperation operation) {
        updatingOperationSelection = true;
        try {
            operationsList.setSelectedValue(operation, true);
        } finally {
            updatingOperationSelection = false;
        }
    }

    private void showSelectedOperation() {
        LiquibaseOperation operation = operationsList.getSelectedValue();
        resultPanel.removeAll();
        if (operation == null) return;

        LiquibaseExecutionResult result = findResult(operation);
        if (result == null) return;

        LiquibaseExecutionResultForm resultForm = resultForms.computeIfAbsent(
                operation, key -> new LiquibaseExecutionResultForm(result, true));
        resultPanel.add(resultForm.getComponent());
        UserInterface.repaint(resultPanel);
    }

    @Nullable
    private LiquibaseExecutionResult findResult(@NotNull LiquibaseOperation operation) {
        List<LiquibaseExecutionResult> results = getExecutionResult().getResults();
        for (LiquibaseExecutionResult result : results) {
            if (result.getOperation() == operation) return result;
        }
        return null;
    }

    @NotNull
    private ListModel<LiquibaseOperation> createModel(@NotNull LiquibaseWorkflowResult result) {
        DefaultListModel<LiquibaseOperation> model = new DefaultListModel<>();
        for (LiquibaseOperation operation : result.getContext().getInput().getWorkflow().getOperations()) {
            model.addElement(operation);
        }
        return model;
    }

    private class OperationListCellRenderer extends ColoredListCellRenderer<LiquibaseOperation> {
        @Override
        protected void customize(
                @NotNull JList<? extends LiquibaseOperation> list,
                LiquibaseOperation value,
                int index,
                boolean selected,
                boolean hasFocus) {
            setIcon(getStatusIcon(value));
            append(value.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }

        private Icon getStatusIcon(@NotNull LiquibaseOperation operation) {
            for (LiquibaseExecutionResult result : getExecutionResult().getResults()) {
                if (result.getOperation() != operation) continue;
                return switch (result.getStatus()) {
                    case RUNNING -> Icons.ACTION_REFRESH;
                    case DONE -> Icons.COMMON_STATUS_SUCCESS;
                    case FAILED -> Icons.COMMON_STATUS_ERROR;
                    case CANCELLED -> Icons.COMMON_WARNING;
                    case SKIPPED, NEW -> Icons.COMMON_WARNING_INACTIVE;
                };
            }
            return Icons.COMMON_WARNING_INACTIVE;
        }
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.LIQUIBASE_WORKFLOW_RESULT.is(dataId)) return getExecutionResult();
        return null;
    }

    @Override
    public void disposeInner() {
        getExecutionResult().removeListener(resultListener);
        super.disposeInner();
    }
}
