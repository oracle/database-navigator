/*
 * Copyright 2025 Oracle and/or its affiliates
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
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.table.DBNTableModel;
import com.dbn.common.util.Strings;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.ToolbarDecorator;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dbn.common.ui.util.Decorators.createToolbarDecorator;
import static com.dbn.common.ui.util.Decorators.createToolbarDecoratorComponent;

@Getter
public class PropertiesEditorForm extends DBNFormBase {
    private JPanel mainPanel;
    private final PropertiesEditorTable table;
    private final Map<String, PropertiesValidator> validators = new HashMap<>();

    public PropertiesEditorForm(DBNComponent parent, Map<String, String> properties, boolean showMoveButtons) {
        this(parent, properties, showMoveButtons, true);
    }
    public PropertiesEditorForm(DBNComponent parent, Map<String, String> properties, boolean showMoveButtons, boolean showAddRemoveButtons) {
        super(parent);
        table = new PropertiesEditorTable(this, properties);
        Disposer.register(this, table);

        JPanel tablePanel = initTableComponent(showMoveButtons, showAddRemoveButtons);
        mainPanel.add(tablePanel, BorderLayout.CENTER);
    }

    private JPanel initTableComponent(boolean showMoveButtons, boolean showAddRemoveButtons) {
        ToolbarDecorator decorator = createToolbarDecorator(table);
        if (showAddRemoveButtons) {
            decorator.setAddAction(b -> table.insertRow());
            decorator.setRemoveAction(b -> table.removeRow());
        }

        if (showMoveButtons) {
            decorator.setMoveUpAction(b -> table.moveRowUp());
            decorator.setMoveDownAction(b -> table.moveRowDown());
        }
        return createToolbarDecoratorComponent(decorator, table);
    }

    protected void initValidation() {
        if (validators.isEmpty()) return;

        addValidator(table, t -> {
            DBNTableModel model = t.getModel();
            List<ValidationInfo> validationInfos = new ArrayList<>();
            int rowCount = table.getRowCount();
            for (int row = 0; row < rowCount; row++) {
                String cellValue = (String) model.getValueAt(row, 1);
                if (Strings.isEmpty(cellValue)) continue;

                String key = (String) model.getValueAt(row, 0);
                PropertiesValidator validator = validators.get(key);
                if (validator == null) continue;

                ValidationInfo result = validator.validate(key, cellValue);
                if (result == null) continue;

                validationInfos.add(result);
            }
            return validationInfos;
        });
    }

    public void addValidator(PropertiesValidator validator, String key) {
        this.validators.put(key, validator);
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
