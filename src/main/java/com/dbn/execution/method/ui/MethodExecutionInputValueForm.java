/*
 * Copyright 2026 Oracle and/or its affiliates
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
import com.dbn.common.routine.Consumer;
import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.Borders;
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
import com.dbn.data.type.GenericDataType;
import com.dbn.execution.common.input.ExecutionVariable;
import com.dbn.execution.common.input.ExecutionVariableHistory;
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
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.dbn.common.ui.util.Accessibility.setAccessibleUnit;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.nls.NlsResources.txt;

/**
 * A method input row. An empty attribute path represents the method argument;
 * a non-empty path represents an attribute nested inside that argument.
 */
public class MethodExecutionInputValueForm extends DBNFormBase {
    private static final int NESTED_INDENT = 24;

    private JPanel mainPanel;
    private JLabel nameLabel;
    private JLabel typeLabel;
    private JPanel inputFieldPanel;
    private JPanel typeAttributesPanel;

    private JTextField inputTextField;
    private UserValueHolderImpl<String> userValueHolder;

    private final DBObjectRef<DBArgument> argument;
    private final List<DBObjectRef<DBTypeAttribute>> attributePath;
    private final List<DBObjectRef<DBType>> declaredTypePath;
    private final List<MethodExecutionInputValueForm> childForms = DisposableContainers.list(this);

    MethodExecutionInputValueForm(MethodExecutionInputForm parentForm, DBArgument argument) {
        this(parentForm, argument, Collections.emptyList(), Collections.emptyList());
    }

    private MethodExecutionInputValueForm(
            DBNFormBase parent,
            DBArgument argument,
            List<DBTypeAttribute> attributePath,
            List<DBType> declaredTypePath) {
        super(parent);
        this.argument = DBObjectRef.of(argument);
        this.attributePath = new ArrayList<>(attributePath.size());
        for (DBTypeAttribute attribute : attributePath) {
            this.attributePath.add(DBObjectRef.of(attribute));
        }
        this.declaredTypePath = new ArrayList<>(declaredTypePath.size());
        for (DBType declaredType : declaredTypePath) {
            this.declaredTypePath.add(DBObjectRef.of(declaredType));
        }

        DBArgument leadArgument = getArgument();
        DBTypeAttribute leadAttribute = getLeadAttribute();
        nameLabel.setText(leadAttribute == null ? leadArgument.getName() : leadAttribute.getName());
        nameLabel.setIcon(leadAttribute == null ? leadArgument.getIcon() : leadAttribute.getIcon());
        if (leadAttribute != null) {
            nameLabel.setBorder(Borders.insetBorder(0, 4, 0, 0));
        }

        DBDataType dataType = leadAttribute == null ? leadArgument.getDataType() : leadAttribute.getDataType();
        DBType declaredType = dataType.getDeclaredType();
        typeLabel.setForeground(UIUtil.getInactiveTextColor());

        boolean nestedType = dataType.isPurelyDeclared() && declaredType != null;
        configureTypeLabel(dataType, declaredType, nestedType);

        if (nestedType) {
            boolean collectionCanBeExpanded = isArgumentRow() || !declaredType.isCollection();
            if (collectionCanBeExpanded && !isRecursiveType(declaredType)) {
                addNestedAttributes(declaredType);
            }
        } else if (leadAttribute != null || (dataType.isNative() && leadArgument.isInput())) {
            initInputField(leadArgument, dataType, leadAttribute != null);
        }
    }

    private void configureTypeLabel(DBDataType dataType, DBType declaredType, boolean nestedType) {
        if (isArgumentRow() && declaredType != null) {
            typeLabel.setText(declaredType.getName());
            typeLabel.setIcon(declaredType.getIcon());
        } else {
            typeLabel.setText(dataType.getQualifiedName());
        }

        if (nestedType) {
            showInlineType(declaredType.getName(), declaredType.getIcon());
        } else if (isArgumentRow() && !dataType.isNative()) {
            showInlineType(typeLabel.getText(), typeLabel.getIcon());
        }
    }

