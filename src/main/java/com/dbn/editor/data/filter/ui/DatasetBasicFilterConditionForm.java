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

package com.dbn.editor.data.filter.ui;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.ui.ValueSelectorOption;
import com.dbn.common.ui.list.ColoredListCellRenderer;
import com.dbn.common.ui.listener.ComboBoxSelectionKeyListener;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.util.TextFields;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Safe;
import com.dbn.data.editor.ui.TextFieldPopupType;
import com.dbn.data.editor.ui.TextFieldWithPopup;
import com.dbn.data.type.GenericDataType;
import com.dbn.editor.data.filter.ConditionOperator;
import com.dbn.editor.data.filter.DatasetBasicFilterCondition;
import com.dbn.editor.data.filter.action.DeleteBasicFilterConditionAction;
import com.dbn.editor.data.filter.action.EnableDisableBasicFilterConditionAction;
import com.dbn.object.DBColumn;
import com.dbn.object.DBDataset;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DatasetBasicFilterConditionForm extends ConfigurationEditorForm<DatasetBasicFilterCondition> {

    private JPanel actionsPanel;
    private JPanel mainPanel;
    private JPanel valueFieldPanel;
    private boolean active = true;

    private DBNComboBox<DBColumn> columnSelector;
    private DBNComboBox<ConditionOperator> operatorSelector;

    private TextFieldWithPopup<?> editorComponent;
    private DatasetBasicFilterForm filterForm;
    private final DBObjectRef<DBDataset> dataset;

    public DatasetBasicFilterConditionForm(DBDataset dataset, DatasetBasicFilterCondition condition) {
        super(condition);
        this.dataset = DBObjectRef.of(dataset);
        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, true,
                new EnableDisableBasicFilterConditionAction(this),
                new DeleteBasicFilterConditionAction(this));
        actionsPanel.add(actionToolbar.getComponent(), BorderLayout.CENTER);

        DBColumn column = dataset.getColumn(condition.getColumnName());
        if (column == null) {
            for (DBColumn col : dataset.getColumns()) {
                if (col.getDataType().isNative()) {
                    column = col;
                    break;
                }
            }
        }
        GenericDataType dataType = column == null ? null : column.getDataType().getGenericDataType();

        columnSelector.set(ValueSelectorOption.HIDE_DESCRIPTION, true);
        columnSelector.setValueLoader(this::loadColumns);
        columnSelector.setSelectedValue(column);
        columnSelector.addListener((oldValue, newValue) -> {
            if (newValue != null) {
                GenericDataType selectedDataType = newValue.getDataType().getGenericDataType();
                editorComponent.setPopupEnabled(TextFieldPopupType.CALENDAR, selectedDataType == GenericDataType.DATE_TIME);
            }
            if (filterForm != null) {
                filterForm.updateNameAndPreview();
            }
            operatorSelector.reloadValues();
        });


        operatorSelector.setValueLoader(this::loadOperators);
        operatorSelector.setSelectedValue(condition.getOperator());
        operatorSelector.addListener((oldValue, newValue) -> {
            if (filterForm != null) {
                filterForm.updateNameAndPreview();
                updateValueTextField();
            }
        });

        editorComponent = new TextFieldWithPopup<>(dataset.getProject());
        editorComponent.createCalendarPopup(false);
        editorComponent.setPopupEnabled(TextFieldPopupType.CALENDAR, dataType == GenericDataType.DATE_TIME);
        
        valueFieldPanel.add(editorComponent, BorderLayout.CENTER);

        JTextField valueTextField = editorComponent.getTextField();
        valueTextField.setText(condition.getValue());
        setActive(condition.isActive());


        TextFields.onTextChange(valueTextField, e -> Safe.run(filterForm, f -> f.updateNameAndPreview()));
        valueTextField.addKeyListener(ComboBoxSelectionKeyListener.create(columnSelector, false));
        valueTextField.addKeyListener(ComboBoxSelectionKeyListener.create(operatorSelector, true));

        updateValueTextField();

        valueTextField.setToolTipText("<html>While editing value, <br> " +
                "press <b>Up/Down</b> keys to change column or <br> " +
                "press <b>Ctrl-Up/Ctrl-Down</b> keys to change operator</html>");

        Disposer.register(this, editorComponent);
    }

    @NotNull
    List<ConditionOperator> loadOperators() {
        DBColumn selectedColumn = getSelectedColumn();
        if (selectedColumn != null) {
            Class typeClass = selectedColumn.getDataType().getTypeClass();
            return Arrays.asList(ConditionOperator.getConditionOperators(typeClass));
        }
        return Collections.emptyList();
    }

    @NotNull
    List<DBColumn> loadColumns() {
        DBDataset dataset1 = dataset.get();
        if (dataset1 != null) {
            List<DBColumn> columns = new ArrayList<>(dataset1.getColumns());
            Collections.sort(columns);
            return columns;
        }
        return Collections.emptyList();
    }

    @Override
    public void focus() {
        JTextField valueTextField = editorComponent.getTextField();
        valueTextField.selectAll();
        valueTextField.grabFocus();
    }

    public void setBasicFilterPanel(DatasetBasicFilterForm filterForm) {
        this.filterForm = Disposer.replace(this.filterForm, filterForm);
    }

    @Nullable
    public DBColumn getSelectedColumn() {
        return columnSelector.getSelectedValue();
    }

    public ConditionOperator getSelectedOperator() {
        return operatorSelector.getSelectedValue();
    }

    public String getValue() {
        return editorComponent.getText();
    }

    public DatasetBasicFilterCondition getCondition() {
        return getConfiguration();
    }

    public DatasetBasicFilterCondition createCondition() {
        DBColumn selectedColumn = getSelectedColumn();
        return new DatasetBasicFilterCondition(
                filterForm.getConfiguration(),
                selectedColumn == null ? null : selectedColumn.getName(),
                editorComponent.getText(), getSelectedOperator(),
                active);
    }

    public void remove() {
        DatasetBasicFilterCondition condition = getConfiguration();
        DatasetBasicFilterForm settingsEditor = (DatasetBasicFilterForm) condition.getFilter().ensureSettingsEditor();
        settingsEditor.removeConditionPanel(this);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        columnSelector.setEnabled(active);
        operatorSelector.setEnabled(active);
        editorComponent.getTextField().setEnabled(active);
        if (filterForm != null) {
            filterForm.updateNameAndPreview();
        }
    }

    private final ListCellRenderer<?> cellRenderer = new ColoredListCellRenderer<>() {
        @Override
        protected void customize(@NotNull JList list, Object value, int index, boolean selected, boolean hasFocus) {
            DBObjectRef<DBColumn> columnRef = (DBObjectRef<DBColumn>) value;
            DBColumn column = DBObjectRef.get(columnRef);
            if (column != null) {
                setIcon(column.getIcon());
                append(column.getName(), active ? SimpleTextAttributes.REGULAR_ATTRIBUTES : SimpleTextAttributes.GRAYED_ATTRIBUTES);
            }
        }
    };

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        DatasetBasicFilterCondition condition = getConfiguration();
        DBColumn column = getSelectedColumn();
        ConditionOperator operator = getSelectedOperator();
        String value = editorComponent.getText();

        condition.setColumnName(column == null ? "" : column.getName());
        condition.setOperator(operator);
        condition.setValue(value == null ? "" : value);
        condition.setActive(active);
    }

    private void updateValueTextField() {
        JTextField valueTextField = editorComponent.getTextField();
        ConditionOperator selectedOperator = getSelectedOperator();
        valueTextField.setEnabled(selectedOperator!= null && !selectedOperator.isTerminal() && active);
        if (selectedOperator == null || selectedOperator.isTerminal()) valueTextField.setText(null);
    }

    @Override
    public void resetFormChanges() {

    }
}
