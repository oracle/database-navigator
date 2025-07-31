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

import com.dbn.assistant.adapter.ui.AssistantContextActionsForm;
import com.dbn.assistant.adapter.ui.AssistantDetailFormBase;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.assistant.chat.window.ui.ChatBoxInputField;
import com.dbn.assistant.service.selectai.SelectAiChatContext;
import com.dbn.assistant.service.selectai.SelectAiContextUtil;
import com.dbn.assistant.state.AssistantState;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.thread.Background;
import com.dbn.common.util.Actions;
import com.dbn.connection.ConnectionId;
import com.dbn.object.DBAIProfile;
import com.dbn.object.event.ObjectChangeListener;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.AsyncProcessIcon;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.List;
import java.util.Objects;

import static com.dbn.assistant.state.AssistantStatus.INITIALIZING;
import static com.dbn.assistant.state.AssistantStatus.UNAVAILABLE;
import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.common.util.Lists.firstElement;
import static com.dbn.object.common.DBObjectUtil.refreshUserObjects;
import static com.dbn.object.type.DBObjectType.AI_PROFILE;
import static com.dbn.object.type.DBObjectType.CREDENTIAL;

@Slf4j
public class SelectAiContextActionsForm extends AssistantDetailFormBase implements AssistantContextActionsForm {
    private JPanel mainPanel;
    private JPanel initializingPanel;
    private JPanel initializingIconPanel;
    private JPanel actionsPanel;

    public SelectAiContextActionsForm(ChatBoxForm parent) {
        super(parent);

        this.initializingIconPanel.add(new AsyncProcessIcon("Loading"));
        this.initializingPanel.setVisible(false);

        createActionPanel();

        Project project = ensureProject();
        ProjectEvents.subscribe(project, this, ObjectChangeListener.TOPIC, createObjectChangeListener());

        loadProfiles();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    private ObjectChangeListener createObjectChangeListener() {
        return (connectionId, ownerId, objectType, action) -> {
            if (!objectType.isOneOf(AI_PROFILE, CREDENTIAL)) return;
            if (!Objects.equals(connectionId, getConnectionId())) return;

            Background.run(() -> loadProfiles(false));
        };
    }

    private void createActionPanel() {
        ActionToolbar contextActions = Actions.createActionToolbar(actionsPanel, true, "DBNavigator.ActionGroup.SelectAiContextActions");
        setAccessibleName(contextActions, txt("app.assistant.aria.ChatProfileActions"));
        this.actionsPanel.add(contextActions.getComponent());
    }

    public void reloadProfiles() {
        Background.run(() -> loadProfiles(true));
    }

    public void loadProfiles() {
        Background.run(() -> loadProfiles(false));
    }

    /**
     * Initializes the profile dropdowns for the chat box
     */
    private void loadProfiles(boolean force) {
        AssistantState assistantState = getAssistantState();
        if (assistantState.is(INITIALIZING)) return;

        try {
            ConnectionId connectionId = getConnectionId();
            if (force) refreshUserObjects(connectionId, AI_PROFILE);
            beforeProfileLoad();

            // make sure profiles are loaded
            SelectAiContextUtil.getProfiles(connectionId);
            afterProfileLoad(null);
        } catch (Throwable e) {
            log.warn("Failed to fetch profiles", e);
            afterProfileLoad(e);
        }
    }

    private void beforeProfileLoad() {
        initializingPanel.setVisible(true);
        AssistantState state = getAssistantState();

        state.set(INITIALIZING, true);
        state.set(UNAVAILABLE, false);
    }

    private void afterProfileLoad(@Nullable Throwable e) {
        initializingPanel.setVisible(false);
        AssistantState state = getAssistantState();
        state.set(INITIALIZING, false);
        ChatBoxForm chatBoxForm = getChatBoxForm();
        if (e != null) {
            state.set(UNAVAILABLE, true);
            chatBoxForm.showErrorHeader(e);
        } else {
            initCurrentChat();

            ChatBoxInputField inputField = chatBoxForm.getInputField();
            inputField.requestFocus();
        }

        updateActionToolbars();
    }

    private void initCurrentChat() {
        AssistantState assistantState = getAssistantState();
        if (!assistantState.isCurrentContextValid()) {
            List<DBAIProfile> profiles = SelectAiContextUtil.getProfiles(getConnectionId());
            DBAIProfile firstProfile = firstElement(profiles);

            SelectAiChatContext context = new SelectAiChatContext(firstProfile);
            assistantState.setCurrentContext(context);
        }
    }

}
