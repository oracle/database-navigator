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

package com.dbn.assistant.selectai.editor;

import com.dbn.assistant.AssistantType;
import com.dbn.assistant.DatabaseAssistantManager;
import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.chat.message.AuthorType;
import com.dbn.assistant.chat.message.ChatMessage;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.assistant.selectai.PromptAction;
import com.dbn.assistant.selectai.SelectAiResponseConsumer;
import com.dbn.assistant.selectai.ui.SelectAiHelpDialog;
import com.dbn.common.exception.Exceptions;
import com.dbn.common.message.MessageType;
import com.dbn.common.thread.Progress;
import com.dbn.common.thread.ThreadBlocker;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.intellij.openapi.project.Project;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.dbn.common.util.Conditional.when;
import static com.dbn.common.util.Messages.options;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

public class SelectAiEditorPromptUtil {
    public static void generate(ConnectionId connectionId, String text, ChatContext context, Consumer<ChatMessage> consumer) {
        ConnectionHandler connection = ConnectionHandler.ensure(connectionId);
        Project project = connection.getProject();
        ThreadBlocker blocker = new ThreadBlocker();

        String processMessage = getPromptText(context.getActionId(), text);
        String processTitle = AssistantType.SELECT_AI.getName();
        Progress.modal(project, connection, true,
                processTitle,
                processMessage, p -> {
                    DatabaseAssistantManager assistantManager = DatabaseAssistantManager.getInstance(project);
                    assistantManager.query(text, connectionId, AssistantType.SELECT_AI, context, new SelectAiResponseConsumer() {
                        @Override
                        public void acceptMessage(String message) {
                            ChatMessage chatMessage = new ChatMessage(MessageType.NEUTRAL, message, AuthorType.AGENT, context);
                            consumer.accept(chatMessage);
                        }

                        @Override
                        public void acceptError(Throwable e) {
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
        String title = AssistantType.SELECT_AI.getName() + " Error";

        String message = getPresentableMessage(provider, e);

        Messages.showErrorDialog(project, title,
                message, options("Help", "Cancel"), 0,
                option -> when(option == 0, () -> showPrerequisitesDialog(connectionId)));
    }

    public static String getPresentableMessage(AIProvider provider, Throwable e) {
        e = Exceptions.rootCauseOf(e);
        String assistantName = AssistantType.SELECT_AI.getName() ;
        String errorMessage = e.getMessage();
        boolean networkAccessDenied = errorMessage != null && errorMessage.contains("ORA-24247");

        if (networkAccessDenied) {
            String accessPoint = provider.getHost();

            return txt("msg.assistant.error.NetworkAccessDenied", accessPoint, errorMessage);
        }

        return txt("msg.assistant.error.SelectAiInvocationFailure", assistantName, errorMessage);
    }

    public static void showPrerequisitesDialog(ConnectionId connectionId) {
        ConnectionHandler connection = ConnectionHandler.ensure(connectionId);
        Dialogs.show(() -> new SelectAiHelpDialog(connection));
    }

    private static String getPromptText(String actionName, String prompt) {
        PromptAction action = PromptAction.get(actionName);
        switch (action) {
            case SHOW_SQL: return txt("prc.assistant.text.PromptAction_SHOW_SQL", prompt);
            case EXPLAIN_SQL: return txt("prc.assistant.text.PromptAction_EXPLAIN_SQL", prompt);
            case EXECUTE_SQL: return txt("prc.assistant.text.PromptAction_EXECUTE_SQL", prompt);
            case NARRATE: return txt("prc.assistant.text.PromptAction_NARRATE", prompt);
            case CHAT: return txt("prc.assistant.text.PromptAction_CHAT", prompt);
            default: return txt("prc.assistant.text.PromptAction_ANY", prompt);
        }
    }
}
