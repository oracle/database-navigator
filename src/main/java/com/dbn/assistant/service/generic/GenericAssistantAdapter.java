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

package com.dbn.assistant.service.generic;

import com.dbn.assistant.AssistantContextUtil;
import com.dbn.assistant.AssistantType;
import com.dbn.assistant.adapter.AssistantAdapterBase;
import com.dbn.assistant.adapter.AssistantResponseAdapter;
import com.dbn.assistant.adapter.AssistantResponseConsumer;
import com.dbn.assistant.adapter.ui.AssistantContextActionsForm;
import com.dbn.assistant.adapter.ui.AssistantIntroductionForm;
import com.dbn.assistant.adapter.ui.AssistantPromptActionsForm;
import com.dbn.assistant.chat.Chat;
import com.dbn.assistant.chat.ChatAvailability;
import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.assistant.credential.AssistantCredential;
import com.dbn.assistant.profile.AssistantProfile;
import com.dbn.assistant.provider.AIModel;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.assistant.provider.AIProviderId;
import com.dbn.assistant.service.generic.model.AssistantModelFactories;
import com.dbn.assistant.service.generic.model.AssistantModelFactory;
import com.dbn.assistant.service.generic.model.AssistantModelInput;
import com.dbn.assistant.service.generic.model.AssistantModelInvoker;
import com.dbn.assistant.service.generic.model.AssistantModelInvokers;
import com.dbn.assistant.service.generic.model.AssistantModelType;
import com.dbn.assistant.service.generic.ui.GenericAssistantContextActionsForm;
import com.dbn.assistant.service.generic.ui.GenericAssistantIntroductionForm;
import com.dbn.assistant.service.generic.ui.GenericAssistantPromptActionsForm;
import com.dbn.assistant.settings.AssistantSettings;
import com.dbn.assistant.state.AssistantState;
import com.dbn.common.exception.Exceptions;
import com.dbn.common.util.Lists;
import com.dbn.common.util.UUIDs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.intellij.openapi.project.Project;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.dbn.assistant.profile.AssistantProfileLookup.getProfile;
import static com.dbn.nls.NlsResources.txt;

public class GenericAssistantAdapter extends AssistantAdapterBase {
    public static final GenericAssistantAdapter INSTANCE = new GenericAssistantAdapter();

    private GenericAssistantAdapter() {
        super(AssistantType.PUBLIC);
    }

    @Override
    public ChatContext createChatContext(ConnectionId connectionId) {
        return AssistantContextUtil.getChatContext(connectionId, getAssistantType());
    }

    @Override
    public AssistantIntroductionForm createIntroductionForm(ChatBoxForm chatBoxForm) {
        return new GenericAssistantIntroductionForm(chatBoxForm);
    }

    @Override
    public AssistantContextActionsForm createContextActionsForm(ChatBoxForm chatBoxForm) {
        return new GenericAssistantContextActionsForm(chatBoxForm);
    }

    @Override
    public AssistantPromptActionsForm createPromptActionsForm(ChatBoxForm chatBoxForm) {
        return new GenericAssistantPromptActionsForm(chatBoxForm);
    }

    @Override
    public ChatAvailability getChatAvailability(ConnectionId connectionId) {
        ChatContext chatContext = getChatContext(connectionId);
        if (chatContext == null) return ChatAvailability.NOT_INITIALIZED;

        AIProvider provider = chatContext.getProvider();
        if (provider == null) return ChatAvailability.NO_PROFILE_SELECTED;

        return ChatAvailability.AVAILABLE;
    }

    @Override
    public void initializeAssistant(ConnectionId connectionId) {

    }

    @Override
    public boolean isCurrentChatActive(ConnectionId connectionId) {
        return true;
    }

    @Override
    public boolean isCurrentContextEnabled(ConnectionId connectionId) {
        return true;
    }

    @Override
    public boolean isCurrentContextValid(ConnectionId connectionId) {
        return true;
    }

    @Override
    public void showHelpDialog(ConnectionId connectionId) {

    }

