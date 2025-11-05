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

import com.dbn.assistant.AssistantType;
import com.dbn.assistant.DatabaseAssistantManager;
import com.dbn.assistant.adapter.AssistantAdapterBase;
import com.dbn.assistant.adapter.AssistantResponseConsumer;
import com.dbn.assistant.adapter.ui.AssistantContextActionsForm;
import com.dbn.assistant.adapter.ui.AssistantIntroductionForm;
import com.dbn.assistant.adapter.ui.AssistantPromptActionsForm;
import com.dbn.assistant.chat.Chat;
import com.dbn.assistant.chat.ChatAvailability;
import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.chat.context.ChatContextImpl;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.assistant.provider.AIModel;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.assistant.service.selectai.editor.SelectAiEditorPromptUtil;
import com.dbn.assistant.service.selectai.profile.wizard.ProfileEditionWizard;
import com.dbn.assistant.service.selectai.ui.SelectAiContextActionsForm;
import com.dbn.assistant.service.selectai.ui.SelectAiHelpDialog;
import com.dbn.assistant.service.selectai.ui.SelectAiIntroductionForm;
import com.dbn.assistant.service.selectai.ui.SelectAiPromptActionsForm;
import com.dbn.assistant.state.AssistantState;
import com.dbn.common.load.ProgressMonitor;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Lists;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.PooledConnection;
import com.dbn.connection.SessionId;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.common.assistant.AssistantQueryResponse;
import com.dbn.database.interfaces.DatabaseAssistantInterface;
import com.dbn.object.DBAIProfile;
import com.intellij.openapi.project.Project;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.dbn.assistant.chat.ChatAvailability.AVAILABLE;
import static com.dbn.assistant.chat.ChatAvailability.DISABLED_PROFILE_SELECTED;
import static com.dbn.assistant.chat.ChatAvailability.NOT_INITIALIZED;
import static com.dbn.assistant.chat.ChatAvailability.NO_PROFILE_AVAILABLE;
import static com.dbn.assistant.chat.ChatAvailability.NO_PROFILE_SELECTED;
import static com.dbn.common.feature.FeatureAcknowledgement.ENGAGED;
import static com.dbn.common.util.Conditional.when;
import static com.dbn.common.util.Messages.options;
import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

public class SelectAiAssistantAdapter extends AssistantAdapterBase {
    public static final SelectAiAssistantAdapter INSTANCE = new SelectAiAssistantAdapter();

    private SelectAiAssistantAdapter() {
        super(AssistantType.SELECT_AI);
    }

    public ChatContext createChatContext(ConnectionId connectionId) {
        DBAIProfile profile = SelectAiContextUtil.getSelectedProfile(connectionId);
        if (profile == null) return null;

        AIModel model = SelectAiContextUtil.getSelectedModel(connectionId);
        if (model == null) return null;

        PromptAction action = SelectAiContextUtil.getSelectedAction(connectionId);
        if (action == null) return null;

        return new ChatContextImpl(
                AssistantType.SELECT_AI,
                profile.getName(),
                profile.getProviderId(),
                model.getId(),
                action.getId(),
                profile.isInteractive());
    }

    @Override
    public AssistantIntroductionForm createIntroductionForm(ChatBoxForm chatBoxForm) {
        return new SelectAiIntroductionForm(chatBoxForm);
    }

    @Override
    public AssistantContextActionsForm createContextActionsForm(ChatBoxForm chatBoxForm) {
        return new SelectAiContextActionsForm(chatBoxForm);
    }

    @Override
    public AssistantPromptActionsForm createPromptActionsForm(ChatBoxForm chatBoxForm) {
        return new SelectAiPromptActionsForm(chatBoxForm);
    }

    public void initializeAssistant(ConnectionId connectionId) {
        DBAIProfile defaultProfile = SelectAiContextUtil.getDefaultProfile(connectionId);
        if (defaultProfile != null) return;

        AssistantState assistantState = getAssistantState(connectionId);
        if (assistantState == null) return;

        if (assistantState.getAcknowledgement() == ENGAGED) {
            // assistant not yet configured -> prompt modal initialization
            promptMissingProfiles(connectionId);
        } else {
            // assistant not yet acknowledged -> show acknowledgment popup
            promptAcknowledgement(connectionId);
        }
    }

