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

package com.dbn.data.grid.ui.table.resultSet.record;

import com.dbn.common.action.BasicAction;
import com.dbn.common.action.ToggleAction;
import com.dbn.common.color.Colors;
import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.ui.util.ComponentAligner;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Strings;
import com.dbn.data.grid.ui.table.resultSet.ResultSetTable;
import com.dbn.data.model.ColumnInfo;
import com.dbn.data.model.resultSet.ResultSetDataModel;
import com.dbn.data.model.resultSet.ResultSetDataModelCell;
import com.dbn.data.model.resultSet.ResultSetDataModelRow;
import com.dbn.data.record.ColumnSortingType;
import com.dbn.data.record.RecordViewInfo;
import com.dbn.editor.data.DatasetEditorManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Comparator;
import java.util.List;

import static com.dbn.common.dispose.Failsafe.guarded;
import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.common.ui.util.ComponentAligner.alignFormComponents;
import static com.dbn.common.ui.util.TextFields.onTextChange;

public class ResultSetRecordViewerForm extends DBNFormBase implements ComponentAligner.Container {
    private JPanel actionsPanel;
    private JPanel columnsPanel;
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JBTextField filterTextField;
    private DBNScrollPane columnsPanelScrollPane;

    private final List<ResultSetRecordViewerColumnForm> columnForms = DisposableContainers.list(this);
    private final ActionToolbar actionToolbar;

    private ResultSetTable<?> table;
    private ResultSetDataModelRow<?, ?> row;

    ResultSetRecordViewerForm(ResultSetRecordViewerDialog parent, ResultSetTable<? extends ResultSetDataModel<?, ?>> table, boolean showDataTypes) {
        super(parent);
        this.table = table;
        ResultSetDataModel<?, ?> model = table.getModel();
        row = model.getRowAtIndex(table.getSelectedRow());

        // HEADER
        RecordViewInfo recordViewInfo = table.getRecordViewInfo();
        String headerTitle = recordViewInfo.getTitle();
        Icon headerIcon = recordViewInfo.getIcon();
        Color headerBackground = Colors.getPanelBackground();
        Project project = ensureProject();
        if (getEnvironmentSettings(project).getVisibilitySettings().getDialogHeaders().value()) {
            headerBackground = model.getConnection().getEnvironmentType().getColor();
        }
        DBNHeaderForm headerForm = new DBNHeaderForm(
                this, headerTitle,
                headerIcon,
                headerBackground
        );
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);

        actionToolbar = Actions.createActionToolbar(actionsPanel, true,
                new SortAlphabeticallyAction(),
                Actions.SEPARATOR,
                new FirstRecordAction(),
                new PreviousRecordAction(),
                new NextRecordAction(),
                new LastRecordAction());
        actionsPanel.add(actionToolbar.getComponent(), BorderLayout.WEST);


        columnsPanel.setLayout(new BoxLayout(columnsPanel, BoxLayout.Y_AXIS));

        ResultSetDataModelRow<?, ?> row = getRow();
        for (Object cell: row.getCells()) {
            ResultSetRecordViewerColumnForm columnForm = new ResultSetRecordViewerColumnForm(this, (ResultSetDataModelCell<?, ?>) cell, showDataTypes);
            columnForms.add(columnForm);
        }
        ColumnSortingType columnSortingType = DatasetEditorManager.getInstance(project).getRecordViewColumnSortingType();
        sortColumns(columnSortingType);
        alignFormComponents(this);

        filterTextField.getEmptyText().setText("Filter");
        onTextChange(filterTextField, e -> filterColumForms());

