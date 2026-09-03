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
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.help.HelpTopic;
import com.dbn.mcp.McpServerBuilderManager;
import com.dbn.mcp.build.McpBuildTask;
import com.dbn.mcp.model.McpServerDefinition;
import com.intellij.openapi.ui.OptionAction;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;

import static com.dbn.nls.NlsResources.txt;

public class McpServerDefinitionDialog extends DBNDialog<McpServerDefinitionForm> {

    private final ConnectionHandler connection;
    private McpServerDefinition definition;

    private Action saveAsAction;

    public McpServerDefinitionDialog(@NotNull ConnectionHandler connection, McpServerDefinition definition) {
        super(connection, txt("msg.mcp.title.McpServerBuilder"), true);
        this.connection = connection;
        this.definition = definition;
        setDefaultSize(600, 600);

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
            return new ValidationInfo(txt("msg.mcp.error.ToolDefinitionRequired"));
        }
        return super.doValidate();
    }

    protected final Action[] initializeActions() {
        renameAction(getOKAction(), txt("msg.mcp.button.BuildServer"));
        renameAction(getCancelAction(), txt("msg.shared.button.Close"));

        Action saveAction = createAction(txt("msg.shared.button.Save"), () -> getForm().saveConfiguration());
        saveAsAction = createAction(txt("msg.shared.button.SaveAs"), () -> getForm().saveConfigurationAs());
        OptionAction saveOptionAction = createCompositeAction(saveAction, saveAsAction);

        return actions(
                getOKAction(),
                saveOptionAction,
                getCancelAction());
    }

    protected void setSaveAsVisible(boolean visible) {
        makeActionVisible(saveAsAction, visible);
    }

    @Override
    public void doCancelAction() {
        snapshotServerDefinition();
        super.doCancelAction();
    }

    @Override
    protected HelpTopic getHelpTopic() {
        return HelpTopic.MCP_SERVER_BUILDER;
    }

    @Override
    protected void doOKAction() {
        snapshotServerDefinition();

        McpBuildTask buildTask = new McpBuildTask(getProject(), connection, definition);
        buildTask.execute(
                () -> closeDialog(),
                () -> reopenDialog());
    }

    private void reopenDialog() {
        dispatch(() -> Dialogs.show(()-> new McpServerDefinitionDialog(connection, definition)));
    }

    private void closeDialog() {
        dispatch(() -> super.doOKAction());
    }

    private void snapshotServerDefinition() {
        McpServerDefinitionForm form = getForm();
        form.applyFormChanges();
        definition = form.getServerDefinition();

        McpServerBuilderManager builderManager = McpServerBuilderManager.getInstance(getProject());
        builderManager.setServerDefinition(connection.getConnectionId(), definition);
    }
}
