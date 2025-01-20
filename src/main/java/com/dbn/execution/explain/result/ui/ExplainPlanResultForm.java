/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.execution.explain.result.ui;

import com.dbn.common.action.DataKeys;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.ui.tree.Trees;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.util.Actions;
import com.dbn.execution.ExecutionManager;
import com.dbn.execution.ExecutionResult;
import com.dbn.execution.common.result.ui.ExecutionResultFormBase;
import com.dbn.execution.explain.result.ExplainPlanResult;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;

public class ExplainPlanResultForm extends ExecutionResultFormBase<ExplainPlanResult> {
    private JPanel mainPanel;
    private JPanel actionsPanel;
    private JPanel resultPanel;
    private DBNScrollPane resultScrollPane;

    private final ExplainPlanTreeTable explainPlanTreeTable;

    public ExplainPlanResultForm(@NotNull ExplainPlanResult explainPlanResult) {
        super(explainPlanResult);
        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, false, "DBNavigator.ActionGroup.ExplainPlanResult");
        setAccessibleName(actionToolbar, txt("app.execution.aria.ExplainPlanResultActions"));

        actionsPanel.add(actionToolbar.getComponent());

        resultPanel.setBorder(Borders.tableBorder(0,1,0,0));
        ExplainPlanTreeTableModel treeTableModel = new ExplainPlanTreeTableModel(explainPlanResult);
        explainPlanTreeTable = new ExplainPlanTreeTable(this, treeTableModel);

        resultScrollPane.setViewportView(explainPlanTreeTable);
    }

    public void show() {
        ExecutionResult<?> executionResult = getExecutionResult();
        Project project = executionResult.getProject();
        ExecutionManager executionManager = ExecutionManager.getInstance(project);
        executionManager.selectResultTab(executionResult);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public void collapseAllNodes() {
        Trees.collapseAll(explainPlanTreeTable.getTree());
    }

    public void expandAllNodes() {
        Trees.expandAll(explainPlanTreeTable.getTree());
    }

    /********************************************************
     *                    Data Provider                     *
     ********************************************************/
    @Override
    public @Nullable Object getData(@NotNull String dataId) {
        if (DataKeys.EXPLAIN_PLAN_RESULT.is(dataId)) return getExecutionResult();
        return null;
    }
}
