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

package com.dbn.execution.method.ui;

import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.TextFields;
import com.dbn.common.util.Commons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.data.editor.text.TextContentType;
import com.dbn.data.editor.ui.ListPopupValuesProvider;
import com.dbn.data.editor.ui.TextFieldWithPopup;
import com.dbn.data.editor.ui.TextFieldWithTextEditor;
import com.dbn.data.editor.ui.UserValueHolderImpl;
import com.dbn.data.type.DBDataType;
import com.dbn.data.type.DBNativeDataType;
import com.dbn.data.type.DataTypeDefinition;
import com.dbn.data.type.GenericDataType;
import com.dbn.execution.method.MethodExecutionArgumentValue;
import com.dbn.execution.method.MethodExecutionArgumentValueHistory;
import com.dbn.execution.method.MethodExecutionInput;
import com.dbn.execution.method.MethodExecutionManager;
import com.dbn.object.DBArgument;
import com.dbn.object.DBType;
import com.dbn.object.DBTypeAttribute;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.ui.util.Accessibility.setAccessibleUnit;
import static java.util.Collections.emptyList;

public class MethodExecutionInputArgumentForm extends DBNFormBase {
    private JPanel mainPanel;
    private JLabel argumentLabel;
    private JLabel argumentTypeLabel;
    private JPanel typeAttributesPanel;
    private JPanel inputFieldPanel;

    private JTextField inputTextField;
    private UserValueHolderImpl<String> userValueHolder;

    private final DBObjectRef<DBArgument> argument;
    private final List<MethodExecutionInputTypeAttributeForm> typeAttributeForms = DisposableContainers.list(this);

    MethodExecutionInputArgumentForm(MethodExecutionInputForm parentForm, final DBArgument argument) {
        super(parentForm);
        this.argument = DBObjectRef.of(argument);
        String argumentName = argument.getName();
        argumentLabel.setText(argumentName);
        argumentLabel.setIcon(argument.getIcon());

        DBDataType dataType = argument.getDataType();

        argumentTypeLabel.setForeground(UIUtil.getInactiveTextColor());

        DBType declaredType = dataType.getDeclaredType();

        if (dataType.isNative()) {
            argumentTypeLabel.setText(dataType.getQualifiedName());
            typeAttributesPanel.setVisible(false);
        } else if (declaredType != null) {
            typeAttributesPanel.setLayout(new BoxLayout(typeAttributesPanel, BoxLayout.Y_AXIS));
            List<DBTypeAttribute> attributes = declaredType.getAttributes();
            for (DBTypeAttribute attribute : attributes) {
                addAttributePanel(attribute);
            }
        }

        if (declaredType != null) {
            argumentTypeLabel.setIcon(declaredType.getIcon());
            argumentTypeLabel.setText(declaredType.getName());
        }

        if (argument.isInput() && dataType.isNative()) {
            DBNativeDataType nativeDataType = dataType.getNativeType();
            DataTypeDefinition dataTypeDefinition = nativeDataType.getDefinition();
            GenericDataType genericDataType = dataTypeDefinition.getGenericDataType();

            Project project = argument.getProject();
            MethodExecutionInput executionInput = parentForm.getExecutionInput();
            String value = executionInput.getInputValue(argument);

            if (genericDataType.is(GenericDataType.XMLTYPE, GenericDataType.CLOB)) {
                TextFieldWithTextEditor inputField = new TextFieldWithTextEditor(project, "[" + genericDataType.name() + "]");

                TextContentType contentType =
                        genericDataType == GenericDataType.XMLTYPE ?
                                TextContentType.get(project, "XML") :
                                TextContentType.getPlainText(project);
                if (contentType == null) {
                    contentType = TextContentType.getPlainText(project);
                }

                userValueHolder = new UserValueHolderImpl<>(argumentName, DBObjectType.ARGUMENT, dataType, project);
                userValueHolder.setUserValue(value);
                userValueHolder.setContentType(contentType);
                inputField.setUserValueHolder(userValueHolder);

                inputField.setPreferredSize(new JBDimension(240, -1));
                inputTextField = inputField.getTextField();
                inputFieldPanel.add(inputField, BorderLayout.CENTER);
            } else {
                TextFieldWithPopup<?> inputField = new TextFieldWithPopup<>(project);
                inputField.setPreferredSize(new JBDimension(240, -1));
                if (genericDataType == GenericDataType.DATE_TIME) {
                    inputField.createCalendarPopup(false);
                }

                inputField.createValuesListPopup(createValuesProvider(), argument, true);
                inputTextField = inputField.getTextField();
                inputTextField.setText(value);
                inputFieldPanel.add(inputField, BorderLayout.CENTER);
            }

            argumentLabel.setLabelFor(inputTextField);
            inputTextField.setDisabledTextColor(inputTextField.getForeground());
            setAccessibleUnit(inputTextField, argumentTypeLabel.getText());
        } else {
            inputFieldPanel.setVisible(false);
        }
    }

