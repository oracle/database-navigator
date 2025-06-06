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

package com.dbn.assistant.state;

import com.dbn.assistant.DatabaseAssistantManager;
import com.dbn.assistant.DatabaseAssistantType;
import com.dbn.assistant.chat.Chat;
import com.dbn.assistant.chat.ChatAvailability;
import com.dbn.assistant.chat.ChatContext;
import com.dbn.assistant.chat.message.PersistentChatMessage;
import com.dbn.assistant.chat.window.PromptAction;
import com.dbn.common.feature.FeatureAcknowledgement;
import com.dbn.common.feature.FeatureAvailability;
import com.dbn.common.property.PropertyHolderBase;
import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Lists;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.object.DBAIProfile;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dbn.assistant.chat.ChatAvailability.AVAILABLE;
import static com.dbn.assistant.chat.ChatAvailability.BUSY_INITIALIZING;
import static com.dbn.assistant.chat.ChatAvailability.BUSY_QUERYING;
import static com.dbn.assistant.chat.ChatAvailability.DISABLED_PROFILE_SELECTED;
import static com.dbn.assistant.chat.ChatAvailability.INACTIVE_CHAT_SELECTED;
import static com.dbn.assistant.chat.ChatAvailability.NOT_INITIALIZED;
import static com.dbn.assistant.chat.ChatAvailability.NOT_SUPPORTED;
import static com.dbn.assistant.chat.ChatAvailability.NO_PROFILE_AVAILABLE;
import static com.dbn.assistant.chat.ChatAvailability.NO_PROFILE_SELECTED;
import static com.dbn.assistant.state.AssistantStatus.INITIALIZING;
import static com.dbn.assistant.state.AssistantStatus.QUERYING;
import static com.dbn.assistant.state.AssistantStatus.UNAVAILABLE;
import static com.dbn.assistant.state.AssistantStatus.VALUES;
import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.connectionIdAttribute;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.common.util.Strings.isNotEmpty;

/**
 * Assistant state holder
 * This class represents the state of the DB Assistant for a given connection, as well as the chat-box state.
 * It encapsulates the current profiles, selected profile,
 * a history of questions, the AI answers, and the current connection.
 *
 *
 * @author Ayoub Aarrasse (Oracle)
 * @author Emmanuel Jannetti (Oracle)
 */
@Setter
@Getter
@NoArgsConstructor
public class AssistantState extends PropertyHolderBase.IntStore<AssistantStatus> implements PersistentStateElement {

  private FeatureAvailability availability = FeatureAvailability.UNCERTAIN;
  private FeatureAcknowledgement acknowledgement = FeatureAcknowledgement.NONE;
  private DatabaseAssistantType assistantType = DatabaseAssistantType.GENERIC;

  private ConnectionId connectionId;
  private Map<String, Chat> chats = new LinkedHashMap<>();

  private String currentChatId;
  private String currentSessionSignature; // the resourceId of the com.dbn.connection.jdbc.Resource
  private String defaultProfileName;

  public static final short MAX_CHAR_MESSAGE_COUNT = 100;

  public AssistantState(ConnectionId connectionId) {
    this.connectionId = connectionId;
  }

  @Override
  protected AssistantStatus[] properties() {
    return VALUES;
  }

  public String getAssistantName() {
    switch (assistantType) {
      case SELECT_AI: return txt("app.assistant.title.DatabaseAssistantName_SELECT_AI");
      case GENERIC:
      default: return txt("app.assistant.title.DatabaseAssistantName_GENERIC");
    }
  }

  public Chat getChat(String chatId) {
    return chats.get(chatId);
  }

  public Set<String> getChatNames() {
    return chats.
            values().
            stream().
            map(c -> c.getTitle()).
            filter(t -> isNotEmpty(t)).
            collect(Collectors.toSet());
  }

  public List<Chat> getSavedChats() {
    return chats.
            values().
            stream().
            filter(c -> c.isPersisted()).
            collect(Collectors.toList());
  }

