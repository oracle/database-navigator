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

package com.dbn.assistant.service.selectai.ui;

import com.dbn.assistant.adapter.ui.AssistantDetailFormBase;
import com.dbn.assistant.adapter.ui.AssistantPromptActionsForm;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.assistant.chat.window.ui.ChatBoxStatusLabel;
import com.dbn.common.util.Actions;
import com.intellij.openapi.actionSystem.ActionToolbar;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.nls.NlsResources.txt;

public class SelectAiPromptActionsForm extends AssistantDetailFormBase implements AssistantPromptActionsForm {
    private JPanel mainPanel;
    private JPanel actionsPanel;
    private JPanel statusPanel;

    public SelectAiPromptActionsForm(@Nullable ChatBoxForm parent) {
        super(parent);

        createStatusLabel();
        createActionPanel();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    private void createStatusLabel() {
        ChatBoxStatusLabel statusLabel = new ChatBoxStatusLabel(ensureParentComponent());
        statusPanel.add(statusLabel);
    }

    private void createActionPanel() {
        ActionToolbar typeActions = Actions.createActionToolbar(actionsPanel, true, "DBNavigator.ActionGroup.AssistantChatTypes");
        setAccessibleName(typeActions, txt("app.assistant.aria.ChatTypeActions"));
        this.actionsPanel.add(typeActions.getComponent());
    }
}