    public void promptMissingProfiles(ConnectionId connectionId) {
        ConnectionHandler connection = getConnection(connectionId);
        Project project = connection.getProject();
        Progress.modal(project, connection, true,
                txt("prc.assistant.title.InitializingAssistant"),
                txt("prc.assistant.text.InitializingDatabaseAssistant"),
                progress -> {
                    List<DBAIProfile> profiles = SelectAiContextUtil.getProfiles(connectionId);
                    // no profiles created yet -> prompt profile creation
                    if (profiles.isEmpty()) {
                        Messages.showQuestionDialog(project,
                                AssistantType.SELECT_AI.getName(),
                                txt("msg.assistant.question.AcknowledgeAndCreateProfile"),
                                options("Create Profile", "Cancel"), 0,
                                option -> when(option == 0, () -> ProfileEditionWizard.showWizard(connection, null, Collections.emptySet(), null)));
                    }
                });
    }

    private void promptAcknowledgement(ConnectionId connectionId) {
        ConnectionHandler connection = getConnection(connectionId);
        Project project = connection.getProject();
        Messages.showQuestionDialog(project,
                AssistantType.SELECT_AI.getName(),
                txt("msg.assistant.question.AcknowledgeAndConfigure"),
                Messages.OPTIONS_CONTINUE_CANCEL, 0,
                option -> when(option == 0, () -> showToolWindow(connectionId)));
    }

    private void showToolWindow(ConnectionId connectionId) {
        ConnectionHandler connection = getConnection(connectionId);
        Project project = connection.getProject();
        DatabaseAssistantManager assistantManager = DatabaseAssistantManager.getInstance(project);
        assistantManager.showToolWindow(connectionId);
    }

    @Override
    public ChatAvailability getChatAvailability(ConnectionId connectionId) {
        AssistantState assistantState = getAssistantState(connectionId);
        if (assistantState == null) return NOT_INITIALIZED;


        List<DBAIProfile> profiles = SelectAiContextUtil.getProfiles(connectionId);
        if (profiles.isEmpty()) return NO_PROFILE_AVAILABLE;

        DBAIProfile selectedProfile = SelectAiContextUtil.getSelectedProfile(connectionId);
        if (selectedProfile == null) return NO_PROFILE_SELECTED;
        if (!selectedProfile.isEnabled()) return DISABLED_PROFILE_SELECTED;

        return AVAILABLE;
    }

    @Override
    public boolean isCurrentChatActive(ConnectionId connectionId) {
        AssistantState assistantState = getAssistantState(connectionId);
        if (assistantState == null) return false;

        Chat chat = assistantState.getCurrentChat();
        if (!chat.isInteractive()) return true; // non-interactive chats are always active if selected

        String currentSessionSignature = assistantState.getCurrentSessionSignature();

        String sessionSignature = chat.getSessionSignature();
        if (isEmpty(sessionSignature) && isEmpty(currentSessionSignature))
            return chat.isEmpty(); // empty chat not yet started or initialized (assume active)

        return Objects.equals(sessionSignature, currentSessionSignature);
    }

    @Override
    public boolean isCurrentContextEnabled(ConnectionId connectionId) {
        ChatContext chatContext = getChatContext(connectionId);
        if (chatContext == null) return false;

        String profileName = chatContext.getProfileId();
        if (isEmpty(profileName)) return false;

        DBAIProfile profile = SelectAiContextUtil.getProfile(connectionId, profileName);
        return profile != null && profile.isEnabled();
    }

    @Override
    public boolean isCurrentContextValid(ConnectionId connectionId) {
        ChatContext chatContext = getChatContext(connectionId);
        if (chatContext == null) return false;

        String profileName = chatContext.getProfileId();
        String modelName = chatContext.getModelId();

        if (isEmpty(profileName)) return false;
        if (isEmpty(modelName)) return false;

        DBAIProfile profile = SelectAiContextUtil.getProfile(connectionId, profileName);
        if (profile == null) return false;
        if (profile.getProvider().getModel(modelName) == null) return false;
        if (profile.isInteractive() != chatContext.isInteractive()) return false;

        return true;
    }