    private void showInlineType(String name, Icon icon) {
        JLabel inlineTypeLabel = new JLabel(name, icon, SwingConstants.LEFT);
        inlineTypeLabel.setForeground(typeLabel.getForeground());
        inlineTypeLabel.setBorder(Borders.insetBorder(4, 0, 4, 0));
        inputFieldPanel.add(inlineTypeLabel, BorderLayout.CENTER);
        typeLabel.setVisible(false);
    }

    private void addNestedAttributes(DBType declaredType) {
        List<DBTypeAttribute> attributes = declaredType.getAttributes();
        if (attributes.isEmpty()) return;

        List<DBType> nestedTypePath = resolveDeclaredTypePath();
        nestedTypePath.add(declaredType);

        JPanel nestedPanel = new JPanel();
        nestedPanel.setLayout(new BoxLayout(nestedPanel, BoxLayout.Y_AXIS));
        nestedPanel.setBorder(Borders.insetBorder(0, NESTED_INDENT, 0, 0));
        typeAttributesPanel.add(nestedPanel, BorderLayout.CENTER);
        typeAttributesPanel.setVisible(true);

        for (DBTypeAttribute attribute : attributes) {
            List<DBTypeAttribute> nestedAttributePath = new ArrayList<>(getAttributePath());
            nestedAttributePath.add(attribute);

            MethodExecutionInputValueForm childForm = new MethodExecutionInputValueForm(
                    this,
                    getArgument(),
                    nestedAttributePath,
                    nestedTypePath);
            nestedPanel.add(childForm.getComponent());
            childForms.add(childForm);
        }
    }

    private List<DBType> resolveDeclaredTypePath() {
        List<DBType> types = new ArrayList<>(declaredTypePath.size() + 1);
        for (DBObjectRef<DBType> typeRef : declaredTypePath) {
            DBType type = DBObjectRef.get(typeRef);
            if (type != null) types.add(type);
        }
        return types;
    }

    private boolean isRecursiveType(DBType declaredType) {
        DBObjectRef<DBType> declaredTypeRef = DBObjectRef.of(declaredType);
        for (DBObjectRef<DBType> typeRef : declaredTypePath) {
            if (declaredTypeRef.equals(typeRef)) return true;
        }
        return false;
    }

    private void initInputField(DBArgument argument, DBDataType dataType, boolean attributeValue) {
        GenericDataType genericDataType = dataType.getGenericDataType();
        Project project = ensureProject();
        List<DBTypeAttribute> resolvedAttributePath = getAttributePath();
        String value = getExecutionInput().getInputValue(argument, resolvedAttributePath);

        if (genericDataType.is(GenericDataType.XMLTYPE, GenericDataType.CLOB)) {
            TextFieldWithTextEditor inputField = new TextFieldWithTextEditor(project, null);
            TextContentType contentType = genericDataType == GenericDataType.XMLTYPE ?
                    TextContentType.get(project, "XML") :
                    TextContentType.getPlainText(project);

            DBObjectType valueType = attributeValue ? DBObjectType.TYPE_ATTRIBUTE : DBObjectType.ARGUMENT;
            userValueHolder = new UserValueHolderImpl<>(getValueName(), valueType, dataType, project);
            userValueHolder.setUserValue(value);
            userValueHolder.setContentType(contentType);
            inputField.setUserValueHolder(userValueHolder);
            inputField.setPreferredSize(new JBDimension(240, -1));
            inputTextField = inputField.getTextField();
            inputFieldPanel.add(inputField, BorderLayout.CENTER);
        } else {
            TextFieldWithPopup inputField = new TextFieldWithPopup(project);
            inputField.setPreferredSize(new JBDimension(240, -1));
            if (genericDataType == GenericDataType.DATE_TIME) {
                inputField.createCalendarPopup(false);
            }
            inputField.createValuesListPopup(createValuesProvider(), argument, true);
            inputTextField = inputField.getTextField();
            inputTextField.setText(value);
            inputFieldPanel.add(inputField, BorderLayout.CENTER);
        }

        nameLabel.setLabelFor(inputTextField);
        inputTextField.setDisabledTextColor(inputTextField.getForeground());
        setAccessibleUnit(inputTextField, typeLabel.getText());
    }

    @Override
    protected void initFieldAlignment() {
        FieldAlignerData alignerData = getFieldAlignerData();
        alignerData.registerForms(() -> childForms);
        alignerData.registerFieldGroup(nameLabel, inputFieldPanel, typeLabel);
    }