    @Override
    public String preparePrompt(ConnectionId connectionId, ChatContext chatContext, String prompt) {
        return prompt;
    }

    @Override
    public String prepareError(ConnectionId connectionId, ChatContext chatContext, Throwable e) {
        e = Exceptions.rootCauseOf(e);
        String errorMessage = Exceptions.getMessage(e);
        return txt("msg.assistant.error.AssistantInvocationFailure", getAssistantType().getName(), errorMessage);
    }

    @Override
    public void generate(String prompt, String chatId, ConnectionId connectionId, ChatContext context, AssistantResponseConsumer responseConsumer) {
        try {
            AssistantModelInput input = createModelInput(connectionId, context);
            AssistantState state = getAssistantState(connectionId);

            var model = resolveModel(context, input);
            var invoker = resolveModelInvoker(model);

            invoker.invokeModel(model, state, chatId, prompt, responseConsumer);

        } catch (Throwable t) {
            responseConsumer.acceptError(t);
            responseConsumer.acceptCompletion();
        }
    }

    @Override
    public String generateTitle(String chatId, ConnectionId connectionId, ChatContext context) {
        AssistantState state = getAssistantState(connectionId);
        if (state == null) return null;

        Chat chat = state.getChat(chatId);
        List<String> userPrompts = chat.getUserPrompts();
        if (userPrompts.isEmpty()) return null;

        String prompts = Lists.toCsv(userPrompts, "\n", s -> "\"" + s + "\"");
        String titlePrompt = "Summarize the following user prompts into a concise title (3-5 words). Respond with the title only, no punctuation, quotes, or filler words:\n\n" + prompts;

        AssistantModelInput input = createModelInput(connectionId, context);
        if (input == null) return null;

        var model = resolveModel(context, input);
        var invoker = resolveModelInvoker(model);

        AtomicReference<String> title = new AtomicReference<>();
        AssistantResponseConsumer responseConsumer = AssistantResponseAdapter.create().withMessageConsumer(m -> title.set(m));
        invoker.invokeModel(model, state, UUIDs.compact(), titlePrompt, responseConsumer);

        return title.get();
    }


    private static AssistantModelInput createModelInput(ConnectionId connectionId, ChatContext chatContext) {
        ConnectionHandler connection = ConnectionHandler.ensure(connectionId);

        AIProvider provider = chatContext.getProvider();
        if (provider == null) return null;

        AIModel model = chatContext.getModel();
        if (model == null) return null;

        String modelName = model.getApiName();
        String profileId = chatContext.getProfileId();

        Project project = connection.getProject();
        AssistantProfile profile = getProfile(project, profileId);
        if (profile == null) return null;

        String credentialId = profile.getCredentialId();
        AssistantCredential credential = getAssistantCredential(project, credentialId);
        if (credential == null) return null;

        AIProviderId baseProviderId = model.getBaseProviderId();
        AIProviderId providerId = provider.getId();
        return AssistantModelInput.create(baseProviderId, providerId, modelName)
                .withUser(credential.getUser())
                .withToken(credential.getKey());
    }

    private static AssistantCredential getAssistantCredential(Project project, String credentialId) {
        AssistantSettings assistantSettings = AssistantSettings.getInstance(project);
        return assistantSettings.getCredentialSettings().getCredentials().getCredential(credentialId);
    }

    private static Object resolveModel(ChatContext context, AssistantModelInput input) {
        AIProviderId providerId = context.getProviderId();
        AssistantModelFactory modelFactory = AssistantModelFactories.get(providerId);

        Class[] modelTypes = AssistantModelInvokers.types();
        for (Class<?> modelType : modelTypes) {
            Object assistantModel = modelFactory.createModel(modelType, input);
            if (assistantModel != null) return assistantModel;
        }

        throw new IllegalArgumentException("Could not resolve assistant model for " + input.getModelName());
    }

    private static AssistantModelInvoker<Object> resolveModelInvoker(Object model) {
        AssistantModelType modelType = AssistantModelType.get(model);
        return AssistantModelInvokers.get(modelType);
    }
}
