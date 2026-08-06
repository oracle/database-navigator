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

import com.dbn.connection.ConnectionRef;
import com.dbn.mcp.deploy.McpDeploymentStep;
import com.dbn.mcp.deploy.McpGraalDeployTask;
import com.dbn.mcp.deploy.McpGraalDeploymentInput;
import com.dbn.mcp.registry.McpServerRecord;
import com.intellij.openapi.project.Project;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

/**
 * Launches deployment outside the deployment dialog, so its output is visible in the dashboard
 * while it runs rather than hidden behind a modal.
 */
@UtilityClass
final class McpServerDeploymentRunner {

    static void deployFrom(
            @NotNull Project project,
            @NotNull ConnectionRef connection,
            @NotNull McpServerRecord record,
            @NotNull McpDeploymentStep from,
            @NotNull McpGraalDeploymentInput input) {

        McpGraalDeployTask task = new McpGraalDeployTask(
                project,
                connection,
                record.getDefinition(),
                record.toBuilderResult(),
                () -> null,                     // the dashboard only offers deployable servers
                ocid -> {},                     // the resolved OCID is stored on the record
                record.getOutputDirectory(),
                () -> {});                      // no dialog actions to re-enable

        task.deployFrom(from, input);
    }
}
