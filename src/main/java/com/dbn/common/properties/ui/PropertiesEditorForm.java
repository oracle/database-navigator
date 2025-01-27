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
import com.intellij.ui.ToolbarDecorator;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.Map;

import static com.dbn.common.ui.util.Decorators.createToolbarDecorator;
import static com.dbn.common.ui.util.Decorators.createToolbarDecoratorComponent;

public class PropertiesEditorForm extends DBNFormBase {
    private JPanel mainPanel;
    private final PropertiesEditorTable table;

    public PropertiesEditorForm(DBNForm parent, Map<String, String> properties, boolean showMoveButtons) {
        super(parent);
        table = new PropertiesEditorTable(this, properties);
        Disposer.register(this, table);

        JPanel tablePanel = initTableComponent(showMoveButtons);
        mainPanel.add(tablePanel, BorderLayout.CENTER);
  }

    private JPanel initTableComponent(boolean showMoveButtons) {
        ToolbarDecorator decorator = createToolbarDecorator(table);
        decorator.setAddAction(b -> table.insertRow());
        decorator.setRemoveAction(b -> table.removeRow());

        if (showMoveButtons) {
            decorator.setMoveUpAction(b -> table.moveRowUp());
            decorator.setMoveDownAction(b -> table.moveRowDown());
        }
        return createToolbarDecoratorComponent(decorator, table);
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
