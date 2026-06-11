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

package com.dbn.execution.statement.result.ui;

import com.dbn.common.action.DataKeys;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.ui.misc.DBNTableScrollPane;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Actions;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SessionId;
import com.dbn.data.find.SearchableDataComponent;
import com.dbn.data.grid.ui.table.basic.BasicTable;
import com.dbn.data.grid.ui.table.resultSet.ResultSetTable;
import com.dbn.data.model.resultSet.ResultSetDataModel;
import com.dbn.data.record.RecordViewInfo;
import com.dbn.execution.ExecutionManager;
import com.dbn.execution.common.result.ui.ExecutionResultFormBase;
import com.dbn.execution.statement.result.StatementExecutionCursorResult;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.nls.NlsResources.txt;

public class StatementExecutionResultForm extends ExecutionResultFormBase<StatementExecutionCursorResult> implements SearchableDataComponent {
    private JPanel mainPanel;
    private JPanel actionsPanel;
    private JPanel statusPanel;
    private JPanel searchPanel;
    private JPanel resultPanel;
    private JLabel statusLabel;
    private ResultSetTable<?> resultTable;
    private DBNTableScrollPane resultScrollPane;
    private final RecordViewInfo recordViewInfo;
    private final ActionToolbar actionToolbar;

    public StatementExecutionResultForm(@NotNull StatementExecutionCursorResult executionResult) {
        super(executionResult);
        actionToolbar = Actions.createActionToolbar(actionsPanel, false, "DBN.Execution.Statement.Result");
        actionsPanel.add(actionToolbar.getComponent());

        recordViewInfo = new RecordViewInfo(executionResult.getName(), executionResult.getIcon());

        resultPanel.setBorder(Borders.lineBorder(JBColor.border(), 0, 1, 1, 0));
        resultTable = new ResultSetTable<>(this, executionResult.getTableModel(), true, recordViewInfo);
        resultTable.setName(executionResult.getName());

        resultScrollPane.setViewportView(resultTable);
        initTableAddons(resultTable);

        Disposer.register(this, resultTable);
        Disposer.register(this, executionResult);
    }

    @Override
    protected void initAccessibility() {
        setAccessibleName(resultTable, txt("app.execution.aria.StatementExecutionResult", getExecutionResult().getName()));
        setAccessibleName(actionToolbar, txt("app.execution.aria.StatementExecutionResultActions"));
    }

    public void rebuildForm() {
        dispatch(() -> {
            StatementExecutionCursorResult executionResult = getExecutionResult();
            JScrollBar horizontalScrollBar = resultScrollPane.getHorizontalScrollBar();
            int horizontalScrolling = horizontalScrollBar.getValue();
            ResultSetTable<?> newResultSetTable = new ResultSetTable<>(this, executionResult.getTableModel(), true, recordViewInfo);
            resultTable = Disposer.replace(resultTable, newResultSetTable);
            resultScrollPane.setViewportView(resultTable);
            resultTable.setName(getExecutionResult().getName());

            initTableAddons(resultTable);
            horizontalScrollBar.setValue(horizontalScrolling);
        });
    }

    private static void initTableAddons(ResultSetTable resultTable) {
        resultTable.initTableGutter(); // TODO convert to addon
        resultTable.installMathAddon();
        resultTable.installValuePopupAddon();
        resultTable.installRecordViewerAddon();
    }

    @NotNull
    public ResultSetTable<?> getResultTable() {
        return Failsafe.nn(resultTable);
    }

    public void updateVisibleComponents() {
        dispatch(() -> {
            StatementExecutionCursorResult executionResult = getExecutionResult();
            ResultSetDataModel<?, ?> dataModel = executionResult.getTableModel();
            ConnectionHandler connection = executionResult.getConnection();
            String connectionName = connection.getName();
            SessionId sessionId = executionResult.getExecutionInput().getTargetSessionId();
            String connectionType =
                    sessionId == SessionId.MAIN ? txt("app.execution.label.MainSession") :
                    sessionId == SessionId.POOL ? txt("app.execution.label.PoolSession") : txt("app.execution.label.Session");
            int rowCount = dataModel.getRowCount();
            String partialResultInfo = dataModel.isResultSetExhausted() ? "" : txt("app.execution.label.PartialResult");
            long executeDuration = dataModel.getExecuteDuration();
            long fetchDuration = dataModel.getFetchDuration();

            String executionDurationInfo = executeDuration == -1 ? "" : txt("app.execution.label.ExecutedMillis", executeDuration);
            String fetchDurationInfo = fetchDuration == -1 ? "" : txt("app.execution.label.FetchedMillis", fetchDuration);

            statusLabel.setText(txt("app.execution.label.StatementResultStatus", connectionName, connectionType, rowCount, partialResultInfo, executionDurationInfo, fetchDurationInfo));
            statusLabel.setIcon(connection.getIcon());
        });
    }

    public void show() {
        StatementExecutionCursorResult executionResult = getExecutionResult();
        Project project = executionResult.getProject();
        ExecutionManager.getInstance(project).selectResultTab(executionResult);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public void highlightLoading(boolean loading) {
        ResultSetTable<?> resultTable = getResultTable();
        resultTable.setLoading(loading);
        UserInterface.repaint(resultTable);
    }

    /*********************************************************
     *              SearchableDataComponent                  *
     *********************************************************/

    @Override
    public @NotNull JPanel getSearchPanel() {
        return searchPanel;
    }

    @NotNull
    @Override
    public BasicTable getTable() {
        return getResultTable();
    }

    /********************************************************
     *                    Data Provider                     *
     ********************************************************/
    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.STATEMENT_EXECUTION_CURSOR_RESULT.is(dataId)) return getExecutionResult();
        return null;
    }
}
