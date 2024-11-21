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

package com.dbn.data.grid.options.ui;

import com.dbn.common.event.ProjectEvents;
import com.dbn.common.options.SettingsChangeNotifier;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.ui.list.EditableStringListForm;
import com.dbn.data.grid.options.DataGridAuditColumnSettings;
import com.dbn.data.grid.options.DataGridSettingsChangeListener;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.util.List;

public class DataGridAuditColumnSettingsForm extends ConfigurationEditorForm<DataGridAuditColumnSettings> {
    private JPanel mainPanel;
    private JCheckBox visibleCheckBox;
    private JCheckBox editableCheckBox;
    private JPanel columnNameListPanel;

    private EditableStringListForm editableStringListForm;

    public DataGridAuditColumnSettingsForm(DataGridAuditColumnSettings settings) {
        super(settings);
        editableStringListForm = new EditableStringListForm(this, "Audit column names", true);
        JComponent listComponent = editableStringListForm.getComponent();
        columnNameListPanel.add(listComponent, BorderLayout.CENTER);

        resetFormChanges();
        editableCheckBox.setEnabled(visibleCheckBox.isSelected());
        registerComponent(mainPanel);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    protected ActionListener createActionListener() {
        return e -> {
            getConfiguration().setModified(true);
            if (e.getSource() == visibleCheckBox) {
                editableCheckBox.setEnabled(visibleCheckBox.isSelected());
            }
        };
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        DataGridAuditColumnSettings configuration = getConfiguration();
        boolean auditColumnsVisible = visibleCheckBox.isSelected();
        boolean visibilityChanged = configuration.isShowColumns() != auditColumnsVisible;
        configuration.setShowColumns(auditColumnsVisible);
        configuration.setAllowEditing(editableCheckBox.isSelected());
        configuration.setColumnNames(editableStringListForm.getStringValues());

        Project project = configuration.getProject();
        SettingsChangeNotifier.register(() -> {
            if (!visibilityChanged) return;
            ProjectEvents.notify(project,
                    DataGridSettingsChangeListener.TOPIC,
                    (listener) -> listener.auditDataVisibilityChanged(auditColumnsVisible));
        });
    }

    @Override
    public void resetFormChanges() {
        DataGridAuditColumnSettings settings = getConfiguration();
        visibleCheckBox.setSelected(settings.isShowColumns());
        editableCheckBox.setSelected(settings.isAllowEditing());
        List<String> columnNamesSet = settings.getColumnNames();
        editableStringListForm.setStringValues(columnNamesSet);
    }
}
