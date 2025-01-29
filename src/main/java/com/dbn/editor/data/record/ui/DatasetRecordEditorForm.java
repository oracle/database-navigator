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

package com.dbn.editor.data.record.ui;

import com.dbn.common.action.BasicAction;
import com.dbn.common.action.ToggleAction;
import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.icon.Icons;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.util.ComponentAligner;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Strings;
import com.dbn.data.record.ColumnSortingType;
import com.dbn.editor.data.DatasetEditorManager;
import com.dbn.editor.data.model.DatasetEditorColumnInfo;
import com.dbn.editor.data.model.DatasetEditorModel;
import com.dbn.editor.data.model.DatasetEditorModelCell;
import com.dbn.editor.data.model.DatasetEditorModelRow;
import com.dbn.object.DBDataset;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.util.Comparator;
import java.util.List;

import static com.dbn.common.ui.util.ComponentAligner.alignFormComponents;
import static com.dbn.common.ui.util.TextFields.onTextChange;

public class DatasetRecordEditorForm extends DBNFormBase implements ComponentAligner.Container {
    private JPanel actionsPanel;
    private JPanel columnsPanel;
    private JPanel mainPanel;
    private JScrollPane columnsPanelScrollPane;
    private JPanel headerPanel;
    private JBTextField filterTextField;

    private final List<DatasetRecordEditorColumnForm> columnForms = DisposableContainers.list(this);

    private WeakRef<DatasetEditorModelRow> row;

