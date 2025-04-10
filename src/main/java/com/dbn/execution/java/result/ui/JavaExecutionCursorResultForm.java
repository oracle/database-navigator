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

package com.dbn.execution.java.result.ui;

import com.dbn.common.action.DataKeys;
import com.dbn.common.action.DataProviders;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.misc.DBNTableScrollPane;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Actions;
import com.dbn.data.find.DataSearchComponent;
import com.dbn.data.find.SearchableDataComponent;
import com.dbn.data.grid.ui.table.resultSet.ResultSetTable;
import com.dbn.data.model.resultSet.ResultSetDataModel;
import com.dbn.data.record.RecordViewInfo;
import com.dbn.execution.java.result.JavaExecutionResult;
import com.dbn.object.DBJavaParameter;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import javax.swing.text.JTextComponent;
import java.awt.Dimension;

public class JavaExecutionCursorResultForm extends DBNFormBase implements SearchableDataComponent {
    private JPanel actionsPanel;
    private JPanel mainPanel;
    private JPanel resultPanel;
    private JPanel searchPanel;
    private DBNTableScrollPane resultScrollPane;

    private final DBObjectRef<DBJavaParameter> parameter;
    private final ResultSetTable<ResultSetDataModel<?, ?>> resultTable;

    JavaExecutionCursorResultForm(JavaExecutionResultForm parent, JavaExecutionResult executionResult, DBJavaParameter parameter) {
        super(parent);
        this.parameter = DBObjectRef.of(parameter);
        ResultSetDataModel<?, ?> dataModel = executionResult.getTableModel(parameter);
        RecordViewInfo recordViewInfo = new RecordViewInfo(
                executionResult.getName(),
                executionResult.getIcon());

        resultTable = new ResultSetTable<>(this, dataModel, true, recordViewInfo);
        resultTable.setPreferredScrollableViewportSize(new Dimension(500, -1));

        resultPanel.setBorder(Borders.lineBorder(JBColor.border(), 1, 0, 1, 0));
        resultScrollPane.setViewportView(resultTable);
        resultTable.initTableGutter();
        resultTable.installMathAddon();
        resultTable.installValuePopupAddon();

        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, true, "DBNavigator.ActionGroup.MethodExecutionCursorResult");
        actionsPanel.add(actionToolbar.getComponent());
        DataProviders.register(actionToolbar.getComponent(), this);
    }

    public DBJavaParameter getParameter() {
        return parameter.get();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    /*********************************************************
     *              SearchableDataComponent                  *
     *********************************************************/
    @NotNull
    @Override
    public JPanel getSearchPanel() {
        return searchPanel;
    }

    @Override
    public void showSearchHeader() {
        resultTable.clearSelection();

        DataSearchComponent dataSearchComponent = getSearchComponent();
        dataSearchComponent.initializeFindModel();
        JTextComponent searchField = dataSearchComponent.getSearchField();
        if (searchPanel.isVisible()) {
            searchField.selectAll();
        } else {
            searchPanel.setVisible(true);
        }
        searchField.requestFocus();

    }

    @Override
    public void hideSearchHeader() {
        getSearchComponent().resetFindModel();
        searchPanel.setVisible(false);
        UserInterface.repaintAndFocus(resultTable);
    }

    @Override
    public void cancelEditActions() {

    }

    @Override
    public String getSelectedText() {
        return null;
    }

    @NotNull
    @Override
    public ResultSetTable<?> getTable() {
        return Failsafe.nn(resultTable);
    }

    /********************************************************
     *                    Data Provider                     *
     ********************************************************/
    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.JAVA_EXECUTION_CURSOR_RESULT_FORM.is(dataId)) {
            return JavaExecutionCursorResultForm.this;
        }
        if (DataKeys.JAVA_EXECUTION_ARGUMENT.is(dataId)) {
            return DBObjectRef.get(parameter);
        }
        return super.getData(dataId);
    }
}
