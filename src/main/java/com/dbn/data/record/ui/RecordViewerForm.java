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

package com.dbn.data.record.ui;

import com.dbn.common.action.ToggleAction;
import com.dbn.common.color.Colors;
import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.ui.util.ComponentAligner;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Strings;
import com.dbn.data.record.ColumnSortingType;
import com.dbn.data.record.DatasetRecord;
import com.dbn.data.record.RecordViewInfo;
import com.dbn.editor.data.DatasetEditorManager;
import com.dbn.object.DBColumn;
import com.dbn.object.DBDataset;
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

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.common.ui.util.ComponentAligner.alignFormComponents;
import static com.dbn.common.ui.util.TextFields.onTextChange;

public class RecordViewerForm extends DBNFormBase implements ComponentAligner.Container {
    private JPanel actionsPanel;
    private JPanel columnsPanel;
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JBTextField filterTextField;
    private DBNScrollPane columnsScrollPane;

    private final List<RecordViewerColumnForm> columnForms = DisposableContainers.list(this);

    private final DatasetRecord record;

    RecordViewerForm(RecordViewerDialog parentComponent, DatasetRecord record) {
        super(parentComponent);
        this.record = record;
        DBDataset dataset = record.getDataset();

        RecordViewInfo recordViewInfo = new RecordViewInfo(dataset.getQualifiedName(), dataset.getIcon());

        // HEADER
        String headerTitle = recordViewInfo.getTitle();
        Icon headerIcon = recordViewInfo.getIcon();
        Color headerBackground = Colors.getPanelBackground();
        Project project = ensureProject();
        if (getEnvironmentSettings(project).getVisibilitySettings().getDialogHeaders().value()) {
            headerBackground = dataset.getEnvironmentType().getColor();
        }
        DBNHeaderForm headerForm = new DBNHeaderForm(
                this, headerTitle,
                headerIcon,
                headerBackground
        );
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);

        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, true,
                new SortAlphabeticallyAction(),
                Actions.SEPARATOR);
        actionsPanel.add(actionToolbar.getComponent(), BorderLayout.WEST);

        columnsPanel.setLayout(new BoxLayout(columnsPanel, BoxLayout.Y_AXIS));

        for (DBColumn column : record.getDataset().getColumns()) {
            RecordViewerColumnForm columnForm = new RecordViewerColumnForm(this, record, column);
            columnForms.add(columnForm);
        }
        ColumnSortingType sortingType = getEditorManager().getRecordViewColumnSortingType();
        sortColumns(sortingType);
        alignFormComponents(this);

        filterTextField.getEmptyText().setText("Filter");
        onTextChange(filterTextField, e -> filterColumForms());

        int scrollUnitIncrement = (int) columnForms.get(0).getComponent().getPreferredSize().getHeight();
        columnsScrollPane.getVerticalScrollBar().setUnitIncrement(scrollUnitIncrement);
    }

    @Override
    protected void initAccessibility() {
        setAccessibleName(columnsScrollPane, "Record columns");
    }

    private DatasetEditorManager getEditorManager() {
        return DatasetEditorManager.getInstance(ensureProject());
    }

    @Override
    public List<RecordViewerColumnForm> getAlignableForms() {
        return columnForms;
    }

    private void filterColumForms() {
        String text = filterTextField.getText();
        for (RecordViewerColumnForm columnForm : columnForms) {
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

    /*********************************************************
     *                   Column sorting                      *
     *********************************************************/
    private void sortColumns(ColumnSortingType sortingType) {
        Comparator<RecordViewerColumnForm> comparator =
                sortingType == ColumnSortingType.ALPHABETICAL ? ALPHANUMERIC_COMPARATOR :
                sortingType == ColumnSortingType.BY_INDEX ? INDEXED_COMPARATOR : null;

        if (comparator != null) {
            columnForms.sort(comparator);
            columnsPanel.removeAll();
            for (RecordViewerColumnForm columnForm : columnForms) {
                columnsPanel.add(columnForm.getComponent());
            }
        }
    }

    private static final Comparator<RecordViewerColumnForm> ALPHANUMERIC_COMPARATOR = (columnPanel1, columnPanel2) -> {
        String name1 = columnPanel1.getColumn().getName();
        String name2 = columnPanel2.getColumn().getName();
        return name1.compareTo(name2);
    };

    private static final Comparator<RecordViewerColumnForm> INDEXED_COMPARATOR = (columnPanel1, columnPanel2) -> {
        int index1 = columnPanel1.getColumn().getPosition();
        int index2 = columnPanel2.getColumn().getPosition();
        return index1-index2;
    };

    void focusNextColumnPanel(RecordViewerColumnForm source) {
        int index = columnForms.indexOf(source);
        if (index < columnForms.size() - 1) {
            RecordViewerColumnForm columnForm = columnForms.get(index + 1);
            columnForm.getViewComponent().requestFocus();
        }
    }

    void focusPreviousColumnPanel(RecordViewerColumnForm source) {
        int index = columnForms.indexOf(source);
        if (index > 0) {
            RecordViewerColumnForm columnForm = columnForms.get(index - 1);
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
            DatasetEditorManager editorManager = getEditorManager();
            ColumnSortingType sortingType = editorManager.getRecordViewColumnSortingType();
            return sortingType == ColumnSortingType.ALPHABETICAL;
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean selected) {
            ColumnSortingType columnSorting = selected ? ColumnSortingType.ALPHABETICAL : ColumnSortingType.BY_INDEX;
            DatasetEditorManager editorManager = getEditorManager();
            editorManager.setRecordViewColumnSortingType(columnSorting);
            sortColumns(columnSorting);
            UserInterface.repaint(columnsPanel);
        }
    }
}
