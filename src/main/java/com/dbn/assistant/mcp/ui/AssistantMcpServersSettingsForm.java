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

package com.dbn.assistant.mcp.ui;


import com.dbn.assistant.DatabaseAssistantManager;
import com.dbn.assistant.mcp.AssistantMcpServerSettings;
import com.dbn.assistant.mcp.ide.IdeMcpServerManager;
import com.dbn.assistant.mcp.model.AssistantMcpServer;
import com.dbn.assistant.mcp.model.AssistantMcpServerBundle;
import com.dbn.common.acknowledgement.UserAcknowledgementManager;
import com.dbn.common.icon.Icons;
import com.dbn.common.options.SettingsChangeNotifier;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.ui.util.Mouse;
import com.dbn.common.util.Dialogs;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ToolbarDecorator;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.dbn.common.ui.util.Decorators.createToolbarDecorator;
import static com.dbn.common.ui.util.Decorators.createToolbarDecoratorComponent;

public class AssistantMcpServersSettingsForm extends ConfigurationEditorForm<AssistantMcpServerSettings> {
    private JPanel mainPanel;
    private JPanel mcpServersTablePanel;
    private JPanel ideMcpServerPanel;

    private final AssistantMcpServersTable mcpServersTable;
    private final Set<String> initialAckKeys = new HashSet<>();
    private final Set<String> userTrustedKeys = new HashSet<>();
    private AssistantIdeMcpServerForm ideMcpServerForm;

    public AssistantMcpServersSettingsForm(AssistantMcpServerSettings settings) {
        super(settings);

        mcpServersTable = new AssistantMcpServersTable(this, settings.getMcpServers());
        mcpServersTablePanel.add(initTableComponent());
        for (AssistantMcpServer mcpServer : settings.getMcpServers().getElements()) {
            initialAckKeys.add(mcpServer.getAcknowledgementKey());
        }

        if (IdeMcpServerManager.isIdeMcpPluginSupported()) {
            ideMcpServerForm = new AssistantIdeMcpServerForm(this);
            ideMcpServerPanel.add(ideMcpServerForm.getComponent());
            ideMcpServerForm.setServerEnabled(getConfiguration().isWorkspaceIntegration());
        }

        registerComponents(mainPanel);
    }


    private JPanel initTableComponent() {
        ToolbarDecorator decorator = createToolbarDecorator(mcpServersTable);
        decorator.setAddAction(b -> openMcpServerEditor(true));
        decorator.setRemoveAction(b -> mcpServersTable.removeRow());
        decorator.setMoveUpAction(b -> mcpServersTable.moveRowUp());
        decorator.setMoveDownAction(b -> mcpServersTable.moveRowDown());
        decorator.setEditAction(b -> openMcpServerEditor(false));
        decorator.addExtraAction(Separator.getInstance());
        decorator.addExtraAction(new AnAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                Dialogs.show(() -> new AssistantMcpToolApprovalDialog(getProject(), getSelectedMcpServer()));
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                Presentation presentation = e.getPresentation();
                presentation.setIcon(Icons.ACTION_CHECK_LIST);
                presentation.setText("Tool Approvals");
                presentation.setEnabled(mcpServersTable.getSelectedRows().length == 1);
            }
        });

        Mouse.onMouseDoubleClick(mcpServersTable, e -> openMcpServerEditor(false));
        return createToolbarDecoratorComponent(decorator, mcpServersTable);
    }

    public void openMcpServerEditor(boolean create) {
        AssistantMcpServer selectedMcpServer = getSelectedMcpServer();
        if (selectedMcpServer == null && !create) return;

        AssistantMcpServer mcpServer = create ? null : selectedMcpServer;
        AssistantMcpServerEditRequest request = AssistantMcpServerEditRequest
                .builder()
                .mcpServer(mcpServer)
                .mcpServers(getConfiguration().getMcpServers())
                .saveConsumer(c -> saveMcpServer(c, create))
                .build();


        Dialogs.show(() -> new AssistantMcpServerEditDialog(getProject(), request));
    }

    private void saveMcpServer(AssistantMcpServer mcpServer, boolean create) {
        if (create) {
            AssistantMcpServersTableModel model = mcpServersTable.getModel();
            model.addElement(mcpServer);
        }
        userTrustedKeys.add(mcpServer.getAcknowledgementKey());
        mackConfigModified();
        mcpServersTable.revalidate();
        mcpServersTable.repaint();
    }

    private AssistantMcpServer getSelectedMcpServer() {
        int[] selectedIndices = mcpServersTable.getSelectionModel().getSelectedIndices();
        if (selectedIndices.length != 1) return null;

        return mcpServersTable.getModel().getElements().get(selectedIndices[0]);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        AssistantMcpServerSettings configuration = getConfiguration();
        configuration.setWorkspaceIntegration(ideMcpServerForm != null && ideMcpServerForm.isServerEnabled());

        AssistantMcpServersTableModel model = mcpServersTable.getModel();
        model.validate();
        model.applyChanges();

        List<AssistantMcpServer> mcpServers = model.getElements();
        configuration.setMcpServers(new AssistantMcpServerBundle(getProject(), mcpServers));
        UserAcknowledgementManager.getInstance().updateAcknowledgements(initialAckKeys, userTrustedKeys, mcpServers);
        // refresh the baseline so the next Apply diffs against the just-applied state
        initialAckKeys.clear();
        for (AssistantMcpServer mcpServer : mcpServers) {
            initialAckKeys.add(mcpServer.getAcknowledgementKey());
        }
        userTrustedKeys.clear();

        if (configuration.isModified()) {
            refreshAssistantStates();
        }
    }

    private void refreshAssistantStates() {
        // notify after setting changes are applied
        SettingsChangeNotifier.register(() -> {
            Project project = ensureProject();
            DatabaseAssistantManager assistantManager = DatabaseAssistantManager.getInstance(project);
            assistantManager.notifyConfigChanges();
        });
    }

    @Override
    public void resetFormChanges() {
        userTrustedKeys.clear();
        mcpServersTable.getModel().resetChanges();
    }
}
