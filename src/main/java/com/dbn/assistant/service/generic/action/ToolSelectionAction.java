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
import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.tool.AssistantToolCategory;
import com.dbn.assistant.tool.AssistantToolData;
import com.dbn.assistant.tool.AssistantToolType;
import com.dbn.assistant.tool.approval.AssistantToolApprovals;
import com.dbn.common.action.ComboBoxAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.util.NlsActions.ActionText;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.util.List;

public class ToolSelectionAction extends ComboBoxAction implements AssistantActionSupport {

    ToolSelectionAction(){
        // TODO only supported in 2024.x or higher
        //getTemplatePresentation().putClientProperty(SHOW_TEXT_IN_TOOLBAR, true);
    }


    @Override
    @NotNull
    protected DefaultActionGroup createPopupActionGroup(@NotNull JComponent component, @NotNull DataContext dataContext) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        AssistantState assistantState = getAssistantState(dataContext);
        if (assistantState == null) return actionGroup;

        actionGroup.add(new ToolApprovalsAction());

        List<AssistantToolType> toolTypes = AssistantToolData.getToolTypes(null);
        AssistantToolCategory toolCategory = null;
        for (AssistantToolType toolType : toolTypes) {
            AssistantToolCategory category = AssistantToolData.getToolCategory(toolType);
            if (category != toolCategory) {
                actionGroup.addSeparator(category.getName());
            }
            toolCategory = category;
            actionGroup.add(new ToolSelectionToggleAction(toolType));
        }

        return actionGroup;
    }
/*
    @Override
    @Compatibility
    public boolean displayTextInToolbar() {
        return true;
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {

    }*/

    @Override
    public void update(@NotNull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        presentation.setText(getText(e));
    }

    private @ActionText String getText(@NotNull AnActionEvent e) {
        AssistantToolApprovals approvals = getToolApprovals(e);
        if (approvals == null) return "Tools";

        List<AssistantToolType> toolTypes = AssistantToolData.getToolTypes(null);
        int available = toolTypes.size();
        int blocked = approvals.countBlockedTools(toolTypes);

        return "Tools (" + (available - blocked) + "/" + available + ")";
    }
}
