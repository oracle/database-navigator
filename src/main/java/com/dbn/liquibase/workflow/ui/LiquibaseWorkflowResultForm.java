/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.liquibase.workflow.ui;

import com.dbn.common.action.DataKeys;
import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.icon.Icons;
import com.dbn.common.task.TaskStatus;
import com.dbn.common.ui.util.Accessibility;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Actions;
import com.dbn.execution.common.result.ui.ExecutionResultFormBase;
import com.dbn.liquibase.operation.LiquibaseOperation;
import com.dbn.liquibase.operation.LiquibaseOperationResult;
import com.dbn.liquibase.operation.ui.LiquibaseOperationResultForm;
import com.dbn.liquibase.workflow.LiquibaseWorkflowResult;
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
    private final Map<LiquibaseOperation, LiquibaseOperationResultForm> resultForms = DisposableContainers.map(this);
    private final AtomicBoolean refreshPending = new AtomicBoolean();
    private boolean operationSelectionChanged;
    private boolean updatingOperationSelection;
    private TaskStatus actionStatus;

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

    @Override
    public void setExecutionResult(@NotNull LiquibaseWorkflowResult result) {
        getExecutionResult().removeListener(resultListener);
        super.setExecutionResult(result);
    }

    @Override
    protected void rebuildForm() {
        resultForms.clear();
        resultPanel.removeAll();
        addBlankCard(resultPanel);
        operationsList.setModel(createModel(getExecutionResult()));
        operationSelectionChanged = false;
        actionStatus = null;
        selectProcessedOperation();
        getExecutionResult().addListener(resultListener);
        updateActionToolbarState();
        resultPanel.revalidate();
        resultPanel.repaint();
    }

    private void initActionsPanel() {
        ActionToolbar actionToolbar = Actions.createActionToolbar(
                actionsPanel,
                false,
                "DBN.Execution.Liquibase.Result");
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
                updateActionToolbarState();
            } finally {
                refreshPending.set(false);
            }
        });
    }

    private void updateActionToolbarState() {
        TaskStatus status = getExecutionResult().getStatus();
        if (status == actionStatus) return;

        actionStatus = status;
        updateActionToolbars();
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

        LiquibaseOperationResult result = findResult(operation);
        if (result == null) {
            showBlankCard(resultPanel);
            return;
        }

        LiquibaseOperationResultForm resultForm = resultForms.computeIfAbsent(
                operation, key -> new LiquibaseOperationResultForm(result, true));
        if (getCard(resultPanel, operation.name()) == null) {
            addCard(resultPanel, resultForm, operation.name());
        }
        showCard(resultPanel, operation.name());
        UserInterface.repaint(resultPanel);
    }

    @Nullable
    private LiquibaseOperationResult findResult(@NotNull LiquibaseOperation operation) {
        List<LiquibaseOperationResult> results = getExecutionResult().getResults();
        for (LiquibaseOperationResult result : results) {
            if (result.getOperation() == operation) return result;
        }
        return null;
    }

    @NotNull
    private ListModel<LiquibaseOperation> createModel(@NotNull LiquibaseWorkflowResult result) {
        DefaultListModel<LiquibaseOperation> model = new DefaultListModel<>();
        for (LiquibaseOperation operation : result.getInput().getWorkflow().getOperations()) {
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
                    : isInactive(value) ? UIUtil.getInactiveTextColor() : list.getForeground());
            operationLabel.setText(value.getName());
            statusLabel.setIcon(statusIcon);
            return this;
        }

        private Icon getStatusIcon(@NotNull LiquibaseOperation operation) {
            for (LiquibaseOperationResult result : getExecutionResult().getResults()) {
                if (result.getOperation() != operation) continue;
                return switch (result.getStatus()) {
                    case NEW -> Icons.COMMON_EMPTY;
                    case RUNNING -> Icons.COMMON_STATUS_RUNNING;
                    case DONE -> Icons.COMMON_STATUS_SUCCESS;
                    case FAILED -> Icons.COMMON_STATUS_ERROR;
                    case CANCELLED -> Icons.COMMON_WARNING;
                    case SKIPPED -> Icons.COMMON_WARNING_OUTLINE;
                    case BYPASSED -> Icons.COMMON_EMPTY;
                };
            }
            return Icons.COMMON_EMPTY;
        }

        private boolean isInactive(@NotNull LiquibaseOperation operation) {
            for (LiquibaseOperationResult result : getExecutionResult().getResults()) {
                if (result.getOperation() == operation) return result.getStatus() == TaskStatus.BYPASSED;
            }
            return true;
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
