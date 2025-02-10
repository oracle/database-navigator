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

package com.dbn.execution.statement.variables.ui;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.icon.Icons;
import com.dbn.common.locale.Formatter;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.listener.ComboBoxSelectionKeyListener;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.util.ComponentAligner;
import com.dbn.common.util.Strings;
import com.dbn.data.editor.ui.ListPopupValuesProvider;
import com.dbn.data.editor.ui.TextFieldPopupType;
import com.dbn.data.editor.ui.TextFieldWithPopup;
import com.dbn.data.type.GenericDataType;
import com.dbn.execution.statement.StatementExecutionManager;
import com.dbn.execution.statement.processor.StatementExecutionProcessor;
import com.dbn.execution.statement.variables.StatementExecutionVariable;
import com.dbn.execution.statement.variables.StatementExecutionVariables;
import com.dbn.execution.statement.variables.VariableValueProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.dbn.common.ui.util.Accessibility.attachSelectionAnnouncer;
import static com.dbn.common.ui.util.Accessibility.setAccessibleDescription;
import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.common.ui.util.Accessibility.setAccessibleUnit;
import static com.dbn.common.ui.util.TextFields.onTextChange;


public class StatementExecutionVariableValueForm extends DBNFormBase implements ComponentAligner.Form {
    private JPanel mainPanel;
    private JLabel variableNameLabel;
    private JPanel valueFieldPanel;
    private DBNComboBox<GenericDataType> dataTypeComboBox;

    @Getter
    private final StatementExecutionVariable variable;
    private final TextFieldWithPopup<?> editorComponent;

    StatementExecutionVariableValueForm(StatementExecutionInputForm parent, StatementExecutionVariable variable) {
        super(parent);
        this.variable = variable;

        variableNameLabel.setText(variable.getName());
        variableNameLabel.setIcon(Icons.DBO_VARIABLE);

        dataTypeComboBox.setValues(
                GenericDataType.LITERAL,
                GenericDataType.NUMERIC,
                GenericDataType.DATE_TIME);

        dataTypeComboBox.setSelectedValue(variable.getDataType());

        StatementExecutionProcessor executionProcessor = parent.getExecutionProcessor();
        Project project = executionProcessor.getProject();
        StatementExecutionManager executionManager = StatementExecutionManager.getInstance(project);
        StatementExecutionVariables variablesCache = executionManager.getExecutionVariables();

        editorComponent = new TextFieldWithPopup<>(project);
        editorComponent.createCalendarPopup(false);
        editorComponent.createValuesListPopup(createValuesProvider(variable, executionProcessor, variablesCache), null, true);
        editorComponent.setPopupEnabled(TextFieldPopupType.CALENDAR, variable.getDataType() == GenericDataType.DATE_TIME);

        valueFieldPanel.add(editorComponent, BorderLayout.CENTER);
        JTextField textField = editorComponent.getTextField();
        String value = variable.getValue();
        if (Strings.isEmpty(value)) {
            VirtualFile virtualFile = executionProcessor.getVirtualFile();
            StatementExecutionVariable cachedVariable = variablesCache.getVariable(virtualFile, variable.getName());
            if (cachedVariable != null) {
                textField.setForeground(UIUtil.getLabelDisabledForeground());
                textField.setText(cachedVariable.getValue());

                onTextChange(textField, e -> textField.setForeground(UIUtil.getTextFieldForeground()));
                dataTypeComboBox.setSelectedValue(cachedVariable.getDataType());
            }
        } else {
            textField.setText(value);
        }


        textField.addKeyListener(ComboBoxSelectionKeyListener.create(dataTypeComboBox, false));
        variableNameLabel.setLabelFor(textField);

        variable.setPreviewValueProvider(new VariableValueProvider() {
            @Override
            public String getValue() {
                return textField.getText().trim();
            }

            @Override
            public GenericDataType getDataType() {
                return dataTypeComboBox.getSelectedValue();
            }

        });

        dataTypeComboBox.addListener((oldValue, newValue) -> {
            variable.setDataType(newValue);
            editorComponent.setPopupEnabled(TextFieldPopupType.CALENDAR, newValue == GenericDataType.DATE_TIME);
            getParentForm().updatePreview();
            setAccessibleUnit(textField, newValue.getName());
        });

        textField.setToolTipText("<html>While editing variable value, press <b>Up/Down</b> keys to change data type");
        Disposer.register(this, editorComponent);
    }

