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
package com.dbn.assistant.chat.window.ui;

import com.dbn.assistant.AssistantType;
import com.dbn.assistant.DatabaseAssistantManager;
import com.dbn.assistant.adapter.AssistantAdapter;
import com.dbn.assistant.adapter.AssistantAdapters;
import com.dbn.assistant.adapter.ui.AssistantContextActionsForm;
import com.dbn.assistant.adapter.ui.AssistantIntroductionForm;
import com.dbn.assistant.adapter.ui.AssistantPromptActionsForm;
import com.dbn.assistant.chat.Chat;
import com.dbn.assistant.chat.ChatAvailability;
import com.dbn.assistant.chat.ChatContextEvent;
import com.dbn.assistant.chat.ChatInterruptionReason;
import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.chat.message.ChatMessage;
import com.dbn.assistant.chat.message.ui.ChatMessagesForm;
import com.dbn.assistant.chat.ui.ChatSaveDialog;
import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.state.AssistantStateListener;
import com.dbn.common.action.DataKeys;
import com.dbn.common.action.DefaultActionGroup;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.thread.Background;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.action.SelectConnectionAction;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Objects;

import static com.dbn.assistant.chat.message.AuthorType.USER;
import static com.dbn.assistant.state.AssistantStatus.QUERYING;
import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.feature.FeatureAcknowledgement.ENGAGED;
import static com.dbn.common.message.MessageType.NEUTRAL;
import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Strings.isEmptyOrSpaces;
import static com.dbn.common.util.Unsafe.cast;

/**
 * Database Assistant ChatBox component
 *
 * @author Ayoub Aarrasse (Oracle)
 * @author Emmanuel Jannetti (Oracle)
 * @author Dan Cioca (Oracle)
 */
