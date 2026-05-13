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

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import com.dbn.mcp.MCPServerManager;
import com.dbn.mcp.build.McpBuildTask;
import com.dbn.mcp.model.McpServerDefinition;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;

public class McpServerDefinitionDialog extends DBNDialog<McpServerDefinitionForm> {

    private final ConnectionHandler connection;
    private final McpServerDefinition definition;

    public McpServerDefinitionDialog(@NotNull ConnectionHandler connection, McpServerDefinition definition) {
        super(connection, "MCP Server Builder", true);
        this.connection = connection;
        this.definition = definition;
        init();
    }

    @NotNull @Override
    protected McpServerDefinitionForm createForm() {
        return new McpServerDefinitionForm(this, connection, definition);
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        if (!getForm().hasTools()) {
            return new ValidationInfo("Please add at least one tool definition");
        }
        return super.doValidate();
    }

    protected final Action[] initializeActions() {
        renameAction(getOKAction(), "Build");
        return actions(
                getOKAction(),
                getCancelAction());
    }

    @Override
    public void doCancelAction() {
        snapshotServerDefinition();
        super.doCancelAction();
    }

    @Override
    protected void doOKAction() {
        McpServerDefinition serverDefinition = snapshotServerDefinition();
        super.doOKAction();

        new McpBuildTask(getProject(), connection, serverDefinition).execute();
    }

    private McpServerDefinition snapshotServerDefinition() {
        McpServerDefinitionForm form = getForm();
        form.applyFormChanges();
        McpServerDefinition serverDefinition = form.getServerDefinition();

        MCPServerManager serverManager = MCPServerManager.getInstance(getProject());
        serverManager.setServerDefinition(connection.getConnectionId(), serverDefinition);
        return serverDefinition;
    }
}
