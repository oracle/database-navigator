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

import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.ui.ValueSelector;
import com.dbn.common.ui.ValueSelectorOption;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Strings;
import com.dbn.data.sorting.SortDirection;
import com.dbn.data.sorting.SortingInstruction;
import com.dbn.data.sorting.SortingState;
import com.dbn.editor.data.DatasetEditor;
import com.dbn.object.DBColumn;
import com.dbn.object.DBDataset;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DatasetEditorSortingForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel sortingInstructionsPanel;
    private JPanel actionsPanel;
    private JPanel headerPanel;
    private JLabel dataSortingLabel;

    private final DBObjectRef<DBDataset> dataset;
    private final List<DatasetSortingColumnForm> sortingInstructionForms = DisposableContainers.concurrentList(this);
    private final SortingState sortingState;


    DatasetEditorSortingForm(DatasetEditorSortingDialog parentComponent, DatasetEditor datasetEditor) {
        super(parentComponent);
        DBDataset dataset = datasetEditor.getDataset();
        sortingState = datasetEditor.getEditorState().getSortingState();
        this.dataset = DBObjectRef.of(dataset);

        BoxLayout sortingInstructionsPanelLayout = new BoxLayout(sortingInstructionsPanel, BoxLayout.Y_AXIS);
        sortingInstructionsPanel.setLayout(sortingInstructionsPanelLayout);

        for (SortingInstruction sortingInstruction : sortingState.getInstructions()) {
            DBColumn column = sortingInstruction.getColumn(dataset);
            if (column != null) {
                DatasetSortingColumnForm sortingInstructionForm = new DatasetSortingColumnForm(this, sortingInstruction.clone());
                sortingInstructionForms.add(sortingInstructionForm);
                sortingInstructionsPanel.add(sortingInstructionForm.getComponent());
            }
        }
        updateIndexes();

        actionsPanel.add(new ColumnSelector(), BorderLayout.CENTER);
        createHeaderForm(dataset);
    }

    List<DatasetSortingColumnForm> getSortingInstructionForms() {
        return sortingInstructionForms;
    }

    private void createHeaderForm(DBDataset dataset) {
        DBNHeaderForm headerForm = new DBNHeaderForm(this, dataset);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);
    }

    private class ColumnSelector extends ValueSelector<DBColumn> {
        ColumnSelector() {
            super(PlatformIcons.ADD_ICON, "Add Sorting Column...", null, ValueSelectorOption.HIDE_DESCRIPTION);
            addListener((oldValue, newValue) -> {
                if (newValue != null) {
                    addSortingColumn(newValue);
                    resetValues();
                }
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
            for (DatasetSortingColumnForm sortingColumnForm : sortingInstructionForms) {
                String columnName = sortingColumnForm.getSortingInstruction().getColumnName();
                if (Strings.equalsIgnoreCase(columnName, value.getName())) {
                    return false;
                }
            }
            return true;
        }
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @NotNull
    public DBDataset getDataset() {
        return dataset.ensure();
    }

    public void addSortingColumn(DBColumn column) {
        SortingInstruction datasetSortingInstruction = new SortingInstruction(column.getName(), SortDirection.ASCENDING);
        DatasetSortingColumnForm sortingInstructionForm = new DatasetSortingColumnForm(this, datasetSortingInstruction);
        sortingInstructionForms.add(sortingInstructionForm);
        sortingInstructionsPanel.add(sortingInstructionForm.getComponent());
        updateIndexes();
        UserInterface.repaint(sortingInstructionsPanel);
    }

    private void updateIndexes() {
        for (int i=0; i<sortingInstructionForms.size(); i++) {
            sortingInstructionForms.get(i).setIndex(i + 1);
        }
    }


    public void removeSortingColumn(DatasetSortingColumnForm sortingInstructionForm) {
        sortingInstructionsPanel.remove(sortingInstructionForm.getComponent());
        sortingInstructionForms.remove(sortingInstructionForm);
        updateIndexes();
        Disposer.dispose(sortingInstructionForm);

        UserInterface.repaint(sortingInstructionsPanel);
    }

    public void applyChanges() {
        sortingState.clear();
        for (DatasetSortingColumnForm sortingColumnForm : sortingInstructionForms) {
            sortingState.addSortingInstruction(sortingColumnForm.getSortingInstruction());
        }
    }
}
