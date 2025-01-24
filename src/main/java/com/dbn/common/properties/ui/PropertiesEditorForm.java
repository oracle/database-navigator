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

package com.dbn.common.properties.ui;

import com.dbn.browser.TreeNavigationHistory;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.table.DBNTableModel;
import com.dbn.common.ui.util.UserInterface;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.ToolbarDecorator;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Container;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropertiesEditorForm extends DBNFormBase {
    private JPanel mainPanel;
    private final PropertiesEditorTable table;
    private Map<String, PropertiesValidator> validators = new HashMap<>();

    public PropertiesEditorForm(DBNForm parent, Map<String, String> properties, boolean showMoveButtons) {
        this(parent, properties, showMoveButtons, true);
    }
    public PropertiesEditorForm(DBNForm parent, Map<String, String> properties, boolean showMoveButtons, boolean showAddRemoveButtons) {
        super(parent);
        table = new PropertiesEditorTable(this, properties);
        Disposer.register(this, table);

        ToolbarDecorator decorator = UserInterface.createToolbarDecorator(table);
        if (showAddRemoveButtons) {
            decorator.setAddAction(button -> table.insertRow());
            decorator.setRemoveAction(button -> table.removeRow());
        }

        if (showMoveButtons) {
            decorator.setMoveUpAction(button -> table.moveRowUp());
            decorator.setMoveDownAction(button -> table.moveRowDown());
        }

        JPanel propertiesPanel = decorator.createPanel();
        Container parentContainer = table.getParent();
        parentContainer.setBackground(table.getBackground());
        mainPanel.add(propertiesPanel, BorderLayout.CENTER);

        parent.validate();
        initValidation();
        //parent.getParentDialog().validateInput();
/*      i\
        propertiesTableScrollPane.setViewportView(propertiesTable);
        propertiesTableScrollPane.setPreferredSize(new Dimension(200, 80));
*/
    }

    private void initValidation() {
        table.getModel().addTableModelListener(l -> {
            if (l.getColumn() == 1) {
                DBNTableModel source = (DBNTableModel) l.getSource();
                int firstRow = l.getFirstRow();
                int lastRow = l.getLastRow();
                List<ValidationInfo> valInfos = new ArrayList<>();
                for (int row = firstRow; row <= lastRow; row++) {
                    Object cellValue =  source.getValueAt(row, 1);
                    if (cellValue != null && !"".equals(cellValue)) {
                        String key = (String) source.getValueAt(row, 0);
                        PropertiesValidator v = validators.get(key);
                        if (v != null) {
                            ValidationInfo result = v.validate(key, cellValue);
                            if (result != null) {
                                valInfos.add(result);
                            }
                        }
                    }
                }
                getParentDialog().processValidation(valInfos);
            }
        });
    }

    public void addValidator(PropertiesValidator validator, String key) {
        this.validators.put(key, validator);
    }

    public PropertiesEditorTable getTable() {
        return table;
    }

    public void setProperties(Map<String, String> properties) {
        table.setProperties(properties);
    } 

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public Map<String, String> getProperties() {
        return table.getModel().exportProperties();
    }
}
