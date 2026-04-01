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

package com.dbn.assistant.mcp.ui;

import com.dbn.assistant.mcp.AssistantMcpServer;
import com.dbn.assistant.mcp.AssistantMcpServerType;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.misc.DBNComboBox;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JLabel;
import javax.swing.JPanel;

import static com.dbn.assistant.mcp.AssistantMcpServerType.HTTP;
import static com.dbn.assistant.mcp.AssistantMcpServerType.STDIO;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.setText;
import static com.dbn.common.util.Strings.isNotEmpty;

public class AssistantMcpServerEditForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JBTextField nameTextField;
    private JBTextField commandTextField;
    private JBTextField urlTextField;
    private DBNComboBox<AssistantMcpServerType> typeComboBox;
    private JLabel commandLabel;
    private JLabel urlLabel;


    private final AssistantMcpServer mcpServer;

    AssistantMcpServerEditForm(AssistantMcpServerEditDialog parent) {
        super(parent);
        this.mcpServer = parent.getMcpServer();

        initComboBox(typeComboBox, AssistantMcpServerType.values());
        resetFormChanges();
        urlTextField.getEmptyText().setText("e.g. http://localhost:3001/mcp-server");
        commandTextField.getEmptyText().setText("e.g. java mcp-server.jar");

        updateFieldAvailability();

        // listeners
        onSelectionChange(typeComboBox, p -> updateFieldAvailability());
    }

    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.initFieldsVisibility(() -> getSelectedServerType() == HTTP, array(urlLabel, urlTextField));
        fieldAdapter.initFieldsVisibility(() -> getSelectedServerType() == STDIO, array(commandLabel, commandTextField));
    }

    private AssistantMcpServerEditRequest getRequest() {
        AssistantMcpServerEditDialog dialog = ensureParentComponent();
        return dialog.getRequest();
    }

    @Override
    protected void initValidation() {
        addTextValidation(nameTextField, n -> isNotEmpty(n), "Please provide a server name");
        addTextValidation(nameTextField, n -> isNotUsed(n), "The server name is already in use");
        addTextValidation(urlTextField, s -> isNotEmpty(s), "Please provide the server URL");
        addTextValidation(commandTextField, s -> isNotEmpty(s), "Please provide the server command");
    }

    @Nullable
    private AssistantMcpServerType getSelectedServerType() {
        return getSelection(typeComboBox);
    }

    public void applyFormChanges() {
        mcpServer.setName(getText(nameTextField));
        mcpServer.setCommand(getText(commandTextField));
        mcpServer.setUrl(getText(urlTextField));
        mcpServer.setType(getSelectedServerType());
    }

    public void resetFormChanges() {
        setText(nameTextField, mcpServer.getName());
        setText(commandTextField, mcpServer.getCommand());
        setText(urlTextField, mcpServer.getUrl());
        setSelection(typeComboBox, mcpServer.getType());
    }

    private boolean isNotUsed(String name) {
        return !getRequest().getUsedNames().contains(name);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

}
