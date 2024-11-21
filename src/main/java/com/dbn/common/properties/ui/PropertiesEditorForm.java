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

import com.dbn.common.dispose.Disposer;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.UserInterface;
import com.intellij.ui.ToolbarDecorator;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Container;
import java.util.Map;

public class PropertiesEditorForm extends DBNFormBase {
    private JPanel mainPanel;
    private final PropertiesEditorTable table;

    public PropertiesEditorForm(DBNForm parent, Map<String, String> properties, boolean showMoveButtons) {
        super(parent);
        table = new PropertiesEditorTable(this, properties);
        Disposer.register(this, table);

        ToolbarDecorator decorator = UserInterface.createToolbarDecorator(table);
        decorator.setAddAction(button -> table.insertRow());
        decorator.setRemoveAction(button -> table.removeRow());

        if (showMoveButtons) {
            decorator.setMoveUpAction(button -> table.moveRowUp());
            decorator.setMoveDownAction(button -> table.moveRowDown());
        }

        JPanel propertiesPanel = decorator.createPanel();
        Container parentContainer = table.getParent();
        parentContainer.setBackground(table.getBackground());
        mainPanel.add(propertiesPanel, BorderLayout.CENTER);
/*
        propertiesTableScrollPane.setViewportView(propertiesTable);
        propertiesTableScrollPane.setPreferredSize(new Dimension(200, 80));
*/
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
