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

package com.dbn.assistant.mcp.action;

import com.dbn.assistant.mcp.ui.AssistantMcpToolApprovalsForm;
import com.dbn.common.action.ProjectAction;
import com.dbn.common.icon.Icons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.action.DataKeys.ASSISTANT_MCP_TOOL_APPROVALS_FORM;
import static com.dbn.nls.NlsResources.txt;

public class AssistantMcpToolsReloadAction extends ProjectAction {
    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        AssistantMcpToolApprovalsForm approvalsForm = getApprovalForm(e);
        if (approvalsForm == null) return;

        approvalsForm.reloadTools();
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        presentation.setIcon(Icons.ACTION_RELOAD);
        presentation.setText(txt("app.assistant.action.ReloadMcpTools"));
        presentation.setEnabled(isEnabled(e));
    }

    private static boolean isEnabled(@NotNull AnActionEvent e) {
        AssistantMcpToolApprovalsForm approvalsForm = getApprovalForm(e);
        return approvalsForm != null && !approvalsForm.isLoading();
    }

    private static @Nullable AssistantMcpToolApprovalsForm getApprovalForm(@NotNull AnActionEvent e) {
        return ASSISTANT_MCP_TOOL_APPROVALS_FORM.getData(e.getDataContext());
    }
}