    @NotNull
    public MethodExecutionInputForm getParentForm() {
        return ensureParentComponent();
    }

    @NotNull
    private ListPopupValuesProvider createValuesProvider() {
        return new ListPopupValuesProvider() {
            @Override
            public String getName() {
                return "Value History";
            }

            @Override
            public List<String> getValues() {
                DBArgument argument = getArgument();
                if (argument == null) return emptyList();

                MethodExecutionInput executionInput = getParentForm().getExecutionInput();
                return executionInput.getInputValueHistory(argument, null);
            }

            @Override
            public List<String> getSecondaryValues() {
                DBArgument argument = getArgument();
                if (argument == null) return emptyList();

                ConnectionHandler connection = argument.getConnection();
                ConnectionId connectionId = connection.getConnectionId();
                MethodExecutionManager executionManager = MethodExecutionManager.getInstance(argument.getProject());
                MethodExecutionArgumentValueHistory valuesHistory = executionManager.getArgumentValuesHistory();
                MethodExecutionArgumentValue argumentValue = valuesHistory.getArgumentValue(connectionId, argument.getName(), false);
                if (argumentValue == null) return emptyList();

                List<String> cachedValues = new ArrayList<>(argumentValue.getValueHistory());
                cachedValues.removeAll(getValues());
                return cachedValues;
            }
        };
    }

    private void addAttributePanel(DBTypeAttribute attribute) {
        MethodExecutionInputTypeAttributeForm argumentComponent = new MethodExecutionInputTypeAttributeForm(this, getArgument(), attribute);
        typeAttributesPanel.add(argumentComponent.getComponent());
        typeAttributeForms.add(argumentComponent);
    }

    public DBArgument getArgument() {
        return DBObjectRef.get(argument);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public void updateExecutionInput() {
        DBArgument argument = getArgument();
        if (argument == null) return;

        MethodExecutionInput executionInput = getParentForm().getExecutionInput();
        if (!typeAttributeForms.isEmpty()) {
            for (MethodExecutionInputTypeAttributeForm typeAttributeComponent : typeAttributeForms) {
                typeAttributeComponent.updateExecutionInput();
            }
        } else if (userValueHolder != null ) {
            String value = userValueHolder.getUserValue();
            executionInput.setInputValue(argument, value);
        } else {
            String value = Commons.nullIfEmpty(inputTextField == null ? null : inputTextField.getText());
            executionInput.setInputValue(argument, value);
        }
    }

    protected int[] getMetrics(int[] metrics) {
        if (!typeAttributeForms.isEmpty()) {
            for (MethodExecutionInputTypeAttributeForm typeAttributeComponent : typeAttributeForms) {
                metrics = typeAttributeComponent.getMetrics(metrics);
            }
        }

        return new int[] {
                Math.max(metrics[0], (int) argumentLabel.getPreferredSize().getWidth()),
                Math.max(metrics[1], (int) inputFieldPanel.getPreferredSize().getWidth()),
                Math.max(metrics[2], (int) argumentTypeLabel.getPreferredSize().getWidth())};
    }

    protected void adjustMetrics(int[] metrics) {
        if (!typeAttributeForms.isEmpty()) {
            for (MethodExecutionInputTypeAttributeForm typeAttributeComponent : typeAttributeForms) {
                typeAttributeComponent.adjustMetrics(metrics);
            }
        }
        argumentLabel.setPreferredSize(new Dimension(metrics[0], argumentLabel.getHeight()));
        inputFieldPanel.setPreferredSize(new Dimension(metrics[1], inputFieldPanel.getHeight()));
        argumentTypeLabel.setPreferredSize(new Dimension(metrics[2], argumentTypeLabel.getHeight()));
    }

    public void addDocumentListener(DocumentListener documentListener){
        TextFields.addDocumentListener(inputTextField, documentListener);

        for (MethodExecutionInputTypeAttributeForm typeAttributeComponent : typeAttributeForms){
            TextFields.addDocumentListener(typeAttributeComponent.getInputTextField(), documentListener);
        }
    }

    public int getScrollUnitIncrement() {
        return (int) (typeAttributeForms.isEmpty() ?
                mainPanel.getPreferredSize().getHeight() :
                typeAttributeForms.get(0).getComponent().getPreferredSize().getHeight());
    }
}
