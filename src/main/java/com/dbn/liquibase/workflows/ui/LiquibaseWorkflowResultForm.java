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
import com.dbn.common.ui.util.Accessibility;
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
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.dbn.common.ui.CardLayouts.addBlankCard;
import static com.dbn.common.ui.CardLayouts.addCard;
import static com.dbn.common.ui.CardLayouts.getCard;
import static com.dbn.common.ui.CardLayouts.showBlankCard;
import static com.dbn.common.ui.CardLayouts.showCard;
import static com.dbn.common.ui.util.Lists.onSelectionChange;
import static com.dbn.nls.NlsResources.txt;

/** Result form for a Liquibase workflow, with one operation result available at a time. */
public class LiquibaseWorkflowResultForm extends ExecutionResultFormBase<LiquibaseWorkflowResult> {
    private JPanel mainPanel;
    private JPanel resultPanel;
    private JList<LiquibaseOperation> operationsList;
    private JPanel actionsPanel;

    private final Runnable resultListener = this::refreshSelectedOperation;
    private final Map<LiquibaseOperation, LiquibaseExecutionResultForm> resultForms =
            DisposableContainers.map(this);
    private final AtomicBoolean refreshPending = new AtomicBoolean();
    private boolean operationSelectionChanged;
    private boolean updatingOperationSelection;

    public LiquibaseWorkflowResultForm(@NotNull LiquibaseWorkflowResult result) {
        super(result);

        initActionsPanel();
        addBlankCard(resultPanel);
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
        if (!refreshPending.compareAndSet(false, true)) return;
        dispatch(() -> {
            try {
                if (isDisposed()) return;
                if (!operationSelectionChanged) selectProcessedOperation();
                operationsList.repaint();
                showSelectedOperation();
            } finally {
                refreshPending.set(false);
            }
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
        if (operation == null) {
            showBlankCard(resultPanel);
            return;
        }

        LiquibaseExecutionResult result = findResult(operation);
        if (result == null) {
            showBlankCard(resultPanel);
            return;
        }

        LiquibaseExecutionResultForm resultForm = resultForms.computeIfAbsent(
                operation, key -> new LiquibaseExecutionResultForm(result, true));
        if (getCard(resultPanel, operation.name()) == null) {
            addCard(resultPanel, resultForm, operation.name());
        }
        showCard(resultPanel, operation.name());
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

    private class OperationListCellRenderer extends JPanel implements ListCellRenderer<LiquibaseOperation> {
        private final JLabel operationLabel = new JLabel();
        private final JLabel statusLabel = new JLabel();

        private OperationListCellRenderer() {
            super(new BorderLayout(16, 0));
            setBorder(JBUI.Borders.empty(2, 4));
            add(operationLabel, BorderLayout.CENTER);
            add(statusLabel, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(
                @NotNull JList<? extends LiquibaseOperation> list,
                LiquibaseOperation value,
                int index,
                boolean selected,
                boolean hasFocus) {
            boolean focused = hasFocus || list.hasFocus();
            setOpaque(true);
            setBackground(selected ? UIUtil.getListSelectionBackground(focused) : list.getBackground());
            Icon statusIcon = getStatusIcon(value);
            operationLabel.setForeground(selected
                    ? UIUtil.getListSelectionForeground(focused)
                    : hasResult(value) ? list.getForeground() : UIUtil.getInactiveTextColor());
            operationLabel.setText(value.getName());
            statusLabel.setIcon(statusIcon);
            return this;
        }

        private Icon getStatusIcon(@NotNull LiquibaseOperation operation) {
            for (LiquibaseExecutionResult result : getExecutionResult().getResults()) {
                if (result.getOperation() != operation) continue;
                return switch (result.getStatus()) {
                    case RUNNING -> Icons.COMMON_STATUS_RUNNING;
                    case DONE -> Icons.COMMON_STATUS_SUCCESS;
                    case FAILED -> Icons.COMMON_STATUS_ERROR;
                    case CANCELLED -> Icons.COMMON_WARNING;
                    case SKIPPED, NEW -> Icons.COMMON_WARNING_INACTIVE;
                };
            }
            return Icons.COMMON_EMPTY;
        }

        private boolean hasResult(@NotNull LiquibaseOperation operation) {
            for (LiquibaseExecutionResult result : getExecutionResult().getResults()) {
                if (result.getOperation() == operation) return true;
            }
            return false;
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
