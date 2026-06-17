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

package com.dbn.assistant.service.generic.ui;

import com.dbn.assistant.adapter.ui.AssistantContextActionsForm;
import com.dbn.assistant.adapter.ui.AssistantDetailFormBase;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.common.util.Actions;
import com.intellij.openapi.actionSystem.ActionToolbar;
import lombok.extern.slf4j.Slf4j;

import javax.swing.JComponent;
import javax.swing.JPanel;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.nls.NlsResources.txt;

@Slf4j
public class GenericAssistantContextActionsForm extends AssistantDetailFormBase implements AssistantContextActionsForm {
    private JPanel mainPanel;
    private JPanel actionsPanel;

    public GenericAssistantContextActionsForm(ChatBoxForm parent) {
        super(parent);

        createActionPanel();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    private void createActionPanel() {
        ActionToolbar contextActions = Actions.createActionToolbar(actionsPanel, true, "DBN.Assistant.Generic.Context");
        setAccessibleName(contextActions, txt("app.assistant.aria.ChatProfileActions"));
        this.actionsPanel.add(contextActions.getComponent());
    }
}