    @Override
    public void showHelpDialog(ConnectionId connectionId) {
        ConnectionHandler connection = getConnection(connectionId);
        Dialogs.show(() -> new SelectAiHelpDialog(connection));
    }

    @Override
    public String buildChatContextTitle(ChatContext context) {
        return context.getProfileId() + " / " +
                context.getModelName() + " / " +
                PromptAction.get(context.getActionId()).getName();
    }

    @Override
    public ChatContext enrichChatContext(ChatContext context) {
        return SelectAiChatContext.wrap(context);
    }

    @Override
    public String preparePrompt(ConnectionId connectionId, ChatContext chatContext, String prompt) {
        PromptAction action = PromptAction.get(chatContext.getActionId());
        if (action == PromptAction.CHAT) {
            return prompt + " (please properly demarcate code-blocks in the output, and qualify them with the programming-language identifier)";
        }

        return prompt;
    }

    @Override
    public String prepareError(ConnectionId connectionId, ChatContext chatContext, Throwable e) {
        AIProvider provider = chatContext.getProvider();
        return SelectAiEditorPromptUtil.getPresentableMessage(provider, e);
    }

    @Override
    public void checkContext(ConnectionId connectionId, ChatContext chatContext, Runnable onSuccess) {
        onSuccess.run();
    }

    @Override
    public final void generate(String prompt, String chatId, ConnectionId connectionId, ChatContext chatContext, AssistantResponseConsumer responseConsumer) {
        try {
            String message = generate(prompt, connectionId, chatContext);
            responseConsumer.acceptMessage(message);
        } catch (Throwable t) {
            conditionallyLog(t);
            responseConsumer.acceptError(t);
        } finally {
            responseConsumer.acceptCompletion();
        }
    }

    private String generate(String prompt, ConnectionId connectionId, ChatContext chatContext) throws Exception {
        ConnectionHandler connection = getConnection(connectionId);

        SelectAiChatContext customChatContext = SelectAiChatContext.wrap(chatContext);
        String profile = chatContext.getProfileId();
        String action = customChatContext.getAction().getApiId();
        String attributes = customChatContext.getAttributes();

        DBNConnection conn = connection.getConnection(SessionId.ASSISTANT);
        DatabaseAssistantInterface assistantInterface = connection.getAssistantInterface();

        AssistantQueryResponse response = assistantInterface.generate(conn, action, profile, attributes, prompt);
        ProgressMonitor.checkCancelled();

        return response.read();
    }

    @Override
    public String generateTitle(String chatId, ConnectionId connectionId, ChatContext context) throws Exception {
        AssistantState assistantState = getAssistantState(connectionId);
        if (assistantState == null) return null;

        Chat chat = assistantState.getChat(chatId);
        if (chat == null) return null;

        ChatContext chatContext = chat.getContext();
        SelectAiChatContext customChatContext = SelectAiChatContext.wrap(chatContext);
        String profile = customChatContext.getProfileId();
        String action = PromptAction.CHAT.getApiId();
        String attributes = customChatContext.getAttributes();

        List<String> userPrompts = chat.getUserPrompts();
        if (userPrompts.isEmpty()) return null;

        String prompts = Lists.toCsv(userPrompts, "\n", s -> "\"" + s + "\"");
        String titlePrompt = "Summarize the following prompts into a concise title (3-5 words). Respond with the title only, no additional information:\n\n" + prompts;

        ConnectionHandler connection = getConnection(connectionId);
        // use pool connection to avoid interfering with the current conversation
        return PooledConnection.call(connection.createConnectionContext(), c -> {
            DatabaseAssistantInterface assistantInterface = connection.getAssistantInterface();
            AssistantQueryResponse response = assistantInterface.generate(c, action, profile, attributes, titlePrompt);
            return response.read();
        });
    }
}
