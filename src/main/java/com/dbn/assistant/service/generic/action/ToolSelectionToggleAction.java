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

package com.dbn.assistant.service.generic.action;

import com.dbn.assistant.chat.window.action.AssistantActionSupport;
import com.dbn.assistant.tool.AssistantToolType;
import com.dbn.assistant.tool.approval.AssistantToolApprovals;
import com.dbn.common.action.ToggleAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

import static com.dbn.assistant.tool.AssistantToolData.getToolDescription;
import static com.dbn.assistant.tool.AssistantToolData.getToolName;
import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.BLOCKED;
import static com.dbn.assistant.tool.approval.AssistantToolApprovalStatus.PROMPTED;
import static com.dbn.common.icon.Icons.ACTION_CHECK;

public class ToolSelectionToggleAction extends ToggleAction implements AssistantActionSupport {
    private final AssistantToolType toolType;

    public ToolSelectionToggleAction(AssistantToolType toolType) {
        this.toolType = toolType;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Presentation presentation = e.getPresentation();

        Icon icon = isSelected(e) ? ACTION_CHECK : null;
        String text = getToolName(toolType);
        String description = getToolDescription(toolType);

        presentation.setIcon(icon);
        presentation.setText(text);
        presentation.setDescription(description);
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        AssistantToolApprovals approvals = getToolApprovals(e);
        if (approvals == null) return false;

        return !approvals.isBlocked(toolType);
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean selected) {
        AssistantToolApprovals toolApprovals = getToolApprovals(e);
        if (toolApprovals == null) return;

        toolApprovals.setStatus(toolType, selected ? PROMPTED : BLOCKED);
    }
}