    public DatasetRecordEditorForm(DatasetRecordEditorDialog parentComponent, DatasetEditorModelRow row) {
        super(parentComponent);
        this.row = WeakRef.of(row);
        DBDataset dataset = row.getModel().getDataset();

        DBNHeaderForm headerForm = new DBNHeaderForm(this, dataset);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);

        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, true,
                new SortAlphabeticallyAction(),
                Actions.SEPARATOR,
                new FirstRecordAction(),
                new PreviousRecordAction(),
                new NextRecordAction(),
                new LastRecordAction());
        actionsPanel.add(actionToolbar.getComponent(), BorderLayout.WEST);


        columnsPanel.setLayout(new BoxLayout(columnsPanel, BoxLayout.Y_AXIS));

        for (DatasetEditorModelCell cell: row.getCells()) {
            DatasetRecordEditorColumnForm columnForm = new DatasetRecordEditorColumnForm(this, cell);
            columnForms.add(columnForm);
        }

        Project project = ensureProject();
        DatasetEditorManager datasetEditorManager = DatasetEditorManager.getInstance(project);
        ColumnSortingType columnSortingType = datasetEditorManager.getRecordViewColumnSortingType();
        sortColumns(columnSortingType);
        alignFormComponents(this);

        filterTextField.getEmptyText().setText("Filter");
        onTextChange(filterTextField, e -> filterColumForms());

        if (columnForms.size() > 0) {
            int scrollUnitIncrement = (int) columnForms.get(0).getComponent().getPreferredSize().getHeight();
            columnsPanelScrollPane.getVerticalScrollBar().setUnitIncrement(scrollUnitIncrement);
        }
    }

    @Override
    public List<? extends ComponentAligner.Form> getAlignableForms() {
        return columnForms;
    }

    private void filterColumForms() {
        String text = filterTextField.getText();
        for (DatasetRecordEditorColumnForm columnForm : columnForms) {
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

    @NotNull
    public DatasetEditorModelRow getRow() {
        return row.ensure();
    }

    public int geRowIndex() {
        return getRow().getIndex();
    }

    @NotNull
    public DatasetEditorModel getModel() {
        return getRow().getModel();
    }

    public void setRow(DatasetEditorModelRow row) {
        this.row = WeakRef.of(row);
        for (DatasetEditorModelCell cell : row.getCells()) {
            DatasetRecordEditorColumnForm columnForm = getColumnPanel(cell.getColumnInfo());
            if (columnForm != null) {
                columnForm.setCell(cell);
            }
        }
    }

    private DatasetRecordEditorColumnForm getColumnPanel(DatasetEditorColumnInfo columnInfo) {
        for (DatasetRecordEditorColumnForm columnForm : columnForms) {
            if (columnForm.getCell().getColumnInfo() == columnInfo) {
                return columnForm;
            }
        }
        return null;
    }

    /*********************************************************
     *                   Column sorting                      *
     *********************************************************/
    private void sortColumns(ColumnSortingType columnSortingType) {
        Comparator<DatasetRecordEditorColumnForm> comparator =
                columnSortingType == ColumnSortingType.ALPHABETICAL ? ALPHANUMERIC_COMPARATOR :
                columnSortingType == ColumnSortingType.BY_INDEX ? INDEXED_COMPARATOR : null;

        if (comparator != null) {
            columnForms.sort(comparator);
            columnsPanel.removeAll();
            for (DatasetRecordEditorColumnForm columnForm : columnForms) {
                columnsPanel.add(columnForm.getComponent());
            }
            UserInterface.repaint(columnsPanel);
        }
    }

    private static final Comparator<DatasetRecordEditorColumnForm> ALPHANUMERIC_COMPARATOR = (columnPanel1, columnPanel2) -> {
        String name1 = columnPanel1.getCell().getColumnInfo().getName();
        String name2 = columnPanel2.getCell().getColumnInfo().getName();
        return name1.compareTo(name2);
    };

    private static final Comparator<DatasetRecordEditorColumnForm> INDEXED_COMPARATOR = (columnPanel1, columnPanel2) -> {
        int index1 = columnPanel1.getCell().getColumnInfo().getIndex();
        int index2 = columnPanel2.getCell().getColumnInfo().getIndex();
        return index1-index2;
    };

    public void focusNextColumnPanel(DatasetRecordEditorColumnForm source) {
        int index = columnForms.indexOf(source);
        if (index < columnForms.size() - 1) {
            DatasetRecordEditorColumnForm columnForm = columnForms.get(index + 1);
            columnForm.getEditorComponent().requestFocus();
        }
    }

    public void focusPreviousColumnPanel(DatasetRecordEditorColumnForm source) {
        int index = columnForms.indexOf(source);
        if (index > 0) {
            DatasetRecordEditorColumnForm columnForm = columnForms.get(index - 1);
            columnForm.getEditorComponent().requestFocus();
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
            Project project = getModel().getDataset().getProject();
            ColumnSortingType columnSortingType = DatasetEditorManager.getInstance(project).getRecordViewColumnSortingType();
            return columnSortingType == ColumnSortingType.ALPHABETICAL;
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean selected) {
            ColumnSortingType columnSortingType = selected ? ColumnSortingType.ALPHABETICAL : ColumnSortingType.BY_INDEX;
            Project project = getModel().getDataset().getProject();
            DatasetEditorManager.getInstance(project).setRecordViewColumnSortingType(columnSortingType);
            sortColumns(columnSortingType);
        }
    }

    private class FirstRecordAction extends BasicAction {
        private FirstRecordAction() {
            super(txt("app.data.action.FirstRecord"), null, Icons.DATA_EDITOR_FIRST_RECORD);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            DatasetEditorModelRow firstRow = getModel().getRowAtIndex(0);
            if (firstRow != null) {
                setRow(firstRow);
                getModel().getEditorTable().selectRow(0);
            }
        }

        @Override
        public void update(AnActionEvent anactionevent) {
            anactionevent.getPresentation().setEnabled(geRowIndex() > 0);
        }
    }

    private class PreviousRecordAction extends BasicAction {
        private PreviousRecordAction() {
            super(txt("app.data.action.PreviousRecord"), null, Icons.DATA_EDITOR_PREVIOUS_RECORD);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            if (geRowIndex() > 0) {
                int index = geRowIndex() - 1;
                DatasetEditorModelRow previousRow = getModel().getRowAtIndex(index);
                if (previousRow != null) {
                    setRow(previousRow);
                    getModel().getEditorTable().selectRow(index);
                }
            }
        }

        @Override
        public void update(AnActionEvent anactionevent) {
            anactionevent.getPresentation().setEnabled(geRowIndex() > 0);
        }
    }

    private class NextRecordAction extends BasicAction {
        private NextRecordAction() {
            super(txt("app.data.action.NextRecord"), null, Icons.DATA_EDITOR_NEXT_RECORD);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            if (geRowIndex() < getModel().getRowCount() -1) {
                int index = geRowIndex() + 1;
                DatasetEditorModelRow nextRow = getModel().getRowAtIndex(index);
                if (nextRow != null) {
                    setRow(nextRow);
                    getModel().getEditorTable().selectRow(index);
                }
            }
        }

        @Override
        public void update(AnActionEvent anactionevent) {
            anactionevent.getPresentation().setEnabled(geRowIndex() < getModel().getRowCount() -1);
        }
    }

    private class LastRecordAction extends BasicAction {
        private LastRecordAction() {
            super(txt("app.data.action.LastRecord"), null, Icons.DATA_EDITOR_LAST_RECORD);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            int index = getModel().getRowCount() - 1 ;
            DatasetEditorModelRow lastRow = getModel().getRowAtIndex(index);
            if (lastRow != null) {
                setRow(lastRow);
                getModel().getEditorTable().selectRow(index);
            }
        }

        @Override
        public void update(AnActionEvent anactionevent) {
            anactionevent.getPresentation().setEnabled(geRowIndex() < getModel().getRowCount() -1);
        }
    }

}
