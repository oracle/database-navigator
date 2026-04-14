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

package com.dbn.assistant.tool.action;

import com.dbn.assistant.chat.message.ui.ChatMessageToolSectionForm;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.component.DBNFoldableComponent;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class ToolExecutionDataAction extends AssistantToolAction {
    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        ChatMessageToolSectionForm foldableComponent = getToolSectionForm(e);
        if (foldableComponent == null) return;

        foldableComponent.toggleFolding();
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        DBNFoldableComponent foldableComponent = getFoldableComponent(e);
        if (foldableComponent == null) return;


        Presentation presentation = e.getPresentation();
        presentation.setText("Tool Details");
        presentation.setIcon(foldableComponent.isFolded() ?
                Icons.ACTION_CONTENT_EXPAND :
                Icons.ACTION_CONTENT_COLLAPSE);
        presentation.setVisible(isVisible(e));
    }

    private boolean isVisible(@NotNull AnActionEvent e) {
        return !isInteractive(e);
    }

}
