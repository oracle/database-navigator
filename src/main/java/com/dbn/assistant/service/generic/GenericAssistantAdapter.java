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

import com.dbn.assistant.AssistantType;
import com.dbn.assistant.adapter.AssistantAdapterBase;
import com.dbn.assistant.adapter.AssistantResponseConsumer;
import com.dbn.assistant.adapter.ui.AssistantContextActionsForm;
import com.dbn.assistant.adapter.ui.AssistantIntroductionForm;
import com.dbn.assistant.adapter.ui.AssistantPromptActionsForm;
import com.dbn.assistant.chat.ChatAvailability;
import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.chat.context.ChatContextImpl;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.assistant.service.generic.model.AssistantModelFactories;
import com.dbn.assistant.service.generic.model.AssistantModelFactory;
import com.dbn.assistant.service.generic.model.AssistantModelInput;
import com.dbn.assistant.service.generic.model.AssistantModelInvoker;
import com.dbn.assistant.service.generic.model.AssistantModelInvokers;
import com.dbn.assistant.service.generic.model.AssistantModelType;
import com.dbn.assistant.service.generic.ui.GenericAssistantContextActionsForm;
import com.dbn.assistant.service.generic.ui.GenericAssistantIntroductionForm;
import com.dbn.assistant.service.generic.ui.GenericAssistantPromptActionsForm;
import com.dbn.assistant.state.AssistantState;
import com.dbn.common.exception.Exceptions;
import com.dbn.connection.ConnectionId;

import static com.dbn.nls.NlsResources.txt;

public class GenericAssistantAdapter extends AssistantAdapterBase {
    public static final GenericAssistantAdapter INSTANCE = new GenericAssistantAdapter();

    private GenericAssistantAdapter() {
        super(AssistantType.PUBLIC);
    }

    @Override
    public ChatContext createChatContext(ConnectionId connectionId) {
        return new ChatContextImpl("OPENAI", "GPT_4_O");
        //return new ChatContextImpl("GOOGLE", "GEMINI_1_5_FLASH");
        //return new ChatContextImpl("ANTHROPIC", "CLAUDE_SONNET_4_20250514");
        //return new ChatContextImpl("MISTRALAI", "CODESTRAL_2501");
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
    public void generate(String prompt, String chatId, ConnectionId connectionId, ChatContext chatContext, AssistantResponseConsumer responseConsumer) {
        try {
            String modelName = chatContext.getModel().getApiName();

            // TODO user, token, url from assistant config...

            AssistantModelInput input = AssistantModelInput.create(modelName)
                    .withUser(System.getProperty("tempOpenAiUser"))
                    .withToken(System.getProperty("tempOpenAiApiKey"));

/*            AssistantModelInput input = AssistantModelInput.create(modelName)
                    .withUser(System.getProperty("tempGoogleAiUser"))
                    .withToken(System.getProperty("tempGoogleAiApiKey"));

            AssistantModelInput input = AssistantModelInput.create(modelName)
                    .withToken(System.getProperty("tempAnthropicApiKey"));


            AssistantModelInput input = AssistantModelInput.create(modelName)
                    .withToken(System.getProperty("tempMistralAiApiKey"));
*/

            AssistantState state = getAssistantState(connectionId);

            var model = resolveModel(chatContext, input);
            var invoker = resolveModelInvoker(model);

            invoker.invokeModel(model, state, chatId, prompt, responseConsumer);

        } catch (Throwable t) {
            responseConsumer.acceptError(t);
            responseConsumer.acceptCompletion();
        }
    }

    private static Object resolveModel(ChatContext context, AssistantModelInput input) {
        AIProvider provider = context.getProvider();
        AssistantModelFactory modelFactory = AssistantModelFactories.get(provider);

        Class[] modelTypes = AssistantModelInvokers.types();
        for (Class<?> modelType : modelTypes) {
            Object assistantModel = modelFactory.createModel(modelType, input);
            if (assistantModel != null) return assistantModel;
        }

        throw new IllegalArgumentException("Could not resolve assistant model for " + input.getModel());
    }

    private static AssistantModelInvoker<Object> resolveModelInvoker(Object model) {
        AssistantModelType modelType = AssistantModelType.get(model);
        return AssistantModelInvokers.get(modelType);
    }

    @Override
    public String generateTitle(String chatId, ConnectionId connectionId) throws Exception {
        return "";
    }
}