        int scrollUnitIncrement = (int) columnForms.get(0).getComponent().getPreferredSize().getHeight();
        columnsPanelScrollPane.getVerticalScrollBar().setUnitIncrement(scrollUnitIncrement);
    }

    @Override
    protected void initAccessibility() {
        setAccessibleName(columnsPanelScrollPane, "Column values");
        setAccessibleName(actionToolbar, "Record navigation");
    }

    @Override
    public List<ResultSetRecordViewerColumnForm> getAlignableForms() {
        return columnForms;
    }

    private void filterColumForms() {
        String text = filterTextField.getText();
        for (ResultSetRecordViewerColumnForm columnForm : columnForms) {
            String columnName = columnForm.getColumnName();
            boolean visible = Strings.indexOfIgnoreCase(columnName, text, 0) > -1;
            columnForm.getMainComponent().setVisible(visible);
        }
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return filterTextField;
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public JComponent getColumnsPanel() {
        return columnsPanel;
    }

    public void setRow(ResultSetDataModelRow<?, ?> row) {
        this.row = row;
        for (Object o : row.getCells()) {
            ResultSetDataModelCell<?, ?> cell = (ResultSetDataModelCell<?, ?>) o;
            ResultSetRecordViewerColumnForm columnForm = getColumnPanel(cell.getColumnInfo());
            if (columnForm != null) {
                columnForm.setCell(cell);
            }
        }
    }

    private ResultSetRecordViewerColumnForm getColumnPanel(ColumnInfo columnInfo) {
        for (ResultSetRecordViewerColumnForm columnForm : columnForms) {
            if (columnForm.getCell().getColumnInfo() == columnInfo) {
                return columnForm;
            }
        }
        return null;
    }

    /*********************************************************
     *                   Column sorting                      *
     *********************************************************/
    private void sortColumns(ColumnSortingType sortingType) {
        Comparator<ResultSetRecordViewerColumnForm> comparator =
                sortingType == ColumnSortingType.ALPHABETICAL ? ALPHANUMERIC_COMPARATOR :
                sortingType == ColumnSortingType.BY_INDEX ? INDEXED_COMPARATOR : null;

        if (comparator != null) {
            columnForms.sort(comparator);
            columnsPanel.removeAll();
            for (ResultSetRecordViewerColumnForm columnForm : columnForms) {
                columnsPanel.add(columnForm.getComponent());
            }

            UserInterface.repaint(columnsPanel);
        }
    }

    private static final Comparator<ResultSetRecordViewerColumnForm> ALPHANUMERIC_COMPARATOR = (columnPanel1, columnPanel2) -> {
        String name1 = columnPanel1.getCell().getColumnInfo().getName();
        String name2 = columnPanel2.getCell().getColumnInfo().getName();
        return name1.compareTo(name2);
    };

    private static final Comparator<ResultSetRecordViewerColumnForm> INDEXED_COMPARATOR = (columnPanel1, columnPanel2) -> {
        int index1 = columnPanel1.getCell().getColumnInfo().getIndex();
        int index2 = columnPanel2.getCell().getColumnInfo().getIndex();
        return index1-index2;
    };

    void focusNextColumnPanel(ResultSetRecordViewerColumnForm source) {
        int index = columnForms.indexOf(source);
        if (index < columnForms.size() - 1) {
            ResultSetRecordViewerColumnForm columnForm = columnForms.get(index + 1);
            columnForm.getViewComponent().requestFocus();
        }
    }

    void focusPreviousColumnPanel(ResultSetRecordViewerColumnForm source) {
        int index = columnForms.indexOf(source);
        if (index > 0) {
            ResultSetRecordViewerColumnForm columnForm = columnForms.get(index - 1);
            columnForm.getViewComponent().requestFocus();
        }
    }

    /*********************************************************      
     *                       Actions                         *
     *********************************************************/
    private class SortAlphabeticallyAction extends ToggleAction {
        private SortAlphabeticallyAction() {
            super("Sort Columns Alphabetically", null, Icons.ACTION_SORT_ALPHA);
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            Project project = ensureProject();
            DatasetEditorManager datasetEditorManager = DatasetEditorManager.getInstance(project);

            ColumnSortingType sortingType = datasetEditorManager.getRecordViewColumnSortingType();
            return sortingType == ColumnSortingType.ALPHABETICAL;
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean selected) {
            Project project = ensureProject();
            DatasetEditorManager datasetEditorManager = DatasetEditorManager.getInstance(project);

            ColumnSortingType sortingType = selected ? ColumnSortingType.ALPHABETICAL : ColumnSortingType.BY_INDEX;
            datasetEditorManager.setRecordViewColumnSortingType(sortingType);
            sortColumns(sortingType);
        }
    }

    private class FirstRecordAction extends BasicAction {
        private FirstRecordAction() {
            super(txt("app.dataEditor.action.FirstRecord"), null, Icons.DATA_EDITOR_FIRST_RECORD);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            ResultSetDataModelRow<?, ?> row = getRow();
            ResultSetDataModelRow<?, ?> firstRow = row.getModel().getRowAtIndex(0);
            if (firstRow != null) {
                setRow(firstRow);
                table.selectRow(0);
            }
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setEnabled(getRowIndex() > 0);
        }
    }

    private class PreviousRecordAction extends BasicAction {
        private PreviousRecordAction() {
            super(txt("app.dataEditor.action.PreviousRecord"), null, Icons.DATA_EDITOR_PREVIOUS_RECORD);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            ResultSetDataModelRow<?, ?> row = getRow();
            int index = row.getIndex();
            if (index <= 0) return;

            index--;
            ResultSetDataModelRow<?, ?> previousRow = row.getModel().getRowAtIndex(index);
            if (previousRow == null) return;

            setRow(previousRow);
            table.selectRow(index);
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(getRowIndex() > 0);
        }
    }

    private class NextRecordAction extends BasicAction {
        private NextRecordAction() {
            super(txt("app.dataEditor.action.NextRecord"), null, Icons.DATA_EDITOR_NEXT_RECORD);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            ResultSetDataModelRow<?, ?> row = getRow();
            ResultSetDataModel<?, ?> model = row.getModel();
            if (row.getIndex() >= model.getRowCount() - 1) return;

            int index = row.getIndex() + 1;
            ResultSetDataModelRow<?, ?> nextRow = model.getRowAtIndex(index);
            if (nextRow == null) return;

            setRow(nextRow);
            table.selectRow(index);
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(getRowIndex() < getRowCount() -1);
        }
    }

    private class LastRecordAction extends BasicAction {
        private LastRecordAction() {
            super(txt("app.dataEditor.action.LastRecord"), null, Icons.DATA_EDITOR_LAST_RECORD);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            ResultSetDataModel<?, ?> model = getRow().getModel();
            int index = model.getRowCount() - 1 ;
            ResultSetDataModelRow<?, ?> lastRow = model.getRowAtIndex(index);
            if (lastRow != null) {
                setRow(lastRow);
                table.selectRow(index);
            }
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(getRowIndex() < getRowCount() -1);
        }
    }

    @NotNull
    public ResultSetDataModelRow<?, ?> getRow() {
        return Failsafe.nn(row);
    }


    private int getRowIndex() {
        return guarded(-1, this, f -> f.getRow().getIndex());
    }

    private int getRowCount() {
        return guarded(0, this, f -> f.getRow().getModel().getRowCount());
    }


    @Override
    public void disposeInner() {
        super.disposeInner();
        table = null;
        row = null;
    }
}