@Slf4j
public class ChatBoxForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel contextActionsPanel;
    private JPanel promptActionsPanel;
    private JPanel promptSubmitActionsPanel;
    private JPanel inputFieldPanel;
    private JPanel introPanel;
    private JPanel chatBoxPanel;
    private JPanel chatActionsPanel;
    private JPanel chatMessagesPanel;

    private final ConnectionRef connection;
    private final AssistantType assistantType;
    private ChatBoxInputField inputField;
    private ChatMessagesForm messagesForm;
    private String currentChatId; // identifier of currently displayed chat (can be temporarels different from the one in the AssistantState)

    private AssistantContextActionsForm contextActionsForm;
    private AssistantPromptActionsForm promptActionsForm;

    public ChatBoxForm(ConnectionHandler connection, AssistantType assistantType) {
        super(connection, connection.getProject());
        this.connection = connection.ref();
        this.assistantType = assistantType;

        // hide all panels until availability status is known
        this.introPanel.setVisible(false);
        this.chatBoxPanel.setVisible(false);

        initHeaderForm();
        initIntroForm();
        initChatBoxForm();

        Project project = connection.getProject();
        ProjectEvents.subscribe(project, this, AssistantStateListener.TOPIC, createStateListener());
    }

    private AssistantStateListener createStateListener() {
        return (project, connectionId) -> {
            if (!Objects.equals(connectionId, getConnectionId())) return;

            AssistantState state = getAssistantState();
            if (Objects.equals(currentChatId, state.getCurrentChatId())) return;

            initMessages();
        };
    }

    private void initIntroForm() {
        if (hasUserEngaged()) return;

        AssistantAdapter assistantAdapter = getAssistantAdapter();
        AssistantIntroductionForm introductionForm = assistantAdapter.createIntroductionForm(this);
        introPanel.add(introductionForm.getComponent());
        introPanel.setVisible(true);
    }

    private void initChatBoxForm() {
        if (!hasUserEngaged()) return;

        chatBoxPanel.setVisible(true);
        createActionPanels();
        createInputField();
        createMessagesPanel();
        initMessages();
    }

    private ActionGroup createConnectionActions() {
        return SelectConnectionAction.createActions(
                ensureProject(), (id) -> selectConnection(id));
    }

    private ActionGroup createAssistantTypeActions() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new ComboBoxAction() {
            @Override
            @NotNull
            protected DefaultActionGroup createPopupActionGroup(@NotNull JComponent component, @NotNull DataContext dataContext) {
                DefaultActionGroup actionGroup = new DefaultActionGroup();
                ConnectionHandler connection = getConnection();
                if (connection.getDatabaseType() == DatabaseType.ORACLE) {
                    actionGroup.add(new SelectAssistantTypeAction(AssistantType.SELECT_AI));
                }
                actionGroup.add(new SelectAssistantTypeAction(AssistantType.PUBLIC));
                //actionGroup.add(new SelectAssistantTypeAction(AssistantType.INTERNAL));
                //actionGroup.add(new SelectAssistantTypeAction(AssistantType.CUSTOM));
                return actionGroup;
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setText(assistantType.getName());
            }
        });

        return actionGroup;
    }

    private void selectConnection(ConnectionId connectionId) {
        DatabaseAssistantManager assistantManager = getManager();
        assistantManager.switchToConnection(connectionId);
    }

    private boolean hasUserEngaged() {
        return getAssistantState().getAcknowledgement() == ENGAGED;
    }

    private void initHeaderForm() {
        ConnectionHandler connection = getConnection();
        DBNHeaderForm headerForm = new DBNHeaderForm(this, connection);
        headerForm.setSelector("Select Connection", createConnectionActions());
        headerForm.setActions(createAssistantTypeActions());

        headerPanel.add(headerForm.getComponent());
    }

    private void createActionPanels() {
        // top left
        AssistantAdapter assistantAdapter = getAssistantAdapter();
        contextActionsForm = assistantAdapter.createContextActionsForm(this);
        this.contextActionsPanel.add(contextActionsForm.getComponent());

        // top right
        ActionToolbar chatActions = Actions.createActionToolbar(chatActionsPanel, true, "DBNavigator.ActionGroup.AssistantChatActions");
        this.chatActionsPanel.add(chatActions.getComponent());

        // bottom left
        promptActionsForm = assistantAdapter.createPromptActionsForm(this);
        this.promptActionsPanel.add(promptActionsForm.getComponent());

        // submit prompt actions (bottom right)
        ActionToolbar promptSubmitActions = Actions.createActionToolbar(promptSubmitActionsPanel, true, "DBNavigator.ActionGroup.AssistantPromptSubmitActions");
        setAccessibleName(promptSubmitActions, txt("app.assistant.aria.ChatActions"));
        this.promptSubmitActionsPanel.add(promptSubmitActions.getComponent());
    }

    public <F extends AssistantContextActionsForm> F getContextActionsForm() {
        return cast(contextActionsForm);
    }

    public <F extends AssistantPromptActionsForm> F getPromptActionsForm() {
        return cast(promptActionsForm);
    }

    private void createInputField() {
        inputField = new ChatBoxInputField(this);
        inputFieldPanel.add(inputField, BorderLayout.CENTER);
    }

    public void initMessages() {
        if (!hasUserEngaged()) return;
        if (messagesForm == null) return; // not yet initialized

        Chat chat = getCurrentChat();
        if (Objects.equals(chat.getId(), currentChatId)) return;

        currentChatId = chat.getId();
        chat.removeProgress();

        messagesForm.clear();
        messagesForm.addMessages(chat.getMessages());
    }

    @NotNull
    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    public AssistantState getAssistantState() {
        return getManager().getAssistantState(getConnectionId(),  assistantType);
    }

    public AssistantAdapter getAssistantAdapter() {
        return AssistantAdapters.get(assistantType);
    }

    public void showHelpDialog() {
        AssistantAdapter assistantAdapter = getAssistantAdapter();
        assistantAdapter.showHelpDialog(getConnectionId());
    }


    public void acknowledgeIntro() {
        AssistantState assistantState = getAssistantState();
        assistantState.setAcknowledgement(ENGAGED);
        initChatBoxForm();

        introPanel.setVisible(false);
        chatBoxPanel.setVisible(true);
    }

    private void startChat(ChatContext context) {
        interruptAssistantSession();
        AssistantState state = getAssistantState();
        state.createChat(context);
        state.deleteObsoleteChats();

        initMessages();
    }

    private void selectChat(String chatId) {
        AssistantState state = getAssistantState();
        state.setCurrentChatId(chatId);
        state.deleteObsoleteChats();

        initMessages();
    }

    private void saveCurrentChat(String title) {
        Chat chat = getCurrentChat();
        chat.removeProgress();
        chat.setTitle(title);
    }

    public void deleteCurrentChat() {
        ChatContext context = getCurrentContext();
        discardCurrentChat();

        // start a new chat
        startChat(context);
    }

    private void discardCurrentChat() {
        AssistantState state = getAssistantState();
        String chatId = state.getCurrentChatId();
        state.deleteChat(chatId);
    }

    public void attemptContextSwitch(ChatContext targetContext) {
        ChatContext currentContext = getAssistantState().getCurrentContext();
        ChatContextEvent event = createContextEvent(
                currentContext,
                targetContext,
                null,
                false);
        processContextEvent(event);
    }

    public ChatContextEvent createContextEvent(
            @NotNull ChatContext currentContext,
            @NotNull ChatContext targetContext,
            @Nullable String targetChatId,
            boolean newChatRequest) {

        AssistantAdapter assistantAdapter = getAssistantAdapter();
        currentContext = assistantAdapter.enrichChatContext(currentContext);
        targetContext = assistantAdapter.enrichChatContext(targetContext);

        return new ChatContextEvent(currentContext, targetContext, targetChatId, newChatRequest);
    }

    private void performContextSwitch(ChatContextEvent event) {
        ChatContext targetContext = event.getTargetContext();
        String targetChatId = event.getTargetChatId();

        if (event.isNewChatRequest()) {
            startChat(targetContext);

        } else if (targetChatId != null) {
            selectChat(targetChatId);

        } else {
            changeChatContext(targetContext);
        }
    }

    private void changeChatContext(ChatContext context) {
        getAssistantState().setCurrentContext(context);
        updateActionToolbars();
    }

    public void cancelContextSwitch() {
        // no changes, just refresh actions
        updateActionToolbars();
    }

    public void processContextEvent(ChatContextEvent event) {
        AssistantState state = getAssistantState();
        ChatInterruptionReason interruptionReason = event.evaluateInterruption(state);

        if (interruptionReason == null) {
            performContextSwitch(event);
            return;
        }

        // indirect start of a new chat due to interrupted state
        if (!event.isChatOpenRequest()) {
            event.setNewChatRequest(true);
        }

        Chat chat = getCurrentChat();
        if (chat.isPersisted() && !chat.isInteractive()) {
            // non-interactive chat, already persisted
            performContextSwitch(event);
            return;
        }

        Dialogs.show(() -> new ChatSaveDialog(
                        ensureProject(),
                        interruptionReason,
                        state.getChatNames()),
                (dialog, exitCode) -> handleDialogResult(event, dialog.getTitle(), exitCode));
    }

    private void handleDialogResult(ChatContextEvent event, String title, int exitCode) {
        switch (exitCode) {
            case 0: {
                // Cancel was selected - stay with current context
                cancelContextSwitch();
                break;
            }
            case 1: {
                // Discard was selected
                discardCurrentChat();
                performContextSwitch(event);
                break;
            }
            case 2: {
                // Save was selected
                saveCurrentChat(title);
                performContextSwitch(event);
            }
        }
    }

    /**
     * Initializes the panel to display messages
     */
    private void createMessagesPanel() {
        messagesForm = new ChatMessagesForm(this);
        chatMessagesPanel.add(messagesForm.getComponent());
    }

    public ChatContext getCurrentContext() {
        return getCurrentChat().getContext();
    }

    public Chat getCurrentChat() {
        return getAssistantState().getCurrentChat();
    }

    public void submitPrompt() {
        submitPrompt(null);
    }

    public void submitPrompt(String question) {
        Background.run(() -> processQuery(question));
    }

    private void processQuery(String question) {
        AssistantState assistantState = getAssistantState();
        ChatAvailability availability = assistantState.getChatAvailability();
        if (availability != ChatAvailability.AVAILABLE) return;

        String prompt = getInputField().getAndClearText();
        question = nvl(question, prompt);
        if (isEmptyOrSpaces(question)) return;

        ConnectionId connectionId = getConnectionId();

        AssistantAdapter assistantAdapter = getAssistantAdapter();
        ChatContext chatContext = assistantAdapter.createChatContext(connectionId);
        if (chatContext == null) return;

        assistantState.set(QUERYING, true);
        ChatMessage userMessage = new ChatMessage(NEUTRAL, question, USER, chatContext);
        userMessage.setProgress(true);

        String chatId = assistantState.getCurrentChatId();
        appendMessage(chatId, userMessage);

        ChatBoxResponseConsumer responseConsumer = new ChatBoxResponseConsumer(this, chatContext, chatId);

        initChatTitle(chatId, connectionId);

        DatabaseAssistantManager assistantManager = getManager();
        assistantManager.query(question, chatId, connectionId, assistantType, chatContext, responseConsumer);
    }

    private void initChatTitle(String chatId, ConnectionId connectionId) {
        Chat chat = getChat(chatId);
        if (chat.isPersisted()) return;

        Background.run(() -> {
            DatabaseAssistantManager assistantManager = getManager();
            String title = assistantManager.generateTitle(chatId, connectionId, assistantType);
            if (title != null) {
                String[] split = title.split("\\s");
                if (split.length > 6) title = null;
            }
            if (title == null) return;

            title = title.replaceAll("\"", "");
            title = title.replaceAll("'", "");
            chat.setTitle(title);
        });
    }

    public ChatBoxInputField getInputField() {
        return nd(inputField);
    }

    public void showErrorHeader(Throwable cause) {
        // TODO show error bar (similar to editor error headers)
    }

    protected void appendMessage(String chatId, ChatMessage message) {
        AssistantState state = getAssistantState();
        Chat chat = state.getChat(chatId);
        if (chat == null) return; // chat already discarded by the time of message arrival

        chat.addMessage(message);
        String currentChatId = state.getCurrentChatId();
        if (Objects.equals(chatId, currentChatId)) {
            // update UI only if chat is still current
            messagesForm.addMessages(List.of(message));
            updateActionToolbars();
        }
    }

    public void refreshMessage(ChatMessage message) {
        messagesForm.refreshMessage(message);
    }

    public void refreshTools(ChatMessage message) {
        messagesForm.refreshTools(message);
    }


    protected Chat getChat(String chatId) {
        AssistantState state = getAssistantState();
        return state.getChat(chatId);
    }

    public void interruptAssistantSession() {
        getManager().interruptAssistantSession(getConnection());
    }

    @NotNull
    public AssistantType getAssistantType() {
        return assistantType;
    }

    @NotNull
    public ConnectionId getConnectionId() {
        return connection.getConnectionId();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    private DatabaseAssistantManager getManager() {
        Project project = ensureProject();
        return DatabaseAssistantManager.getInstance(project);
    }

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.ASSISTANT_CHAT_BOX.is(dataId)) return this;
        return null;
    }

    public void focusInputField() {
        if (inputField == null) return;
        inputField.requestFocus();
    }

    private class SelectAssistantTypeAction extends AnAction {
        private final AssistantType assistantType;
        public SelectAssistantTypeAction(AssistantType assistantType) {
            super(assistantType.getName());
            this.assistantType = assistantType;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            DatabaseAssistantManager assistantManager = getManager();
            assistantManager.switchToAssistant(getConnectionId(), assistantType);
        }
    }

}
