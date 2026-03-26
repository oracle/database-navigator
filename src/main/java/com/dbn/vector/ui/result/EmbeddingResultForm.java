/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.vector.ui.result;

import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.util.Actions;
import com.dbn.execution.common.result.ui.ExecutionResultFormBase;
import com.dbn.vector.model.VectorEmbeddingExecutionResult;
import com.dbn.vector.model.VectorEmbeddingResult;
import com.intellij.openapi.actionSystem.ActionToolbar;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;

public class EmbeddingResultForm extends ExecutionResultFormBase<VectorEmbeddingExecutionResult> {
    private JPanel mainPanel;
    private DBNScrollPane sourceDataScrollPane;
    private JPanel actionsPanel;
    private JPanel summaryPanel;

    public EmbeddingResultForm(@NotNull VectorEmbeddingExecutionResult executionResult) {
        super(executionResult);
        initializeComponents();
    }

    private VectorEmbeddingResult getEmbeddingResult() {
        return getExecutionResult().getVectorEmbeddingResult();
    }

    private void initializeComponents() {
        initializeTable();
        initializeSummary();
        createActionsPanel();
    }

    private void initializeSummary() {
        VectorEmbeddingResult result = getEmbeddingResult();
        EmbeddingResultSummaryForm summaryForm = new EmbeddingResultSummaryForm(this, result);
        summaryPanel.add(summaryForm.getComponent());
    }

    private void createActionsPanel() {
        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, false, "DBNavigator.ActionGroup.VectorEmbeddingResult");
        setAccessibleName(actionToolbar, txt("app.execution.aria.VectorEmbeddingExecutionResultActions"));
        actionsPanel.add(actionToolbar.getComponent());
    }

    private void initializeTable() {
        VectorEmbeddingResult result = getEmbeddingResult();
        VectorEmbeddingResultsTableModel sourceDataModel = new VectorEmbeddingResultsTableModel(result);
        VectorEmbeddingResultsTable sourceDataTable = new VectorEmbeddingResultsTable(this, sourceDataModel);
        sourceDataScrollPane.setViewportView(sourceDataTable);
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
