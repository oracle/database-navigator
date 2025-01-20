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

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.util.Commons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.data.editor.text.TextContentType;
import com.dbn.data.editor.ui.ListPopupValuesProvider;
import com.dbn.data.editor.ui.TextFieldWithPopup;
import com.dbn.data.editor.ui.TextFieldWithTextEditor;
import com.dbn.data.editor.ui.UserValueHolderImpl;
import com.dbn.data.type.DBDataType;
import com.dbn.data.type.GenericDataType;
import com.dbn.execution.method.MethodExecutionArgumentValue;
import com.dbn.execution.method.MethodExecutionArgumentValueHistory;
import com.dbn.execution.method.MethodExecutionInput;
import com.dbn.execution.method.MethodExecutionManager;
import com.dbn.object.DBArgument;
import com.dbn.object.DBTypeAttribute;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MethodExecutionInputTypeAttributeForm extends DBNFormBase {
    private JLabel attributeTypeLabel;
    private JLabel attributeLabel;
    private JPanel mainPanel;
    private JPanel attributePanel;
    private JPanel inputFieldPanel;

    @Getter
    private final JTextField inputTextField;
    private UserValueHolderImpl<String> userValueHolder;

    private final DBObjectRef<DBArgument> argument;
    private final DBObjectRef<DBTypeAttribute> typeAttribute;

    MethodExecutionInputTypeAttributeForm(MethodExecutionInputArgumentForm parent, DBArgument argument, DBTypeAttribute typeAttribute) {
        super(parent);
        this.argument = DBObjectRef.of(argument);
        this.typeAttribute = DBObjectRef.of(typeAttribute);
        attributeLabel.setText(typeAttribute.getName());
        attributeLabel.setIcon(typeAttribute.getIcon());
        attributeTypeLabel.setForeground(UIUtil.getInactiveTextColor());
        attributeTypeLabel.setText(typeAttribute.getDataType().getQualifiedName());

        DBDataType dataType = typeAttribute.getDataType();
        GenericDataType genericDataType = dataType.getGenericDataType();

        Project project = ensureProject();

        String value = getExecutionInput().getInputValue(argument, typeAttribute);
        if (genericDataType.is(GenericDataType.XMLTYPE, GenericDataType.CLOB)) {
            TextFieldWithTextEditor inputField = new TextFieldWithTextEditor(project, "[" + genericDataType.name() + "]");

            TextContentType contentType =
                    genericDataType == GenericDataType.XMLTYPE ?
                            TextContentType.get(project, "XML") :
                            TextContentType.getPlainText(project);
            if (contentType == null) {
                contentType = TextContentType.getPlainText(project);
            }

            String typeAttributeName = argument.getName() + "." + typeAttribute.getName();
            userValueHolder = new UserValueHolderImpl<>(typeAttributeName, DBObjectType.TYPE_ATTRIBUTE, dataType, project);
            userValueHolder.setUserValue(value);
            userValueHolder.setContentType(contentType);
            inputField.setUserValueHolder(userValueHolder);

            inputField.setPreferredSize(new Dimension(240, -1));
            inputTextField = inputField.getTextField();
            inputFieldPanel.add(inputField, BorderLayout.CENTER);
        } else {
            TextFieldWithPopup inputField = new TextFieldWithPopup(project);
            inputField.setPreferredSize(new Dimension(240, -1));
            if (genericDataType == GenericDataType.DATE_TIME) {
                inputField.createCalendarPopup(false);
            }

            inputField.createValuesListPopup(createValuesProvider(), argument, true);
            inputTextField = inputField.getTextField();
            inputFieldPanel.add(inputField, BorderLayout.CENTER);
            inputTextField.setText(value);
        }

        inputTextField.setDisabledTextColor(inputTextField.getForeground());
    }

    public MethodExecutionInputArgumentForm getParentForm() {
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
                DBTypeAttribute typeAttribute = getTypeAttribute();
                if (argument != null && typeAttribute != null) {
                    return getExecutionInput().getInputValueHistory(argument, typeAttribute);
                }
                return Collections.emptyList();
            }

            @Override
            public List<String> getSecondaryValues() {
                DBArgument argument = getArgument();
                DBTypeAttribute typeAttribute = getTypeAttribute();
                if (argument != null && typeAttribute != null) {
                    ConnectionHandler connection = argument.getConnection();
                    MethodExecutionManager executionManager = MethodExecutionManager.getInstance(argument.getProject());
                    MethodExecutionArgumentValueHistory argumentValuesCache = executionManager.getArgumentValuesHistory();
                    MethodExecutionArgumentValue argumentValue = argumentValuesCache.getArgumentValue(
                            connection.getConnectionId(),
                            getAttributeQualifiedName(),
                            false);

                    if (argumentValue != null) {
                        List<String> cachedValues = new ArrayList<>(argumentValue.getValueHistory());
                        cachedValues.removeAll(getValues());
                        return cachedValues;
                    }
                }
                return Collections.emptyList();
            }
        };
    }

    @NotNull
    private String getAttributeQualifiedName() {
        return argument.getObjectName() + '.' + typeAttribute.getObjectName();
    }

    public DBArgument getArgument() {
        return DBObjectRef.get(argument);
    }

    public DBTypeAttribute getTypeAttribute() {
        return DBObjectRef.get(typeAttribute);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public void updateExecutionInput() {
        DBArgument argument = getArgument();
        DBTypeAttribute typeAttribute = getTypeAttribute();
        if (argument != null && typeAttribute != null) {
            MethodExecutionInput executionInput = getExecutionInput();
            if (userValueHolder != null ) {
                String value = Commons.nullIfEmpty(userValueHolder.getUserValue());
                executionInput.setInputValue(argument, typeAttribute, value);
            } else {
                String value = Commons.nullIfEmpty(inputTextField == null ? null : inputTextField.getText());
                executionInput.setInputValue(argument, typeAttribute, value);
            }
        }
    }

    private MethodExecutionInput getExecutionInput() {
        return getParentForm().getParentForm().getExecutionInput();
    }

    protected int[] getMetrics(int[] metrics) {
        if (metrics == null) metrics = new int[]{0, 0, 0};
        return new int[] {
                Math.max(metrics[0], (int) attributePanel.getPreferredSize().getWidth()),
                Math.max(metrics[1], (int) inputFieldPanel.getPreferredSize().getWidth()),
                Math.max(metrics[2], (int) attributeTypeLabel.getPreferredSize().getWidth())};
    }

    protected void adjustMetrics(int[] metrics) {
        attributePanel.setPreferredSize(new Dimension(metrics[0], attributePanel.getHeight()));
        inputFieldPanel.setPreferredSize(new Dimension(metrics[1], inputFieldPanel.getHeight()));
        attributeTypeLabel.setPreferredSize(new Dimension(metrics[2], attributeTypeLabel.getHeight()));
    }
}
