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
import com.dbn.assistant.chat.ChatContext;
import com.dbn.assistant.chat.ChatContextEvent;
import com.dbn.assistant.chat.ChatConversation;
import com.dbn.assistant.chat.ChatInterruptionReason;
import com.dbn.assistant.chat.message.AuthorType;
import com.dbn.assistant.chat.message.PersistentChatMessage;
import com.dbn.assistant.chat.ui.ChatStatusLabel;
import com.dbn.assistant.chat.ui.SaveOrDiscardConversationDialog;
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
  private JPanel chatActionsPanel;
  private JPanel introPanel;
  private JPanel chatBoxPanel;
  private JPanel initializingIconPanel;
  private JPanel initializingPanel;
  private JPanel helpActionPanel;
  private JPanel chatStatusPanel;
  public ChatStatusLabel statusLabel = new ChatStatusLabel();

  private RollingMessageContainer messageContainer;
  private final ConnectionRef connection;
  private ChatBoxInputField inputField;

  public ChatBoxForm(ConnectionHandler connection) {
    super(connection, connection.getProject());
    this.connection = connection.ref();

    initializingIconPanel.add(new AsyncProcessIcon("Loading"), BorderLayout.CENTER);

    // hide all panels until availability status is known
    this.introPanel.setVisible(false);
    this.chatBoxPanel.setVisible(false);
    this.initializingPanel.setVisible(false);

    chatStatusPanel.add(statusLabel, BorderLayout.CENTER);
    initHeaderForm();
    initIntroForm();
    initChatBoxForm();
    updateStatusLabel();
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
    createInputField();
    configureConversationPanel();
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
    this.profileActionsPanel.add(profileActions.getComponent(), BorderLayout.CENTER);

/*
    ActionToolbar helpActions = Actions.createActionToolbar(helpActionPanel, "DBNavigator.ActionGroup.AssistantChatBoxHelp", "", true);
    this.helpActionPanel.add(helpActions.getComponent(), BorderLayout.CENTER);
*/

    ActionToolbar typeActions = Actions.createActionToolbar(typeActionsPanel, true, "DBNavigator.ActionGroup.AssistantChatBoxTypes");
    setAccessibleName(profileActions, txt("app.assistant.aria.ChatTypeActions"));
    this.typeActionsPanel.add(typeActions.getComponent(), BorderLayout.CENTER);

    ActionToolbar chatActions = Actions.createActionToolbar(chatActionsPanel, true, "DBNavigator.ActionGroup.AssistantChatBoxPrompt");
    setAccessibleName(profileActions, txt("app.assistant.aria.ChatActions"));
    this.chatActionsPanel.add(chatActions.getComponent(), BorderLayout.CENTER);
  }

  private void updateStatusLabel() {
    statusLabel.update(getCurrentConversation());
  }

  private void createInputField() {
    inputField = new ChatBoxInputField(this);
    inputFieldPanel.add(inputField, BorderLayout.CENTER);
  }

  public void initMessages() {
    ChatConversation conversation = getCurrentConversation();
    conversation.removeProgress();

    messageContainer.clear();
    messageContainer.addAll(conversation.getMessages(), this);
    updateStatusLabel();
    dispatch(() -> scrollConversationDown());
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
    ChatContext oldContext = getAssistantState().getCurrentContext();
    ChatContext newContext = new ChatContext(profile.getName(), profile.getModel(), getAssistantState().getSelectedAction(), profile.isInteractive());
    ChatContextEvent event = new ChatContextEvent(oldContext, newContext, null, false);
    processContextEvent(event);
  }

  public void selectModel(AIModel model) {
    ChatContext oldContext = getAssistantState().getCurrentContext();
    ChatContext newContext = new ChatContext(oldContext.getProfile(), model, getAssistantState().getSelectedAction(), oldContext.isInteractive());
    ChatContextEvent event = new ChatContextEvent(oldContext, newContext, null, false);
    processContextEvent(event);
  }

  public void selectAction(PromptAction action) {
    ChatContext oldContext = getAssistantState().getCurrentContext();
    ChatContext newContext = new ChatContext(oldContext.getProfile(), oldContext.getModel(), action, oldContext.isInteractive());
    ChatContextEvent event = new ChatContextEvent(oldContext, newContext, null, false);
    processContextEvent(event);
  }

  private void startConversation(ChatContext chatContext) {
    AssistantState state = getAssistantState();
    state.createConversation(chatContext);
    state.deleteObsoleteConversations();

    initMessages();
  }

  private void selectConversation(String conversationId) {
    AssistantState state = getAssistantState();
    state.setCurrentConversationId(conversationId);
    state.deleteObsoleteConversations();

    initMessages();
  }

  private void saveCurrentConversation(String title) {
    ChatConversation conversation = getCurrentConversation();
    conversation.removeProgress();
    conversation.setTitle(title);

    // deactivate interactive conversations on save
    conversation.setActive(!conversation.isInteractive());
  }

  private void deleteCurrentConversation() {
    AssistantState assistantState = getAssistantState();
    deleteConversation(assistantState.getCurrentConversationId());
  }

  private void deleteConversation(String conversationId) {
    AssistantState assistantState = getAssistantState();
    assistantState.deleteConversation(conversationId);
  }

  public void performContextSwitch(ChatContextEvent event) {
    ChatContext targetContext = event.getTargetContext();
    String targetConversationId = event.getTargetConversationId();

    if (event.isNewConversationRequest()) {
      startConversation(targetContext);

    } else if (targetConversationId != null) {
      selectConversation(targetConversationId);

    } else {
      changeConversationContext(targetContext);
    }
  }

  private void changeConversationContext(ChatContext context) {
    getAssistantState().setCurrentContext(context);
    updateActionToolbars();
    updateStatusLabel();
  }

  public void cancelContextSwitch() {
    // no changes, just refresh actions
    updateActionToolbars();
  }

  public void clearConversation() {
    // TODO delete conversation vs clear conversation
    messageContainer.clear();
    getAssistantState().clearMessages();
  }

  public void processContextEvent(ChatContextEvent event) {
    AssistantState state = getAssistantState();
    ChatInterruptionReason interruptionReason = event.evaluateInterruption(state);

    if (interruptionReason == null) {
      performContextSwitch(event);
      return;
    }

    // indirect start of a new conversation due to interrupted state
    if (!event.isConversationOpenRequest()) {
      event.setNewConversationRequest(true);
    }

    ChatConversation conversation = getCurrentConversation();
    if (conversation.isPersisted() && !conversation.isInteractive()) {
      // non-interactive conversation, already persisted
      performContextSwitch(event);
      return;
    }

    Dialogs.show(() -> new SaveOrDiscardConversationDialog(
                    ensureProject(),
                    interruptionReason,
                    state.getConversationTitles()),
            (dialog, exitCode) -> handleDialogResult(event, dialog.getConversationTitle(), exitCode));
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
        deleteCurrentConversation();
        performContextSwitch(event);
        break;
      }
      case 2: {
        // Save was selected
        saveCurrentConversation(title);
        performContextSwitch(event);
      }
    }
  }

  /**
   * Initializes the panel to display messages
   */
  private void configureConversationPanel() {
    chatScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    messageContainer = new RollingMessageContainer(AssistantState.MAX_CHAR_MESSAGE_COUNT, chatPanel);
  }

  public boolean isPromptingAvailable() {
    return getAssistantState().isPromptingAvailable();
  }

  public ChatConversation getCurrentConversation() {
    return getAssistantState().getCurrentConversation();
  }


  public void submitPrompt() {
    submitPrompt(null);
  }

  public void submitPrompt(String question) {
    Background.run(() -> processQuery(question));
  }

  private void processQuery(String question) {
    if (!isPromptingAvailable()) return;

    ConnectionId connectionId = getConnectionId();
    DatabaseAssistantManager manager = getManager();

    DBAIProfile profile = manager.getSelectedProfile(connectionId);
    AIModel model = manager.getSelectedModel(connectionId);
    if (profile == null) return;
    if (model == null) return;

    String prompt = getInputField().getAndClearText();
    question = nvl(question, prompt);
    if (Strings.isEmptyOrSpaces(question)) return;

    AssistantState state = getAssistantState();
    state.set(QUERYING, true);

    PromptAction actionType = state.getSelectedAction();

    ChatContext context = new ChatContext(profile.getName(), model, actionType, false);
    PersistentChatMessage inputChatMessage = new PersistentChatMessage(MessageType.NEUTRAL, question, AuthorType.USER, context);
    inputChatMessage.setProgress(true);
    appendMessage(inputChatMessage);

    if (actionType == PromptAction.CHAT) {
      question = question + " (please properly demarcate code-blocks in the output, and qualify them with the programming-language identifier)";
    }

    try {
      String response = manager.query(connectionId, question, context);
      state.set(QUERYING, false);
      PersistentChatMessage outPutChatMessage = new PersistentChatMessage(MessageType.NEUTRAL, response, AuthorType.AGENT, context);
      appendMessage(outPutChatMessage);
      log.info("AI Query processed successfully.");
    } catch (Exception e) {
      state.set(QUERYING, false);
      log.warn("Error processing AI query", e);
      String message = manager.getPresentableMessage(connectionId, profile.getProvider(), e);
      PersistentChatMessage errorMessage = new PersistentChatMessage(MessageType.ERROR, message, AuthorType.SYSTEM, context);
      appendMessage(errorMessage);
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
      initCurrentConversation();
      getInputField().requestFocus();
    }

    updateActionToolbars();
  }

  private void initCurrentConversation() {
    ChatConversation conversation = getCurrentConversation();
    ChatContext context = conversation.getContext();
    if (!context.isInitialized()) {
      DBAIProfile firstProfile = firstElement(getProfiles());
      context.initialize(firstProfile);
    }
  }

  public ChatBoxInputField getInputField() {
    return nd(inputField);
  }

  private void showErrorHeader(Throwable cause) {
    // TODO show error bar (similar to editor error headers)
  }

  private void appendMessage(PersistentChatMessage message) {
    List<PersistentChatMessage> messages = List.of(message);
    getAssistantState().addMessages(messages);
    dispatch(() -> messageContainer.addAll(messages, this));
    dispatch(() -> scrollConversationDown());
    updateActionToolbars();
  }



  private void scrollConversationDown() {
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
