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

package com.dbn.editor.data.nested;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.misc.DBNTableScrollPane;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.util.Messages;
import com.dbn.connection.PooledConnection;
import com.dbn.connection.Resources;
import com.dbn.connection.jdbc.DBNResultSet;
import com.dbn.connection.jdbc.DBNStatement;
import com.dbn.data.grid.ui.table.resultSet.ResultSetTable;
import com.dbn.data.model.resultSet.ResultSetDataModel;
import com.dbn.data.record.RecordViewInfo;
import com.dbn.database.common.statement.SqlLiterals;
import com.dbn.editor.data.filter.DatasetFilterInput;
import com.dbn.object.DBColumn;
import com.dbn.object.DBNestedTable;
import com.dbn.object.DBTable;
import com.intellij.openapi.Disposable;
import com.intellij.util.ui.AsyncProcessIcon;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.sql.SQLException;
import java.util.List;

import static com.dbn.common.thread.Dispatch.async;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

class DBNestedTableViewerForm extends DBNFormBase {
    private static final int MAX_ROWS = 1000;

    private final DBNestedTable nestedTable;
    private final DatasetFilterInput parentFilter;
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JLabel loadingLabel;
    private JPanel loadingIconPanel;
    private JPanel loadingActionPanel;
    private JPanel loadingDataPanel;
    private JPanel resultPanel;
    private DBNTableScrollPane resultScrollPane;
    private final ResultSetTable resultTable;
    private ResultSetDataModel<?, ?> dataModel;

    DBNestedTableViewerForm(@NotNull Disposable parent, @NotNull DBNestedTable nestedTable, DatasetFilterInput parentFilter) {
        super(parent, nestedTable.getProject());
        this.nestedTable = nestedTable;
        this.parentFilter = parentFilter;
        this.dataModel = new ResultSetDataModel<>(nestedTable.getConnection());
        this.dataModel.setHeader(new DBNestedTableViewerModelHeader(nestedTable));

        DBColumn parentTableColumn = nestedTable.getParentTableColumn();
        DBNHeaderForm headerForm = new DBNHeaderForm(this, nvl(parentTableColumn, nestedTable));
        headerPanel.add(headerForm.getComponent());

        loadingIconPanel.add(new AsyncProcessIcon("Loading"));
        loadingDataPanel.setVisible(true);

        RecordViewInfo recordViewInfo = new RecordViewInfo(nestedTable.getPresentableName(), nestedTable.getIcon());
        this.resultTable = new ResultSetTable<>(this, dataModel, false, recordViewInfo);
        this.resultTable.setName(nestedTable.getPresentableName());
        this.resultTable.installValuePopupAddon();
        this.resultTable.installRecordViewerAddon();
        this.resultTable.setLoading(true);

        resultScrollPane.setViewportView(resultTable);
        resultPanel.setBorder(Borders.COMPONENT_OUTLINE_BORDER);

        Disposer.register(this, dataModel);
        whenFirstShown(this::loadData);
    }

    private void loadData() {
        async(mainPanel, this::loadResult, this::applyResult);
    }

    private LoadResult loadResult() {
        try {
            return new LoadResult(executeQuery(), null);
        } catch (Exception e) {
            conditionallyLog(e);
            return new LoadResult(null, e);
        }
    }

    private ResultSetDataModel<?, ?> executeQuery() throws SQLException {
        String query = createQuery();
        return PooledConnection.call(nestedTable.getConnection().createConnectionContext(), conn -> {
            DBNStatement statement = null;
            DBNResultSet resultSet = null;
            try {
                statement = conn.createStatement();
                statement.setFetchSize(MAX_ROWS);
                resultSet = statement.executeQuery(query);

                ResultSetDataModel<?, ?> model = new ResultSetDataModel<>(resultSet, nestedTable.getConnection(), 0);
                model.fetchNextRecords(MAX_ROWS, false);
                model.setResultSetExhausted(true);
                return model;
            } finally {
                Resources.close(resultSet);
                Resources.close(statement);
            }
        });
    }

    @NonNls
    private String createQuery() {
        DBTable table = nestedTable.getParentTable();
        DBColumn parentTableColumn = nestedTable.getParentTableColumn();
        if (parentTableColumn == null) {
            throw new IllegalStateException("Nested table collection column is not available");
        }

        String quotedCollectionColumn = parentTableColumn.getName(true);
        @NonNls
        StringBuilder query = new StringBuilder("select ");
        List<DBColumn> primaryKeyColumns = table.getPrimaryKeyColumns();
        if (primaryKeyColumns.isEmpty()) {
            query.append("n.*");
        } else {
            for (int i = 0; i < primaryKeyColumns.size(); i++) {
                if (i > 0) query.append(", ");
                query.append("p.").append(primaryKeyColumns.get(i).getName(true));
            }
            query.append(", n.*");
        }

        query.append(" from ")
                .append(table.getQualifiedName(true))
                .append(" p, table(p.")
                .append(quotedCollectionColumn)
                .append(") n");
        appendParentFilter(query);
        return query.toString();
    }

    private void appendParentFilter(StringBuilder query) {
        if (parentFilter == null || parentFilter.isEmpty()) return;

        boolean conditionAdded = false;
        for (DBColumn column : parentFilter.getColumns()) {
            Object value = parentFilter.getColumnValue(column);
            query.append(conditionAdded ? " and " : " where ")
                    .append("p.")
                    .append(column.getName(true));
            if (value == null) {
                query.append(" is null");
            } else {
                query.append(" = ").append(SqlLiterals.renderLiteral(value));
            }
            conditionAdded = true;
        }
    }

    private void applyResult(LoadResult result) {
        try {
            if (result.error != null) {
                applyError(result.error);
                return;
            }

            setModel(result.data);
        } finally {
            resultTable.setLoading(false);
            loadingDataPanel.setVisible(false);
        }
    }

    private void setModel(ResultSetDataModel<?, ?> model) {
        ResultSetDataModel<?, ?> oldModel = dataModel;
        dataModel = model;
        Disposer.register(this, dataModel);
        resultTable.setModel(dataModel);
        Disposer.dispose(oldModel);
    }

    private void applyError(Exception error) {
        try {
            Messages.showErrorDialog(
                    getProject(),
                    txt("msg.dataEditor.title.FailedToOpenEditor"),
                    txt("msg.dataEditor.error.FailedToOpenEditor", nestedTable.getQualifiedNameWithType()),
                    error);
        } finally {
            resultTable.setLoading(false);
            loadingDataPanel.setVisible(false);
        }
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    private record LoadResult(ResultSetDataModel<?, ?> data, Exception error) {}
}
