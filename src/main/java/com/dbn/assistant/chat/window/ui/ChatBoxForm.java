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

import com.dbn.assistant.DatabaseAssistantManager;
import com.dbn.assistant.chat.Chat;
import com.dbn.assistant.chat.ChatAvailability;
import com.dbn.assistant.chat.ChatContext;
import com.dbn.assistant.chat.ChatContextEvent;
import com.dbn.assistant.chat.ChatInterruptionReason;
import com.dbn.assistant.chat.message.AuthorType;
import com.dbn.assistant.chat.message.PersistentChatMessage;
import com.dbn.assistant.chat.ui.ChatSaveDialog;
import com.dbn.assistant.chat.window.PromptAction;
import com.dbn.assistant.chat.window.util.RollingMessageContainer;
import com.dbn.assistant.init.ui.AssistantIntroductionForm;
import com.dbn.assistant.provider.AIModel;
import com.dbn.assistant.state.AssistantState;
import com.dbn.common.action.DataKeys;
import com.dbn.common.message.MessageType;
import com.dbn.common.thread.Background;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionRef;
import com.dbn.object.DBAIProfile;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.AsyncProcessIcon;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Objects;

import static com.dbn.assistant.state.AssistantStatus.INITIALIZING;
import static com.dbn.assistant.state.AssistantStatus.QUERYING;
import static com.dbn.assistant.state.AssistantStatus.UNAVAILABLE;
import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.feature.FeatureAcknowledgement.ENGAGED;
import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Lists.firstElement;
import static com.dbn.object.common.DBObjectUtil.refreshUserObjects;
import static com.dbn.object.type.DBObjectType.AI_PROFILE;

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
  private JPanel chatPanel;
  private JScrollPane chatScrollPane;
  private JPanel profileActionsPanel;
  private JPanel headerPanel;
  private JPanel typeActionsPanel;
  private JPanel inputFieldPanel;
  private JPanel chatPromptActionsPanel;
  private JPanel introPanel;
  private JPanel chatBoxPanel;
  private JPanel initializingIconPanel;
  private JPanel initializingPanel;
  private JPanel chatActionsPanel;
  private JPanel chatStatusPanel;

  private RollingMessageContainer messageContainer;
  private final ConnectionRef connection;
  private ChatBoxInputField inputField;
  private ChatBoxStatusLabel statusLabel;

  public ChatBoxForm(ConnectionHandler connection) {
    super(connection, connection.getProject());
    this.connection = connection.ref();

    initializingIconPanel.add(new AsyncProcessIcon("Loading"), BorderLayout.CENTER);

    // hide all panels until availability status is known
    this.introPanel.setVisible(false);
    this.chatBoxPanel.setVisible(false);
    this.initializingPanel.setVisible(false);

    initHeaderForm();
    initIntroForm();
    initChatBoxForm();
  }


  private void initIntroForm() {
    if (hasUserEngaged()) return;

    AssistantIntroductionForm introductionForm = new AssistantIntroductionForm(this);
    introPanel.add(introductionForm.getComponent(), BorderLayout.CENTER);
    introPanel.setVisible(true);
  }

  private void initChatBoxForm() {
    if (!hasUserEngaged()) return;
    chatBoxPanel.setVisible(true);
    createActionPanels();
    createStatusLabel();
    createInputField();
    createChatPanel();
    loadProfiles();
    initMessages();
  }

  private boolean hasUserEngaged() {
    return getAssistantState().getAcknowledgement() == ENGAGED;
  }

  private void initHeaderForm() {
    ConnectionHandler connection = getConnection();
    DBNHeaderForm headerForm = new DBNHeaderForm(this, connection);
    headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);
  }

  private void createActionPanels() {
    ActionToolbar profileActions = Actions.createActionToolbar(profileActionsPanel, true, "DBNavigator.ActionGroup.AssistantChatBoxProfiles");
    setAccessibleName(profileActions, txt("app.assistant.aria.ChatProfileActions"));
    this.profileActionsPanel.add(profileActions.getComponent());

    ActionToolbar helpActions = Actions.createActionToolbar(chatActionsPanel, true, "DBNavigator.ActionGroup.AssistantChatActions");
    this.chatActionsPanel.add(helpActions.getComponent());

    ActionToolbar typeActions = Actions.createActionToolbar(typeActionsPanel, true, "DBNavigator.ActionGroup.AssistantChatTypes");
    setAccessibleName(profileActions, txt("app.assistant.aria.ChatTypeActions"));
    this.typeActionsPanel.add(typeActions.getComponent());

    ActionToolbar chatActions = Actions.createActionToolbar(chatPromptActionsPanel, true, "DBNavigator.ActionGroup.AssistantChatBoxPrompt");
    setAccessibleName(profileActions, txt("app.assistant.aria.ChatActions"));
    this.chatPromptActionsPanel.add(chatActions.getComponent());
  }

  private void createStatusLabel() {
    statusLabel = new ChatBoxStatusLabel(this);
    chatStatusPanel.add(statusLabel);
  }

  private void createInputField() {
    inputField = new ChatBoxInputField(this);
    inputFieldPanel.add(inputField, BorderLayout.CENTER);
  }

  public void initMessages() {
    Chat chat = getCurrentChat();
    chat.removeProgress();

    messageContainer.clear();
    messageContainer.addAll(chat.getMessages(), this);
    dispatch(() -> scrollDown());
  }

  @NotNull
  public ConnectionHandler getConnection() {
    return connection.ensure();
  }

  public AssistantState getAssistantState() {
    return getManager().getAssistantState(getConnectionId());
  }

  public void acknowledgeIntro() {
    getAssistantState().setAcknowledgement(ENGAGED);
    initChatBoxForm();
    introPanel.setVisible(false);
    chatBoxPanel.setVisible(true);
  }

  public List<DBAIProfile> getProfiles() {
    DatabaseAssistantManager manager = getManager();
    ConnectionId connectionId = getConnectionId();
    return manager.getProfiles(connectionId);
  }

  @Nullable
  public DBAIProfile getSelectedProfile() {
    DatabaseAssistantManager manager = getManager();
    ConnectionId connectionId = getConnectionId();
    return manager.getSelectedProfile(connectionId);
  }

  public AIModel getSelectedModel() {
    DatabaseAssistantManager manager = getManager();
    ConnectionId connectionId = getConnectionId();
    return manager.getSelectedModel(connectionId);
  }

  public void selectProfile(DBAIProfile profile) {
    // preserve action from the current context
    ChatContext currentContext = getAssistantState().getCurrentContext();
    ChatContext targetContext = new ChatContext(profile.getName(), profile.getModel(), currentContext.getAction(), profile.isInteractive());
    ChatContextEvent event = new ChatContextEvent(currentContext, targetContext, null, false);
    processContextEvent(event);
  }

  public void selectModel(AIModel model) {
    // preserve profile and action from the current context
    ChatContext currentContext = getAssistantState().getCurrentContext();
    ChatContext targetContext = new ChatContext(currentContext.getProfile(), model, currentContext.getAction(), currentContext.isInteractive());
    ChatContextEvent event = new ChatContextEvent(currentContext, targetContext, null, false);
    processContextEvent(event);
  }

  public void selectAction(PromptAction action) {
    // preserve profile and model from the current context
    ChatContext currentContext = getAssistantState().getCurrentContext();
    ChatContext targetContext = new ChatContext(currentContext.getProfile(), currentContext.getModel(), action, currentContext.isInteractive());
    ChatContextEvent event = new ChatContextEvent(currentContext, targetContext, null, false);
    processContextEvent(event);
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
  private void createChatPanel() {
    chatScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    messageContainer = new RollingMessageContainer(AssistantState.MAX_CHAR_MESSAGE_COUNT, chatPanel);
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
    AssistantState state = getAssistantState();
    ChatAvailability availability = state.getChatAvailability();
    if (availability != ChatAvailability.AVAILABLE) return;

    ConnectionId connectionId = getConnectionId();
    DatabaseAssistantManager manager = getManager();

    DBAIProfile profile = manager.getSelectedProfile(connectionId);
    AIModel model = manager.getSelectedModel(connectionId);
    if (profile == null) return;
    if (model == null) return;

    String prompt = getInputField().getAndClearText();
    question = nvl(question, prompt);
    if (Strings.isEmptyOrSpaces(question)) return;

    state.set(QUERYING, true);

    PromptAction actionType = state.getSelectedAction();

    ChatContext context = new ChatContext(profile.getName(), model, actionType, false);
    PersistentChatMessage inputChatMessage = new PersistentChatMessage(MessageType.NEUTRAL, question, AuthorType.USER, context);
    inputChatMessage.setProgress(true);


    String chatId = state.getCurrentChatId();
    appendMessage(chatId, inputChatMessage);

    if (actionType == PromptAction.CHAT) {
      question = question + " (please properly demarcate code-blocks in the output, and qualify them with the programming-language identifier)";
    }

    try {
      String response = manager.query(connectionId, question, context);
      state.set(QUERYING, false);
      PersistentChatMessage outPutChatMessage = new PersistentChatMessage(MessageType.NEUTRAL, response, AuthorType.AGENT, context);
      appendMessage(chatId, outPutChatMessage);
      log.info("AI Query processed successfully.");
    } catch (Exception e) {
      state.set(QUERYING, false);
      log.warn("Error processing AI query", e);
      String message = manager.getPresentableMessage(connectionId, profile.getProvider(), e);
      PersistentChatMessage errorMessage = new PersistentChatMessage(MessageType.ERROR, message, AuthorType.SYSTEM, context);
      appendMessage(chatId, errorMessage);
    }
  }


  public void reloadProfiles() {
    Background.run(() -> doLoadProfiles(true));
  }
  public void loadProfiles() {
    Background.run(() -> doLoadProfiles(false));
  }

  /**
   * Initializes the profile dropdowns for the chat box
   */
  private void doLoadProfiles(boolean force) {
    if (getAssistantState().is(INITIALIZING)) return;
    try {
      if (force) refreshUserObjects(getConnectionId(), AI_PROFILE);
      beforeProfileLoad();
      DatabaseAssistantManager manager = getManager();
      // make sure profiles are loaded
      manager.getProfiles(getConnectionId());
      afterProfileLoad(null);
    } catch (Throwable e){
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
    if (e != null) {
      state.set(UNAVAILABLE, true);
      showErrorHeader(e);
    } else {
      initCurrentChat();
      getInputField().requestFocus();
    }

    updateActionToolbars();
  }

  private void initCurrentChat() {
    AssistantState state = getAssistantState();
    if (!state.isCurrentContextValid()) {
      DBAIProfile firstProfile = firstElement(getProfiles());

      ChatContext context = new ChatContext(firstProfile);
      state.setCurrentContext(context);
    }
  }

  public ChatBoxInputField getInputField() {
    return nd(inputField);
  }

  private void showErrorHeader(Throwable cause) {
    // TODO show error bar (similar to editor error headers)
  }

  private void appendMessage(String chatId, PersistentChatMessage message) {
    AssistantState state = getAssistantState();
    Chat chat = state.getChat(chatId);
    if (chat == null) return; // chat already discarded by the time of message arrival

    chat.addMessage(message);
    String currentChatId = state.getCurrentChatId();
    if (Objects.equals(chatId, currentChatId)) {
      // update UI only if chat is still current
      dispatch(() -> messageContainer.addAll(List.of(message), this));
      dispatch(() -> scrollDown());
      updateActionToolbars();
    }
  }



  private void scrollDown() {
    chatScrollPane.validate();
    JScrollBar verticalBar = chatScrollPane.getVerticalScrollBar();
    verticalBar.setValue(verticalBar.getMaximum());
  }

  public void interruptAssistantSession() {
    getManager().interruptAssistantSession(getConnection());
  }

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
}
