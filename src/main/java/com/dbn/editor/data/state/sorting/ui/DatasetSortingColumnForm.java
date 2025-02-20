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

package com.dbn.editor.data.state.sorting.ui;

import com.dbn.common.color.Colors;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.ValueSelector;
import com.dbn.common.ui.ValueSelectorOption;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Strings;
import com.dbn.data.sorting.SortingInstruction;
import com.dbn.editor.data.state.sorting.action.ChangeSortingDirectionAction;
import com.dbn.editor.data.state.sorting.action.DeleteSortingCriteriaAction;
import com.dbn.object.DBColumn;
import com.dbn.object.DBDataset;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.common.ui.util.Accessibility.setAccessibleUnit;

public class DatasetSortingColumnForm extends DBNFormBase {
    private JPanel actionsPanel;
    private JPanel mainPanel;
    private JLabel indexLabel;
    private JLabel dataTypeLabel;
    private DBNComboBox<DBColumn> columnComboBox;

    private final SortingInstruction sortingInstruction;

    DatasetSortingColumnForm(DatasetEditorSortingForm parent, @NotNull SortingInstruction sortingInstruction) {
        super(parent);
        this.sortingInstruction = sortingInstruction;

        DBDataset dataset = parent.getDataset();
        DBColumn column = sortingInstruction.getColumn(dataset);
        columnComboBox.setValues(dataset.getColumns());
        columnComboBox.setSelectedValue(column);
        columnComboBox.set(ValueSelectorOption.HIDE_DESCRIPTION, true);
        columnComboBox.setBackground(Colors.getTextFieldBackground());
        dataTypeLabel.setText(column.getDataType().getQualifiedName());
        dataTypeLabel.setForeground(UIUtil.getInactiveTextColor());

        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, true,
                new ChangeSortingDirectionAction(this),
                new DeleteSortingCriteriaAction(this));
        actionsPanel.add(actionToolbar.getComponent(), BorderLayout.CENTER);

        setAccessibleUnit(columnComboBox, dataTypeLabel.getText());
    }

    @NotNull
    public DatasetEditorSortingForm getParentForm() {
        return ensureParentComponent();
    }

    private class ColumnSelector extends ValueSelector<DBColumn>{
        ColumnSelector(DBColumn selectedColumn) {
            super(Icons.DBO_COLUMN_HIDDEN, "Select column...", selectedColumn, ValueSelectorOption.HIDE_DESCRIPTION);
            addListener((oldValue, newValue) -> {
                sortingInstruction.setColumnName(newValue.getName());
                dataTypeLabel.setText(newValue.getDataType().getQualifiedName());
            });
        }

        @Override
        public List<DBColumn> loadValues() {
            DBDataset dataset = getDataset();
            List<DBColumn> columns = new ArrayList<>(dataset.getColumns());
            Collections.sort(columns);
            return columns;
        }

        @Override
        public boolean isVisible(DBColumn value) {
            List<DatasetSortingColumnForm> sortingInstructionForms = getParentForm().getSortingInstructionForms();
            for (DatasetSortingColumnForm sortingColumnForm : sortingInstructionForms) {
                String columnName = sortingColumnForm.getSortingInstruction().getColumnName();
                if (Strings.equalsIgnoreCase(columnName, value.getName())) {
                    return false;
                }
            }
            return true;

        }
    }

    public void setIndex(int index) {
        sortingInstruction.setIndex(index);
        indexLabel.setText(Integer.toString(index));
        setAccessibleName(columnComboBox, "Sorting column " + index);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public SortingInstruction getSortingInstruction() {
        return sortingInstruction;
    }

    public void remove() {
        getParentForm().removeSortingColumn(this);
    }

    @NotNull
    public DBDataset getDataset() {
        return getParentForm().getDataset();
    }
}
