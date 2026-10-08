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
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.util.Borders;
import com.dbn.object.DBType;
import com.dbn.object.DBTypeAttribute;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.util.ui.JBDimension;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.sql.SQLException;
import java.sql.Struct;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class DeclaredTypeValueEditorForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JLabel typeLabel;
    private JScrollPane attributesScrollPane;
    private JPanel attributesPanel;

    private final List<DeclaredTypeAttributeForm> attributeForms = DisposableContainers.list(this);

    DeclaredTypeValueEditorForm(
            DeclaredTypeValueEditorDialog parent,
            DBType declaredType,
            Object initialValue) {
        super(parent);

        installContextHeader(headerPanel);

        typeLabel.setText(declaredType.getName());
        typeLabel.setIcon(declaredType.getIcon());
        typeLabel.setForeground(UIUtil.getInactiveTextColor());
        attributesPanel.setLayout(new BoxLayout(attributesPanel, BoxLayout.Y_AXIS));
        attributesScrollPane.setBorder(Borders.COMPONENT_OUTLINE_BORDER);

        Object[] values = readAttributes(initialValue);
        Set<DBObjectRef<DBType>> typePath = new HashSet<>();
        typePath.add(DBObjectRef.of(declaredType));
        List<DBTypeAttribute> attributes = new ArrayList<>(declaredType.getAttributes());
        for (int i = 0; i < attributes.size(); i++) {
            Object value = i < values.length ? values[i] : null;
            DeclaredTypeAttributeForm attributeForm = new DeclaredTypeAttributeForm(
                    this,
                    attributes.get(i),
                    value,
                    typePath);
            attributesPanel.add(attributeForm.getComponent());
            attributeForms.add(attributeForm);
        }
        mainPanel.setPreferredSize(new JBDimension(590, 410));
    }

    Object[] getAttributeValues() throws ParseException {
        Object[] attributeValues = new Object[attributeForms.size()];
        for (int i = 0; i < attributeForms.size(); i++) {
            attributeValues[i] = attributeForms.get(i).getValue(ensureFormatter());
        }
        return attributeValues;
    }

    private static Object[] readAttributes(Object value) {
        if (value instanceof Object[] attributes) {
            return attributes;
        }
        if (value instanceof Struct struct) {
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
        getFieldAlignerData().registerForms(() -> attributeForms);
        if (!attributeForms.isEmpty()) {
            updateFieldAlignment();
        }
    }

    @NotNull
    @Override
    protected JPanel getMainComponent() {
        return mainPanel;
    }
}