    private ListPopupValuesProvider createValuesProvider() {
        return new ListPopupValuesProvider() {
            @Override
            public String getName() {
                return txt("msg.execution.title.ValueHistory");
            }

            @Override
            public List<String> getValues() {
                DBArgument argument = getArgument();
                List<DBTypeAttribute> attributes = getAttributePath();
                if (argument == null || hasInvalidAttributePath(attributes)) return Collections.emptyList();
                return getExecutionInput().getInputValueHistory(argument, attributes);
            }

            @Override
            public List<String> getSecondaryValues() {
                DBArgument argument = getArgument();
                List<DBTypeAttribute> attributes = getAttributePath();
                if (argument == null || hasInvalidAttributePath(attributes)) return Collections.emptyList();

                ConnectionHandler connection = argument.getConnection();
                ConnectionId connectionId = connection.getConnectionId();
                MethodExecutionManager executionManager = MethodExecutionManager.getInstance(argument.getProject());
                ExecutionVariableHistory valuesHistory = executionManager.getArgumentValuesHistory();
                ExecutionVariable argumentValue = valuesHistory.getExecutionVariable(connectionId, getValueName(), false);
                if (argumentValue == null) return Collections.emptyList();

                List<String> cachedValues = new ArrayList<>(argumentValue.getValueHistory());
                cachedValues.removeAll(getValues());
                return cachedValues;
            }
        };
    }

    private boolean hasInvalidAttributePath(List<DBTypeAttribute> attributes) {
        return !attributePath.isEmpty() && attributes.isEmpty();
    }

    private String getValueName() {
        StringBuilder name = new StringBuilder(argument.getObjectName());
        for (DBObjectRef<DBTypeAttribute> attributeRef : attributePath) {
            name.append('.').append(attributeRef.getObjectName());
        }
        return name.toString();
    }

    private boolean isArgumentRow() {
        return attributePath.isEmpty();
    }

    private DBTypeAttribute getLeadAttribute() {
        return attributePath.isEmpty() ? null : DBObjectRef.get(attributePath.get(attributePath.size() - 1));
    }

    private DBArgument getArgument() {
        return DBObjectRef.get(argument);
    }

    private List<DBTypeAttribute> getAttributePath() {
        if (attributePath.isEmpty()) return Collections.emptyList();

        List<DBTypeAttribute> attributes = new ArrayList<>(attributePath.size());
        for (DBObjectRef<DBTypeAttribute> attributeRef : attributePath) {
            DBTypeAttribute attribute = DBObjectRef.get(attributeRef);
            if (attribute == null) return Collections.emptyList();
            attributes.add(attribute);
        }
        return attributes;
    }

    private MethodExecutionInput getExecutionInput() {
        return ensureParentFrom(MethodExecutionInputForm.class).getExecutionInput();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public void updateExecutionInput() {
        DBArgument argument = getArgument();
        if (argument == null) return;

        if (!childForms.isEmpty()) {
            for (MethodExecutionInputValueForm childForm : childForms) {
                childForm.updateExecutionInput();
            }
        } else if (inputTextField != null) {
            MethodExecutionInput executionInput = getExecutionInput();
            String value = userValueHolder != null ?
                    Commons.nullIfEmpty(userValueHolder.getUserValue()) :
                    Commons.nullIfEmpty(getText(inputTextField));
            List<DBTypeAttribute> attributes = getAttributePath();
            if (hasInvalidAttributePath(attributes)) return;
            if (attributes.isEmpty()) {
                executionInput.setInputValue(argument, value);
            } else {
                executionInput.setInputValue(argument, attributes, value);
            }
        }
    }

    public void onInputChange(Consumer<DocumentEvent> consumer) {
        if (inputTextField != null) {
            TextFields.onTextChange(inputTextField, consumer);
        }
        for (MethodExecutionInputValueForm childForm : childForms) {
            childForm.onInputChange(consumer);
        }
    }

    public int getScrollUnitIncrement() {
        return (int) (childForms.isEmpty() ?
                mainPanel.getPreferredSize().getHeight() :
                childForms.get(0).getComponent().getPreferredSize().getHeight());
    }
}
