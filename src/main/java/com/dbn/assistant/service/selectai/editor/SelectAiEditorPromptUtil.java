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

package com.dbn.assistant.service.selectai.editor;

import com.dbn.assistant.DatabaseAssistantManager;
import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.chat.message.ChatMessage;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.assistant.service.selectai.PromptAction;
import com.dbn.assistant.service.selectai.SelectAiResponseConsumer;
import com.dbn.assistant.service.selectai.ui.SelectAiHelpDialog;
import com.dbn.common.thread.Progress;
import com.dbn.common.thread.ThreadBlocker;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.UUIDs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.intellij.openapi.project.Project;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.dbn.assistant.AssistantType.SELECT_AI;
import static com.dbn.assistant.chat.message.AuthorType.AGENT;
import static com.dbn.common.exception.Exceptions.getLocalizedMessages;
import static com.dbn.common.exception.Exceptions.rootCauseOf;
import static com.dbn.common.message.MessageType.NEUTRAL;
import static com.dbn.common.util.Conditional.when;
import static com.dbn.common.util.Messages.options;
import static com.dbn.common.util.Messages.showErrorDialog;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

public class SelectAiEditorPromptUtil {
    public static void generate(ConnectionId connectionId, String text, ChatContext context, Consumer<ChatMessage> consumer) {
        ConnectionHandler connection = ConnectionHandler.ensure(connectionId);
        Project project = connection.getProject();
        ThreadBlocker blocker = new ThreadBlocker();

        String processMessage = getPromptText(context.getActionId(), text);
        String processTitle = SELECT_AI.getName();
        Progress.modal(project, connection, true,
                processTitle,
                processMessage, p -> {
                    DatabaseAssistantManager assistantManager = DatabaseAssistantManager.getInstance(project);

                    String chatId = UUIDs.compact(); // conversation not supported in editor context
                    // TODO is there any interactive use-case possible
                    //  (e.g. popup dialog with suggestion to improve the output)

                    assistantManager.query(text, chatId, connectionId, SELECT_AI, context, new SelectAiResponseConsumer() {
                        @Override
                        public void acceptMessage(String message) {
                            ChatMessage chatMessage = new ChatMessage(SELECT_AI, NEUTRAL, message, AGENT, context);
                            consumer.accept(chatMessage);
                        }

                        @Override
                        public void acceptError(String message, Throwable e) {
                            conditionallyLog(e);
                            AIProvider provider = context.getModel().getProvider();
                            handleGenerateException(project, connectionId, provider, e);
                        }

                        @Override
                        public void acceptCompletion() {
                            blocker.unblock();
                        }
                    });
                    blocker.block(1, TimeUnit.MINUTES); // TODO configuration
                });
    }

    private static void handleGenerateException(Project project, ConnectionId connectionId, AIProvider provider, Throwable e) {
        String title = txt("msg.assistant.title.AssistantError", SELECT_AI.getName());

        String message = getPresentableMessage(provider, e);

        showErrorDialog(project, title,
                message, options(txt("msg.shared.button.Help"), txt("msg.shared.button.Cancel")), 0,
                option -> when(option == 0, () -> showPrerequisitesDialog(connectionId)));
    }

    public static String getPresentableMessage(AIProvider provider, Throwable e) {
        e = rootCauseOf(e);
        String assistantName = SELECT_AI.getName() ;
        String errorMessage = e.getMessage();
        boolean networkAccessDenied = errorMessage != null && errorMessage.contains("ORA-24247");

        if (networkAccessDenied) {
            String accessPoint = provider.getHost();

            String message = txt("msg.assistant.error.NetworkAccessDenied", accessPoint);
            return txt("msg.shared.error.ErrorDetails", message, getLocalizedMessages(e));
        }

        String message = txt("msg.assistant.error.SelectAiInvocationFailure", assistantName);
        return txt("msg.shared.error.ErrorDetails", message, getLocalizedMessages(e));
    }

    public static void showPrerequisitesDialog(ConnectionId connectionId) {
        ConnectionHandler connection = ConnectionHandler.ensure(connectionId);
        Dialogs.show(() -> new SelectAiHelpDialog(connection));
    }

    private static String getPromptText(String actionName, String prompt) {
        PromptAction action = PromptAction.get(actionName);
        return switch (action) {
            case SHOW_SQL -> txt("prc.assistant.text.PromptAction_SHOW_SQL", prompt);
            case EXPLAIN_SQL -> txt("prc.assistant.text.PromptAction_EXPLAIN_SQL", prompt);
            case EXECUTE_SQL -> txt("prc.assistant.text.PromptAction_EXECUTE_SQL", prompt);
            case NARRATE -> txt("prc.assistant.text.PromptAction_NARRATE", prompt);
            case CHAT -> txt("prc.assistant.text.PromptAction_CHAT", prompt);
            default -> txt("prc.assistant.text.PromptAction_ANY", prompt);
        };
    }
}
