/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.assistant.state;

import com.dbn.assistant.AssistantType;
import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.feature.FeatureAcknowledgement;
import com.dbn.common.feature.FeatureAvailability;
import com.dbn.common.project.ProjectRef;
import com.dbn.connection.ConnectionId;
import com.intellij.openapi.project.Project;

import java.util.Objects;

/**
 * Monitored delegation of {@link AssistantState} allowing to notify changes of the state
 * to listeners of type {@link AssistantStateListener}
 *
 * @author Dan Cioca (Oracle)
 */
public class AssistantStateDelegate extends AssistantState {
    private final ProjectRef project;

    public AssistantStateDelegate(Project project) {
        this(project, null, null);
    }
    public AssistantStateDelegate(Project project, ConnectionId connectionId,  AssistantType assistantType) {
        super(connectionId, assistantType);
        this.project = ProjectRef.of(project);
    }

    public void setAvailability(FeatureAvailability availability) {
        if (getAvailability() == availability) return;

        super.setAvailability(availability);
        notifyStateListeners();
    }

    public void setAssistantType(AssistantType assistantType) {
        if (getAssistantType() == assistantType) return;

        super.setAssistantType(assistantType);
        notifyStateListeners();
    }

    public void setAcknowledgement(FeatureAcknowledgement acknowledgement) {
        if (getAcknowledgement() == acknowledgement) return;

        super.setAcknowledgement(acknowledgement);
        notifyStateListeners();
    }

    @Override
    public void setDefaultProfileName(String profileName) {
        if (Objects.equals(getDefaultProfileName(), profileName)) return;

        super.setDefaultProfileName(profileName);
        notifyStateListeners();
    }

    @Override
    public void setCurrentContext(ChatContext chatContext) {
        super.setCurrentContext(chatContext);
        notifyStateListeners();
    }

    @Override
    public void setCurrentChatId(String currentChatId) {
        if (Objects.equals(getCurrentChatId(), currentChatId)) return;

        super.setCurrentChatId(currentChatId);
        notifyStateListeners();
    }

    @Override
    public void deleteChat(String conversationId) {
        super.deleteChat(conversationId);
        notifyStateListeners();
    }

    @Override
    protected void propertyChanged(AssistantStatus property, boolean value) {
        notifyStateListeners();
    }

    private void notifyStateListeners() {
        Project project = getProject();
        ProjectEvents.notify(project, AssistantStateListener.TOPIC, l -> l.stateChanged(project, getConnectionId()));
    }

    public Project getProject() {
        return ProjectRef.ensure(project);
    }

}
