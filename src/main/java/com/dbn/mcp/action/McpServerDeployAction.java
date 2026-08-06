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

package com.dbn.mcp.action;

import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.mcp.deploy.McpGraalDeployDialog;
import com.dbn.mcp.registry.McpServerRecord;
import com.dbn.mcp.registry.McpServerStatus;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.icon.Icons.ACTION_UPLOAD;
import static com.dbn.nls.NlsResources.txt;

/**
 * Deploys a generated server to Graal. Only the Micronaut implementations qualify: they compile to
 * the native image the service runs, and they carry the deployment assets in their source project.
 */
public class McpServerDeployAction extends McpServerAction {
    public McpServerDeployAction() {
        super(txt("msg.mcp.action.DeployServer"), ACTION_UPLOAD);
    }

    @Override
    protected void actionPerformed(
            @NotNull AnActionEvent e,
            @NotNull Project project,
            @NotNull McpServerRecord record) {
        ConnectionHandler connection = ConnectionHandler.get(record.getConnectionId());
        if (connection == null) return;

        Dialogs.show(() -> new McpGraalDeployDialog(project, ConnectionRef.of(connection), record));
    }

    @Override
    protected void update(
            @NotNull AnActionEvent e,
            @NotNull Presentation presentation,
            @NotNull Project project,
            @Nullable McpServerRecord record) {
        if (record == null) return;

        String problem = deploymentProblem(record);
        presentation.setEnabled(problem == null);
        presentation.setDescription(problem);
    }

    /** Returns why this server cannot be deployed, or null when it can. */
    private static @Nullable String deploymentProblem(@NotNull McpServerRecord record) {
        if (!record.getImplementation().isNative()) {
            return txt("msg.mcp.error.GraalDeploymentRequiresMicronaut");
        }
        if (record.getStatus() == McpServerStatus.BUILDING) return txt("msg.mcp.text.StatusBuilding");
        if (record.getStatus() == McpServerStatus.FAILED) return txt("msg.mcp.text.StatusFailed");
        if (record.getStatus() == McpServerStatus.STALE) return txt("msg.mcp.text.StaleServerHint");
        if (ConnectionHandler.get(record.getConnectionId()) == null) {
            return txt("msg.mcp.text.ConnectionUnavailable");
        }
        return null;
    }
}