  public Chat createChat(ChatContext chatContext) {
    Chat conversation = new Chat(chatContext);
    conversation.setSessionSignature(currentSessionSignature);
    String conversationId = conversation.getId();

    chats.put(conversationId, conversation);
    setCurrentChatId(conversationId);
    return conversation;
  }

  public void deleteChat(String conversationId){
    chats.remove(conversationId);
  }

  public void deleteChats(List<String> conversationIds){
    conversationIds.forEach(id -> deleteChat(id));
  }

  public void deleteObsoleteChats() {
    chats.keySet().removeIf(id -> isObsoleteConversation(id));
  }

  public boolean isCurrentConversation(Chat conversation) {
    return Objects.equals(currentChatId, conversation.getId());
  }

  private boolean isObsoleteConversation(String conversationId) {
    Chat conversation = getChat(conversationId);
    if (conversation.isPersisted()) return false;
    if (isCurrentConversation(conversation)) return false;
    if (conversation.isEmpty()) return true; // empty unsaved conversations are
    return false;
  }

  public synchronized Chat getCurrentChat() {
    Chat currentConversation = chats.get(currentChatId);
    if (currentConversation == null) {
      currentConversation = new Chat();
      currentConversation.setSessionSignature(currentSessionSignature);
      currentChatId = currentConversation.getId();
      chats.put(currentChatId, currentConversation);
    }
    return currentConversation;
  }

  public ChatContext getCurrentContext() {
    return getCurrentChat().getContext();
  }

  public void setCurrentContext(ChatContext context) {
    getCurrentChat().setContext(context);
  }

  public void setCurrentSessionSignature(String currentSessionSignature) {
    this.currentSessionSignature = currentSessionSignature;
    Chat conversation = getCurrentChat();

    if (!conversation.isSigned() || !conversation.isInteractive() || conversation.isEmpty() || conversation.isErrorsOnly()) {
      // safe to update conversation signature
      conversation.setSessionSignature(currentSessionSignature);
    }
  }

  public boolean isSupported() {
    return availability == FeatureAvailability.AVAILABLE;
  }

  /**
   * State utility indicating the feature is initialized and ready to use
   * @return true if the chat box is properly initialized and can be interacted with
   */
  public boolean isAvailable() {
    return isSupported() &&
            isNot(INITIALIZING) &&
            isNot(UNAVAILABLE) &&
            isNot(QUERYING);
  }

  public ChatAvailability getChatAvailability() {
    if (!isSupported()) return NOT_SUPPORTED;

    if (is(UNAVAILABLE)) return NOT_INITIALIZED;
    if (is(INITIALIZING)) return BUSY_INITIALIZING;
    if (is(QUERYING)) return BUSY_QUERYING;

    if (!isCurrentChatActive()) return INACTIVE_CHAT_SELECTED;
    if (getProfiles().isEmpty()) return NO_PROFILE_AVAILABLE;

    DBAIProfile selectedProfile = getSelectedProfile();
    if (selectedProfile == null) return NO_PROFILE_SELECTED;
    if (!selectedProfile.isEnabled()) return DISABLED_PROFILE_SELECTED;

    return AVAILABLE;
  }

  public boolean isCurrentChatInteractive() {
    Chat conversation = getCurrentChat();
    return conversation.isInteractive();
  }

  public boolean isCurrentChatActive() {
    Chat conversation = getCurrentChat();
    if (!conversation.isInteractive()) return true; // non-interactive chats are always active if selected

    String sessionSignature = conversation.getSessionSignature();
    if (isEmpty(sessionSignature) && isEmpty(currentSessionSignature)) return conversation.isEmpty(); // empty chat not yet started or initialized (assume active)

    return Objects.equals(sessionSignature, currentSessionSignature);
  }

  public boolean isCurrentContextEnabled() {
    DBAIProfile profile = getSelectedProfile();
    return profile != null && profile.isEnabled();
  }