    @Override
    protected void initValidation() {
        addTextValidation(editorComponent.getTextField(), f -> validateDataType());
    }

    private String validateDataType() {
        Formatter formatter = Formatter.getInstance(ensureProject());
        String value = editorComponent.getTextField().getText().trim();
        if (Strings.isEmpty(value)) return null;

        GenericDataType dataType = dataTypeComboBox.getSelectedValue();
        if (dataType == GenericDataType.DATE_TIME){
            try {
                formatter.parseDateTime(value);
            } catch (ParseException e) {
                String pattern = formatter.getDateFormatPattern();
                String sample = formatter.formatDate(new Date());
                return "Invalid date. Expected format \"" + pattern + "\" (e.g. \"" + sample + "\")";
            }
        } else if (dataType == GenericDataType.NUMERIC){
            try {
                formatter.parseNumber(value);
            } catch (ParseException e) {
                String pattern = formatter.getNumberFormatPattern();
                String sample = formatter.formatNumber(123456.7890);
                return "Invalid number. Expected format \"" + pattern + "\" (e.g. \"" + sample + "\")";
            }
        }
        return null;
    }

    @Override
    protected void initAccessibility() {
        JTextField textField = editorComponent.getTextField();
        setAccessibleUnit(textField, dataTypeComboBox.getSelectedValueName());
        setAccessibleDescription(textField, "Press up or down arrow keys to change data type");
        setAccessibleName(dataTypeComboBox, "Data type");
        setAccessibleDescription(dataTypeComboBox, "Data type for " + variableNameLabel.getText() + " variable");
        attachSelectionAnnouncer(dataTypeComboBox, "Data type");
    }

    private static @NotNull ListPopupValuesProvider createValuesProvider(StatementExecutionVariable variable, StatementExecutionProcessor executionProcessor, StatementExecutionVariables variablesCache) {
        return new ListPopupValuesProvider() {
            @Override
            public String getName() {
                return "Value History";
            }

            @Override
            public List<String> getValues() {
                List<String> values = new ArrayList<>();
                VirtualFile virtualFile = executionProcessor.getVirtualFile();
                Set<StatementExecutionVariable> variables = variablesCache.getVariables(virtualFile);
                for (StatementExecutionVariable executionVariable : variables) {
                    if (Objects.equals(executionVariable.getName(), variable.getName())) {
                        Iterable<String> valueHistory = executionVariable.getValueHistory();
                        for (String value : valueHistory) {
                            values.add(value);
                        }
                    }
                }

                return values;
            }
        };
    }

    public StatementExecutionInputForm getParentForm() {
        return ensureParentComponent();
    }

    void saveValue() {
        String trim = editorComponent.getTextField().getText().trim();
        variable.setValue(trim);
        variable.setDataType(dataTypeComboBox.getSelectedValue());
        StatementExecutionProcessor executionProcessor = getParentForm().getExecutionProcessor();
        Project project = executionProcessor.getProject();
        StatementExecutionManager executionManager = StatementExecutionManager.getInstance(project);
        executionManager.cacheVariable(executionProcessor.getVirtualFile(), variable);
    }

    @Override
    public Component[] getAlignableComponents() {
        return new Component[]{variableNameLabel, valueFieldPanel};
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public JTextField getEditorComponent() {
        return editorComponent.getTextField();
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return dataTypeComboBox;
    }

    @Override
    public void disposeInner() {
        variable.setPreviewValueProvider(null);
        super.disposeInner();
    }
}
