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
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.mcp.model.McpServerDefinition;
import com.dbn.mcp.model.McpToolDefinition;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.util.ui.UIUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.util.List;
import java.util.stream.Collectors;

import static com.dbn.common.ui.Layouts.verticalBoxLayout;

public class McpToolDefinitionListForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel listPanel;
    private JPanel actionPanel;
    private JButton addButton;
    private JLabel emptyLabel;

    private final List<McpToolDefinitionListItemForm> toolDefinitionForms = DisposableContainers.list(this);
    private final @Getter ConnectionHandler connection;
    private final @Getter McpServerDefinition serverDefinition;

    public McpToolDefinitionListForm(@Nullable Disposable parent, @NotNull ConnectionHandler connection, @NotNull McpServerDefinition serverDefinition) {
        super(parent);
        this.connection = connection;
        this.serverDefinition = serverDefinition;

        verticalBoxLayout(listPanel);

        initEmptyLabel();
        initAddButton();

        serverDefinition.getTools().forEach(this::createToolDefinitionForm);
        actionPanel.add(addButton);
    }

    private void initEmptyLabel() {
        emptyLabel = new JLabel("No tools defined", SwingConstants.CENTER);
        emptyLabel.setForeground(UIUtil.getLabelForeground());
        listPanel.add(emptyLabel, BorderLayout.CENTER);
    }

    private void initAddButton() {
        addButton = new JButton("Add Tool");
        addButton.addActionListener(e -> {
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
        });
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    public void removeToolDefinitionForm(McpToolDefinitionListItemForm toolDefinitionForm) {
        toolDefinitionForms.remove(toolDefinitionForm);
        listPanel.remove(toolDefinitionForm.getComponent());
        serverDefinition.deleteToolDefinition(toolDefinitionForm.getToolDefinition());

        emptyLabel.setVisible(toolDefinitionForms.isEmpty());
        UserInterface.repaint(mainPanel);
        validateInput();
    }

    public void createToolDefinitionForm(McpToolDefinition mcpToolDefinition) {
        McpToolDefinitionListItemForm toolDefinitionListItemForm = new McpToolDefinitionListItemForm(
                this,
                mcpToolDefinition
        );
        toolDefinitionForms.add(toolDefinitionListItemForm);
        listPanel.add(toolDefinitionListItemForm.getComponent());
        emptyLabel.setVisible(false);

        if (isInitialized()) {
            UserInterface.repaint(mainPanel);
        }
    }

    public List<McpToolDefinition> getToolDefinitionModelList() {
        return toolDefinitionForms.stream()
                .map(McpToolDefinitionListItemForm::getToolDefinition)
                .collect(Collectors.toList());
    }
}
