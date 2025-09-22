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

package com.dbn.assistant.service.selectai;

import com.dbn.assistant.AssistantContextUtil;
import com.dbn.assistant.AssistantType;
import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.provider.AIModel;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.assistant.service.selectai.editor.action.SelectAiProfileSelectAction;
import com.dbn.assistant.service.selectai.ui.ProfilesAndCredentialsDialog;
import com.dbn.assistant.state.AssistantState;
import com.dbn.common.action.Selectable;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.object.DBAIProfile;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObjectBundle;
import com.intellij.openapi.editor.Editor;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import static com.dbn.common.ui.util.Popups.popupBuilder;
import static com.dbn.common.util.Lists.convert;
import static com.dbn.common.util.Lists.first;
import static com.dbn.common.util.Lists.firstElement;
import static com.dbn.common.util.Strings.isEmpty;
import static java.util.Collections.emptyList;

@UtilityClass
public class SelectAiContextUtil {

    @Nullable
    public static DBAIProfile getDefaultProfile(ConnectionId connectionId) {
        List<DBAIProfile> profiles = getProfiles(connectionId);
        if (profiles.isEmpty()) return null;

        AssistantState assistantState = getAssistantState(connectionId);
        if (assistantState == null) return null;

        String profileName = assistantState.getDefaultProfileName();

        DBAIProfile profile = getProfile(connectionId, profileName);
        if (profile == null) profile = firstElement(profiles);
        if (profile == null) return null;

        setDefaultProfile(connectionId, profile);
        return profile;
    }

    @Nullable
    public static DBAIProfile getSelectedProfile(ConnectionId connectionId) {
        ChatContext chatContext = getChatContext(connectionId);
        if (chatContext == null) return null;

        List<DBAIProfile> profiles = getProfiles(connectionId);
        if (profiles.isEmpty()) return null;

        String profileName = chatContext.getProfileId();
        if (isEmpty(profileName)) return null;

        return getProfile(connectionId, profileName);
    }

    @Nullable
    public static AIModel getSelectedModel(ConnectionId connectionId) {
        ChatContext chatContext = getChatContext(connectionId);
        if (chatContext == null) return null;

        DBAIProfile profile = getSelectedProfile(connectionId);
        if (profile == null) return null;

        AIProvider provider = profile.getProvider();
        if (provider == null) return null;

        String modelName = chatContext.getModelId();
        if (isEmpty(modelName)) return null;

        AIModel model = provider.getModel(modelName);
        return model == null ? provider.getDefaultModel() : model;
    }

    @Nullable
    public static PromptAction getSelectedAction(ConnectionId connectionId) {
        ChatContext chatContext = getChatContext(connectionId);
        if (chatContext == null) return null;

        String actionName = chatContext.getActionId();
        if (isEmpty(actionName)) return null;

        return PromptAction.get(actionName);
    }

    public static List<DBAIProfile> getProfiles(ConnectionId connectionId) {
        ConnectionHandler connection = getConnection(connectionId);
        if (connection == null) return emptyList();

        DBObjectBundle objectBundle = connection.getObjectBundle();
        DBSchema userSchema = objectBundle.getUserSchema();
        if (userSchema == null) return emptyList();

        return userSchema.getAIProfiles();
    }

    @Nullable
    private static ConnectionHandler getConnection(ConnectionId connectionId) {
        return ConnectionHandler.get(connectionId);
    }

    @Nullable
    public static AssistantState getAssistantState(ConnectionId connectionId) {
        return AssistantContextUtil.getAssistantState(connectionId, AssistantType.SELECT_AI);
    }

    @Nullable
    public static ChatContext getChatContext(ConnectionId connectionId) {
        return AssistantContextUtil.getChatContext(connectionId, AssistantType.SELECT_AI);
    }


    @Nullable
    public DBAIProfile getProfile(ConnectionId connectionId, String profileName) {
        List<DBAIProfile> profiles = getProfiles(connectionId);
        return first(profiles, p -> p.getName().equalsIgnoreCase(profileName));
    }

    public static void setDefaultProfile(ConnectionId connectionId, @Nullable DBAIProfile profile) {
        if (profile == null) return;

        AssistantState assistantState = getAssistantState(connectionId);
        if (assistantState == null) return;

        assistantState.setDefaultProfileName(profile.getName());
    }

    public static boolean isDefaultProfile(ConnectionId connectionId, DBAIProfile profile) {
        DBAIProfile defaultProfile = getDefaultProfile(connectionId);
        return Objects.equals(defaultProfile, profile);
    }


    public static void openProfileConfiguration(ConnectionId connectionId) {
        ConnectionHandler connection = ConnectionHandler.ensure(connectionId);
        Dialogs.show(() -> new ProfilesAndCredentialsDialog(connection));
    }

    public static void promptProfileSelector(Editor editor, ConnectionId connectionId) {
        List<DBAIProfile> profiles = getProfiles(connectionId);
        DBAIProfile defaultProfile = getDefaultProfile(connectionId);

        List<SelectAiProfileSelectAction> actions = convert(profiles, p -> new SelectAiProfileSelectAction(connectionId, p, defaultProfile));

        popupBuilder(actions, editor).
                withTitle("Select AI Profile").
                withPreselectCondition(Selectable.selector()).
                withSpeedSearch().
                buildAndShow();
    }
}