  public boolean isCurrentContextValid() {
    ChatContext context = getCurrentContext();
    String profileName = context.getProfileName();
    String modelName = context.getModelName();

    if (isEmpty(profileName)) return false;
    if (isEmpty(modelName)) return false;

    DBAIProfile profile = getProfile(profileName);
    if (profile == null) return false;
    if (profile.getProvider().getModel(modelName) == null) return false;
    if (profile.isInteractive() != context.isInteractive()) return false;

    return true;
  }

  private DBAIProfile getSelectedProfile() {
    ChatContext context = getCurrentContext();
    String profileName = context.getProfileName();
    return getProfile(profileName);
  }

  public void setDefaultProfile(@Nullable DBAIProfile profile) {
    setDefaultProfileName(profile == null ? null : profile.getName());
  }

  @Nullable
  public String getSelectedProfileName() {
    ChatContext context = getCurrentContext();
    return context.getProfileName();
  }

  @Nullable
  public String getSelectedModelName() {
    ChatContext context = getCurrentContext();
    return context.getModelName();
  }

  public PromptAction getSelectedAction() {
    return getCurrentChat().getContext().getAction();
  }

  public DBAIProfile getProfile(String name) {
    if (isEmpty(name)) return null;
    List<DBAIProfile> profiles = getProfiles();
    return Lists.first(profiles, p -> p.getName().equals(name));
  }

  public List<DBAIProfile> getProfiles() {
    ConnectionHandler connection = ConnectionHandler.get(connectionId);
    if (connection == null) return Collections.emptyList();

    Project project = connection.getProject();
    DatabaseAssistantManager manager = DatabaseAssistantManager.getInstance(project);
    return manager.getProfiles(connectionId);
  }


  @Override
  public void readState(Element element) {
    connectionId = connectionIdAttribute(element, "connection-id");
    defaultProfileName = stringAttribute(element, "default-profile-name");
    currentChatId = stringAttribute(element, "selected-conversation-id");
    assistantType = enumAttribute(element, "assistant-type", assistantType);
    availability = enumAttribute(element, "availability", availability);
    acknowledgement = enumAttribute(element, "acknowledgement", acknowledgement);

    Element conversationsElement = element.getChild("conversations");
    List<Element> conversationElements = childrenOf(conversationsElement);
    for (Element conversationElement : conversationElements) {
      Chat conversation = new Chat();
      conversation.readState(conversationElement);
      chats.put(conversation.getId(), conversation);
    }

    //TODO to be removed later
    Element messagesElement = element.getChild("messages");
    List<Element> messageElements = childrenOf(messagesElement);
    String selectedProfileName = stringAttribute(element, "selected-profile-name");
    String selectedModelName = stringAttribute(element, "selected-model-name");
    PromptAction selectedAction = enumAttribute(element, "selected-action", PromptAction.class);
    if(selectedProfileName != null) {
      ChatContext context = new ChatContext(selectedProfileName, selectedModelName, selectedAction, false);
      Chat currentConversation = createChat(context);
      for (Element messageElement : messageElements) {
        PersistentChatMessage message = new PersistentChatMessage();
        message.readState(messageElement);
        currentConversation.addMessage(message);
      }
    }
  }

  @Override
  public void writeState(Element element) {
    setStringAttribute(element, "connection-id", connectionId.id());
    setStringAttribute(element, "default-profile-name", defaultProfileName);
    setStringAttribute(element, "selected-conversation-id", currentChatId);
    setEnumAttribute(element, "assistant-type", assistantType);
    setEnumAttribute(element, "availability", availability);
    setEnumAttribute(element, "acknowledgement", acknowledgement);

    Element conversationsElement = newElement(element, "conversations");
    for (Chat conversation : chats.values()) {
      if (isObsoleteConversation(conversation.getId())) continue;

      Element conversationElement = newElement(conversationsElement, "conversation");
      conversation.writeState(conversationElement);
    }
  }

}
