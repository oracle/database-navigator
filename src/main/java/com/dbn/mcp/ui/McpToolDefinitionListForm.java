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

package com.dbn.mcp.ui;

import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.util.Buttons;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.mcp.model.McpServerDefinition;
import com.dbn.mcp.model.McpToolDefinition;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBScrollPane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.List;
import java.util.stream.Collectors;

import static com.dbn.common.ui.Layouts.verticalBoxLayout;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.intellij.util.ui.UIUtil.getContextHelpForeground;

@Slf4j
public class McpToolDefinitionListForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel toolListPanel;
    private JButton addToolButton;
    private JLabel noToolsLabel;
    private JBScrollPane toolListScrollPane;

    private final List<McpToolDefinitionListItemForm> toolDefinitionForms = DisposableContainers.list(this);
    private final @Getter ConnectionHandler connection;
    private final @Getter McpServerDefinition serverDefinition;

    public McpToolDefinitionListForm(@Nullable Disposable parent, @NotNull ConnectionHandler connection, @NotNull McpServerDefinition serverDefinition) {
        super(parent);
        this.connection = connection;
        this.serverDefinition = serverDefinition;

        verticalBoxLayout(toolListPanel);

        initEmptyLabel();
        initAddButton();

        for (McpToolDefinition toolDefinition : serverDefinition.getTools()) {
            createToolDefinitionForm(toolDefinition);
        }
    }

    @Override
    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.initFieldsVisibility(() -> noToolsDefined(), array(noToolsLabel));
        fieldAdapter.initFieldsVisibility(() -> !noToolsDefined(), array(toolListScrollPane));
    }

    private boolean noToolsDefined() {
        return toolDefinitionForms.isEmpty();
    }

    private void initEmptyLabel() {
        noToolsLabel.setForeground(getContextHelpForeground());
    }

    private void initAddButton() {
        Buttons.onButtonClick(addToolButton, e -> openToolDefinitionEditor());
    }

    private void openToolDefinitionEditor() {
        Dialogs.show(() -> new McpToolDefinitionDialog(getProject(), connection, serverDefinition, null),
                (dialog, exitCode) -> {
                    if (exitCode != DialogWrapper.OK_EXIT_CODE) return;
                    McpToolDefinitionForm form = dialog.getForm();
                    form.applyFormChanges();
                    McpToolDefinition toolDefinition = form.getToolDefinition();
                    createToolDefinitionForm(toolDefinition);

                    serverDefinition.addToolDefinition(toolDefinition);

                    validateInput();
                });
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    public void removeToolDefinitionForm(McpToolDefinitionListItemForm toolDefinitionForm) {
        toolDefinitionForms.remove(toolDefinitionForm);
        toolListPanel.remove(toolDefinitionForm.getComponent());
        serverDefinition.deleteToolDefinition(toolDefinitionForm.getToolDefinition());

        noToolsLabel.setVisible(toolDefinitionForms.isEmpty());
        toolListScrollPane.setVisible(!toolDefinitionForms.isEmpty());
        updateFieldAvailability();
        revalidateForm();
        validateInput();
    }

    public void createToolDefinitionForm(McpToolDefinition toolDefinition) {
        McpToolDefinitionListItemForm toolDefinitionForm = new McpToolDefinitionListItemForm(this, toolDefinition);
        toolDefinitionForms.add(toolDefinitionForm);
        toolListPanel.add(toolDefinitionForm.getComponent());
        updateFieldAvailability();
        revalidateForm();
    }

    public List<McpToolDefinition> getToolDefinitionModelList() {
        return toolDefinitionForms.stream()
                .map(McpToolDefinitionListItemForm::getToolDefinition)
                .collect(Collectors.toList());
    }
}
