/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.assistant.tool.config.action;

import com.dbn.assistant.tool.approval.AssistantToolApprovalStatus;
import com.dbn.assistant.tool.config.ui.AssistantToolApprovalItemForm;
import com.dbn.common.action.DataKeys;
import com.dbn.common.action.ProjectAction;
import com.dbn.common.icon.Icons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.APPROVED;
import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.DISABLED;
import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.PROMPTED;
import static com.intellij.icons.AllIcons.General.GreenCheckmark;
import static com.intellij.openapi.actionSystem.ex.ActionUtil.SHOW_TEXT_IN_TOOLBAR;

public class AssistantToolStatusAction extends ProjectAction {

    private boolean isEnabled(@NotNull AnActionEvent e) {
        AssistantToolApprovalStatus parentApprovalStatus = getParentApprovalStatus(e);
        return parentApprovalStatus != DISABLED;
    }

    @Override
    public void update(@NotNull AnActionEvent e, @NotNull Project project) {
        boolean enabled = isEnabled(e);

        var status = getApprovalStatus(e);
        if (status == null) return;

        Presentation presentation = e.getPresentation();
        presentation.setEnabled(enabled);
        presentation.setText(status.getName());

        Icon icon =
                status == PROMPTED ? Icons.ACTION_TOOL_PROMPTED :
                status == APPROVED ? GreenCheckmark /*Icons.ACTION_TOOL_APPROVED*/ : null;

        presentation.setIcon(icon);
        presentation.putClientProperty(SHOW_TEXT_IN_TOOLBAR, true);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        var status = getApprovalStatus(e);
        if (status == null) return;

        AssistantToolApprovalItemForm approvalItemForm = getApprovalItemForm(e);
        if (approvalItemForm == null) return;

        approvalItemForm.setApprovalStatus(status.next());
    }

    @Nullable
    private AssistantToolApprovalItemForm getApprovalItemForm(@NotNull AnActionEvent e) {
        return e.getData(DataKeys.ASSISTANT_TOOL_APPROVAL_FORM);
    }

    @Nullable
    private AssistantToolApprovalStatus getApprovalStatus(@NotNull AnActionEvent e) {
        AssistantToolApprovalItemForm approvalItemForm = getApprovalItemForm(e);
        if (approvalItemForm == null) return null;

        return approvalItemForm.getApprovalStatus();
    }

    @Nullable
    private AssistantToolApprovalStatus getParentApprovalStatus(@NotNull AnActionEvent e) {
        AssistantToolApprovalItemForm approvalItemForm = getApprovalItemForm(e);
        if (approvalItemForm == null) return null;

        return approvalItemForm.getParentApprovalStatus();
    }
}
