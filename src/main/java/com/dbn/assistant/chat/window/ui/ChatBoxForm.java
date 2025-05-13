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
import com.dbn.assistant.chat.ChatConversation;
import com.dbn.assistant.chat.PersistentChatConversation;
import com.dbn.assistant.chat.message.AuthorType;
import com.dbn.assistant.chat.ChatContext;
import com.dbn.assistant.chat.message.PersistentChatMessage;
import com.dbn.assistant.chat.ui.ChatStatusLabel;
import com.dbn.assistant.chat.window.ContextChangeEvent;
import com.dbn.assistant.chat.window.PromptAction;
import com.dbn.assistant.chat.window.util.RollingMessageContainer;
import com.dbn.assistant.init.ui.AssistantIntroductionForm;
import com.dbn.assistant.provider.AIModel;
import com.dbn.assistant.state.AssistantState;
import com.dbn.common.action.DataKeys;
import com.dbn.common.feature.FeatureAvailability;
import com.dbn.common.message.MessageType;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Progress;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Messages;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionRef;
import com.dbn.object.DBAIProfile;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.application.ApplicationManager;
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.assistant.state.AssistantStatus.INITIALIZING;
import static com.dbn.assistant.state.AssistantStatus.QUERYING;
import static com.dbn.assistant.state.AssistantStatus.UNAVAILABLE;
import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.feature.FeatureAcknowledgement.ENGAGED;
import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.common.util.Commons.nvl;
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

    statusLabel.update(getLabelStatus());
    chatStatusPanel.add(statusLabel, BorderLayout.CENTER);
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

  public int getLabelStatus() {
    ChatConversation chatConversation = getAssistantState().getCurrentConversation();
    if(!chatConversation.isInteractive()) return 1;
    if(!chatConversation.getContext().isActive()) return 2;
    else return 0;
  }
  private void createInputField() {
    inputField = new ChatBoxInputField(this);
    inputFieldPanel.add(inputField, BorderLayout.CENTER);
  }

  public void initMessages() {
    ChatConversation chatConversation = getAssistantState().getCurrentConversation();
    List<PersistentChatMessage> messages = chatConversation.getMessages();
    messages.forEach(message -> message.setProgress(false));
    updateMessages(messages);
  }

  private void updateMessages(List<PersistentChatMessage> messages) {
    messageContainer.clear();
    messageContainer.addAll(messages, this);
    statusLabel.update(getLabelStatus());
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

  public List<PersistentChatConversation> getConversations() {
    return new ArrayList<>(getAssistantState().getConversations().values());
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
    ChatContext oldContext = getAssistantState().getChatContext();
    ChatContext newContext = new ChatContext(profile.getName(), profile.getModel(), getAssistantState().getSelectedAction(), profile.isInteractive());
    ContextChangeEvent contextChangeEvent = new ContextChangeEvent(oldContext, newContext, null, false, this);
    contextChangeEvent.trigger();
  }

  public void selectModel(AIModel model) {
    ChatContext oldContext = getAssistantState().getChatContext();
    ChatContext newContext = new ChatContext(oldContext.getProfile(), model, getAssistantState().getSelectedAction(), oldContext.isInteractive());
    ContextChangeEvent contextChangeEvent = new ContextChangeEvent(oldContext, newContext, null, false, this);
    contextChangeEvent.trigger();
  }

  public void selectAction(PromptAction action) {
    ChatContext oldContext = getAssistantState().getChatContext();
    ChatContext newContext = new ChatContext(oldContext.getProfile(), oldContext.getModel(), action, oldContext.isInteractive());
    ContextChangeEvent contextChangeEvent = new ContextChangeEvent(oldContext, newContext, null, false, this);
    contextChangeEvent.trigger();
  }



  public void showConversation(PersistentChatConversation conversation) {
    getAssistantState().setAvailability(FeatureAvailability.UNAVAILABLE);
    updateMessages(conversation.getMessages());
  }

  public void clearConversation() {
    messageContainer.clear();
    getAssistantState().clearMessages();
  }

  /**
   * Initializes the panel to display messages
   */
  private void configureConversationPanel() {
    chatScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    messageContainer = new RollingMessageContainer(AssistantState.MAX_CHAR_MESSAGE_COUNT, chatPanel);
  }

  public boolean isPromptingAvailable() {
    ConnectionId connectionId = getConnectionId();
    DatabaseAssistantManager manager = getManager();
    return manager.isPromptingAvailable(connectionId);
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
      question = question + " (please triple-quote all code-contents in your output, and qualify them with the programming-language identifier)";
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
    }
    DBAIProfile profile = getSelectedProfile();
    if(state.getCurrentConversation().getContext().getModel() == null && profile != null) {
      String unwantedConvId = state.getCurrentConversation().getId();
      state.setCurrentConversation(new ChatContext(profile.getName(), getSelectedModel(), state.getSelectedAction(), profile.isInteractive()));
      state.getConversations().remove(unwantedConvId);
    }

    getInputField().requestFocus();
    updateActionToolbars();
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

  public void updateActionToolbars(){
      dispatch(super::updateActionToolbars);
  }

  public void interruptCurrentConversation() {
      Progress.modal(getProject(), null, true,
              "Interrupting current conversation",
              "Interrupting current conversation",
              indicator -> {
                  try {
                    getManager().interruptAssistantConnection(getConnection());
                  } catch (SQLException e) {
                    Messages.showErrorDialog(getProject(), "Error while interrupting current conversation");
                  }
              });
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
