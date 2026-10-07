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

package com.dbn.data.type.ui;

import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.locale.Formatter;
import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.Borders;
import com.dbn.data.editor.ui.TextFieldWithPopup;
import com.dbn.data.type.DBDataType;
import com.dbn.data.type.DBNativeDataType;
import com.dbn.data.type.GenericDataType;
import com.dbn.object.DBType;
import com.dbn.object.DBTypeAttribute;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.dbn.common.ui.util.Accessibility.setAccessibleUnit;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

/** A single type attribute row and, for record attributes, its nested rows. */
public class DeclaredTypeAttributeForm extends DBNFormBase {
    private JPanel mainPanel;
    private JLabel nameLabel;
    private JLabel typeLabel;
    private JPanel inputFieldPanel;
    private JPanel typeAttributesPanel;

    private final DBDataType dataType;
    private final Object initialValue;
    private final List<DeclaredTypeAttributeForm> childForms = DisposableContainers.list(this);

    private TextFieldWithPopup<?> inputField;
    private DBType editableNestedType;

    DeclaredTypeAttributeForm(
            DBNFormBase parent,
            DBTypeAttribute attribute,
            Object initialValue,
            Set<DBObjectRef<DBType>> typePath) {
        super(parent);
        this.dataType = attribute.getDataType();
        this.initialValue = initialValue;

        nameLabel.setText(attribute.getName());
        nameLabel.setIcon(attribute.getIcon());
        nameLabel.setBorder(Borders.insetBorder(0, 4, 0, 0));
        typeLabel.setText(dataType.getQualifiedName());
        typeLabel.setForeground(UIUtil.getInactiveTextColor());
        typeAttributesPanel.setLayout(new BorderLayout());

        Set<DBObjectRef<DBType>> nestedTypePath = new HashSet<>(typePath);
        DBType nestedType = dataType.getDeclaredType();
        boolean recursive = nestedType != null && nestedTypePath.contains(DBObjectRef.of(nestedType));
        if (dataType.isPurelyDeclared() && nestedType != null && !nestedType.isCollection() && !recursive) {
            nestedTypePath.add(DBObjectRef.of(nestedType));
            editableNestedType = nestedType;
            showInlineType(nestedType.getName(), nestedType.getIcon());
            addNestedAttributes(nestedType, nestedTypePath, initialValue);
        } else if (supportsScalarInput(dataType)) {
            initInputField(parent.ensureProject());
        } else {
            showInlineType(dataType.getQualifiedName(), nestedType == null ? null : nestedType.getIcon());
        }
    }

    private void addNestedAttributes(DBType nestedType, Set<DBObjectRef<DBType>> typePath, Object value) {
        Object[] values = readAttributes(value);
        List<DBTypeAttribute> attributes = nestedType.getAttributes();
        JPanel nestedPanel = new JPanel();
        nestedPanel.setLayout(new BoxLayout(nestedPanel, BoxLayout.Y_AXIS));
        nestedPanel.setBorder(Borders.insetBorder(0, 24, 0, 0));

        for (int i = 0; i < attributes.size(); i++) {
            Object nestedValue = i < values.length ? values[i] : null;
            DeclaredTypeAttributeForm childForm = new DeclaredTypeAttributeForm(
                    this,
                    attributes.get(i),
                    nestedValue,
                    typePath);
            nestedPanel.add(childForm.getComponent());
            childForms.add(childForm);
        }

        if (!childForms.isEmpty()) {
            typeAttributesPanel.add(nestedPanel, BorderLayout.CENTER);
            typeAttributesPanel.setVisible(true);
        }
    }

    private void showInlineType(String name, javax.swing.Icon icon) {
        JLabel inlineTypeLabel = new JLabel(name, icon, SwingConstants.LEFT);
        inlineTypeLabel.setForeground(typeLabel.getForeground());
        inputFieldPanel.add(inlineTypeLabel, BorderLayout.CENTER);
        typeLabel.setVisible(false);
    }

    private void initInputField(Project project) {
        inputField = new TextFieldWithPopup<>(project);
        inputField.setPreferredSize(new JBDimension(260, -1));
        if (dataType.isNative() && dataType.getGenericDataType() == GenericDataType.DATE_TIME) {
            inputField.createCalendarPopup(false);
        }

        JTextField textField = inputField.getTextField();
        textField.setText(ensureFormatter().formatObject(initialValue));
        nameLabel.setLabelFor(textField);
        setAccessibleUnit(textField, typeLabel.getText());
        inputFieldPanel.add(inputField, BorderLayout.CENTER);
    }

    private static boolean supportsScalarInput(DBDataType dataType) {
        DBNativeDataType nativeType = dataType.getNativeType();
        if (nativeType != null) return !nativeType.isCollection();
        return !dataType.isDeclared();
    }

    Object getValue(Formatter formatter) throws ParseException {
        if (editableNestedType != null) {
            Object[] values = new Object[childForms.size()];
            for (int i = 0; i < childForms.size(); i++) {
                values[i] = childForms.get(i).getValue(formatter);
            }
            return values;
        }

        if (inputField == null) return initialValue;
        String text = inputField.getTextField().getText();
        if (text.isEmpty()) return null;

        DBNativeDataType nativeType = dataType.getNativeType();
        if (nativeType == null) return text;

        GenericDataType genericDataType = nativeType.getGenericDataType();
        Object parsedValue = genericDataType == GenericDataType.BOOLEAN ?
                Boolean.parseBoolean(text) :
                formatter.parseObject(dataType.getTypeClass(), text);
        return nativeType.getDefinition().convert(parsedValue);
    }

    private static Object[] readAttributes(Object value) {
        if (value instanceof Object[] attributes) {
            return attributes;
        }
        if (value instanceof java.sql.Struct struct) {
            try {
                return struct.getAttributes();
            } catch (SQLException e) {
                conditionallyLog(e);
            }
        }
        return new Object[0];
    }

    @Override
    protected void initFieldAlignment() {
        FieldAlignerData alignerData = getFieldAlignerData();
        alignerData.registerForms(() -> childForms);
        alignerData.registerFieldGroup(nameLabel, inputFieldPanel, typeLabel);
    }

    @NotNull
    @Override
    protected JPanel getMainComponent() {
        return mainPanel;
    }
}
