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

import com.dbn.assistant.chat.ChatAvailability;
import com.dbn.assistant.chat.window.action.AssistantActionSupport;
import com.dbn.assistant.state.AssistantState;
import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.ToggleAction;
import com.dbn.common.compatibility.Compatibility;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

import static com.dbn.assistant.chat.ChatAvailability.AVAILABLE;
//import static com.intellij.openapi.actionSystem.ex.ActionUtil.SHOW_TEXT_IN_TOOLBAR;

/**
 * Action for selecting the type of interaction with the AI-assistant engine
 *
 * @author Dan Cioca (Oracle)
 */
@BackgroundUpdate
public class VectorSearchAction extends ToggleAction implements AssistantActionSupport {
    VectorSearchAction() {
        super("RAG Mode", "Retrieval-Augmented Generation", null);
        getTemplatePresentation().setIcon(null);

        // TODO only supported in 2024.x or higher
        //getTemplatePresentation().putClientProperty(SHOW_TEXT_IN_TOOLBAR, true);
    }

    @Override
    @Compatibility
    public boolean displayTextInToolbar() {
        return true;
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
        AssistantState assistantState = getAssistantState(e);
        if (assistantState == null) return false;

        return assistantState.isVectorSearch();
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean selected) {
        AssistantState assistantState = getAssistantState(e);
        if (assistantState == null) return;

        assistantState.setVectorSearch(selected);
    }

    private boolean isEnabled(@NotNull AnActionEvent e) {
        ChatAvailability availability = getChatAvailability(e);
        return availability == AVAILABLE;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);

        boolean enabled = isEnabled(e);
        Presentation presentation = e.getPresentation();
        presentation.setEnabled(enabled);
        presentation.setText("RAG Mode");
    }
}
