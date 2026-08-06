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

package com.dbn.mcp.deploy;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionRef;
import com.dbn.mcp.registry.McpServerRecord;
import com.dbn.mcp.registry.McpServerRegistry;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;

import static com.dbn.common.util.Strings.isEmptyOrSpaces;
import static com.dbn.nls.NlsResources.txt;

/**
 * Collects where a generated server should be published: the registry coordinates that identify
 * the image. Nothing is deployed from here - the deployment steps run from the dashboard, where
 * their output stays visible while they work.
 */
public class McpGraalDeployDialog extends DBNDialog<McpGraalDeployForm> {
    private final McpServerRecord record;

    public McpGraalDeployDialog(
            @Nullable Project project,
            @NotNull ConnectionRef connection,
            @NotNull McpServerRecord record) {
        super(project, txt("msg.mcp.title.DeploymentTarget"), true);
        this.record = record;
        setDefaultSize(560, 300);
        init();
    }

    @NotNull
    @Override
    protected McpGraalDeployForm createForm() {
        return new McpGraalDeployForm(this, record);
    }

    @Override
    protected final Action[] initializeActions() {
        return actions(getOKAction(), getCancelAction());
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        McpGraalDeploymentInput input = getForm().getDeploymentInput();

        if (!McpGraalDeploymentInput.isValidRegionKey(input.getRegionKey())) {
            return new ValidationInfo(txt("msg.mcp.error.OcirRegionInvalid"));
        }
        if (isEmptyOrSpaces(input.getNamespace())) {
            return new ValidationInfo(txt("msg.mcp.error.OcirNamespaceRequired"));
        }
        if (isEmptyOrSpaces(input.getRepository())) {
            return new ValidationInfo(txt("msg.mcp.error.OcirRepositoryRequired"));
        }
        if (isEmptyOrSpaces(input.getTag())) {
            return new ValidationInfo(txt("msg.mcp.error.ImageTagRequired"));
        }
        return super.doValidate();
    }

    @Override
    protected void doOKAction() {
        McpGraalDeploymentInput input = getForm().getDeploymentInput();
        McpServerRegistry.getInstance(ensureProject()).saveDeploymentTarget(
                record.getOutputDirectory(),
                input.getRegionKey(), input.getNamespace(), input.getRepository(), input.getTag());
        super.doOKAction();
    }
}
