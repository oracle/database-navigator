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

import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.mcp.deploy.McpDeploymentStep;
import com.dbn.mcp.deploy.McpGraalDeployDialog;
import com.dbn.mcp.deploy.McpGraalDeploymentInput;
import com.dbn.mcp.registry.McpDeploymentInfo;
import com.dbn.mcp.registry.McpServerRecord;
import com.intellij.openapi.project.Project;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.nls.NlsResources.txt;

/** Runs a single deployment step for a dashboard record, or collects what it still needs. */
@UtilityClass
final class McpServerDeploymentActions {

    static void run(
            @NotNull Project project,
            @NotNull McpServerRecord record,
            @NotNull McpDeploymentStep step,
            @Nullable String imageOcid) {
        ConnectionHandler connection = ConnectionHandler.get(record.getConnectionId());
        if (connection == null) {
            Messages.showErrorDialog(project, txt("msg.mcp.title.GraalDeployment"),
                    txt("msg.mcp.text.ConnectionUnavailable"));
            return;
        }

        McpGraalDeploymentInput input = deploymentInput(record, imageOcid);
        if (input == null) {
            // the registry coordinates are not known yet, so ask for them before doing anything
            openRegistrySettings(project, record);
            return;
        }
        McpServerDeploymentRunner.run(project, ConnectionRef.of(connection), record, step, input);
    }

    /** Opens the deployment dialog purely to capture the target registry coordinates. */
    static void openRegistrySettings(@NotNull Project project, @NotNull McpServerRecord record) {
        ConnectionHandler connection = ConnectionHandler.get(record.getConnectionId());
        if (connection == null) return;

        Dialogs.show(() -> new McpGraalDeployDialog(project, ConnectionRef.of(connection), record));
    }

    private static @Nullable McpGraalDeploymentInput deploymentInput(
            McpServerRecord record, @Nullable String imageOcid) {
        McpDeploymentInfo deployment = record.getDeployment();
        if (deployment == null
                || deployment.getNamespace() == null
                || deployment.getRepository() == null) {
            return null;
        }

        return new McpGraalDeploymentInput(
                record.getServerName(),
                deployment.getRegionKey(),
                deployment.getNamespace(),
                deployment.getRepository(),
                deployment.getTag(),
                imageOcid == null ? "" : imageOcid);
    }
}
