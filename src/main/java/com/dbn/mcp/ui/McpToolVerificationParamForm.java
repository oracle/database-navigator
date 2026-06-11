package com.dbn.mcp.ui;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.locale.Formatter;
import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.listener.ComboBoxSelectionKeyListener;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.util.Strings;
import com.dbn.data.editor.ui.TextFieldPopupType;
import com.dbn.data.editor.ui.TextFieldWithPopup;
import com.dbn.data.type.GenericDataType;
import com.dbn.execution.statement.variables.StatementExecutionVariable;
import com.dbn.execution.statement.variables.VariableValueProvider;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.text.ParseException;
import java.util.Date;

import static com.dbn.common.ui.util.Accessibility.attachSelectionAnnouncer;
import static com.dbn.common.ui.util.Accessibility.setAccessibleDescription;
import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.common.ui.util.Accessibility.setAccessibleUnit;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.nls.NlsResources.txt;

public class McpToolVerificationParamForm extends DBNFormBase {
    private JPanel mainPanel;
    private JLabel variableNameLabel;
    private JPanel valueFieldPanel;
    private DBNComboBox<GenericDataType> dataTypeComboBox;

    private final StatementExecutionVariable variable;
    private final TextFieldWithPopup<?> editorComponent;
    private final Runnable previewUpdater;

    public McpToolVerificationParamForm(
            @NotNull Disposable parent,
            @NotNull Project project,
            @NotNull StatementExecutionVariable variable,
            @NotNull Runnable previewUpdater) {
        super(parent);
        this.variable = variable;
        this.previewUpdater = previewUpdater;

        variableNameLabel.setText(variable.getName());
        //variableNameLabel.setIcon(Icons.DBO_VARIABLE);

        dataTypeComboBox.setValues(
                GenericDataType.LITERAL,
                GenericDataType.NUMERIC,
                GenericDataType.DATE_TIME);

        dataTypeComboBox.setSelectedValue(variable.getDataType());

        editorComponent = new TextFieldWithPopup<>(project);
        editorComponent.createCalendarPopup(false);
        editorComponent.setPopupEnabled(TextFieldPopupType.CALENDAR, variable.getDataType() == GenericDataType.DATE_TIME);

        valueFieldPanel.add(editorComponent, BorderLayout.CENTER);
        JTextField textField = editorComponent.getTextField();
        String value = variable.getValue();
        textField.setText(value == null ? "" : value);

        textField.addKeyListener(ComboBoxSelectionKeyListener.create(dataTypeComboBox, false));
        variableNameLabel.setLabelFor(textField);

        variable.setPreviewValueProvider(new VariableValueProvider() {
            @Override
            public String getValue() {
                return getText(textField);
            }

            @Override
            public GenericDataType getDataType() {
                return dataTypeComboBox.getSelectedValue();
            }
        });

        dataTypeComboBox.addListener((oldValue, newValue) -> {
            variable.setDataType(newValue);
            editorComponent.setPopupEnabled(TextFieldPopupType.CALENDAR, newValue == GenericDataType.DATE_TIME);
            previewUpdater.run();
            setAccessibleUnit(textField, newValue.getName());
            validateInput(textField);
        });

        textField.setToolTipText(txt("app.shared.tooltip.ChangeVariableDataType"));
        Disposer.register(this, editorComponent);
    }

    @Override
    protected void initValidation() {
        addTextValidation(editorComponent.getTextField(), f -> validateDataType());
    }

    @Override
    protected void initFieldAlignment() {
        FieldAlignerData alignerData = getFieldAlignerData();
        alignerData.registerFieldGroup(variableNameLabel, valueFieldPanel);
    }

    private String validateDataType() {
        Formatter formatter = Formatter.getInstance(ensureProject());
        String value = getText(editorComponent.getTextField());
        if (Strings.isEmpty(value)) return null;

        GenericDataType dataType = dataTypeComboBox.getSelectedValue();
        if (dataType == GenericDataType.DATE_TIME) {
            try {
                formatter.parseDateTime(value);
            } catch (ParseException e) {
                String pattern = formatter.getDateFormatPattern();
                String sample = formatter.formatDate(new Date());
                return txt("msg.shared.error.InvalidDateFormat", pattern, sample);
            }
        } else if (dataType == GenericDataType.NUMERIC) {
            try {
                formatter.parseNumber(value);
            } catch (ParseException e) {
                String pattern = formatter.getNumberFormatPattern();
                String sample = formatter.formatNumber(123456.7890);
                return txt("msg.shared.error.InvalidNumberFormat", pattern, sample);
            }
        }
        return null;
    }

    @Override
    protected void initAccessibility() {
        JTextField textField = editorComponent.getTextField();
        setAccessibleName(dataTypeComboBox, txt("app.shared.aria.DataType"));
        setAccessibleUnit(textField, dataTypeComboBox.getSelectedValueName());
        setAccessibleDescription(textField, txt("app.shared.aria.ChangeDataTypeHint"));
        setAccessibleDescription(dataTypeComboBox, txt("app.shared.aria.DataTypeForVariable", variableNameLabel.getText()));
        attachSelectionAnnouncer(dataTypeComboBox, txt("app.shared.aria.DataType"));
    }

    public void saveValue() {
        String trim = getText(editorComponent.getTextField());
        variable.setValue(trim);
        variable.setDataType(dataTypeComboBox.getSelectedValue());
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
